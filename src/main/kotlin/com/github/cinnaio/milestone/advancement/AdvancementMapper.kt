package com.github.cinnaio.milestone.advancement

import org.bukkit.NamespacedKey
import com.github.cinnaio.milestone.core.Milestone
import com.github.cinnaio.milestone.core.MilestoneType
import com.github.cinnaio.milestone.util.ColorUtil

object AdvancementMapper {

    fun toAdvancementJson(milestone: Milestone): String {
        val icon = milestone.icon.name.lowercase()
        val title = ColorUtil.translate(milestone.title)
        val description = ColorUtil.translate(milestone.description.joinToString("\n"))
        val frame = when(milestone.type) {
             is MilestoneType.OneTime -> "task"
             is MilestoneType.Counter -> "goal"
             is MilestoneType.MultiCondition -> "challenge"
             else -> "task"
        }
        
        // Root advancements MUST have a background
        val isRoot = milestone.parentId == null
        val parentJson = if (!isRoot) {
            "\"parent\": \"${milestone.parentId}\","
        } else {
            ""
        }
        
        val backgroundJson = if (isRoot) {
            "\"background\": \"minecraft:textures/gui/advancements/backgrounds/stone.png\","
        } else {
            ""
        }
        
        val safeTitle = title.replace("\"", "\\\"")
        val safeDesc = description.replace("\"", "\\\"")

        return """
        {
            "display": {
                "icon": {
                    "id": "minecraft:$icon"
                },
                "title": "$safeTitle",
                "description": "$safeDesc",
                $backgroundJson
                "frame": "$frame",
                "show_toast": ${milestone.showToast},
                "announce_to_chat": false,
                "hidden": ${!milestone.isVisible}
            },
            $parentJson
            "criteria": {
                "completed": {
                    "trigger": "minecraft:impossible"
                }
            }
        }
        """.trimIndent()
    }
    
    fun getNamespacedKey(milestone: Milestone): NamespacedKey {
        val parts = milestone.id.split(":")
        return if (parts.size > 1) {
            NamespacedKey(parts[0], parts[1])
        } else {
            NamespacedKey("milestone", milestone.id)
        }
    }
}
