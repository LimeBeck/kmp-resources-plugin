rootProject.name = "kmp-resources"

include(":core")
include(":plugin")

project(":core").name = "kmp-resources-core"

includeBuild("example")
