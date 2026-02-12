package com.github.cinnaio.milestone.util

import net.md_5.bungee.api.ChatColor
import org.bukkit.command.CommandSender
import java.util.regex.Pattern
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

object ColorUtil {
    // Hex Color Patterns
    private val MINI_MESSAGE_HEX = Pattern.compile("<(?:color:)?(#(?:[A-Fa-f0-9]{6}))>") // Matches <color:#RRGGBB> or <#RRGGBB>
    private val MINI_MESSAGE_NAMED = Pattern.compile("<([a-z_]+)>") // Matches <red>, <dark_blue>, etc.
    private val HEX_AMP_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})")
    private val HEX_BRACE_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})}")

    private val miniMessage = MiniMessage.miniMessage()

    // Define Theme Colors as Strings to be parsed later
    const val PRIMARY = "<color:#E6E6E6>"   // 浅灰白
    const val SECONDARY = "<color:#A0A0A0>" // 深灰
    const val HIGHLIGHT = "<color:#FFD479>" // 暖金
    const val SUCCESS = "<color:#6BFF95>"   // 柔绿
    const val ERROR = "<color:#FF6B6B>"     // 柔红

    fun parse(message: String): Component {
        var msg = message
        // Preprocess custom formats to standard MiniMessage
        // <color:#RRGGBB> -> <#RRGGBB>
        val matcher = MINI_MESSAGE_HEX.matcher(msg)
        val sb = StringBuffer()
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<${matcher.group(1)}>")
        }
        matcher.appendTail(sb)
        msg = sb.toString()

        // Handle Legacy &#RRGGBB -> <#RRGGBB>
        msg = HEX_AMP_PATTERN.matcher(msg).replaceAll("<#$1>")
        
        // Handle {#RRGGBB} -> <#RRGGBB>
        msg = HEX_BRACE_PATTERN.matcher(msg).replaceAll("<#$1>")
        
        // Handle Legacy &c -> <red> (Basic mapping, or rely on LegacyComponentSerializer if needed)
        // For now, let's assume users migrate to MiniMessage or we just support MiniMessage tags primarily.
        // If we want to support & codes, we should use LegacyComponentSerializer to deserialize first, then MiniMessage?
        // Actually, mixing is hard. Let's just do basic & color replacement to MiniMessage tags if we want to be nice.
        // But keeping it simple: Just pass to MiniMessage after fixing our custom hex tags.
        
        // Note: MiniMessage is strict. If it sees &c, it treats it as literal text.
        // If we want to support &c, we should replace it.
        msg = msg.replace("&0", "<black>")
            .replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>")
            .replace("&5", "<dark_purple>")
            .replace("&6", "<gold>")
            .replace("&7", "<gray>")
            .replace("&8", "<dark_gray>")
            .replace("&9", "<blue>")
            .replace("&a", "<green>")
            .replace("&b", "<aqua>")
            .replace("&c", "<red>")
            .replace("&d", "<light_purple>")
            .replace("&e", "<yellow>")
            .replace("&f", "<white>")
            .replace("&k", "<obfuscated>")
            .replace("&l", "<bold>")
            .replace("&m", "<strikethrough>")
            .replace("&n", "<underlined>")
            .replace("&o", "<italic>")
            .replace("&r", "<reset>")

        return miniMessage.deserialize(msg)
    }

    fun translate(message: String, sender: CommandSender? = null): String {
        var msg = message
        
        // Match <color:#RRGGBB> or <#RRGGBB>
        var matcher = MINI_MESSAGE_HEX.matcher(msg)
        while (matcher.find()) {
            val hex = matcher.group(1)
            msg = msg.replace(matcher.group(), ChatColor.of(hex).toString())
        }
        
        // Match <name> (basic support for standard colors)
        matcher = MINI_MESSAGE_NAMED.matcher(msg)
        while (matcher.find()) {
            val name = matcher.group(1)
            try {
                // Try to map <red> -> ChatColor.RED
                val color = ChatColor.valueOf(name.uppercase())
                msg = msg.replace(matcher.group(), color.toString())
            } catch (ignored: IllegalArgumentException) {
                // Not a valid color name, ignore (could be other tags like <bold>)
            }
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
        
        msg = ChatColor.translateAlternateColorCodes('&', msg)

        return msg
    }
}