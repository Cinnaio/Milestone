package com.github.cinnaio.milestone.service

import com.github.cinnaio.milestone.api.MilestoneApi
import com.github.cinnaio.milestone.core.Milestone
import java.util.UUID

interface MilestoneService : MilestoneApi {
    fun registerMilestone(milestone: Milestone)
    fun getMilestone(id: String): Milestone?
    fun getAllMilestones(): List<Milestone>
    fun clearMilestones()
    
    fun loadPlayerData(playerId: UUID, playerName: String): java.util.concurrent.CompletableFuture<Void>
    fun unloadPlayerData(playerId: UUID)
    fun savePlayerData(playerId: UUID)
}
