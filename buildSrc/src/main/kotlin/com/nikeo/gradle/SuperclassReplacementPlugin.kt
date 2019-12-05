package com.nikeo.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findPlugin

class SuperclassReplacementPlugin : Plugin<Project> {

    override fun apply(project: Project) = project.run {
        val appPlugin = plugins.findPlugin(AppPlugin::class)
        requireNotNull(appPlugin) {
            "SuperclassReplacementPlugin must apply to app"
        }
        val superclassReplacement = extensions.create(
            "superclassReplacement",
            SuperclassReplacement::class,
            project
        )
        appPlugin.extension.registerTransform(
            SuperclassReplacementTransform(
                project,
                superclassReplacement
            )
        )
    }
}