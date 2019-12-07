@file:JvmMultifileClass

package com.nikeo.gradle

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.nikeo.anx.files.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findPlugin
import org.gradle.kotlin.dsl.property
import org.objectweb.asm.*
import java.io.File
import java.io.FileOutputStream
import com.nikeo.anx.jars.modifyJar
import com.nikeo.anx.files.eachFileRecurse


class LollipopCrashWebViewFixingPlugin : Plugin<Project> {

    override fun apply(project: Project) = project.run {
        val appPlugin = plugins.findPlugin(AppPlugin::class)
        requireNotNull(appPlugin) {
            "LollipopCrashWebViewFixingPlugin must apply to app"
        }
        val crashWebViews = extensions.create(
            "crashWebViews",
            CrashWebViews::class,
            project
        )
        appPlugin.extension.registerTransform(LollipopCrashWebViewFixingTransform(project, crashWebViews))
    }
}

@Suppress("UnstableApiUsage")
open class CrashWebViews(project: Project) {
    @get:Input
    val qualifiedNames = project.objects.property<MutableList<String>>()
}

private class LollipopCrashWebViewFixingTransform(
    private val project: Project,
    private val crashWebViews: CrashWebViews
) : Transform() {
    override fun getName(): String = "lollipopCrashWebViewFixer"

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

            jarInput.file.modifyJar(tmpFile) { jarEntry, inputStream ->
                crashWebViews.qualifiedNames.orNull?.firstOrNull { qualifiedName ->
                    val qualifiedNamePath = qualifiedName.replace(
                        ".",
                        File.separator
                    )
                    jarEntry.name == qualifiedNamePath + SdkConstants.DOT_CLASS
                }?.let { qualifiedName ->
                    val qualifiedNamePath = qualifiedName.replace(
                        ".",
                        File.separator
                    )
                    project.logInfo("start fix $qualifiedName")

                    val cr = ClassReader(inputStream)
                    val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)

                    cr.accept(MyClassVisitor(cw, qualifiedNamePath), ClassReader.EXPAND_FRAMES)
                    cw.toByteArray()
                }
            }

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
            file.eachFileRecurse(FileType.FILES) { classFile ->
                crashWebViews.qualifiedNames.orNull?.forEach { qualifiedName ->
                    val qualifiedNamePath = qualifiedName.replace(
                        ".",
                        File.separator
                    )
                    if (classFile.path.contains(qualifiedNamePath + SdkConstants.DOT_CLASS)) {
                        project.logInfo("start fix $qualifiedName")
                        val classReader = ClassReader(classFile.inputStream())
                        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)

                        classReader.accept(
                            MyClassVisitor(classWriter, qualifiedNamePath),
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

    private class MyClassVisitor(classWriter: ClassWriter, private val className: String) :
        ClassVisitor(Opcodes.ASM6, classWriter) {

        init {
            classWriter.generateFixingMethod()
        }

        private fun ClassWriter.generateFixingMethod() {
            visitInnerClass("android/os/Build\$VERSION", "android/os/Build", "VERSION", Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC);
            visitInnerClass("android/os/Build\$VERSION_CODES", "android/os/Build", "VERSION_CODES", Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC);

            val mv = visitMethod(
                Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
                FIX_METHOD_NAME,
                "(Landroid/content/Context;)Landroid/content/Context;",
                null,
                null
            )
            mv.visitCode()
            val l0 = Label()
            mv.visitLabel(l0)
            mv.visitLineNumber(9, l0)
            mv.visitFieldInsn(Opcodes.GETSTATIC, "android/os/Build\$VERSION", "SDK_INT", "I")
            mv.visitIntInsn(Opcodes.BIPUSH, 21)
            val l1 = Label()
            mv.visitJumpInsn(Opcodes.IF_ICMPLT, l1)
            mv.visitFieldInsn(Opcodes.GETSTATIC, "android/os/Build\$VERSION", "SDK_INT", "I")
            mv.visitIntInsn(Opcodes.BIPUSH, 23)
            mv.visitJumpInsn(Opcodes.IF_ICMPGE, l1)
            val l2 = Label()
            mv.visitLabel(l2)
            mv.visitLineNumber(10, l2)
            mv.visitVarInsn(Opcodes.ALOAD, 0)
            mv.visitTypeInsn(Opcodes.NEW, "android/content/res/Configuration")
            mv.visitInsn(Opcodes.DUP)
            mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "android/content/res/Configuration",
                "<init>",
                "()V",
                false
            )
            mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "android/content/Context",
                "createConfigurationContext",
                "(Landroid/content/res/Configuration;)Landroid/content/Context;",
                false
            )
            mv.visitInsn(Opcodes.ARETURN)
            mv.visitLabel(l1)
            mv.visitLineNumber(12, l1)
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null)
            mv.visitVarInsn(Opcodes.ALOAD, 0)
            mv.visitInsn(Opcodes.ARETURN)
            val l3 = Label()
            mv.visitLabel(l3)
            mv.visitLocalVariable("context", "Landroid/content/Context;", null, l0, l3, 0)
            mv.visitMaxs(3, 1)
            mv.visitEnd()
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            desc: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, name, desc, signature, exceptions)
            if (mv != null && "<init>" == name) {
                return object : MethodVisitor(Opcodes.ASM5, mv) {
                    override fun visitVarInsn(opcode: Int, `var`: Int) {
                        super.visitVarInsn(opcode, `var`)
                        if (opcode == Opcodes.ALOAD && `var` == 1) {
                            mv.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                className,
                                FIX_METHOD_NAME,
                                "(Landroid/content/Context;)Landroid/content/Context;",
                                false
                            )
                        }
                    }
                }
            }
            return mv
        }

        companion object {
            private const val FIX_METHOD_NAME = "_autoGeneratedFixMethodByGradlePlugin"
        }
    }
}