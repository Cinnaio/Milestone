package com.github.cinnaio.milestone.core

import org.bukkit.Material

import com.github.cinnaio.milestone.trigger.MilestoneTrigger

data class Milestone(
    val id: String, // Namespaced ID e.g. "namespace:id"
    val parentId: String? = null,
    val type: MilestoneType,
    val icon: Material,
    val title: String,
    val description: List<String>,
    val seasonId: String? = null,
    val isVisible: Boolean = true,
    val rewards: List<String> = emptyList(), // Command strings or identifiers
    val trigger: MilestoneTrigger? = null,
    val category: String? = null,
    val showToast: Boolean = true,
    val announceToChat: Boolean = true
) {
    fun getNamespace(): String {
        return if (id.contains(":")) id.split(":")[0] else "milestone"
    }

    fun getKey(): String {
        return if (id.contains(":")) id.split(":")[1] else id
    }
}
