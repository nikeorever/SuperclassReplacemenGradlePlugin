# SuperclassReplacemenGradlePlugin
Use android transform api to dynamically replace super class

# Groovy DSL
# Using the plugins DSL:
plugins {
  id "com.nikeo.gradle.superclass-replacement" version "1.0"
}

# Using legacy plugin application:
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "org.nikeo.gradle:superclass-replacement-plugin:1.0"
  }
}

apply plugin: 'com.nikeo.gradle.superclass-replacement'
