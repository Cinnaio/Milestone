package com.github.cinnaio.milestone.advancement

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import com.github.cinnaio.milestone.util.FoliaExecutor

class VanillaBlocker(private val plugin: Plugin) {
    
    enum class Mode {
        DISABLE_VANILLA,
        HYBRID,
        VANILLA_ONLY
    }

    fun apply(mode: Mode, blockedNamespaces: List<String>, blockedIds: List<String>, debug: Boolean) {
        if (mode == Mode.VANILLA_ONLY) {
            if (debug) plugin.logger.info("[VanillaBlocker] Mode is VANILLA_ONLY. No advancements will be blocked.")
            return
        }
        
        FoliaExecutor.runGlobal(plugin) {
             val iterator = Bukkit.advancementIterator()
             val toRemove = mutableListOf<NamespacedKey>()
             
             if (debug) plugin.logger.info("[VanillaBlocker] Starting advancement scan. Mode: $mode")
             
             while (iterator.hasNext()) {
                 val adv = iterator.next()
                 val key = adv.key
                 
                 // Skip our own advancements
                 if (key.namespace == "milestone") continue
                 
                 val shouldBlock = (mode == Mode.DISABLE_VANILLA) ||
                                   blockedNamespaces.contains(key.namespace) ||
                                   blockedIds.contains(key.toString())
                 
                 if (shouldBlock) {
                     toRemove.add(key)
                 }
             }
             
             var blockedCount = 0
             for (key in toRemove) {
                 try {
                     @Suppress("DEPRECATION")
                     Bukkit.getUnsafe().removeAdvancement(key)
                     blockedCount++
                     if (debug) {
                         plugin.logger.info("[VanillaBlocker] Blocked advancement: $key")
                     }
                 } catch (e: Exception) {
                     plugin.logger.warning("[VanillaBlocker] Failed to block advancement: $key")
                     e.printStackTrace()
                 }
             }
             
             if (debug) {
                 plugin.logger.info("[VanillaBlocker] Total blocked advancements: $blockedCount")
             }
        }
    }
}
