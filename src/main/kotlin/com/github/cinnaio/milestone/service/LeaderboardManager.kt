package com.github.cinnaio.milestone.service

import com.github.cinnaio.milestone.storage.ProgressRepository
import com.github.cinnaio.milestone.util.FoliaExecutor
import org.bukkit.plugin.Plugin
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class LeaderboardManager(
    private val plugin: Plugin,
    private val repository: ProgressRepository
) {
    @Volatile
    var weeklyTop: List<Pair<String, Int>> = emptyList()
        private set
        
    @Volatile
    var monthlyTop: List<Pair<String, Int>> = emptyList()
        private set
        
    @Volatile
    var totalTop: List<Pair<String, Int>> = emptyList()
        private set
        
    @Volatile
    var weeklyTotalPlayers: Int = 0
        private set
        
    @Volatile
    var monthlyTotalPlayers: Int = 0
        private set

    fun start() {
        // Run immediately then every 5 minutes
        FoliaExecutor.runTimerAsync(plugin, 20L, 20L * 60 * 5) {
            refresh()
        }
    }
    
    private fun refresh() {
        try {
            val now = LocalDate.now()
            val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val firstDay = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val epoch = Instant.EPOCH
            
            // Fetch top 100 for cache
            weeklyTop = repository.getTopPlayers(monday, 100)
            monthlyTop = repository.getTopPlayers(firstDay, 100)
            totalTop = repository.getTopPlayers(epoch, 100)
            
            weeklyTotalPlayers = repository.getPlayerCountWithCompletions(monday)
            monthlyTotalPlayers = repository.getPlayerCountWithCompletions(firstDay)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to refresh leaderboard cache: ${e.message}")
            e.printStackTrace()
        }
    }
}
