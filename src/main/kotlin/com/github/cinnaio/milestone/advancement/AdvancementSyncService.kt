package com.github.cinnaio.milestone.advancement

import com.github.cinnaio.milestone.core.Progress
import org.bukkit.entity.Player

interface AdvancementSyncService {
    fun sync(player: Player, progressList: Collection<Progress>)
    fun syncOne(player: Player, progress: Progress)
    fun init()
}
