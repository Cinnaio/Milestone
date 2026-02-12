package com.github.cinnaio.milestone.core

import java.time.Instant
import java.util.UUID

data class Progress(
    val playerId: UUID,
    val playerName: String,
    val milestoneId: String,
    var currentCount: Int = 0,
    var isCompleted: Boolean = false,
    var completedTime: Instant? = null
)
