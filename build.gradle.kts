plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.pluginPublish) apply false
    alias(libs.plugins.versions) apply false
}

allprojects {
    group = "dev.limebeck"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}
