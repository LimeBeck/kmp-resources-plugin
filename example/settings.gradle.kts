dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

pluginManagement {
    includeBuild("..")
}

includeBuild("..") {
    dependencySubstitution {
        substitute(module("dev.limebeck.libs:kmp-resources-core"))
            .using(project(":core"))
    }
}
