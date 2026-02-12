package com.github.cinnaio.milestone.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import com.github.cinnaio.milestone.Milestone
import com.github.cinnaio.milestone.service.MilestoneService
import com.github.cinnaio.milestone.config.MessageManager
import java.util.Collections
import org.bukkit.entity.Player

class MilestoneCommand(
    private val plugin: Milestone,
    private val service: MilestoneService,
    private val messageManager: MessageManager
) : CommandExecutor, TabCompleter {
    
    private fun msg(sender: CommandSender, key: String, placeholders: Map<String, String> = emptyMap()) {
        val prefix = messageManager.get(sender, "prefix")
        val message = messageManager.get(sender, key, placeholders)
        sender.sendMessage(prefix + message)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("milestone.admin")) {
                    msg(sender, "no_permission")
                    return true
                }
                plugin.reload()
                msg(sender, "reload_success")
            }
            "grant" -> {
                if (!sender.hasPermission("milestone.admin")) return true
                if (args.size < 3) return false
                val player = Bukkit.getPlayer(args[1]) ?: return true
                val milestoneId = args[2]
                service.grant(player.uniqueId, milestoneId)
                msg(sender, "grant_success", mapOf("milestone" to milestoneId, "player" to player.name))
            }
            "revoke" -> {
                if (!sender.hasPermission("milestone.admin")) return true
                if (args.size < 3) return false
                val player = Bukkit.getPlayer(args[1]) ?: return true
                val milestoneId = args[2]
                service.revoke(player.uniqueId, milestoneId)
                msg(sender, "revoke_success", mapOf("milestone" to milestoneId, "player" to player.name))
            }
            "progress" -> {
                if (args.size < 3) return false
                val player = Bukkit.getPlayer(args[1]) ?: return true
                val milestoneId = args[2]
                val progress = service.getProgress(player.uniqueId, milestoneId)
                val current = progress?.currentCount?.toString() ?: "0"
                val completed = progress?.isCompleted?.toString() ?: "false"
                msg(sender, "progress_info", mapOf(
                    "player" to player.name,
                    "milestone" to milestoneId,
                    "current" to current,
                    "completed" to completed
                ))
            }
            else -> sendHelp(sender)
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.isEmpty()) return Collections.emptyList()

        if (args.size == 1) {
            val subcommands = mutableListOf("progress")
            if (sender.hasPermission("milestone.admin")) {
                subcommands.add("reload")
                subcommands.add("grant")
                subcommands.add("revoke")
            }
            return subcommands.filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size == 2) {
            // Player name completion
            if (args[0].equals("grant", ignoreCase = true) || 
                args[0].equals("revoke", ignoreCase = true) || 
                args[0].equals("progress", ignoreCase = true)) {
                return null // Return null to use default player completion
            }
        }

        if (args.size == 3) {
            // Milestone ID completion
            if (args[0].equals("grant", ignoreCase = true) || 
                args[0].equals("revoke", ignoreCase = true) || 
                args[0].equals("progress", ignoreCase = true)) {
                return service.getAllMilestones().map { it.id }.filter { it.startsWith(args[2]) }
            }
        }

        return Collections.emptyList()
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage(messageManager.getComponent(sender, "command_help_header"))
        sender.sendMessage(messageManager.getComponent(sender, "command_help_progress"))
        if (sender.hasPermission("milestone.admin")) {
            sender.sendMessage(messageManager.getComponent(sender, "command_help_reload"))
            sender.sendMessage(messageManager.getComponent(sender, "command_help_grant"))
            sender.sendMessage(messageManager.getComponent(sender, "command_help_revoke"))
        }
    }
}
