package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.KSerializer
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Writes the configuration object to a file in YAML format, adding comments.
 *
 * @param T The type of the configuration object.
 * @param file The file where the configuration will be written.
 * @param config The configuration object to write.
 * @param serializer The serializer for the configuration type.
 * @param clazz The configuration type class (for replacing reified).
 * @param yamlConfiguration YAML configuration settings (non-strict mode by default).
 * @throws Exception in case of writing or serialization errors.
 */
fun <T : Any> writeConfigWithComments(
    file: File,
    config: T,
    serializer: KSerializer<T>,
    clazz: KClass<T>,
    yamlConfiguration: YamlConfiguration = YamlConfiguration(
        strictMode = false,
        breakScalarsAt = 400,
        yamlNamingStrategy = YamlNamingStrategy.KebabCase
    )
) {
    val yaml = Yaml(configuration = yamlConfiguration)
    val serialized = yaml.encodeToString(serializer, config)
    val withComments = addComments(serialized, clazz)

    val content = if (withComments.endsWith("\n")) withComments else "$withComments\n"
    atomicWriteUtf8(file, content)
}

private fun atomicWriteUtf8(file: File, content: String) {
    val parent = file.parentFile
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
        throw IllegalStateException("Failed to create directory: ${parent.absolutePath}")
    }

    val tempFile = File(
        parent ?: File("."),
        "${file.name}.tmp-${UUID.randomUUID()}"
    )

    try {
        tempFile.writeText(content, Charsets.UTF_8)
        try {
            Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    } finally {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}
