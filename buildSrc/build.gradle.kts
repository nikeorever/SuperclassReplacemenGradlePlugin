plugins {
    kotlin("jvm") version "1.3.50"
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "0.10.1"
    `java-gradle-plugin`
}

// Use java-gradle-plugin to generate plugin descriptors and specify plugin ids
gradlePlugin {
    plugins {
        create("superclassReplacementPlugin") {
            id = "com.nikeo.gradle.superclass-replacement"
            implementationClass = "com.nikeo.gradle.SuperclassReplacementPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/nikeorever/SuperclassReplacemenGradlePlugin"
    vcsUrl = "https://github.com/nikeorever/SuperclassReplacemenGradlePlugin.git"
    description = "Use android transform api to dynamically replace super class"
    (plugins) {
        "superclassReplacementPlugin" {
            // id is captured from java-gradle-plugin configuration
            displayName = "Superclass Replacement plugin"
            tags = listOf("android", "gradle", "asm", "plugin")
            version = "1.0"
        }
    }

    mavenCoordinates {
        groupId = "org.nikeo.gradle"
        artifactId = "superclass-replacement-plugin"
        version = "1.0"
    }
}

repositories {
    mavenCentral()
    google()
    jcenter()
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8", "1.3.50"))
    compileOnly(gradleApi())
    compileOnly("org.ow2.asm:asm:6.0")
    implementation("com.android.tools.build:gradle:3.5.0")
}

