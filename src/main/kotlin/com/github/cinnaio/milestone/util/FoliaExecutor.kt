package com.github.cinnaio.milestone.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

object FoliaExecutor {

    private fun isFolia(): Boolean {
        return try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    fun runAsync(plugin: Plugin, task: Runnable) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin) { task.run() }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        }
    }

    fun runGlobal(plugin: Plugin, task: Runnable) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(plugin) { task.run() }
        } else {
            Bukkit.getScheduler().runTask(plugin, task)
        }
    }

    fun runPlayer(plugin: Plugin, player: Player, task: Runnable) {
        if (isFolia()) {
            player.scheduler.run(plugin, Consumer { task.run() }, null)
        } else {
            Bukkit.getScheduler().runTask(plugin, task)
        }
    }

    fun <T> supplyAsync(plugin: Plugin, supplier: () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        runAsync(plugin) {
            try {
                future.complete(supplier())
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }
}
