package org.endera.enderalib.utils.async

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.Plugin
import org.endera.enderalib.isFolia
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class EntitySchedulerRetiredException(entity: Entity) :
    CancellationException("Entity ${entity.uniqueId} retired before the scheduled task could run")

class PluginCoroutineCancelledException(plugin: Plugin) :
    CancellationException("Coroutine scope for plugin ${plugin.name} was cancelled")

class PluginCoroutines internal constructor(
    private val plugin: Plugin
) : CoroutineScope {
    val io: CoroutineDispatcher = ioDispatcher
    val main: CoroutineDispatcher = BukkitDispatcher(plugin)
    private val job = SupervisorJob()
    private val disposed = AtomicBoolean(false)
    private val disableListener = object : Listener {
        @EventHandler
        fun onPluginDisable(event: PluginDisableEvent) {
            if (event.plugin === plugin) {
                dispose(PluginCoroutineCancelledException(plugin))
            }
        }
    }

    override val coroutineContext: CoroutineContext =
        job + main + CoroutineName("${plugin.name}-coroutines")

    init {
        plugin.server.pluginManager.registerEvents(disableListener, plugin)
    }

    fun region(location: Location): CoroutineDispatcher =
        BukkitRegionDispatcher(plugin, location)

    suspend fun <T> withIo(block: suspend () -> T): T =
        withContext(io) {
            block()
        }

    suspend fun <T> withMain(block: suspend () -> T): T =
        withContext(main) {
            block()
        }

    suspend fun <T> withRegion(location: Location, block: suspend () -> T): T =
        withContext(region(location)) {
            block()
        }

    suspend fun <T> withEntity(entity: Entity, block: () -> T): T =
        suspendCancellableCoroutine { continuation ->
            val task = Runnable {
                if (!continuation.isActive) {
                    return@Runnable
                }

                try {
                    continuation.resume(block())
                } catch (throwable: Throwable) {
                    continuation.resumeWithException(throwable)
                }
            }

            if (isFolia) {
                if (Bukkit.isOwnedByCurrentRegion(entity)) {
                    task.run()
                    return@suspendCancellableCoroutine
                }

                val scheduled = entity.scheduler.execute(
                    plugin,
                    task,
                    {
                        if (continuation.isActive) {
                            continuation.cancel(EntitySchedulerRetiredException(entity))
                        }
                    },
                    0L
                )

                if (!scheduled && continuation.isActive) {
                    continuation.cancel(EntitySchedulerRetiredException(entity))
                }

                return@suspendCancellableCoroutine
            }

            if (Bukkit.isPrimaryThread()) {
                task.run()
            } else {
                Bukkit.getScheduler().runTask(plugin, task)
            }
        }

    fun launchIo(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = launch(context = io, start = start, block = block)

    fun launchMain(
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = launch(context = main, start = start, block = block)

    fun launchRegion(
        location: Location,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = launch(context = region(location), start = start, block = block)

    fun launchEntity(
        entity: Entity,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: () -> Unit
    ): Job = launch(start = start) {
        withEntity(entity, block)
    }

    fun cancel() {
        dispose(PluginCoroutineCancelledException(plugin))
    }

    private fun dispose(cause: CancellationException) {
        if (!disposed.compareAndSet(false, true)) {
            return
        }

        HandlerList.unregisterAll(disableListener)
        pluginCoroutineCache.remove(plugin, this)
        job.cancel(cause)
    }
}

private val pluginCoroutineCache = ConcurrentHashMap<Plugin, PluginCoroutines>()

val Plugin.coroutines: PluginCoroutines
    get() = pluginCoroutineCache.computeIfAbsent(this) {
        PluginCoroutines(this)
    }
