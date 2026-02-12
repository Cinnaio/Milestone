package com.github.cinnaio.milestone.service

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import com.github.cinnaio.milestone.advancement.AdvancementSyncService
import com.github.cinnaio.milestone.config.MessageManager
import com.github.cinnaio.milestone.core.Milestone
import com.github.cinnaio.milestone.core.MilestoneType
import com.github.cinnaio.milestone.core.Progress
import com.github.cinnaio.milestone.storage.ProgressRepository
import com.github.cinnaio.milestone.util.FoliaExecutor
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MilestoneServiceImpl(
    private val plugin: Plugin,
    private val repository: ProgressRepository,
    private val advancementSync: AdvancementSyncService,
    private val messageManager: MessageManager
) : MilestoneService {

    private val milestones = ConcurrentHashMap<String, Milestone>()
    private val playerProgress = ConcurrentHashMap<UUID, MutableMap<String, Progress>>()

    override fun registerMilestone(milestone: Milestone) {
        milestones[milestone.id] = milestone
    }

    override fun getMilestone(id: String): Milestone? {
        return milestones[id]
    }

    override fun getAllMilestones(): List<Milestone> {
        return milestones.values.toList()
    }

    override fun clearMilestones() {
        milestones.clear()
    }

    override fun loadPlayerData(playerId: UUID, playerName: String) {
        FoliaExecutor.supplyAsync(plugin) {
            repository.loadPlayerProgress(playerId)
        }.thenAccept { loaded ->
            val playerMap = ConcurrentHashMap(loaded)
            playerProgress[playerId] = playerMap
            
            val player = Bukkit.getPlayer(playerId)
            if (player != null) {
                advancementSync.sync(player, playerMap.values)
            }
        }
    }

    override fun unloadPlayerData(playerId: UUID) {
        savePlayerData(playerId)
        playerProgress.remove(playerId)
    }

    override fun savePlayerData(playerId: UUID) {
        val progressMap = playerProgress[playerId] ?: return
        val toSave = progressMap.values.toList()
        FoliaExecutor.runAsync(plugin) {
            repository.saveAll(toSave)
        }
    }

    private fun getOrCreateProgress(playerId: UUID, milestoneId: String): Progress {
        val map = playerProgress.computeIfAbsent(playerId) { ConcurrentHashMap() }
        val playerName = Bukkit.getPlayer(playerId)?.name ?: "Unknown"
        
        return map.getOrPut(milestoneId) {
            Progress(playerId, playerName, milestoneId)
        }
    }

    override fun grant(playerId: UUID, milestoneId: String) {
        val milestone = milestones[milestoneId] ?: return
        val progress = getOrCreateProgress(playerId, milestoneId)

        if (progress.isCompleted) return

        progress.isCompleted = true
        progress.completedTime = Instant.now()
        
        if (milestone.type is MilestoneType.Counter) {
            progress.currentCount = milestone.type.max
        }

        onProgressUpdated(playerId, progress)
    }

    override fun revoke(playerId: UUID, milestoneId: String) {
        val progress = getOrCreateProgress(playerId, milestoneId)
        progress.isCompleted = false
        progress.completedTime = null
        progress.currentCount = 0
        
        onProgressUpdated(playerId, progress)
    }

    override fun addProgress(playerId: UUID, milestoneId: String, amount: Int) {
        val milestone = milestones[milestoneId] ?: return
        val progress = getOrCreateProgress(playerId, milestoneId)

        if (progress.isCompleted) return
        
        val type = milestone.type
        if (type is MilestoneType.Counter) {
            progress.currentCount += amount
            if (progress.currentCount >= type.max) {
                progress.currentCount = type.max
                progress.isCompleted = true
                progress.completedTime = Instant.now()
            }
            onProgressUpdated(playerId, progress)
        } else if (type is MilestoneType.OneTime) {
             if (amount > 0) grant(playerId, milestoneId)
        }
    }

    override fun getProgress(playerId: UUID, milestoneId: String): Progress? {
        return playerProgress[playerId]?.get(milestoneId)
    }
    
    private fun onProgressUpdated(playerId: UUID, progress: Progress) {
        FoliaExecutor.runAsync(plugin) {
            repository.savePlayerProgress(progress)
        }
        
        val player = Bukkit.getPlayer(playerId)
        if (player != null) {
            advancementSync.syncOne(player, progress)
            
            if (progress.isCompleted) {
                val milestone = milestones[progress.milestoneId]
                if (milestone != null && milestone.announceToChat) {
                    broadcastCompletion(player, milestone)
                }
            }
        }
    }

    private fun broadcastCompletion(player: org.bukkit.entity.Player, milestone: Milestone) {
        // Broadcast to all online players with their locale
        for (recipient in Bukkit.getOnlinePlayers()) {
            val message = messageManager.getComponent(recipient, "broadcast_completion", mapOf(
                "player" to player.name,
                "milestone" to milestone.title,
                "description" to milestone.description.joinToString("<newline>")
            ))
            recipient.sendMessage(message)
        }
        
        val console = Bukkit.getConsoleSender()
        val consoleMsg = messageManager.getComponent(console, "broadcast_completion", mapOf(
            "player" to player.name,
            "milestone" to milestone.title,
            "description" to milestone.description.joinToString("<newline>")
        ))
        console.sendMessage(consoleMsg)
    }
}
