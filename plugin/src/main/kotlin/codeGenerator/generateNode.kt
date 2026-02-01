package dev.limebeck.kmpResources.codeGenerator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import java.util.*

fun generateNode(
    builder: TypeSpec.Builder,
    node: ResourceNode,
    isExpect: Boolean,
    isNative: Boolean
) {
    val itemClassName = ClassName("dev.limebeck.kmpResources", "ResourceItem")
    val fileClassName = ClassName("dev.limebeck.kmpResources", "ResourceFile")
    val directoryClassName = ClassName("dev.limebeck.kmpResources", "ResourceDirectory")

    builder.addSuperinterface(directoryClassName)

    val nodeName = node.name.ifEmpty { "Res" }

    val listType = ClassName("kotlin.collections", "List").parameterizedBy(itemClassName)

    if (isExpect) {
        builder.addProperty(PropertySpec.builder("name", String::class, KModifier.OVERRIDE).build())
        builder.addProperty(PropertySpec.builder("items", listType, KModifier.OVERRIDE).build())
    } else {
        val modifiers = if (node.isCommon)
            arrayOf(KModifier.OVERRIDE, KModifier.ACTUAL)
        else
            arrayOf(KModifier.OVERRIDE)

        builder.addProperty(
            PropertySpec.builder("name", String::class, *modifiers).initializer("%S", nodeName).build()
        )

        val itemRefs = mutableListOf<String>()
        node.children.keys.forEach { itemRefs.add(it.replace(".", "_")) }
        node.files.keys.forEach { itemRefs.add(it.substringBeforeLast(".").replace(".", "_")) }

        builder.addProperty(
            PropertySpec.builder("items", listType, *modifiers)
                .delegate("lazy { listOf(%L) }", itemRefs.joinToString())
                .build()
        )
    }

    // Add sub-folders as objects
    node.children.forEach { (name, childNode) ->
        val subObjectName = name.replace(".", "_")
        val subBuilder = TypeSpec.objectBuilder(subObjectName)
        if (isExpect) {
            subBuilder.addModifiers(KModifier.EXPECT)
        } else {
            if (childNode.isCommon) {
                subBuilder.addModifiers(KModifier.ACTUAL)
            }
        }
        generateNode(subBuilder, childNode, isExpect, isNative)
        builder.addType(subBuilder.build())
    }

    // Add files as properties
    node.files.forEach { (name, fileInfo) ->
        val file = fileInfo.file
        val propName = name.substringBeforeLast(".").replace(".", "_")
        val propBuilder = PropertySpec.builder(propName, fileClassName)
        if (isExpect) {
            propBuilder.addModifiers(KModifier.EXPECT)
        } else {
            if (fileInfo.isCommon) {
                propBuilder.addModifiers(KModifier.ACTUAL)
            }

            val mimeType = getMimeType(file)
            val size = file.length()

            val readBytesFun = FunSpec.builder("readBytes")
                .addModifiers(KModifier.OVERRIDE)
                .returns(ByteArray::class)

            if (isNative) {
                val base64 = Base64.getEncoder().encodeToString(file.readBytes())
                readBytesFun.addAnnotation(
                    AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                        .addMember("%T::class", ClassName("kotlin.io.encoding", "ExperimentalEncodingApi"))
                        .build()
                )
                readBytesFun.addStatement(
                    "return %T.Default.decode(%S)",
                    ClassName("kotlin.io.encoding", "Base64"),
                    base64
                )
            } else {
                val resourcePath = fileInfo.path.replace(File.separatorChar, '/')
                readBytesFun.addStatement(
                    "return Thread.currentThread().contextClassLoader.getResourceAsStream(%S).readAllBytes()",
                    resourcePath
                )
            }

            val fileObject = TypeSpec.anonymousClassBuilder()
                .addSuperinterface(fileClassName)
                .addProperty(
                    PropertySpec.builder("name", String::class, KModifier.OVERRIDE).initializer("%S", name).build()
                )
                .addProperty(
                    PropertySpec.builder("size", Long::class, KModifier.OVERRIDE).initializer("%L", size).build()
                )
                .addProperty(
                    PropertySpec.builder("type", String::class, KModifier.OVERRIDE).initializer("%S", mimeType).build()
                )
                .addFunction(readBytesFun.build())
                .build()

            propBuilder.initializer("%L", fileObject)
        }
        builder.addProperty(propBuilder.build())
    }
}
