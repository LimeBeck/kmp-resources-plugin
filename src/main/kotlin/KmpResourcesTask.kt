package dev.limebeck.kmpResources

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import dev.limebeck.kmpResources.codeGenerator.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

abstract class KmpResourcesTask : DefaultTask() {

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val overrideStrategy: Property<ResourceOverrideStrategy>

    @get:Input
    abstract val targetSourceSet: Property<String>

    /**
     * Map of source set name to its resource directories
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resourceFiles: ConfigurableFileCollection

    /**
     * Map of source set name to its resource directories paths (for mapping)
     */
    @get:Input
    abstract val sourceSetToDirs: MapProperty<String, List<File>>

    /**
     * Map of source set name to its hierarchy (list of parent source sets)
     */
    @get:Input
    abstract val sourceSetHierarchy: MapProperty<String, List<String>>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val outDir = outputDirectory.get().asFile
        outDir.deleteRecursively()
        outDir.mkdirs()

        val targetSourceSet = targetSourceSet.get()
        val hierarchy = sourceSetHierarchy.get()

        // Logical structure: SourceSet -> RelativePath -> File
        val resourcesBySourceSet = mutableMapOf<String, MutableMap<String, File>>()

        sourceSetToDirs.get().forEach { (sourceSetName, dirs) ->
            val setResources = resourcesBySourceSet.getOrPut(sourceSetName) { mutableMapOf() }
            dirs.forEach { root ->
                if (root.exists() && root.isDirectory) {
                    root.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relativePath = file.relativeTo(root).path
                        setResources[relativePath] = file
                    }
                }
            }
        }

        // Build consolidated resources for the target source set including its parents
        val rootNode = ResourceNode("", true)

        // To respect hierarchy, we should probably traverse from top (common) to bottom (specific)
        // or collect all and apply overrides.
        // Let's collect all source sets in hierarchy
        val sourceSetOrder = mutableListOf<String>()
        val toProcess = mutableListOf(targetSourceSet)
        while (toProcess.isNotEmpty()) {
            val current = toProcess.removeAt(0)
            if (current !in sourceSetOrder) {
                sourceSetOrder.add(0, current) // Add to beginning to have common first
                toProcess.addAll(hierarchy[current] ?: emptyList())
            }
        }


        sourceSetOrder.forEach { ss ->
            val isCommon = ss == "commonMain"
            resourcesBySourceSet[ss]?.forEach { (path, file) ->
                rootNode.addFile(path, file, overrideStrategy.get(), isCommon)
            }
        }

        val fileSpec = FileSpec.builder(packageName.get(), "Res")

        // Generate Code
        val isCommon = targetSourceSet == "commonMain"
        if (isCommon) {
            fileSpec.addType(generateResourceItemInterface())
            fileSpec.addType(generateResourceFileInterface(packageName.get()))
            fileSpec.addType(generateResourceDirectoryInterface(packageName.get()))
        }

        val resObject = if (isCommon) {
            TypeSpec.objectBuilder("Res")
                .addModifiers(KModifier.EXPECT)
        } else {
            TypeSpec.objectBuilder("Res")
                .addModifiers(KModifier.ACTUAL)
        }

        generateNode(resObject, rootNode, isCommon, isNativeTarget(targetSourceSet), packageName.get())

        fileSpec.addType(resObject.build())

        fileSpec.build().writeTo(outDir)
    }
}
