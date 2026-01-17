package dev.limebeck.kmpResources.codeGenerator

import dev.limebeck.kmpResources.ResourceOverrideStrategy
import java.io.File

class ResourceNode(val name: String) {
    val children = mutableMapOf<String, ResourceNode>()
    val files = mutableMapOf<String, FileInfo>()

    data class FileInfo(val file: File, val path: String)

    fun addFile(path: String, file: File, strategy: ResourceOverrideStrategy) {
        val parts = path.split(File.separatorChar)
        var currentPath = this
        for (i in 0 until parts.size - 1) {
            currentPath = currentPath.children.getOrPut(parts[i]) { ResourceNode(parts[i]) }
        }
        val fileName = parts.last()
        if (currentPath.files.containsKey(fileName)) {
            when (strategy) {
                ResourceOverrideStrategy.OVERRIDE -> currentPath.files[fileName] = FileInfo(file, path)
                ResourceOverrideStrategy.USE_COMMON -> { /* keep existing */ }
                ResourceOverrideStrategy.FAIL -> throw RuntimeException("Duplicate resource found: $path")
            }
        } else {
            currentPath.files[fileName] = FileInfo(file, path)
        }
    }
}
