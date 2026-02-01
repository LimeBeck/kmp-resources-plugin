package dev.limebeck.kmpResources

interface ResourceFile : ResourceItem {
    val size: Long
    val type: String

    fun readBytes(): ByteArray
}
