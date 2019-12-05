package com.nikeo.gradle

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.objectweb.asm.*
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry


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
        invocation.outputProvider.deleteAll()
        replacement.qualifiedNameReplacements.get().forEach { (source, replace) ->
            project.logInfo("$source's super class will replace to $replace by SuperclassReplacementTransform")
        }
        val inputs = invocation.inputs
        val outputProvider = invocation.outputProvider

        inputs.forEach { input ->
            val jarInputs = input.jarInputs
            val directoryInputs = input.directoryInputs

            jarInputs.forEach {
                handJarInput(it, outputProvider)
            }

            directoryInputs.forEach {
                handDirectoryInput(it, outputProvider)
            }
        }
    }

    private fun handJarInput(jarInput: JarInput, outputProvider: TransformOutputProvider) {
        if (jarInput.file.absolutePath.endsWith(SdkConstants.DOT_JAR)) {
            var jarName = jarInput.name
            val md5Name = DigestUtils.md5Hex(jarInput.file.absolutePath)
            if (jarName.endsWith(SdkConstants.DOT_JAR)) {
                jarName = jarName.substring(0, jarName.length - 4)
            }

            val tmpFile = File(jarInput.file.parent + File.separator + "classes_temp.jar")
            if (tmpFile.exists()) {
                tmpFile.delete()
            }

            val jarFile = JarFile(jarInput.file)
            val enumeration = jarFile.entries()

            val jarOutputStream = JarOutputStream(FileOutputStream(tmpFile))

            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement() as JarEntry
                val entryName = jarEntry.name
                val zipEntry = ZipEntry(entryName)
                val inputStream = jarFile.getInputStream(zipEntry)

                replacement.qualifiedNameReplacements.orNull?.forEach { source, replacement ->
                    if (entryName == source.replace(".", File.separator) + SdkConstants.DOT_CLASS) {
                        project.logInfo("start replace $source'superclass to $replacement")
                        jarOutputStream.putNextEntry(zipEntry)

                        val classReader = ClassReader(IOUtils.toByteArray(inputStream))
                        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)

                        classReader.accept(
                            MyClassVisitor(classWriter, replacement.replace(".", File.separator)),
                            ClassReader.EXPAND_FRAMES
                        )
                        val byteArr = classWriter.toByteArray()

                        jarOutputStream.write(byteArr)
                    } else {
                        jarOutputStream.putNextEntry(zipEntry)
                        jarOutputStream.write(IOUtils.toByteArray(inputStream))
                    }
                } ?: run {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }

                jarOutputStream.closeEntry()
            }
            //结束
            jarOutputStream.close()
            jarFile.close()

            val dest = outputProvider.getContentLocation(
                jarName + md5Name,
                jarInput.contentTypes,
                jarInput.scopes,
                Format.JAR
            )
            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        }

    }

    private fun handDirectoryInput(
        directoryInput: DirectoryInput,
        outputProvider: TransformOutputProvider
    ) {
        val file = directoryInput.file
        if (file.isDirectory) {
            childFileOf(file) { classFile ->
                replacement.qualifiedNameReplacements.orNull?.forEach { source, replacement ->
                    if (classFile.path.contains(
                            source.replace(
                                ".",
                                File.separator
                            ) + SdkConstants.DOT_CLASS
                        )
                    ) {
                        project.logInfo("start replace ${classFile.path}'superclass to $replacement")
                        val classReader = ClassReader(classFile.inputStream())
                        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)

                        classReader.accept(
                            MyClassVisitor(classWriter, replacement.replace(".", File.separator)),
                            ClassReader.EXPAND_FRAMES
                        )
                        val byteArr = classWriter.toByteArray()
                        val fileOutputStream =
                            FileOutputStream(classFile.parentFile.absolutePath + File.separator + classFile.name)
                        fileOutputStream.write(byteArr)
                        fileOutputStream.flush()
                        fileOutputStream.close()
                    }
                }
            }
        }
        val dest = outputProvider.getContentLocation(
            directoryInput.name,
            directoryInput.contentTypes,
            directoryInput.scopes,
            Format.DIRECTORY
        )

        FileUtils.copyDirectory(file, dest)
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