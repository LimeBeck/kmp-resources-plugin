package dev.limebeck.kmpResources.codeGenerator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec

fun generateResourceItemInterface(): TypeSpec {
    return TypeSpec.interfaceBuilder("ResourceItem")
        .addProperty("name", String::class)
        .build()
}

fun generateResourceFileInterface(packageName: String): TypeSpec {
    val itemClassName = ClassName(packageName, "ResourceItem")
    return TypeSpec.interfaceBuilder("ResourceFile")
        .addSuperinterface(itemClassName)
        .addProperty("size", Long::class)
        .addProperty("type", String::class)
        .addFunction(
            FunSpec.builder("readBytes")
                .addModifiers(KModifier.ABSTRACT)
                .returns(ByteArray::class)
                .build()
        )
        .build()
}

fun generateResourceDirectoryInterface(packageName: String): TypeSpec {
    val itemClassName = ClassName(packageName, "ResourceItem")
    val listClassName = ClassName("kotlin.collections", "List")
    return TypeSpec.interfaceBuilder("ResourceDirectory")
        .addSuperinterface(itemClassName)
        .addProperty("items", listClassName.parameterizedBy(itemClassName))
        .build()
}
