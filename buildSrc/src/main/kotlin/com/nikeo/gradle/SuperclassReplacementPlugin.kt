package com.nikeo.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class SuperclassReplacementPlugin : Plugin<Project> {

    override fun apply(project: Project) = project.run {
        plugins.mapNotNull { plugin ->
            when (plugin) {
                is AppPlugin -> {
                    project.logger.info("[SuperclassReplacementPlugin], apply to app:${project.name}")
                    plugin.extension
                }
                is LibraryPlugin -> {
                    project.logger.info("[SuperclassReplacementPlugin], apply to android library:${project.name}")
                    plugin.extension
                }
                else -> null
            }
        }.forEach {
            val superclassReplacement = extensions.create(
                "superclassReplacement",
                SuperclassReplacement::class,
                project
            )
            it.registerTransform(SuperclassReplacementTransform(project, superclassReplacement))
        }
    }
}