package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.KSerializer
import java.io.File


/**
 * Loads configuration from a YAML file.
 *
 * @param T The type of the configuration object.
 * @param file The file to load the configuration from.
 * @param serializer The serializer for the configuration object type.
 * @return The loaded configuration object of type T.
 * @throws Exception on file reading or parsing errors.
 */
fun <T> loadConfig(
    file: File,
    serializer: KSerializer<T>,
    yamlConfiguration: YamlConfiguration = YamlConfiguration(
        strictMode = true,
        breakScalarsAt = 400,
        yamlNamingStrategy = YamlNamingStrategy.KebabCase
    )
): T {
    val yaml = Yaml(configuration = yamlConfiguration)
    val text = file.readText(Charsets.UTF_8).stripUtf8Bom()

    return yaml.decodeFromString(serializer, text)
}
