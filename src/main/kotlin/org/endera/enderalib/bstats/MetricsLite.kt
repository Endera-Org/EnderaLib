package org.endera.enderalib.bstats

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import org.endera.enderalib.utils.async.coroutines
import org.endera.enderalib.utils.async.ioDispatcher
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.logging.Level
import java.util.zip.GZIPOutputStream
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

/**
 * Reports server and plugin stats to bStats every 30 minutes, without custom charts.
 * Server owners can opt out in `plugins/bStats/config.yml`, which is shared with every other bStats plugin.
 *
 * @param pluginId see [What is my plugin id?](https://bstats.org/what-is-my-plugin-id)
 */
class MetricsLite(private val plugin: Plugin, private val pluginId: Int) {
    private val configFile = File(plugin.dataFolder.parentFile, "bStats/config.yml")
    private val config = YamlConfiguration.loadConfiguration(configFile)
    private val serverUuid = config.getString("serverUuid") ?: createConfig()

    val isEnabled = config.getBoolean("enabled", true)
    private val logFailedRequests = config.getBoolean("logFailedRequests", false)
    private val logSentData = config.getBoolean("logSentData", false)
    private val logResponseStatusText = config.getBoolean("logResponseStatusText", false)

    init {
        if (isEnabled) {
            // bStats blocks plugins that submit on a different schedule, so the timings mirror the official client
            plugin.coroutines.launch {
                delay(Random.nextDouble(3.0, 6.0).minutes)
                submit()
                delay(Random.nextDouble(0.0, 30.0).minutes)
                while (true) {
                    submit()
                    delay(30.minutes)
                }
            }
        }
    }

    private fun createConfig(): String {
        val uuid = UUID.randomUUID().toString()
        config.addDefault("enabled", true)
        config.addDefault("serverUuid", uuid)
        config.addDefault("logFailedRequests", false)
        config.addDefault("logSentData", false)
        config.addDefault("logResponseStatusText", false)
        config.options().header(
            """
            bStats collects some data for plugin authors like how many servers are using their plugins.
            To honor their work, you should not disable it.
            This has nearly no effect on the server performance!
            Check out https://bStats.org/ to learn more :)
            """.trimIndent()
        ).copyDefaults(true)
        try {
            config.save(configFile)
        } catch (_: IOException) {
        }
        return uuid
    }

    private fun collectData(): JsonObject = buildJsonObject {
        put("playerAmount", Bukkit.getOnlinePlayers().size)
        put("onlineMode", if (Bukkit.getOnlineMode()) 1 else 0)
        put("bukkitVersion", Bukkit.getVersion())
        put("bukkitName", Bukkit.getName())
        put("javaVersion", System.getProperty("java.version"))
        put("osName", System.getProperty("os.name"))
        put("osArch", System.getProperty("os.arch"))
        put("osVersion", System.getProperty("os.version"))
        put("coreCount", Runtime.getRuntime().availableProcessors())
        putJsonObject("service") {
            put("id", pluginId)
            put("pluginVersion", plugin.description.version)
            putJsonArray("customCharts") {}
        }
        put("serverUUID", serverUuid)
        put("metricsVersion", METRICS_VERSION)
    }

    private suspend fun submit() {
        val data = collectData().toString()
        if (logSentData) {
            plugin.logger.info("Sending data to bStats: $data")
        }
        withContext(ioDispatcher) {
            try {
                val response = httpClient.send(request(data), HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() !in 200..299) {
                    throw IOException("bStats responded with ${response.statusCode()}: ${response.body()}")
                }
                if (logResponseStatusText) {
                    plugin.logger.info("Sent data to bStats and received response: ${response.body()}")
                }
            } catch (e: IOException) {
                if (logFailedRequests) {
                    plugin.logger.log(Level.WARNING, "Could not submit plugin stats of ${plugin.name}", e)
                }
            }
        }
    }

    private companion object {
        const val METRICS_VERSION = "3.0.2"
        val REPORT_URI: URI = URI.create("https://bStats.org/api/v2/data/bukkit")
        val TIMEOUT: Duration = Duration.ofSeconds(10)

        val httpClient: HttpClient by lazy { HttpClient.newBuilder().connectTimeout(TIMEOUT).build() }

        fun request(json: String): HttpRequest = HttpRequest.newBuilder(REPORT_URI)
            .timeout(TIMEOUT)
            .header("Accept", "application/json")
            .header("Content-Encoding", "gzip")
            .header("Content-Type", "application/json")
            .header("User-Agent", "Metrics-Service/1")
            .POST(HttpRequest.BodyPublishers.ofByteArray(gzip(json)))
            .build()

        fun gzip(text: String): ByteArray = ByteArrayOutputStream()
            .also { out -> GZIPOutputStream(out).use { it.write(text.toByteArray()) } }
            .toByteArray()
    }
}
