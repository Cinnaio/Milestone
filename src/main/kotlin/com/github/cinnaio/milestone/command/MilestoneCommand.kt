package com.github.cinnaio.milestone.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import com.github.cinnaio.milestone.Milestone
import com.github.cinnaio.milestone.service.MilestoneService
import java.util.Collections

import com.github.cinnaio.milestone.util.ColorUtil

class MilestoneCommand(
    private val plugin: Milestone,
    private val service: MilestoneService
) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("milestone.admin")) {
                    sender.sendMessage("${ColorUtil.ERROR}You do not have permission to use this command.")
                    return true
                }
                plugin.reload()
                sender.sendMessage("${ColorUtil.SUCCESS}Milestone reloaded successfully.")
            }
            "grant" -> {
                if (!sender.hasPermission("milestone.admin")) return true
                if (args.size < 3) return false
                val player = Bukkit.getPlayer(args[1]) ?: return true
                val milestoneId = args[2]
                service.grant(player.uniqueId, milestoneId)
                sender.sendMessage("${ColorUtil.SUCCESS}Granted ${ColorUtil.HIGHLIGHT}$milestoneId ${ColorUtil.SUCCESS}to ${ColorUtil.PRIMARY}${player.name}")
            }
            "revoke" -> {
                if (!sender.hasPermission("milestone.admin")) return true
                if (args.size < 3) return false
                val player = Bukkit.getPlayer(args[1]) ?: return true
                val milestoneId = args[2]
                service.revoke(player.uniqueId, milestoneId)
                sender.sendMessage("${ColorUtil.SUCCESS}Revoked ${ColorUtil.HIGHLIGHT}$milestoneId ${ColorUtil.SUCCESS}from ${ColorUtil.PRIMARY}${player.name}")
            }
            "progress" -> {
                if (args.size < 3) return false
                val player = Bukkit.getPlayer(args[1]) ?: return true
                val milestoneId = args[2]
                val progress = service.getProgress(player.uniqueId, milestoneId)
                sender.sendMessage("${ColorUtil.PRIMARY}Progress for ${ColorUtil.HIGHLIGHT}${player.name} ${ColorUtil.PRIMARY}on ${ColorUtil.HIGHLIGHT}$milestoneId${ColorUtil.PRIMARY}: ${ColorUtil.HIGHLIGHT}${progress?.currentCount} ${ColorUtil.SECONDARY}(Completed: ${progress?.isCompleted})")
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
        sender.sendMessage("${ColorUtil.HIGHLIGHT}Milestone Commands:")
        sender.sendMessage("${ColorUtil.HIGHLIGHT}/milestone progress <player> <milestone> ${ColorUtil.SECONDARY}- Check progress")
        if (sender.hasPermission("milestone.admin")) {
            sender.sendMessage("${ColorUtil.HIGHLIGHT}/milestone reload ${ColorUtil.SECONDARY}- Reload plugin")
            sender.sendMessage("${ColorUtil.HIGHLIGHT}/milestone grant <player> <milestone> ${ColorUtil.SECONDARY}- Grant milestone")
            sender.sendMessage("${ColorUtil.HIGHLIGHT}/milestone revoke <player> <milestone> ${ColorUtil.SECONDARY}- Revoke milestone")
        }
    }
}
