# Kotlin Multiplatform Resources Gradle Plugin

![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/dev.limebeck.kmp-resources)

Gradle plugin for providing build-time multiplatform resources for Kotlin applications.

## Usage

`build.gradle.kts`:

```kotlin
plugins {
    kotlin("multiplatform") version "2.0.0"
    id("dev.limebeck.kmp-resources") version "0.0.1"
}

kmpResources {
    // Optional: Package name for generated classes. Default is "dev.limebeck.res"
    packageName.set("dev.limebeck.example.res") 
    
    // Optional: Name of the folder in src/<sourceSet>/ containing resources. Default is "resources"
    resourcesFolderName.set("resources") 
    
    // Optional: Strategy for handling resource overrides between source sets. Default is OVERRIDE
    // OVERRIDE - files in child source sets override files in parent source sets
    // USE_COMMON - keep files from parent source sets
    // FAIL - throw exception on duplicate
    overrideStrategy.set(dev.limebeck.kmpResources.ResourceOverrideStrategy.OVERRIDE)

    // Optional: Generate resources on project sync. Default is true
    generateOnSync.set(true)
}
```

It will generate code like this (for `src/commonMain/resources/image.png`):

```kotlin
package dev.limebeck.example.res

import kotlin.ByteArray
import kotlin.Long
import kotlin.String
import kotlin.collections.List

public interface ResourceItem {
  public val name: String
}

public interface ResourceFile : ResourceItem {
  public val size: Long

  public val type: String

  public fun readBytes(): ByteArray
}

public interface ResourceDirectory : ResourceItem {
  public val items: List<ResourceItem>
}

public expect object Res : ResourceDirectory {
  override val name: String

  override val items: List<ResourceItem>

  public val image: ResourceFile
}
```

You can use resources in your code:

```kotlin
import dev.limebeck.example.res.Res

fun main() {
    val imageName = Res.image.name
    val imageType = Res.image.type
    val imageBytes = Res.image.readBytes()
}
```

Example available in `example/build.gradle.kts` and `example/src/linuxX64Test/kotlin/ResourcesTest.kt`
