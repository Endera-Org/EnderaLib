package org.endera.enderalib

import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.adventure.stringToComponent
import org.endera.enderalib.bstats.MetricsLite
import org.endera.enderalib.config.ConfigScheme
import org.endera.enderalib.config.defaultConfig
import org.endera.enderalib.utils.PluginException
import org.endera.enderalib.utils.configuration.ConfigurationManager
import java.io.File

internal var config: ConfigScheme = defaultConfig

val isFolia: Boolean =
    runCatching { Class.forName("io.papermc.paper.threadedregions.RegionizedServer") }.isSuccess

internal class EnderaLib : JavaPlugin() {

    override fun onEnable() {
        MetricsLite(this, 23669)

        getCommand("enderalib")?.setExecutor { sender, _, _, args ->
            val isVersion = args.firstOrNull().equals("version", ignoreCase = true)
            val message = if (isVersion) {
                "<green>EnderaLib <yellow>version: <gray>${description.version}"
            } else {
                "<red>This command doesn't exist"
            }
            sender.sendMessage(message.stringToComponent())
            isVersion
        }

        val configManager = ConfigurationManager(
            configFile = File(dataFolder, "config.yml"),
            dataFolder = dataFolder,
            defaultConfig = defaultConfig,
            serializer = ConfigScheme.serializer(),
            logger = logger,
            clazz = ConfigScheme::class
        )

        // Dependent plugins keep calling into the library either way, so a broken config falls back to the defaults
        try {
            org.endera.enderalib.config = configManager.loadOrCreateConfig()
        } catch (e: PluginException) {
            logger.severe("Could not load configuration, using the defaults: ${e.message}")
        }
        logger.info("Plugin is loaded")
    }
}
