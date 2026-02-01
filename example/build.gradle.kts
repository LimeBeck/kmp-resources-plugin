plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("dev.limebeck.kmp-resources")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64()

    sourceSets.commonMain.dependencies {
        implementation("dev.limebeck.libs:kmp-resources-core:0.0.3")
        implementation(kotlin("test"))
    }
}

kmpResources {
    packageName.set("dev.limebeck.example.res")
    generateOnSync = true
}
