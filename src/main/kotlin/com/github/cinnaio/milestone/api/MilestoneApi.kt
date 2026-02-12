package com.github.cinnaio.milestone.api

import com.github.cinnaio.milestone.core.Progress
import java.util.UUID

interface MilestoneApi {
    fun grant(playerId: UUID, milestoneId: String)
    fun revoke(playerId: UUID, milestoneId: String)
    fun addProgress(playerId: UUID, milestoneId: String, amount: Int)
    fun getProgress(playerId: UUID, milestoneId: String): Progress?
}
