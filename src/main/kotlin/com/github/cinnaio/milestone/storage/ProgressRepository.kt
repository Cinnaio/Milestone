package com.github.cinnaio.milestone.storage

import com.github.cinnaio.milestone.core.Progress
import java.util.UUID

interface ProgressRepository {
    fun init()
    fun shutdown()
    
    fun loadPlayerProgress(playerId: UUID): Map<String, Progress>
    fun savePlayerProgress(progress: Progress)
    fun saveAll(progressList: List<Progress>)
    
    fun getTopPlayers(after: java.time.Instant, limit: Int): List<Pair<String, Int>>
    fun getPlayerCountWithCompletions(after: java.time.Instant): Int
}
