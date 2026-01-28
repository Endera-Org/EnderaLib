package org.endera.enderalib.utils.async

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.endera.enderalib.isFolia

/**
 * A wrapper for scheduled tasks that works on both Folia and Paper/Spigot.
 * Provides a unified interface for cancelling tasks regardless of the server implementation.
 */
sealed interface ScheduledTaskHandle {
    fun cancel()
    val isCancelled: Boolean
}

@JvmInline
value class FoliaTaskHandle(private val task: ScheduledTask) : ScheduledTaskHandle {
    override fun cancel() { task.cancel() }
    override val isCancelled: Boolean get() = task.isCancelled
}

@JvmInline
value class BukkitTaskHandle(private val task: BukkitTask) : ScheduledTaskHandle {
    override fun cancel() = task.cancel()
    override val isCancelled: Boolean get() = task.isCancelled
}

/**
 * Global scheduler utilities for Folia/Paper compatibility.
 * Use these for tasks that don't belong to any specific region or entity.
 * Examples: world day time, weather cycle, console commands, global state updates.
 */
object GlobalScheduler {

    /**
     * Runs a task on the global region scheduler (Folia) or main thread (Paper/Spigot).
     */
    fun run(plugin: Plugin, task: Runnable): ScheduledTaskHandle {
        return if (isFolia) {
            FoliaTaskHandle(plugin.server.globalRegionScheduler.run(plugin) { task.run() })
        } else {
            BukkitTaskHandle(Bukkit.getScheduler().runTask(plugin, task))
        }
    }

    /**
     * Runs a task after a delay on the global region scheduler (Folia) or main thread (Paper/Spigot).
     * @param delayTicks Delay in ticks (20 ticks = 1 second)
     */
    fun runLater(plugin: Plugin, delayTicks: Long, task: Runnable): ScheduledTaskHandle {
        return if (isFolia) {
            FoliaTaskHandle(plugin.server.globalRegionScheduler.runDelayed(plugin, { task.run() }, delayTicks))
        } else {
            BukkitTaskHandle(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks))
        }
    }

    /**
     * Runs a repeating task on the global region scheduler (Folia) or main thread (Paper/Spigot).
     * @param delayTicks Initial delay in ticks before first execution
     * @param periodTicks Period in ticks between executions
     */
    fun runTimer(plugin: Plugin, delayTicks: Long, periodTicks: Long, task: Runnable): ScheduledTaskHandle {
        return if (isFolia) {
            FoliaTaskHandle(plugin.server.globalRegionScheduler.runAtFixedRate(plugin, { task.run() }, delayTicks, periodTicks))
        } else {
            BukkitTaskHandle(Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks))
        }
    }
}

/**
 * Region scheduler utilities for Folia/Paper compatibility.
 * Use these for tasks that operate on a specific location/chunk.
 * Examples: block changes, explosions, world modifications at a location.
 */
object RegionScheduler {

    /**
     * Runs a task on the region that owns the specified location.
     */
    fun run(plugin: Plugin, location: Location, task: Runnable): ScheduledTaskHandle {
        return if (isFolia) {
            FoliaTaskHandle(plugin.server.regionScheduler.run(plugin, location) { task.run() })
        } else {
            BukkitTaskHandle(Bukkit.getScheduler().runTask(plugin, task))
        }
    }

    /**
     * Runs a task after a delay on the region that owns the specified location.
     * @param delayTicks Delay in ticks (20 ticks = 1 second)
     */
    fun runLater(plugin: Plugin, location: Location, delayTicks: Long, task: Runnable): ScheduledTaskHandle {
        return if (isFolia) {
            FoliaTaskHandle(plugin.server.regionScheduler.runDelayed(plugin, location, { task.run() }, delayTicks))
        } else {
            BukkitTaskHandle(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks))
        }
    }

    /**
     * Runs a repeating task on the region that owns the specified location.
     * @param delayTicks Initial delay in ticks before first execution
     * @param periodTicks Period in ticks between executions
     */
    fun runTimer(plugin: Plugin, location: Location, delayTicks: Long, periodTicks: Long, task: Runnable): ScheduledTaskHandle {
        return if (isFolia) {
            FoliaTaskHandle(plugin.server.regionScheduler.runAtFixedRate(plugin, location, { task.run() }, delayTicks, periodTicks))
        } else {
            BukkitTaskHandle(Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks))
        }
    }

    /**
     * Executes a task immediately if on the correct region thread, otherwise schedules it.
     * This is useful when you're not sure if you're already on the correct thread.
     */
    fun execute(plugin: Plugin, location: Location, task: Runnable) {
        if (isFolia) {
            plugin.server.regionScheduler.execute(plugin, location, task)
        } else {
            if (Bukkit.isPrimaryThread()) {
                task.run()
            } else {
                Bukkit.getScheduler().runTask(plugin, task)
            }
        }
    }
}

