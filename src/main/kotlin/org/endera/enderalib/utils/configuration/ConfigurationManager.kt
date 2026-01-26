package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.KSerializer
import org.endera.enderalib.utils.PluginException
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * Configuration manager class responsible for loading or creating a configuration file,
 * dynamically merging it with the default configuration, and saving it with comments.
 *
 * @param T The type of the configuration object.
 * @param configFile The configuration file.
 * @param dataFolder The plugin's data folder.
 * @param defaultConfig The default configuration object.
 * @param serializer The serializer for the configuration type.
 * @param logger The logger for error and warning messages.
 * @param clazz The configuration type class (for replacing reified).
 */
class ConfigurationManager<T : Any>(
    private val configFile: File,
    private val dataFolder: File,
    private val defaultConfig: T,
    private val serializer: KSerializer<T>,
    private val logger: Logger,
    private val clazz: KClass<T>
) {

    val type: KClass<T>
        get() = clazz

    /**
     * Loads or creates the configuration, merges it with the default one,
     * and writes the changes to the file.
     *
     * @return The loaded or newly created configuration.
     * @throws PluginException if the configuration could not be loaded or created.
     */
    fun loadOrCreateConfig(): T {
        if (!configFile.exists()) {
            logger.info("Configuration '${configFile.name}' file not found, creating new one.")
            return createNewConfig()
        }

        var fileConfig: T? = null

        try {
            fileConfig = loadConfig(
                configFile,
                serializer,
                YamlConfiguration(
                    strictMode = true,
                    breakScalarsAt = 400,
                    yamlNamingStrategy = YamlNamingStrategy.KebabCase
                )
            ).also {
                logger.info("Configuration '${configFile.name}' successfully loaded in strict mode!")
            }
        } catch (e: Exception) {
            logger.warning("Parsing of '${configFile.name}' in strict mode failed: ${e.message ?: "Unknown error"}.")
        }

        if (fileConfig == null) {
            try {
                val timestamp = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd_HH-mm-ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now())
                val backupFile = if (configFile.extension.isNotBlank()) {
                    File(configFile.parentFile, "${configFile.nameWithoutExtension}-backup-$timestamp.${configFile.extension}")
                } else {
                    File(configFile.parentFile, "${configFile.nameWithoutExtension}-backup-$timestamp")
                }

                configFile.copyTo(backupFile, overwrite = false)
                logger.info("Backup of original configuration '${configFile.name}' saved to: ${backupFile.absolutePath}")
            } catch (e: Exception) {
                logger.warning("Failed to backup original configuration '${configFile.name}': ${e.message ?: "Unknown error"}.")
            }

            try {
                logger.info("Attempting dynamic merge of configuration '${configFile.name}'.")
                val fileContent = configFile.readText(Charsets.UTF_8)
                fileConfig = mergeYamlConfigs(fileContent, defaultConfig, serializer).also {
                    logger.info("Dynamic merge of '${configFile.name}' is successful!")
                }
            } catch (e: Exception) {
                logger.severe("Dynamic merge of '${configFile.name}' failed: ${e.message ?: "Unknown error"}.")
            }
        }

        if (fileConfig == null) {
            logger.warning("Invalid configuration detected. Renaming invalid configuration file and creating a new one.")
            try {
                renameInvalidConfig(configFile)
                return createNewConfig().also {
                    logger.info("Configuration successfully regenerated!")
                }
            } catch (e: Exception) {
                logger.severe("Error while regenerating configuration: ${e.message ?: "Unknown error"}.")
                throw PluginException("Failed to regenerate configuration", e)
            }
        }
        writeConfigWithComments(configFile, fileConfig, serializer, clazz)
        return fileConfig
    }

    /**
     * Creates a new configuration file using the default configuration.
     *
     * @return A new configuration object.
     * @throws PluginException if the configuration could not be created or loaded.
     */
    private fun createNewConfig(): T {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw PluginException("Failed to create data folder: ${dataFolder.absolutePath}")
        }
        try {
            writeConfigWithComments(configFile, defaultConfig, serializer, clazz)
        } catch (e: Exception) {
            logger.severe("Error while creating new configuration: ${e.message ?: "Unknown error"}")
            throw PluginException("Failed to create configuration", e)
        }
        return try {
            loadConfig(configFile, serializer).also {
                logger.info("Configuration '${configFile.name}' successfully created and loaded!")
            }
        } catch (e: Exception) {
            logger.severe("Error while loading the created configuration '${configFile.name}': ${e.message ?: "Unknown error"}")
            throw PluginException("Failed to load configuration after creation", e)
        }
    }
}
