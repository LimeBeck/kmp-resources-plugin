plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

group = "dev.limebeck.libs"

base {
    archivesName.set("kmp-resources-core")
}

kotlin {
    jvm()
    linuxX64()
    js {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting
    }

    jvmToolchain(21)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("dev.limebeck.libs", "kmp-resources-core", project.version.toString())

    pom {
        url.set("https://github.com/LimeBeck/kmp-resources-plugin")
        name.set("kmp-resources-core")
        description.set("Kotlin Multiplatform Resources")
        developers {
            developer {
                id.set("LimeBeck")
                name.set("Anatoly Nechay-Gumen")
                email.set("mail@limebeck.dev")
            }
        }
        licenses {
            license {
                name.set("MIT license")
                url.set("https://github.com/LimeBeck/kmp-resources-plugin/blob/master/LICENCE")
                distribution.set("repo")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/LimeBeck/kmp-resources-plugin.git")
            developerConnection.set("scm:git:ssh://github.com/LimeBeck/kmp-resources-plugin.git")
            url.set("https://github.com/LimeBeck/kmp-resources-plugin")
        }
    }
}

dokka {
    moduleName.set("Kotlin Multiplatform Resources")

    dokkaPublications.html {
        suppressInheritedMembers.set(true)
        failOnWarning.set(true)
    }

//    dokkaSourceSets.configureEach {
//        includes.from("../README.MD")
//    }

    pluginsConfiguration.html {
        footerMessage.set("(c) LimeBeck.Dev")
    }
}
