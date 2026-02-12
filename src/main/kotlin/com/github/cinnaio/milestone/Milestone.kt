package com.github.cinnaio.milestone

import org.bukkit.plugin.java.JavaPlugin
import com.github.cinnaio.milestone.advancement.AdvancementPacketBlocker
import com.github.cinnaio.milestone.advancement.AdvancementSyncServiceImpl
import com.github.cinnaio.milestone.advancement.VanillaBlocker
import com.github.cinnaio.milestone.command.MilestoneCommand
import com.github.cinnaio.milestone.config.MilestoneLoader
import com.github.cinnaio.milestone.service.MilestoneService
import com.github.cinnaio.milestone.service.MilestoneServiceImpl
import com.github.cinnaio.milestone.storage.MySqlRepository
import com.github.cinnaio.milestone.storage.ProgressRepository
import com.github.cinnaio.milestone.storage.SqliteRepository
import com.github.cinnaio.milestone.trigger.VanillaTriggerListeners
import java.io.File
import java.util.logging.Level

import com.github.cinnaio.milestone.util.ColorUtil
import org.apache.logging.log4j.Level as Log4jLevel
import org.apache.logging.log4j.core.config.Configurator
import java.util.logging.Logger

class Milestone : JavaPlugin() {

    private lateinit var repository: ProgressRepository
    private lateinit var service: MilestoneService
    private lateinit var advancementSync: AdvancementSyncServiceImpl
    private lateinit var packetBlocker: AdvancementPacketBlocker
    private lateinit var vanillaBlocker: VanillaBlocker

    override fun onEnable() {
        saveDefaultConfig()
        
        // --- Startup Info ---
        val description = description
        val console = server.consoleSender
        
        console.sendMessage(" ")
        console.sendMessage("${ColorUtil.HIGHLIGHT}${description.name} ${ColorUtil.PRIMARY}v${description.version}")
        console.sendMessage("${ColorUtil.SECONDARY}Running on ${server.version} (MC: ${server.bukkitVersion})")
        console.sendMessage(" ")
        
        if (server.pluginManager.getPlugin("ProtocolLib") != null) {
             console.sendMessage("${ColorUtil.SUCCESS}ProtocolLib was found - Enabling capabilities.")
        } else {
             console.sendMessage("${ColorUtil.ERROR}ProtocolLib not found! Packet blocking features will be disabled.")
        }
        
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
             console.sendMessage("${ColorUtil.SUCCESS}PlaceholderAPI was found - Enabling placeholders.")
        }
        
        val dbType = config.getString("database.type", "sqlite")?.uppercase()
        console.sendMessage("${ColorUtil.HIGHLIGHT}Database: ${ColorUtil.PRIMARY}$dbType")
        console.sendMessage(" ")
        
        val debug = config.getBoolean("debug", false)
        if (debug) {
            logger.info("Debug mode enabled.")
        }
        
        // --- Database Initialization ---
        console.sendMessage("${ColorUtil.PRIMARY}Database starting...")
        
        // Suppress HikariCP logs (using Log4j2)
        Configurator.setLevel("com.zaxxer.hikari", Log4jLevel.ERROR)

        // Initialize Database
        try {
            val dbTypeLower = config.getString("database.type", "sqlite")?.lowercase()
            if (dbTypeLower == "mysql") {
                val dbConfig = config.getConfigurationSection("database") 
                    ?: throw IllegalArgumentException("Database configuration missing for MySQL")
                repository = MySqlRepository(dbConfig, logger)
            } else {
                val dataFolder = dataFolder.absolutePath
                repository = SqliteRepository(dataFolder, logger)
            }
            repository.init()
            console.sendMessage("${ColorUtil.PRIMARY}Database completed.")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to initialize database", e)
            server.pluginManager.disablePlugin(this)
            return
        } finally {
             // Restore log level (Optional: Keep ERROR if you want to stay silent, or restore to INFO)
             Configurator.setLevel("com.zaxxer.hikari", Log4jLevel.INFO)
        }
        
        console.sendMessage(" ")
        
        // --- Loading Milestones ---
        console.sendMessage("${ColorUtil.PRIMARY}Loading milestones...")
        // --------------------

        advancementSync = AdvancementSyncServiceImpl(this)
        service = MilestoneServiceImpl(this, repository, advancementSync)
        
        // Load milestones from config
        loadMilestones(debug)
        
        // Register advancements after loading milestones
        advancementSync.registerMilestones(service.getAllMilestones())
        
        // Initialize Blocker Mode
        val modeStr = config.getString("advancement.mode", "HYBRID")!!
        val mode = VanillaBlocker.Mode.valueOf(modeStr)
        val blockedNs = config.getStringList("advancement.block-namespaces")
        val blockedIds = config.getStringList("advancement.block-ids")
        
        // 1. Initialize Packet Blocker (Client-side suppression)
        packetBlocker = AdvancementPacketBlocker(this, service)
        packetBlocker.init(mode, blockedNs, blockedIds, debug)
        
        // 2. Initialize Vanilla Blocker (Server-side suppression)
        // User requested NOT to remove advancements server-side, just hide them.
        // So we disable VanillaBlocker application.
        // vanillaBlocker = VanillaBlocker(this)
        // vanillaBlocker.apply(mode, blockedNs, blockedIds, debug)

        server.pluginManager.registerEvents(VanillaTriggerListeners(service), this)
        getCommand("milestone")?.setExecutor(MilestoneCommand(this, service))
        
        console.sendMessage(" ")
        console.sendMessage("${ColorUtil.SUCCESS}Milestone plugin enabled!")
    }

    override fun onDisable() {
        if (::repository.isInitialized) {
            repository.shutdown()
        }
        logger.info("Milestone plugin disabled!")
    }

    fun reload() {
        reloadConfig()
        val debug = config.getBoolean("debug", false)
        
        // Reload Milestones
        service.clearMilestones()
        loadMilestones(debug)
        
        // Re-register advancements (Sync)
        advancementSync.registerMilestones(service.getAllMilestones())
        
        // Reload Blocker
        val modeStr = config.getString("advancement.mode", "HYBRID")!!
        val mode = VanillaBlocker.Mode.valueOf(modeStr)
        val blockedNs = config.getStringList("advancement.block-namespaces")
        val blockedIds = config.getStringList("advancement.block-ids")
        
        packetBlocker.init(mode, blockedNs, blockedIds, debug)
        
        logger.info("Milestone configuration and milestones reloaded.")
    }

    private fun loadMilestones(debug: Boolean) {
        val loader = MilestoneLoader(this)
        val milestonesDir = File(dataFolder, "milestones")
        val milestones = loader.loadAll(milestonesDir)
        
        for (milestone in milestones) {
            service.registerMilestone(milestone)
            if (debug) {
                logger.info("Registered milestone: ${milestone.id} (Type: ${milestone.type})")
            }
        }
        
        server.consoleSender.sendMessage("${ColorUtil.SUCCESS}Loaded ${milestones.size} milestones in total.")
    }
}
