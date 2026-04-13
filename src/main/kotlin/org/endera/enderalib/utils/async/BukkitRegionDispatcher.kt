package org.endera.enderalib.utils.async

import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.endera.enderalib.isFolia
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
class BukkitRegionDispatcher(private val plugin: Plugin, private val location: Location) : AbstractBukkitDispatcher(plugin) {
    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        if (isFolia) {
            !Bukkit.isOwnedByCurrentRegion(location)
        } else {
            !Bukkit.isPrimaryThread()
        }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (isFolia) {
            plugin.server.regionScheduler.execute(plugin, location, block)
        } else {
            runFallback(block)
        }
    }
}
