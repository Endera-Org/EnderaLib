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
        // Simple regex to find just the key line start - avoids catastrophic backtracking
        val keyRegex = Regex("(?m)^($baseIndent)($escapedKey:)")
        val keyMatch = keyRegex.find(result) ?: return@forEach

        val blockStart = keyMatch.range.first
        val indent = keyMatch.groupValues[1]

        // Find block end by scanning lines instead of using regex with nested quantifiers
        val blockEnd = findBlockEnd(result, keyMatch.range.last + 1, baseIndent)
        var block = result.substring(keyMatch.range.first + indent.length, blockEnd)

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

        result = result.replaceRange(blockStart, blockEnd, spacerStr + commentStr + indent + block)
    }
    return result
}

/**
 * Finds the end of a YAML block by scanning for the next line that starts with
 * the base indentation followed by a non-whitespace character.
 */
private fun findBlockEnd(text: String, startIndex: Int, baseIndent: String): Int {
    var i = startIndex
    while (i < text.length) {
        // Find next newline
        val newlineIndex = text.indexOf('\n', i)
        if (newlineIndex == -1) {
            return text.length
        }

        val lineStart = newlineIndex + 1
        if (lineStart >= text.length) {
            return text.length
        }

        // Check if this line starts with baseIndent followed by non-whitespace
        if (text.startsWith(baseIndent, lineStart)) {
            val afterIndent = lineStart + baseIndent.length
            if (afterIndent < text.length && !text[afterIndent].isWhitespace()) {
                return newlineIndex
            }
        }

        i = lineStart
    }
    return text.length
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
