package org.endera.enderalib.utils.async

import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin

@Suppress("unused")
suspend fun <T> withIo(block: suspend () -> T): T =
    withContext(ioDispatcher) {
        block()
    }

@Suppress("unused")
suspend fun <T> Plugin.withMain(block: suspend () -> T): T =
    coroutines.withMain(block)

@Suppress("unused")
suspend fun <T> Plugin.withRegion(location: Location, block: suspend () -> T): T =
    coroutines.withRegion(location, block)

@Suppress("unused")
suspend fun <T> Entity.withScheduler(plugin: Plugin, block: () -> T): T =
    plugin.coroutines.withEntity(this, block)

@Suppress("unused")
suspend fun <T> Entity.withScheduler(coroutines: PluginCoroutines, block: () -> T): T =
    coroutines.withEntity(this, block)
