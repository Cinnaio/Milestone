package com.github.cinnaio.milestone.advancement

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import com.github.cinnaio.milestone.core.Milestone
import com.github.cinnaio.milestone.core.Progress
import com.github.cinnaio.milestone.util.FoliaExecutor
import java.util.concurrent.ConcurrentHashMap

class AdvancementSyncServiceImpl(
    private val plugin: Plugin
) : AdvancementSyncService {

    private val registeredKeys = ConcurrentHashMap.newKeySet<NamespacedKey>()
    private val milestoneIdToKey = ConcurrentHashMap<String, NamespacedKey>()

    override fun init() {
    }

    fun registerMilestones(milestones: List<Milestone>) {
        milestoneIdToKey.clear() // Clear old mapping
        FoliaExecutor.runGlobal(plugin) {
            // Unregister previously registered keys
            val it = registeredKeys.iterator()
            while (it.hasNext()) {
                val key = it.next()
                try {
                    @Suppress("DEPRECATION")
                    Bukkit.getUnsafe().removeAdvancement(key)
                } catch (e: Exception) {
                    // Ignore
                }
                it.remove()
            }

            // Sort milestones
            val sortedMilestones = sortMilestonesTopologically(milestones)
            
            for (milestone in sortedMilestones) {
                val key = AdvancementMapper.getNamespacedKey(milestone)
                milestoneIdToKey[milestone.id] = key
                val json = AdvancementMapper.toAdvancementJson(milestone)
                
                try {
                    // Always try to remove first to ensure clean state
                    try {
                        @Suppress("DEPRECATION")
                        Bukkit.getUnsafe().removeAdvancement(key)
                    } catch (e: Exception) {
                        // Ignore
                    }
                    
                    @Suppress("DEPRECATION")
                    Bukkit.getUnsafe().loadAdvancement(key, json)
                    registeredKeys.add(key)
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to register milestone advancement: ${milestone.id}")
                    e.printStackTrace()
                }
            }
            
            // Reload data to ensure clients update? 
            // Bukkit.reloadData() is too heavy.
            // Usually remove+load is enough for new advancements to appear.
        }
    }

    private fun sortMilestonesTopologically(milestones: List<Milestone>): List<Milestone> {
        val idMap = milestones.associateBy { it.id }
        val visited = HashSet<String>()
        val result = ArrayList<Milestone>()

        fun visit(milestone: Milestone) {
            if (visited.contains(milestone.id)) return

            if (milestone.parentId != null) {
                val parent = idMap[milestone.parentId]
                if (parent != null) {
                    visit(parent)
                }
            }
            
            visited.add(milestone.id)
            result.add(milestone)
        }

        for (milestone in milestones) {
            visit(milestone)
        }

        return result
    }

    override fun sync(player: Player, progressList: Collection<Progress>) {
        // Create a set of completed milestone IDs for fast lookup
        val completedIds = progressList.filter { it.isCompleted }.map { it.milestoneId }.toSet()

        FoliaExecutor.runPlayer(plugin, player) {
            // Iterate over ALL registered milestones to ensure state consistency
            // This ensures that if a milestone is NOT in the completed list (e.g. database reset),
            // it will be revoked from the vanilla advancement system.
            for ((id, key) in milestoneIdToKey) {
                if (id in completedIds) {
                    grantCriteria(player, key)
                } else {
                    revokeCriteria(player, key)
                }
            }
        }
    }

    override fun syncOne(player: Player, progress: Progress) {
        FoliaExecutor.runPlayer(plugin, player) {
             val key = milestoneIdToKey[progress.milestoneId]
             if (key != null) {
                 if (progress.isCompleted) {
                    grantCriteria(player, key)
                } else {
                    revokeCriteria(player, key)
                }
             } else {
                 // Fallback if key not found in map (should not happen if registered correctly)
                 // But for safety, use the old logic or log warning
                 if (progress.isCompleted) {
                    grantCriteria(player, progress.milestoneId)
                } else {
                    revokeCriteria(player, progress.milestoneId)
                }
             }
        }
    }

    private fun grantCriteria(player: Player, key: NamespacedKey) {
        val adv = Bukkit.getAdvancement(key) ?: return
        val prog = player.getAdvancementProgress(adv)
        if (!prog.isDone) {
            prog.awardCriteria("completed")
        }
    }

    private fun revokeCriteria(player: Player, key: NamespacedKey) {
        val adv = Bukkit.getAdvancement(key) ?: return
        val prog = player.getAdvancementProgress(adv)
        if (prog.isDone) {
            prog.revokeCriteria("completed")
        }
    }

    private fun grantCriteria(player: Player, milestoneId: String) {
        val parts = milestoneId.split(":")
        val key = if (parts.size > 1) NamespacedKey(parts[0], parts[1]) else NamespacedKey("milestone", milestoneId)
        grantCriteria(player, key)
    }
    
    private fun revokeCriteria(player: Player, milestoneId: String) {
        val parts = milestoneId.split(":")
        val key = if (parts.size > 1) NamespacedKey(parts[0], parts[1]) else NamespacedKey("milestone", milestoneId)
        revokeCriteria(player, key)
    }
}
