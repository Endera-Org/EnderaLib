package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNamingStrategy
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.KSerializer

/**
 * Merges the YAML representation of the file content and the default object.
 *
 * @param fileContent Raw data from the configuration file.
 * @param defaultConfig The default configuration object.
 * @param serializer The serializer for the configuration type.
 * @return A configuration object of type [T] after merging.
 */
fun <T> mergeYamlConfigs(
    fileContent: String,
    defaultConfig: T,
    serializer: KSerializer<T>,
): T {
    val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false,
            breakScalarsAt = 400,
            yamlNamingStrategy = YamlNamingStrategy.KebabCase,
        ),
    )

    val fileNode = yaml.decodeFromString(YamlNode.serializer(), fileContent.stripUtf8Bom())
    val defaultYamlString = yaml.encodeToString(serializer, defaultConfig)
    val defaultNode = yaml.decodeFromString(YamlNode.serializer(), defaultYamlString)

    val mergedNode = mergeYamlNodesInternal(fileNode, defaultNode, schemaAware = false)
    val mergedYamlString = yaml.encodeToString(YamlNode.serializer(), mergedNode)
    return try {
        yaml.decodeFromString(serializer, mergedYamlString)
    } catch (first: Exception) {
        val schemaAwareNode = mergeYamlNodesInternal(fileNode, defaultNode, schemaAware = true)
        val schemaAwareYamlString = yaml.encodeToString(YamlNode.serializer(), schemaAwareNode)
        try {
            yaml.decodeFromString(serializer, schemaAwareYamlString)
        } catch (second: Exception) {
            second.addSuppressed(first)
            throw second
        }
    }
}

/**
 * Recursively merges two YAML trees (YamlNode).
 *
 * If both nodes are maps, for each key:
 *   - If the value is present in fileNode, it merges it with the default value.
 *   - Otherwise, the default value is used.
 * For lists, the value from fileNode is used.
 * In other cases, fileNode is returned.
 */
fun mergeYamlNodes(fileNode: YamlNode, defaultNode: YamlNode): YamlNode =
    mergeYamlNodesInternal(fileNode, defaultNode, schemaAware = false)

private fun mergeYamlNodesInternal(fileNode: YamlNode, defaultNode: YamlNode, schemaAware: Boolean): YamlNode {
    if (schemaAware) {
        when (defaultNode) {
            is YamlMap -> if (fileNode !is YamlMap) return defaultNode
            is YamlList -> if (fileNode !is YamlList) return defaultNode
            else -> {
                if (fileNode is YamlMap || fileNode is YamlList) return defaultNode
                if (fileNode is YamlNull && defaultNode !is YamlNull) return defaultNode
            }
        }
    }

    return when (fileNode) {
        is YamlMap if defaultNode is YamlMap -> mergeYamlMaps(fileNode, defaultNode, schemaAware)
        is YamlList if defaultNode is YamlList -> fileNode
        else -> fileNode
    }
}

private fun mergeYamlMaps(fileNode: YamlMap, defaultNode: YamlMap, schemaAware: Boolean): YamlMap {
    val mergedEntries = linkedMapOf<YamlScalar, YamlNode>()

    val fileEntriesByContent = fileNode.entries.entries.associateBy { it.key.content }
    val fileEntriesByNormalized = linkedMapOf<String, Map.Entry<YamlScalar, YamlNode>>().apply {
        for (entry in fileNode.entries.entries) {
            putIfAbsent(entry.key.content.toKebabCase(), entry)
        }
    }

    for ((defaultKey, defaultValue) in defaultNode.entries) {
        val fileEntry = fileEntriesByContent[defaultKey.content]
            ?: fileEntriesByNormalized[defaultKey.content.toKebabCase()]
        val mergedValue = if (fileEntry != null) {
            mergeYamlNodesInternal(fileEntry.value, defaultValue, schemaAware)
        } else {
            defaultValue
        }
        mergedEntries[defaultKey] = mergedValue
    }

    for ((fileKey, fileValue) in fileNode.entries) {
        if (!mergedEntries.keys.any { it.content == fileKey.content }) {
            mergedEntries[fileKey] = fileValue
        }
    }

    return YamlMap(mergedEntries, path = defaultNode.path)
}

