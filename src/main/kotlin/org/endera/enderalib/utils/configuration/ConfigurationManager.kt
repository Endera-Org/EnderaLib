package org.endera.enderalib.utils.configuration

import kotlinx.serialization.KSerializer
import org.endera.enderalib.utils.PluginException
import java.io.File
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
     * Loads the configuration, falling back to a merge with the default one and then to regenerating it,
     * and rewrites the file so its keys and comments are up to date.
     *
     * @return The loaded or newly created configuration.
     * @throws PluginException if the configuration could not be loaded or created.
     */
    fun loadOrCreateConfig(): T {
        if (!configFile.exists()) {
            logger.info("Configuration '${configFile.name}' file not found, creating new one.")
            return createNewConfig()
        }

        val fileConfig = loadStrict() ?: mergeWithDefault() ?: return regenerate()
        writeConfigWithComments(configFile, fileConfig, serializer, clazz)
        return fileConfig
    }

    private fun loadStrict(): T? =
        try {
            loadConfig(configFile, serializer).also {
                logger.info("Configuration '${configFile.name}' successfully loaded in strict mode!")
            }
        } catch (e: Exception) {
            logger.warning("Parsing of '${configFile.name}' in strict mode failed: ${e.message}.")
            null
        }

    private fun mergeWithDefault(): T? {
        try {
            val backupFile = configFile.timestampedSibling("-backup-")
            configFile.copyTo(backupFile, overwrite = false)
            logger.info("Backup of original configuration '${configFile.name}' saved to: ${backupFile.absolutePath}")
        } catch (e: Exception) {
            logger.warning("Failed to backup original configuration '${configFile.name}': ${e.message}.")
        }

        return try {
            logger.info("Attempting dynamic merge of configuration '${configFile.name}'.")
            mergeYamlConfigs(configFile.readText(Charsets.UTF_8), defaultConfig, serializer).also {
                logger.info("Dynamic merge of '${configFile.name}' is successful!")
            }
        } catch (e: Exception) {
            logger.severe("Dynamic merge of '${configFile.name}' failed: ${e.message}.")
            null
        }
    }

    private fun regenerate(): T {
        logger.warning("Invalid configuration detected. Renaming invalid configuration file and creating a new one.")
        try {
            renameInvalidConfig(configFile)
        } catch (e: Exception) {
            logger.severe("Error while regenerating configuration: ${e.message}.")
            throw PluginException("Failed to regenerate configuration", e)
        }
        return createNewConfig().also {
            logger.info("Configuration successfully regenerated!")
        }
    }

    private fun createNewConfig(): T {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw PluginException("Failed to create data folder: ${dataFolder.absolutePath}")
        }
        try {
            writeConfigWithComments(configFile, defaultConfig, serializer, clazz)
        } catch (e: Exception) {
            logger.severe("Error while creating new configuration: ${e.message}")
            throw PluginException("Failed to create configuration", e)
        }
        logger.info("Configuration '${configFile.name}' successfully created!")
        return defaultConfig
    }
}
