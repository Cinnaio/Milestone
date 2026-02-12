package com.github.cinnaio.milestone.config

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import com.github.cinnaio.milestone.core.Milestone
import com.github.cinnaio.milestone.core.MilestoneType
import java.io.File
import java.util.logging.Level

import com.github.cinnaio.milestone.trigger.MilestoneTrigger
import com.github.cinnaio.milestone.trigger.TriggerType

import com.github.cinnaio.milestone.util.ColorUtil
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration

class MilestoneLoader(private val plugin: Plugin) {
    
    // Store templates globally or reload them every time. 
    // Given the recursive loadAll, we can store them in a map during the process.
    private val templates = mutableMapOf<String, ConfigurationSection>()

    fun loadAll(directory: File): List<Milestone> {
        templates.clear()
        
        if (!directory.exists()) {
            directory.mkdirs()
            // Save example if empty
            if (directory.listFiles().isNullOrEmpty()) {
                // Check if example.yml exists in JAR before trying to save
                if (plugin.getResource("milestones/example.yml") != null) {
                    plugin.saveResource("milestones/example.yml", false)
                } else {
                    // Try to save new default files if available
                    if (plugin.getResource("milestones/newbie/newbie.yml") != null) {
                        plugin.saveResource("milestones/newbie/newbie.yml", false)
                    }
                    if (plugin.getResource("milestones/step_further/step_further.yml") != null) {
                         plugin.saveResource("milestones/step_further/step_further.yml", false)
                    }
                }
            }
        }
        
        // 1. First pass: Load all templates
        directory.walk().filter { it.isFile && it.name.endsWith(".yml") }.forEach { file ->
            try {
                loadTemplatesFromFile(file)
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "Failed to load templates from ${file.name}", e)
            }
        }