/**
 * Entity scheduler utilities for Folia/Paper compatibility.
 * Use these for tasks that operate on a specific entity.
 * The task will follow the entity across region boundaries in Folia.
 * Examples: entity AI, player effects, entity teleportation, inventory operations.
 */
object EntityScheduler {

    /**
     * Runs a task on the entity's scheduler.
     * @param retired Called if the entity is removed before the task runs (Folia only, nullable)
     */
    fun run(plugin: Plugin, entity: Entity, task: Runnable, retired: Runnable? = null): Boolean {
        return if (isFolia) {
            entity.scheduler.run(plugin, { task.run() }, retired) != null
        } else {
            Bukkit.getScheduler().runTask(plugin, task)
            true
        }
    }

    /**
     * Runs a task after a delay on the entity's scheduler.
     * @param delayTicks Delay in ticks (20 ticks = 1 second)
     * @param retired Called if the entity is removed before the task runs (Folia only, nullable)
     */
    fun runLater(plugin: Plugin, entity: Entity, delayTicks: Long, task: Runnable, retired: Runnable? = null): Boolean {
        return if (isFolia) {
            entity.scheduler.runDelayed(plugin, { task.run() }, retired, delayTicks) != null
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks)
            true
        }
    }

    /**
     * Runs a repeating task on the entity's scheduler.
     * @param delayTicks Initial delay in ticks before first execution
     * @param periodTicks Period in ticks between executions
     * @param retired Called if the entity is removed (Folia only, nullable)
     */
    fun runTimer(plugin: Plugin, entity: Entity, delayTicks: Long, periodTicks: Long, task: Runnable, retired: Runnable? = null): Boolean {
        return if (isFolia) {
            entity.scheduler.runAtFixedRate(plugin, { task.run() }, retired, delayTicks, periodTicks) != null
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks)
            true
        }
    }

    /**
     * Executes a task immediately if on the correct entity thread, otherwise schedules it.
     * @param retired Called if the entity is removed before the task runs (Folia only, nullable)
     */
    fun execute(plugin: Plugin, entity: Entity, task: Runnable, retired: Runnable? = null): Boolean {
        return if (isFolia) {
            entity.scheduler.execute(plugin, task, retired, 0L)
        } else {
            if (Bukkit.isPrimaryThread()) {
                task.run()
            } else {
                Bukkit.getScheduler().runTask(plugin, task)
            }
            true
        }
    }
}

/**
 * Async scheduler utilities for Folia/Paper compatibility.
 * Use these for tasks that don't interact with the game world directly.
 * Examples: database operations, file I/O, network requests, heavy computations.
 */
object AsyncScheduler {

    /**
     * Runs a task asynchronously.
     */
    fun run(plugin: Plugin, task: Runnable): ScheduledTaskHandle {
        return if (isFolia) {
            FoliaTaskHandle(plugin.server.asyncScheduler.runNow(plugin) { task.run() })
        } else {
            BukkitTaskHandle(Bukkit.getScheduler().runTaskAsynchronously(plugin, task))
        }
    }

    /**
     * Runs a task asynchronously after a delay.
     * @param delayTicks Delay in ticks (20 ticks = 1 second)
     */
    fun runLater(plugin: Plugin, delayTicks: Long, task: Runnable): ScheduledTaskHandle {
        return if (isFolia) {
            FoliaTaskHandle(plugin.server.asyncScheduler.runDelayed(plugin, { task.run() }, delayTicks * 50, java.util.concurrent.TimeUnit.MILLISECONDS))
        } else {
            BukkitTaskHandle(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks))
        }
    }

    /**
     * Runs a repeating task asynchronously.
     * @param delayTicks Initial delay in ticks before first execution
     * @param periodTicks Period in ticks between executions
     */
    fun runTimer(plugin: Plugin, delayTicks: Long, periodTicks: Long, task: Runnable): ScheduledTaskHandle {
        return if (isFolia) {
            FoliaTaskHandle(plugin.server.asyncScheduler.runAtFixedRate(plugin, { task.run() }, delayTicks * 50, periodTicks * 50, java.util.concurrent.TimeUnit.MILLISECONDS))
        } else {
            BukkitTaskHandle(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks))
        }
    }
}

