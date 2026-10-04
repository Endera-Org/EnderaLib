package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

/**
 * Repairs a configuration file that no longer matches its schema by filling the gaps from [defaultConfig]:
 * missing keys, keys written in another case (`maxHomes`, `max_homes`), and values of the wrong shape.
 * Everything the file sets validly is kept, and keys unknown to the schema are dropped.
 *
 * @throws Exception if the file is not valid YAML or still cannot be decoded after merging.
 */
fun <T> mergeYamlConfigs(
    fileContent: String,
    defaultConfig: T,
    serializer: KSerializer<T>,
): T {
    val yaml = Yaml(configuration = configYaml(strict = false))
    val fileNode = yaml.parseToYamlNode(fileContent.stripUtf8Bom())
    val defaultNode = yaml.parseToYamlNode(yaml.encodeToString(serializer, defaultConfig))
    return yaml.decodeFromYamlNode(serializer, merge(fileNode, defaultNode, serializer.descriptor))
}

private fun merge(file: YamlNode, default: YamlNode, descriptor: SerialDescriptor): YamlNode {
    if (file is YamlNull && descriptor.isNullable) return file
    if (descriptor.isInline) return merge(file, default, descriptor.getElementDescriptor(0))
    if (file is YamlNull) return default
    if (default is YamlNull) return file
    if ((file is YamlMap) != (default is YamlMap) || (file is YamlList) != (default is YamlList)) return default
    if (file !is YamlMap || default !is YamlMap) return file

    return when (descriptor.kind) {
        StructureKind.CLASS -> mergeClass(file, default, descriptor)
        StructureKind.MAP -> mergeMap(file, default, descriptor.getElementDescriptor(1))
        else -> file
    }
}

private fun mergeClass(file: YamlMap, default: YamlMap, descriptor: SerialDescriptor): YamlMap {
    val elements = (0 until descriptor.elementsCount).associate {
        descriptor.getElementName(it).toKebabCase() to descriptor.getElementDescriptor(it)
    }
    val fileValues = file.entries.mapKeys { it.key.content }
    val fileValuesByKebab = fileValues.mapKeys { it.key.toKebabCase() }

    val merged = default.entries.mapValues { (key, defaultValue) ->
        val kebabKey = key.content.toKebabCase()
        val fileValue = fileValues[key.content] ?: fileValuesByKebab[kebabKey]
        val element = elements[kebabKey]
        if (fileValue != null && element != null) merge(fileValue, defaultValue, element) else fileValue ?: defaultValue
    }
    return YamlMap(merged, default.path)
}

// Keys of a Map field belong to the server owner: defaults only patch the entries the file still has
private fun mergeMap(file: YamlMap, default: YamlMap, valueDescriptor: SerialDescriptor): YamlMap {
    val defaults = default.entries.mapKeys { it.key.content }
    val merged = file.entries.mapValues { (key, value) ->
        defaults[key.content]?.let { merge(value, it, valueDescriptor) } ?: value
    }
    return YamlMap(merged, file.path)
}
