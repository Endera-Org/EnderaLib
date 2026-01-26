package org.endera.enderalib.utils.configuration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

fun String.toKebabCase(): String =
    trim()
        .replace('_', '-')
        .replace(' ', '-')
        .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
        .replace(Regex("-{2,}"), "-")
        .lowercase()

fun addCommentsForClass(clazz: KClass<*>, yamlText: String, baseIndent: String = ""): String {
    var result = yamlText
    clazz.memberProperties.forEach { property ->
        val comment = property.findAnnotation<Comment>()
        val spacer = property.findAnnotation<Spacer>()
        val key = property.findAnnotation<SerialName>()?.value ?: property.name.toKebabCase()

        val escapedKey = Regex.escape(key)
        val regex = Regex("(?m)^($baseIndent)($escapedKey:.*(?:\n(?!$baseIndent\\S).*)*)")
        val match = regex.find(result)
        if (match != null) {
            val indent = match.groupValues[1]
            var block = match.groupValues[2]

            val nestedType = property.returnType.classifier as? KClass<*>
            if (nestedType != null && nestedType.findAnnotation<Serializable>() != null) {
                block = addCommentsForClass(nestedType, block, "$baseIndent  ")
            }

            val commentStr = if (comment != null) {
                comment.text
                    .trimIndent()
                    .lines()
                    .joinToString("\n") { "$indent# $it" } + "\n"
            } else ""

            val spacerStr = spacer?.let { "\n".repeat(it.count) } ?: ""

            result = result.replaceRange(match.range, spacerStr + commentStr + indent + block)
        }
    }
    return result
}

/**
 * Adds comments to the YAML text using information from class annotations.
 *
 * @param T The type of the configuration object.
 * @param yamlText The original YAML text.
 * @param clazz The configuration type class.
 * @return YAML text with added comments.
 */
fun <T : Any> addComments(yamlText: String, clazz: KClass<T>): String {
    return addCommentsForClass(clazz, yamlText, "")
}
