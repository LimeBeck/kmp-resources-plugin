package dev.limebeck.kmpResources.codeGenerator

import java.io.File
import java.net.URLConnection
import java.nio.file.Files

fun isNativeTarget(sourceSetName: String): Boolean {
    // Simple heuristic for demo.
    // In real world we should check the actual target type from KotlinTarget
    return sourceSetName.contains("linux", ignoreCase = true) ||
            sourceSetName.contains("native", ignoreCase = true) ||
            sourceSetName.contains("ios", ignoreCase = true) ||
            sourceSetName.contains("macos", ignoreCase = true)
}

fun isTestSourceSet(sourceSetName: String): Boolean {
    return sourceSetName.contains("test", ignoreCase = true)
}

fun getMimeType(file: File): String {
    return Files.probeContentType(file.toPath())
        ?: URLConnection.guessContentTypeFromName(file.name)
        ?: "application/octet-stream"
}