        val milestones = mutableListOf<Milestone>()
        // 2. Second pass: Load all milestones using templates
        directory.walk().filter { it.isFile && it.name.endsWith(".yml") }.forEach { file ->
            try {
                // Calculate relative path to determine default namespace
                val relativePath = file.relativeTo(directory).path
                val pathParts = relativePath.split(File.separatorChar)
                val defaultNamespace = if (pathParts.size > 1) pathParts[0] else null
                
                // Formatted Log Output
                val console = Bukkit.getConsoleSender()
                console.sendMessage("${ColorUtil.PRIMARY}  > Loading ${ColorUtil.HIGHLIGHT}${file.name} ${ColorUtil.SECONDARY}(Namespace: ${ColorUtil.HIGHLIGHT}${defaultNamespace ?: "milestone"}${ColorUtil.SECONDARY})")
                
                milestones.addAll(loadFromFile(file, defaultNamespace))
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "Failed to load milestones from ${file.name}", e)
            }
        }
        return milestones
    }

    private fun loadTemplatesFromFile(file: File) {
        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("templates") ?: return
        
        for (key in section.getKeys(false)) {
            val templateSection = section.getConfigurationSection(key)
            if (templateSection != null) {
                // Store template with its key (e.g., "newbie/test")
                templates[key] = templateSection
            }
        }
    }

    private fun loadFromFile(file: File, defaultNamespace: String?): List<Milestone> {
        val config = YamlConfiguration.loadConfiguration(file)
        val list = mutableListOf<Milestone>()
        val section = config.getConfigurationSection("milestones") ?: return emptyList()

        for (key in section.getKeys(false)) {
            val path = "milestones.$key"
            
            // Determine ID and Category based on default namespace if needed
            var id = key
            if (!id.contains(":") && defaultNamespace != null) {
                id = "$defaultNamespace:$key"
            }
            
            // --- Template Processing ---
            val milestoneSection = section.getConfigurationSection(key) ?: continue
            val templateId = milestoneSection.getString("template")
            
            // Create a merged configuration
            val mergedConfig: ConfigurationSection = MemoryConfiguration()
            
            if (templateId != null) {
                val template = templates[templateId]
                if (template != null) {
                    // 1. Copy template values
                    copySection(template, mergedConfig)
                    
                    // 2. Process variable substitution (augments)
                    val augments = milestoneSection.getConfigurationSection("augments")
                    if (augments != null) {
                        substituteVariables(mergedConfig, augments)
                    }
                    
                    // 3. Process overrides (from overrides section AND root overrides like parent)
                    // First, explicit overrides section
                    val overrides = milestoneSection.getConfigurationSection("overrides")
                    if (overrides != null) {
                        copySection(overrides, mergedConfig)
                    }
                    
                    // Then, milestone root properties that are NOT special keywords
                    // Special keywords to exclude from overwriting template properties:
                    // template, overrides, augments
                    copySection(milestoneSection, mergedConfig, ignoreKeys = setOf("template", "overrides", "augments"))
                    
                } else {
                    plugin.logger.warning("Template '$templateId' not found for milestone '$id'")
                    // Fallback to raw section
                    copySection(milestoneSection, mergedConfig)
                }
            } else {
                // No template, just use the section as is
                copySection(milestoneSection, mergedConfig)
            }
            
            // --- Parse from mergedConfig ---
            val parentIdRaw = mergedConfig.getString("parent")
            var parentId = parentIdRaw
            // If parent is not defined in mergedConfig (which includes template), try raw config
            if (parentId == null) {
                parentId = config.getString("$path.parent")
            }
            
            if (parentId != null && !parentId.contains(":") && defaultNamespace != null) {
                parentId = "$defaultNamespace:$parentId"
            }

            val typeStr = mergedConfig.getString("type", "ONE_TIME")!!.uppercase()
            val iconStr = mergedConfig.getString("icon", "STONE")!!.uppercase()
            val title = mergedConfig.getString("title", "Untitled")!!
            val description = mergedConfig.getStringList("description")
            val seasonId = mergedConfig.getString("season")
            val isVisible = mergedConfig.getBoolean("visible", true)
            val rewards = mergedConfig.getStringList("rewards")
            
            // Try to infer category from ID (namespace) if not provided
            var category = mergedConfig.getString("category")
            if (category == null && id.contains(":")) {
                category = id.split(":")[0]
            } else if (category == null && defaultNamespace != null) {
                category = defaultNamespace
            }
            
            // UI Control
            // Default chat to true if not specified
            val showToast = mergedConfig.getBoolean("show_toast", true)
            val announceToChat = mergedConfig.getBoolean("announce_to_chat", true)

            // Parse Trigger
            var trigger: MilestoneTrigger? = null
            if (mergedConfig.isConfigurationSection("trigger")) {
                val triggerTypeStr = mergedConfig.getString("trigger.type", "UNKNOWN")!!.uppercase()
                val triggerValue = mergedConfig.getString("trigger.value")
                
                // Read 'progress' or fallback to old 'amount' key for compatibility, default to 0
                val triggerProgress = if (mergedConfig.contains("trigger.progress")) {
                    mergedConfig.getInt("trigger.progress")
                } else {
                    mergedConfig.getInt("trigger.amount", 0)
                }
                
                val triggerType = try {
                    TriggerType.valueOf(triggerTypeStr)
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("Invalid trigger type $triggerTypeStr in $id")
                    TriggerType.UNKNOWN
                }
                
                trigger = MilestoneTrigger(triggerType, triggerValue, triggerProgress)
            }

            val type = when (typeStr) {
                "COUNTER" -> {
                    val max = mergedConfig.getInt("max", 1)
                    MilestoneType.Counter(max)
                }
                "MULTI_CONDITION" -> {
                    // TODO: Implement condition parsing if needed
                    MilestoneType.MultiCondition(emptyList())
                }
                "HIDDEN" -> MilestoneType.Hidden
                else -> MilestoneType.OneTime
            }

            val icon = try {
                Material.valueOf(iconStr)
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid material $iconStr in $id, defaulting to STONE")
                Material.STONE
            }

            list.add(Milestone(
                id = id,
                parentId = parentId,
                type = type,
                icon = icon,
                title = title,
                description = description,
                seasonId = seasonId,
                isVisible = isVisible,
                rewards = rewards,
                trigger = trigger,
                category = category,
                showToast = showToast,
                announceToChat = announceToChat
            ))
        }
        return list
    }

    private fun copySection(source: ConfigurationSection, target: ConfigurationSection, ignoreKeys: Set<String> = emptySet()) {
        for (key in source.getKeys(true)) { // Deep copy keys
             // Check if top-level key is ignored
             val rootKey = if (key.contains(".")) key.split(".")[0] else key
             if (ignoreKeys.contains(rootKey)) continue
             
             if (source.isConfigurationSection(key)) {
                 target.createSection(key)
             } else {
                 target.set(key, source.get(key))
             }
        }
    }
    
    private fun substituteVariables(section: ConfigurationSection, augments: ConfigurationSection) {
        // Collect replacements map
        val replacements = mutableMapOf<String, String>()
        for (key in augments.getKeys(false)) {
            replacements[key] = augments.getString(key, "")!!
        }
        
        // Iterate over all values in section and replace
        for (key in section.getKeys(true)) {
            if (section.isString(key)) {
                var value = section.getString(key)!!
                for ((varName, varValue) in replacements) {
                    value = value.replace("\${$varName}", varValue)
                }
                section.set(key, value)
            } else if (section.isList(key)) {
                // Handle string lists (e.g. description, rewards)
                val list = section.getList(key)
                val newList = mutableListOf<Any?>()
                
                if (list != null) {
                    for (item in list) {
                        if (item is String) {
                            var newValue: String = item
                            for ((varName, varValue) in replacements) {
                                newValue = newValue.replace("\${$varName}", varValue)
                            }
                            newList.add(newValue)
                        } else {
                            newList.add(item)
                        }
                    }
                    section.set(key, newList)
                }
            }
        }
    }
}
