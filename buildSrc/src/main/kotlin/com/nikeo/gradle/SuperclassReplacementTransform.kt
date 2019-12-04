package com.nikeo.gradle

import com.android.SdkConstants
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project
import org.objectweb.asm.*
import java.io.File
import java.io.FileOutputStream


internal class SuperclassReplacementTransform(
    private val project: Project,
    private val replacement: SuperclassReplacement
) :
    Transform() {
    override fun getName(): String = "superclassReplacement"

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun isIncremental(): Boolean = false

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun transform(invocation: TransformInvocation?) {
        if (invocation == null) {
            return
        }
        replacement.qualifiedNameReplacements.get().forEach { (source, replace) ->
            project.logger.info("$source's super class will replace to $replace by SuperclassReplacementTransform")
        }
        val inputs = invocation.inputs
        val outputProvider = invocation.outputProvider

        inputs.forEach { input ->
            val jarInputs = input.jarInputs
            val directoryInputs = input.directoryInputs

            jarInputs.forEach {
                var jarName = it.name
                val md5Name = DigestUtils.md5Hex(it.file.absolutePath)
                if (jarName.endsWith(SdkConstants.DOT_JAR)) {
                    jarName = jarName.substring(0, jarName.length - 4)
                }
                val dest = outputProvider.getContentLocation(
                    jarName + md5Name,
                    it.contentTypes,
                    it.scopes,
                    Format.JAR
                )
                FileUtils.copyFile(it.file, dest)
            }

            directoryInputs.forEach {
                childFileOf(it.file) { classFile ->
                    replacement.qualifiedNameReplacements.orNull
                        ?.filterKeys { source ->
                            val sourceClassFilePath =
                                source.replace(".", "/") + ".class"
                            classFile.path.contains(sourceClassFilePath)
                        }
                        ?.takeIf(Map<String, String>::isNotEmpty)
                        ?.values
                        ?.first()
                        ?.let { replacement ->
                            val classReader = ClassReader(classFile.inputStream())
                            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)

                            classReader.accept(
                                MyClassVisitor(classWriter, replacement.replace(".", "/")),
                                ClassReader.EXPAND_FRAMES
                            )
                            val byteArr = classWriter.toByteArray()
                            val fileOutputStream = FileOutputStream(classFile)
                            fileOutputStream.write(byteArr)
                            fileOutputStream.flush()
                            fileOutputStream.close()
                        }
                }

                val dest = outputProvider.getContentLocation(
                    it.name,
                    it.contentTypes,
                    it.scopes,
                    Format.DIRECTORY
                )

                FileUtils.copyDirectory(it.file, dest)
            }
        }
    }

    private class MyClassVisitor(classWriter: ClassWriter, private val superClassName: String) :
        ClassVisitor(Opcodes.ASM6, classWriter) {
        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            super.visit(version, access, name, signature, superClassName, interfaces)
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            desc: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, name, desc, signature, exceptions)
            if (mv != null) {
                return object : MethodVisitor(Opcodes.ASM5, mv) {
                    override fun visitMethodInsn(
                        opcode: Int,
                        owner: String?,
                        name: String,
                        desc: String?,
                        itf: Boolean
                    ) { // 调用父类的构造函数时
                        super.visitMethodInsn(
                            opcode,
                            if (opcode == Opcodes.INVOKESPECIAL && name == "<init>") {
                                superClassName
                            } else {
                                owner
                            },
                            name,
                            desc,
                            itf
                        ) // 改写父类为 superClassName
                    }
                }
            }
            return mv
        }
    }

    private fun childFileOf(file: File, consumer: (File) -> Unit) {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                childFileOf(it, consumer)
            }
        } else if (file.isFile) {
            consumer(file)
        }
    }
}