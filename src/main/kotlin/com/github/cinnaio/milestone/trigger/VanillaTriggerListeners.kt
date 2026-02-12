package com.github.cinnaio.milestone.trigger

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.inventory.CraftItemEvent
import com.github.cinnaio.milestone.service.MilestoneService
import org.bukkit.entity.Player

class VanillaTriggerListeners(private val milestoneService: MilestoneService) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        milestoneService.loadPlayerData(event.player.uniqueId, event.player.name)
        checkTriggers(event.player, TriggerType.JOIN)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        milestoneService.unloadPlayerData(event.player.uniqueId)
    }
    
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        checkTriggers(event.player, TriggerType.BLOCK_BREAK, event.block.type.name)
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        checkTriggers(event.player, TriggerType.BLOCK_PLACE, event.block.type.name)
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        checkTriggers(killer, TriggerType.KILL_ENTITY, event.entityType.name)
    }

    @EventHandler
    fun onConsume(event: PlayerItemConsumeEvent) {
        checkTriggers(event.player, TriggerType.CONSUME_ITEM, event.item.type.name)
    }
    
    @EventHandler
    fun onCraft(event: CraftItemEvent) {
        val whoClicked = event.whoClicked
        if (whoClicked is Player) {
            val result = event.recipe.result
            checkTriggers(whoClicked, TriggerType.CRAFT_ITEM, result.type.name, result.amount)
        }
    }

    private fun checkTriggers(player: Player, type: TriggerType, value: String? = null, eventAmount: Int = 1) {
        val milestones = milestoneService.getAllMilestones()
        
        for (milestone in milestones) {
            val trigger = milestone.trigger ?: continue
            
            if (trigger.type == type) {
                // Check Value Match (if defined)
                if (trigger.value != null && value != null) {
                    // Simple wildcard check or exact match
                    if (trigger.value != "*" && !trigger.value.equals(value, ignoreCase = true)) {
                        continue
                    }
                }
                
                // Determine Progress to Add
                // If trigger.progress is set (>0), use it as fixed increment.
                // Otherwise, use the event's natural amount (e.g. crafted count, or 1 for block break).
                val progressToAdd = if (trigger.progress > 0) trigger.progress else eventAmount
                
                milestoneService.addProgress(player.uniqueId, milestone.id, progressToAdd)
                
                // If it's OneTime and no counter max, addProgress might not complete it immediately 
                // unless we handle it. But assuming Counter logic handles "completion".
                // For simple OneTime triggers without counter, we can just grant.
                if (milestone.type !is com.github.cinnaio.milestone.core.MilestoneType.Counter) {
                     milestoneService.grant(player.uniqueId, milestone.id)
                }
            }
        }
    }
}
