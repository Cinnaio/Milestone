package com.github.cinnaio.milestone.trigger

enum class TriggerType {
    JOIN,
    BLOCK_BREAK,
    BLOCK_PLACE,
    KILL_ENTITY,
    CONSUME_ITEM,
    CRAFT_ITEM,
    UNKNOWN
}

data class MilestoneTrigger(
    val type: TriggerType,
    val value: String? = null, // Material name, EntityType name, etc.
    val progress: Int = 0 // Progress to add per event. If 0, use event's natural amount (or 1).
)