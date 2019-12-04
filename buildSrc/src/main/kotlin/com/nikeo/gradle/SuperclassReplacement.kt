package com.nikeo.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property

@Suppress("UnstableApiUsage")
open class SuperclassReplacement(project: Project) {
    @get:Input
    val qualifiedNameReplacements = project.objects.property<MutableMap<String, String>>()
}