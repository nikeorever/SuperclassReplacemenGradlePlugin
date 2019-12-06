# [SuperclassReplacementGradlePlugin](https://plugins.gradle.org/plugin/com.nikeo.gradle.superclass-replacement)
Use android transform api to dynamically replace super class

* Using the plugins DSL:
``` groovy
plugins {
  id "com.nikeo.gradle.superclass-replacement" version "2.3"
}
```
* Using legacy plugin application:
``` groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "com.nikeo.gradle:superclass-replacement-gradle-plugin:2.3"
  }
}

apply plugin: "com.nikeo.gradle.superclass-replacement"

superclassReplacement {
    qualifiedNameReplacements = [ 'com.foo.TargetClass': 'com.foo.replacedClass' ]
}
```

# [LollipopCrashWebViewFixingPlugin](https://plugins.gradle.org/plugin/com.nikeo.gradle.lollipop-crashWebView-fix)
Use android transform api to fix webView crash problem in Lollipop

* Using the plugins DSL:
``` groovy
plugins {
  id "com.nikeo.gradle.lollipop-crashWebView-fix" version "1.3"
}
```
* Using legacy plugin application:
``` groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "com.nikeo.gradle:superclass-replacement-gradle-plugin:2.3"
  }
}

apply plugin: "com.nikeo.gradle.lollipop-crashWebView-fix"

crashWebViews {
    qualifiedNames = ['com.foo.android.asm.LollipopCrashWebView']
}
```
