package com.github.cinnaio.milestone.papi

import com.github.cinnaio.milestone.Milestone
import com.github.cinnaio.milestone.core.MilestoneType
import com.github.cinnaio.milestone.service.LeaderboardManager
import com.github.cinnaio.milestone.service.MilestoneService
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import kotlin.math.ceil

class MilestoneExpansion(
    private val plugin: Milestone,
    private val service: MilestoneService,
    private val leaderboard: LeaderboardManager
) : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "milestone"
    }

    override fun getAuthor(): String {
        return plugin.description.authors.joinToString(", ")
    }

    override fun getVersion(): String {
        return plugin.description.version
    }

    override fun persist(): Boolean {
        return true
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        // Global Placeholders (Leaderboards)
        if (params.startsWith("top_")) {
            // weekly/monthly/total
            val type = when {
                params.contains("_weekly_") -> "weekly"
                params.contains("_monthly_") -> "monthly"
                params.contains("_total_") -> "total"
                else -> return null
            }
            
            val list = when (type) {
                "weekly" -> leaderboard.weeklyTop
                "monthly" -> leaderboard.monthlyTop
                "total" -> leaderboard.totalTop
                else -> emptyList()
            }
            
            // name/score
            val isName = params.contains("_name_")
            val isScore = params.contains("_score_")
            
            if (!isName && !isScore) return null
            
            // Extract rank
            val rankStr = params.substringAfterLast("_")
            val rank = rankStr.toIntOrNull() ?: return null
            
            if (rank < 1 || rank > list.size) return if (isName) "---" else "0"
            
            val entry = list[rank - 1]
            return if (isName) entry.first else entry.second.toString()
        }
        
        // Percent names
        if (params.contains("_top_percent_names_")) {
            val type = when {
                params.startsWith("weekly_") -> "weekly"
                params.startsWith("monthly_") -> "monthly"
                else -> return null
            }
            
            val percentStr = params.substringAfterLast("_")
            val percent = percentStr.toIntOrNull() ?: return null
            if (percent <= 0 || percent > 100) return ""
            
            val list = if (type == "weekly") leaderboard.weeklyTop else leaderboard.monthlyTop
            val total = if (type == "weekly") leaderboard.weeklyTotalPlayers else leaderboard.monthlyTotalPlayers
            
            if (total == 0) return ""
            
            val limit = ceil(total * (percent / 100.0)).toInt()
            val safeLimit = limit.coerceAtMost(list.size)
            
            return list.take(safeLimit).joinToString(", ") { it.first }
        }

        if (player == null) return null

        // %milestone_status_<id>%
        if (params.startsWith("status_")) {
            val id = params.substring("status_".length)
            val progress = service.getProgress(player.uniqueId, id)
            return if (progress?.isCompleted == true) "completed" else "incomplete"
        }
        
        // %milestone_is_completed_<id>%
        if (params.startsWith("is_completed_")) {
            val id = params.substring("is_completed_".length)
            val progress = service.getProgress(player.uniqueId, id)
            return (progress?.isCompleted == true).toString()
        }

        // %milestone_current_<id>%
        if (params.startsWith("current_")) {
            val id = params.substring("current_".length)
            val progress = service.getProgress(player.uniqueId, id)
            return (progress?.currentCount ?: 0).toString()
        }
        
        // %milestone_max_<id>%
        if (params.startsWith("max_")) {
            val id = params.substring("max_".length)
            val milestone = service.getMilestone(id) ?: return "0"
            return when (val type = milestone.type) {
                is MilestoneType.Counter -> type.max.toString()
                else -> "1"
            }
        }
        
        // %milestone_percent_<id>%
        if (params.startsWith("percent_")) {
            val id = params.substring("percent_".length)
            val milestone = service.getMilestone(id) ?: return "0"
            val progress = service.getProgress(player.uniqueId, id)
            
            if (progress?.isCompleted == true) return "100"
            
            val current = progress?.currentCount ?: 0
            val max = when (val type = milestone.type) {
                is MilestoneType.Counter -> type.max
                else -> 1
            }
            
            if (max == 0) return "0"
            return ((current.toDouble() / max.toDouble()) * 100).toInt().toString()
        }

        return null
    }
}
