plugins {
    kotlin("jvm") version "1.3.61"
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

        create("lollipopCrashWebViewFixingPlugin") {
            id = "com.nikeo.gradle.lollipop-crashWebView-fix"
            implementationClass = "com.nikeo.gradle.LollipopCrashWebViewFixingPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/nikeorever/SuperclassReplacemenGradlePlugin"
    vcsUrl = "https://github.com/nikeorever/SuperclassReplacemenGradlePlugin.git"
    (plugins) {
        "superclassReplacementPlugin" {
            // id is captured from java-gradle-plugin configuration
            displayName = "Superclass Replacement plugin"
            description = "Use android transform api to dynamically replace super class"
            tags = listOf("android", "gradle", "asm", "plugin")
            version = "2.3"
        }

        "lollipopCrashWebViewFixingPlugin" {
            // id is captured from java-gradle-plugin configuration
            displayName = "LollipopCrashWebViewFixingPlugin"
            description = "Use android transform api to fix webView crash problem in Lollipop"
            tags = listOf("android", "webView", "asm", "Lollipop")
            version = "1.3"
        }
    }

    mavenCoordinates {
        groupId = "com.nikeo.gradle"
        artifactId = "superclass-replacement-gradle-plugin"
        version = "2.3"
    }
}

repositories {
    mavenCentral()
    google()
    jcenter()
    maven {
        url = uri(properties["SNAPSHOT_REPOSITORY_URL"] as String)
        credentials {
            username = properties["SONATYPE_NEXUS_USERNAME"] as String
            password = properties["SONATYPE_NEXUS_PASSWORD"] as String
        }
    }
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8", "1.3.61"))
    compileOnly(gradleApi())
    compileOnly("org.ow2.asm:asm:7.0")
    implementation("com.android.tools.build:gradle:3.5.3")
    implementation("com.nikeo:anx:1.0.2-SNAPSHOT")
}

apply(from = "../gradle/gradle-mvn-push.gradle.kts")

