package com.github.cinnaio.milestone.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import com.github.cinnaio.milestone.config.MessageManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import net.kyori.adventure.title.Title
import java.time.Duration

class RewardExecutor(private val plugin: Plugin, private val messageManager: MessageManager) {

    private var economy: Economy? = null
    
    private val isFolia: Boolean by lazy {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    init {
        setupEconomy()
    }

    private fun setupEconomy() {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            return
        }
        val rsp = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp != null) {
            economy = rsp.provider
        }
    }

    fun execute(player: Player, rewards: List<String>) {
        for (rewardLine in rewards) {
            try {
                processReward(player, rewardLine)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to execute reward '$rewardLine' for player ${player.name}: ${e.message}")
            }
        }
    }

    private fun processReward(player: Player, line: String) {
        val trimmed = line.trim()
        val content = if (trimmed.contains(" ")) trimmed.substring(trimmed.indexOf(" ") + 1).trim() else ""
        
        // Replace placeholders
        val processedContent = content.replace("%player%", player.name)

        when {
            trimmed.startsWith("[player]", true) -> {
                executePlayerCommand(player, processedContent)
            }
            trimmed.startsWith("[console]", true) -> {
                if (isFolia) {
                    plugin.logger.warning("Console commands [console] are disabled on Folia for safety.")
                } else {
                    executeConsoleCommand(processedContent)
                }
            }
            trimmed.startsWith("[op]", true) -> {
                executeOpCommand(player, processedContent)
            }
            trimmed.startsWith("[message]", true) -> {
                sendMessage(player, processedContent)
            }
            trimmed.startsWith("[broadcast]", true) -> {
                broadcastMessage(processedContent)
            }
            trimmed.startsWith("[title]", true) -> {
                sendTitle(player, processedContent)
            }
            trimmed.startsWith("[potion]", true) -> {
                givePotion(player, processedContent)
            }
            trimmed.startsWith("[money]", true) -> {
                giveMoney(player, processedContent)
            }
            else -> {
                // Default to console command
                if (isFolia) {
                    plugin.logger.warning("Console commands (default) are disabled on Folia for safety.")
                } else {
                    executeConsoleCommand(trimmed.replace("%player%", player.name))
                }
            }
        }
    }

    private fun executePlayerCommand(player: Player, command: String) {
        player.performCommand(command)
    }

    private fun executeConsoleCommand(command: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }

    private fun executeOpCommand(player: Player, command: String) {
        val wasOp = player.isOp
        try {
            player.isOp = true
            player.performCommand(command)
        } finally {
            player.isOp = wasOp
        }
    }

    private fun sendMessage(player: Player, message: String) {
        // Use MessageManager to parse colors/MiniMessage
        // Since MessageManager.getComponent handles raw strings if key not found:
        val component = messageManager.getComponent(player, message) 
        player.sendMessage(component)
    }

    private fun broadcastMessage(message: String) {
        // Broadcast to all online players
        for (recipient in Bukkit.getOnlinePlayers()) {
            val component = messageManager.getComponent(recipient, message)
            recipient.sendMessage(component)
        }
        // And console
        val console = Bukkit.getConsoleSender()
        val consoleMsg = messageManager.getComponent(console, message)
        console.sendMessage(consoleMsg)
    }

    private fun sendTitle(player: Player, args: String) {
        // Format: title;subtitle
        val parts = args.split(";")
        val titleText = parts.getOrElse(0) { "" }
        val subtitleText = parts.getOrElse(1) { "" }
        
        val titleComponent = messageManager.getComponent(player, titleText)
        val subtitleComponent = messageManager.getComponent(player, subtitleText)
        
        val title = Title.title(titleComponent, subtitleComponent)
        player.showTitle(title)
    }

    private fun givePotion(player: Player, args: String) {
        // Format: effect:duration(seconds):amplifier
        // e.g. SPEED:60:1
        val parts = args.split(":")
        if (parts.isEmpty()) return

        val typeName = parts[0].uppercase()
        val type = PotionEffectType.getByName(typeName) ?: return
        
        val durationSeconds = parts.getOrElse(1) { "30" }.toIntOrNull() ?: 30
        val amplifier = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
        
        player.addPotionEffect(PotionEffect(type, durationSeconds * 20, amplifier))
    }

    private fun giveMoney(player: Player, args: String) {
        if (economy == null) {
            plugin.logger.warning("Cannot give money: Vault economy not found!")
            return
        }
        val amount = args.toDoubleOrNull()
        if (amount != null && amount > 0) {
            economy?.depositPlayer(player, amount)
        }
    }
}
