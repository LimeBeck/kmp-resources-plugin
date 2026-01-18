plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("dev.limebeck.kmp-resources")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64()

    sourceSets.commonMain.dependencies {
        implementation(kotlin("test"))
    }
}

kmpResources {
    packageName.set("dev.limebeck.example.res")
    generateOnSync = true
}
