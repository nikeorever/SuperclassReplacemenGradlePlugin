package com.nikeo.gradle

import org.gradle.api.Project

fun Project.logInfo(msg: String) {
    logger.info(msg)
    println(msg)
}