package dev.limebeck.kmpResources

interface ResourceDirectory : ResourceItem {
    val items: List<ResourceItem>
}

fun ResourceDirectory.resolvePath(path: List<String>): ResourceItem? {
    if (path.isEmpty()) return this
    val nextItem = items.find { it.name == path.first() }
    return when (nextItem) {
        is ResourceDirectory -> nextItem.resolvePath(path.drop(1))
        is ResourceFile -> nextItem
        else -> null
    }
}

fun ResourceDirectory.resolvePath(path: String): ResourceItem? {
    return resolvePath(path.split('/'))
}