// Extension functions for idiomatic Kotlin usage

/**
 * Runs a task on the global scheduler with lambda syntax.
 */
inline fun Plugin.runGlobal(crossinline task: () -> Unit): ScheduledTaskHandle =
    GlobalScheduler.run(this) { task() }

/**
 * Runs a task after a delay on the global scheduler with lambda syntax.
 */
inline fun Plugin.runGlobalLater(delayTicks: Long, crossinline task: () -> Unit): ScheduledTaskHandle =
    GlobalScheduler.runLater(this, delayTicks) { task() }

/**
 * Runs a repeating task on the global scheduler with lambda syntax.
 */
inline fun Plugin.runGlobalTimer(delayTicks: Long, periodTicks: Long, crossinline task: () -> Unit): ScheduledTaskHandle =
    GlobalScheduler.runTimer(this, delayTicks, periodTicks) { task() }

/**
 * Runs a task asynchronously with lambda syntax.
 */
inline fun Plugin.runAsync(crossinline task: () -> Unit): ScheduledTaskHandle =
    AsyncScheduler.run(this) { task() }

/**
 * Runs a task asynchronously after a delay with lambda syntax.
 */
inline fun Plugin.runAsyncLater(delayTicks: Long, crossinline task: () -> Unit): ScheduledTaskHandle =
    AsyncScheduler.runLater(this, delayTicks) { task() }

/**
 * Runs a repeating task asynchronously with lambda syntax.
 */
inline fun Plugin.runAsyncTimer(delayTicks: Long, periodTicks: Long, crossinline task: () -> Unit): ScheduledTaskHandle =
    AsyncScheduler.runTimer(this, delayTicks, periodTicks) { task() }

/**
 * Runs a task on the entity's scheduler with lambda syntax.
 */
inline fun Entity.runTask(plugin: Plugin, noinline retired: (() -> Unit)? = null, crossinline task: () -> Unit): Boolean =
    EntityScheduler.run(plugin, this, { task() }, retired?.let { Runnable { it() } })

/**
 * Runs a task after a delay on the entity's scheduler with lambda syntax.
 */
inline fun Entity.runTaskLater(plugin: Plugin, delayTicks: Long, noinline retired: (() -> Unit)? = null, crossinline task: () -> Unit): Boolean =
    EntityScheduler.runLater(plugin, this, delayTicks, { task() }, retired?.let { Runnable { it() } })

/**
 * Runs a repeating task on the entity's scheduler with lambda syntax.
 */
inline fun Entity.runTaskTimer(plugin: Plugin, delayTicks: Long, periodTicks: Long, noinline retired: (() -> Unit)? = null, crossinline task: () -> Unit): Boolean =
    EntityScheduler.runTimer(plugin, this, delayTicks, periodTicks, { task() }, retired?.let { Runnable { it() } })

/**
 * Runs a task on the region scheduler with lambda syntax.
 */
inline fun Location.runTask(plugin: Plugin, crossinline task: () -> Unit): ScheduledTaskHandle =
    RegionScheduler.run(plugin, this) { task() }

/**
 * Runs a task after a delay on the region scheduler with lambda syntax.
 */
inline fun Location.runTaskLater(plugin: Plugin, delayTicks: Long, crossinline task: () -> Unit): ScheduledTaskHandle =
    RegionScheduler.runLater(plugin, this, delayTicks) { task() }

/**
 * Runs a repeating task on the region scheduler with lambda syntax.
 */
inline fun Location.runTaskTimer(plugin: Plugin, delayTicks: Long, periodTicks: Long, crossinline task: () -> Unit): ScheduledTaskHandle =
    RegionScheduler.runTimer(plugin, this, delayTicks, periodTicks) { task() }
