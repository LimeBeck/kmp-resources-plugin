package dev.limebeck.kmpResources.codeGenerator

import dev.limebeck.kmpResources.ResourceOverrideStrategy
import java.io.File

data class ResourceNode(
    val name: String,
    var isCommon: Boolean,
    val children: MutableMap<String, ResourceNode> = mutableMapOf(),
    val files: MutableMap<String, FileInfo> = mutableMapOf(),
) {


    data class FileInfo(
        val file: File,
        val path: String,
        val isCommon: Boolean
    )

    fun addFile(path: String, file: File, strategy: ResourceOverrideStrategy, isCommon: Boolean) {
        val parts = path.split(File.separatorChar)
        var currentPath = this
        for (i in 0 until parts.size - 1) {
            currentPath = currentPath.children.getOrPut(parts[i]) { ResourceNode(parts[i], isCommon) }
            if (!currentPath.isCommon && isCommon) currentPath.isCommon = true
        }
        val fileName = parts.last()
        if (currentPath.files.containsKey(fileName)) {
            when (strategy) {
                ResourceOverrideStrategy.OVERRIDE -> currentPath.files[fileName] = FileInfo(file, path, true)
                ResourceOverrideStrategy.USE_COMMON -> { /* keep existing */ }

                ResourceOverrideStrategy.FAIL -> throw RuntimeException("Duplicate resource found: $path")
            }
        } else {
            currentPath.files[fileName] = FileInfo(file, path, isCommon)
        }
    }
}
