plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.versions)
    `java-gradle-plugin`
}

group = "dev.limebeck"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    implementation(libs.kotlin.plugin)
    implementation(libs.kotlinpoet)
    testImplementation(libs.kotlin.stdlib)
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}

gradlePlugin {
    website.set("https://github.com/LimeBeck/BuildTimeConfig")
    vcsUrl.set("https://github.com/LimeBeck/BuildTimeConfig.git")
    val kmpResourcesPlugin by plugins.creating {
        id = "dev.limebeck.kmp-resources"
        displayName = "Kotlin Multiplatform Resources"
        description = "Gradle plugin for providing build-time multiplatform resources for kotlin application"
        tags.set(listOf("kotlin", "resources", "kmp", "multiplatform"))
        implementationClass = "dev.limebeck.kmpResources.KmpResourcesPlugin"
    }
}
