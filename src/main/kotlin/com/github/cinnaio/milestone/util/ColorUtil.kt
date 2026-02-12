package com.github.cinnaio.milestone.util

import net.md_5.bungee.api.ChatColor
import java.util.regex.Pattern

object ColorUtil {
    // Hex Color Patterns
    private val MINI_MESSAGE_PATTERN = Pattern.compile("<color:#([A-Fa-f0-9]{6})>")
    private val HEX_AMP_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})")
    private val HEX_BRACE_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})}")

    // Define Theme Colors
    val PRIMARY = ChatColor.of("#E6E6E6")   // 浅灰白
    val SECONDARY = ChatColor.of("#A0A0A0") // 深灰
    val HIGHLIGHT = ChatColor.of("#FFD479") // 暖金
    val SUCCESS = ChatColor.of("#6BFF95")   // 柔绿
    val ERROR = ChatColor.of("#FF6B6B")     // 柔红

    fun translate(message: String): String {
        var msg = message
        
        // Match <color:#RRGGBB>
        var matcher = MINI_MESSAGE_PATTERN.matcher(msg)
        while (matcher.find()) {
            val hex = matcher.group(1)
            msg = msg.replace(matcher.group(), ChatColor.of("#$hex").toString())
        }
        
        // Match &#RRGGBB
        matcher = HEX_AMP_PATTERN.matcher(msg)
        while (matcher.find()) {
            val hex = matcher.group(1)
            msg = msg.replace(matcher.group(), ChatColor.of("#$hex").toString())
        }
        
        // Match {#RRGGBB}
        matcher = HEX_BRACE_PATTERN.matcher(msg)
        while (matcher.find()) {
            val hex = matcher.group(1)
            msg = msg.replace(matcher.group(), ChatColor.of("#$hex").toString())
        }
        
        return ChatColor.translateAlternateColorCodes('&', msg)
    }
}