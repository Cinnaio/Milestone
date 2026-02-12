package com.github.cinnaio.milestone.config

import org.bukkit.configuration.file.YamlConfiguration
import com.github.cinnaio.milestone.util.ColorUtil
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

class MessageManager(private val plugin: Plugin) {

    private val messages = mutableMapOf<String, MutableMap<String, String>>()
    private val defaultLocale = "en_us"

    fun reload() {
        messages.clear()
        val file = File(plugin.dataFolder, "messages.yml")
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        
        val config = YamlConfiguration.loadConfiguration(file)
        for (lang in config.getKeys(false)) {
            val section = config.getConfigurationSection(lang) ?: continue
            val langMap = mutableMapOf<String, String>()
            for (key in section.getKeys(true)) {
                 if (section.isString(key)) {
                     langMap[key] = section.getString(key)!!
                 }
            }
            messages[lang.lowercase()] = langMap
        }
        plugin.logger.info("Loaded messages for languages: ${messages.keys}")
    }

    fun get(sender: CommandSender?, key: String, placeholders: Map<String, String> = emptyMap()): String {
        // Determine locale
        val player = sender as? Player
        val locale = player?.locale?.lowercase() ?: defaultLocale
        
        // Try exact match (e.g. en_us)
        // Then try language only (e.g. en)
        // Then default
        val langMap = messages[locale] 
            ?: messages[locale.split("_")[0]] 
            ?: messages[defaultLocale]
            ?: emptyMap()
            
        var message = langMap[key] ?: key
        
        // Apply placeholders
        for ((k, v) in placeholders) {
            message = message.replace("{$k}", v)
        }
        
        // Translate colors, handling console downsampling if needed
        return ColorUtil.translate(message, sender)
    }
    
    fun get(key: String, placeholders: Map<String, String> = emptyMap()): String {
        return get(null, key, placeholders)
    }

    fun getComponent(sender: CommandSender?, key: String, placeholders: Map<String, String> = emptyMap()): Component {
        // Determine locale
        val player = sender as? Player
        val locale = player?.locale?.lowercase() ?: defaultLocale
        
        // Try exact match (e.g. en_us)
        // Then try language only (e.g. en)
        // Then default
        val langMap = messages[locale] 
            ?: messages[locale.split("_")[0]] 
            ?: messages[defaultLocale]
            ?: emptyMap()
            
        var message = langMap[key] ?: key
        
        // Apply placeholders
        for ((k, v) in placeholders) {
            message = message.replace("{$k}", v)
        }
        
        // Parse with MiniMessage
        return ColorUtil.parse(message)
    }
}
