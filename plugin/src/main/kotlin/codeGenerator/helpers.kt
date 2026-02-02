package dev.limebeck.kmpResources.codeGenerator

import java.io.File
import java.net.URLConnection
import java.nio.file.Files


fun isTestSourceSet(sourceSetName: String): Boolean {
    return sourceSetName.contains("test", ignoreCase = true)
}

fun getMimeType(file: File): String {
    return Files.probeContentType(file.toPath())
        ?: URLConnection.guessContentTypeFromName(file.name)
        ?: "application/octet-stream"
}
