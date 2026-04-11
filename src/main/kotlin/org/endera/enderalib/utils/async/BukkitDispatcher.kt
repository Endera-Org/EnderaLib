package org.endera.enderalib.utils.async

import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.endera.enderalib.isFolia
import kotlin.coroutines.CoroutineContext

/**
 * A dispatcher that handles task execution on Bukkit servers.
 *
 * <p>This class extends {@link AbstractBukkitDispatcher} and provides a specialized
 * implementation of the dispatch mechanism for Bukkit servers. It determines whether
 * the server is running on Folia, and if so, executes the given block on the global
 * region scheduler. Otherwise, it falls back to the default execution method.
 *
 * @param plugin The plugin instance associated with this dispatcher.
 */
class BukkitDispatcher(private val plugin: Plugin) : AbstractBukkitDispatcher(plugin) {
    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        if (isFolia) {
            !Bukkit.isGlobalTickThread()
        } else {
            !Bukkit.isPrimaryThread()
        }

    /**
     * Dispatches a runnable block of code to be executed on the appropriate scheduler.
     *
     * <p>If the server is running Folia, the block is submitted to the global region scheduler.
     * Otherwise, it uses a fallback (Bukkit) execution mechanism.
     *
     * @param context the coroutine context in which the dispatch is executed.
     * @param block   the runnable block of code to execute.
     */
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (isFolia) {
            plugin.server.globalRegionScheduler.execute(plugin, block)
        } else {
            runFallback(block)
        }
    }
}
