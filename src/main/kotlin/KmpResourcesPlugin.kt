package dev.limebeck.kmpResources

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

class KmpResourcesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("kmpResources", KmpResourcesExtension::class.java, target)

        target.afterEvaluate {
            val kmpExtension = target.extensions.findByType(KotlinMultiplatformExtension::class.java)
            if (kmpExtension == null) {
                target.logger.warn("KmpResourcesPlugin requires Kotlin Multiplatform plugin")
                return@afterEvaluate
            }

            val task = target.tasks.register("generateKmpResources", KmpResourcesTask::class.java) { t ->
                t.packageName.set(extension.packageName)
                t.overrideStrategy.set(extension.overrideStrategy)
                t.outputDirectory.set(target.layout.buildDirectory.dir("generated/kmpResources"))

                val resourceFolderName = extension.resourcesFolderName.get()
                
                val hierarchy = mutableMapOf<String, List<String>>()
                val resourceFilesList = mutableListOf<File>()
                val sourceSetToDirsMap = mutableMapOf<String, List<File>>()

                kmpExtension.sourceSets.forEach { sourceSet ->
                    hierarchy[sourceSet.name] = sourceSet.dependsOn.map { it.name }
                    
                    val resDir = target.file("src/${sourceSet.name}/$resourceFolderName")
                    if (resDir.exists()) {
                        resourceFilesList.add(resDir)
                        sourceSetToDirsMap[sourceSet.name] = listOf(resDir)
                    }
                }

                t.sourceSetHierarchy.set(hierarchy)
                t.resourceFiles.from(resourceFilesList)
                t.sourceSetToDirs.set(sourceSetToDirsMap)
            }

            // Register generated sources
            kmpExtension.sourceSets.forEach { sourceSet ->
                if (sourceSet.name == "commonMain") {
                    sourceSet.kotlin.srcDir(task.map { it.outputDirectory.dir("commonMain/kotlin") })
                } else {
                    sourceSet.kotlin.srcDir(task.map { it.outputDirectory.dir("${sourceSet.name}/kotlin") })
                }
            }

            // Ensure task runs before processResources
            target.tasks.matching { it.name.contains("processResources", ignoreCase = true) }.configureEach {
                it.dependsOn(task)
            }

            if (extension.generateOnSync.get()) {
                target.tasks.maybeCreate("prepareKotlinIdeaImport").dependsOn(task)
            }
        }
    }
}
