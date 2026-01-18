package dev.limebeck.kmpResources

import dev.limebeck.kmpResources.codeGenerator.isTestSourceSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
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

            val resourceFolderName = extension.resourcesFolderName.get()

            val generationTasks = kmpExtension.targets
                .filter { !isTestSourceSet(it.name) }
                .mapNotNull { ktTarget ->
                    val compilation = ktTarget.compilations.findByName("main") ?: return@mapNotNull null
                    val defaultSourceSet = compilation.defaultSourceSet

                    val taskName = "generateKmpResources${ktTarget.name.replaceFirstChar { it.uppercaseChar() }}"
                    val task = target.tasks.register(taskName, KmpResourcesTask::class.java) { t ->
                        t.packageName.set(extension.packageName)
                        t.overrideStrategy.set(extension.overrideStrategy)
                        t.targetSourceSet.set(defaultSourceSet.name)
                        t.outputDirectory.set(target.layout.buildDirectory.dir("generated/kmpResources/${ktTarget.name}"))

                        val hierarchy = mutableMapOf<String, List<String>>()
                        val sourceSetToDirsMap = mutableMapOf<String, List<File>>()

                        compilation.allKotlinSourceSets.forEach { ss ->
                            hierarchy[ss.name] = ss.dependsOn.map { it.name }
                            val resDir = target.file("src/${ss.name}/$resourceFolderName")
                            if (resDir.exists()) {
                                sourceSetToDirsMap[ss.name] = listOf(resDir)
                                t.resourceFiles.from(resDir)
                            }
                        }

                        t.sourceSetHierarchy.set(hierarchy)
                        t.sourceSetToDirs.set(sourceSetToDirsMap)
                    }

                    defaultSourceSet.kotlin.srcDir(task.map { it.outputDirectory })
                    task
                }

            // Ensure tasks run before processResources
            target.tasks.matching { it.name.contains("processResources", ignoreCase = true) }
                .configureEach { processResourcesTask ->
                    generationTasks.forEach { processResourcesTask.dependsOn(it) }
                }

            if (extension.generateOnSync.get()) {
                val prepareKotlinIdeaImport = target.tasks.maybeCreate("prepareKotlinIdeaImport")
                generationTasks.forEach { prepareKotlinIdeaImport.dependsOn(it) }
            }
        }
    }
}
