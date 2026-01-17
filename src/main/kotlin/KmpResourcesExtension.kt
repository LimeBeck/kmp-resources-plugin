package dev.limebeck.kmpResources

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import java.io.File

enum class ResourceOverrideStrategy {
    OVERRIDE,
    USE_COMMON,
    FAIL
}

open class KmpResourcesExtension(project: Project) {
    @Input
    val resourcesFolderName: Property<String> = project.objects.property(String::class.java).convention("resources")

    @Input
    val packageName: Property<String> = project.objects.property(String::class.java).convention("dev.limebeck.res")

    @Input
    val overrideStrategy: Property<ResourceOverrideStrategy> = project.objects.property(ResourceOverrideStrategy::class.java).convention(ResourceOverrideStrategy.OVERRIDE)

    @Input
    val generateOnSync: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)
}
