package org.endera.enderalib.utils.async

import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import org.endera.enderalib.isFolia
import java.util.concurrent.RejectedExecutionException
import kotlin.coroutines.CoroutineContext

@Deprecated(
    message = "Entity scheduler execution can be rejected when the entity retires. Use Entity.withScheduler(plugin) instead so retirement can be handled explicitly."
)
@Suppress("unused")
class BukkitEntityDispatcher(private val plugin: Plugin, private val entity: Entity) : AbstractBukkitDispatcher(plugin) {
    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        if (isFolia) {
            !Bukkit.isOwnedByCurrentRegion(entity)
        } else {
            !Bukkit.isPrimaryThread()
        }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (isFolia) {
            throw RejectedExecutionException(
                "BukkitEntityDispatcher cannot safely dispatch on Folia. Use Entity.withScheduler(plugin) instead."
            )
        } else {
            runFallback(block)
        }
    }
}
