package com.github.cinnaio.milestone

import org.bukkit.plugin.java.JavaPlugin
import com.github.cinnaio.milestone.advancement.AdvancementPacketBlocker
import com.github.cinnaio.milestone.advancement.AdvancementSyncServiceImpl
import com.github.cinnaio.milestone.advancement.VanillaBlocker
import com.github.cinnaio.milestone.command.MilestoneCommand
import com.github.cinnaio.milestone.config.MessageManager
import com.github.cinnaio.milestone.config.MilestoneLoader
import com.github.cinnaio.milestone.service.MilestoneService
import com.github.cinnaio.milestone.service.MilestoneServiceImpl
import com.github.cinnaio.milestone.storage.MySqlRepository
import com.github.cinnaio.milestone.storage.ProgressRepository
import com.github.cinnaio.milestone.storage.SqliteRepository
import com.github.cinnaio.milestone.trigger.VanillaTriggerListeners
import com.github.cinnaio.milestone.util.RewardExecutor
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
    lateinit var messageManager: MessageManager
        private set

    override fun onEnable() {
        saveDefaultConfig()
        
        // Initialize MessageManager
        messageManager = MessageManager(this)
        messageManager.reload()

        val console = server.consoleSender
        val debug = config.getBoolean("debug", false)
        val debugLogs = if (debug) mutableListOf<String>() else null
        
        // --- Startup Info ---
        val description = description
        
        console.sendMessage(" ")
        console.sendMessage(ColorUtil.translate("${ColorUtil.HIGHLIGHT}${description.name} ${ColorUtil.PRIMARY}v${description.version}", console))
        console.sendMessage(ColorUtil.translate("${ColorUtil.SECONDARY}Running on ${ColorUtil.PRIMARY}${server.version} (MC: ${server.bukkitVersion})", console))
        console.sendMessage(" ")
        
        if (server.pluginManager.getPlugin("ProtocolLib") != null) {
             console.sendMessage(ColorUtil.translate("${ColorUtil.SUCCESS}ProtocolLib was found - Enabling capabilities.", console))
        } else {
             console.sendMessage(ColorUtil.translate("${ColorUtil.ERROR}ProtocolLib not found! Packet blocking features will be disabled.", console))
        }
        
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
             console.sendMessage(ColorUtil.translate("${ColorUtil.SUCCESS}PlaceholderAPI was found - Enabling placeholders.", console))
        }
        
        val dbType = config.getString("database.type", "sqlite")?.uppercase()
        console.sendMessage(ColorUtil.translate("${ColorUtil.HIGHLIGHT}Database: ${ColorUtil.PRIMARY}$dbType", console))
        console.sendMessage(" ")
        
        // --- Database Initialization ---
        console.sendMessage(ColorUtil.translate("${ColorUtil.PRIMARY}Database starting...", console))
        
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
            console.sendMessage(ColorUtil.translate("${ColorUtil.PRIMARY}Database completed.", console))
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
        console.sendMessage(ColorUtil.translate("${ColorUtil.PRIMARY}Loading milestones...", console))
        // --------------------

        advancementSync = AdvancementSyncServiceImpl(this)
        val rewardExecutor = RewardExecutor(this, messageManager)
        service = MilestoneServiceImpl(this, repository, advancementSync, messageManager, rewardExecutor)
        
        // Load milestones from config
        loadMilestones(debug, debugLogs)
        
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
        getCommand("milestone")?.setExecutor(MilestoneCommand(this, service, messageManager))
        
        console.sendMessage(" ")
        console.sendMessage(ColorUtil.translate("${ColorUtil.SUCCESS}Milestone plugin enabled!", console))
        
        if (debug && debugLogs != null) {
            console.sendMessage(ColorUtil.translate("${ColorUtil.HIGHLIGHT}Debug mode enabled.", console))
            for (log in debugLogs) {
                console.sendMessage(log)
            }
        }
    }

    override fun onDisable() {
        if (::repository.isInitialized) {
            repository.shutdown()
        }
        server.consoleSender.sendMessage(ColorUtil.translate("${ColorUtil.SUCCESS}Milestone plugin disabled!", server.consoleSender))
    }

    fun reload() {
        reloadConfig()
        val debug = config.getBoolean("debug", false)
        val console = server.consoleSender
        
        // Reload Messages
        messageManager.reload()
        
        // Reload Blocker
        val modeStr = config.getString("advancement.mode", "HYBRID")!!
        val mode = VanillaBlocker.Mode.valueOf(modeStr)
        val blockedNs = config.getStringList("advancement.block-namespaces")
        val blockedIds = config.getStringList("advancement.block-ids")
        
        packetBlocker.init(mode, blockedNs, blockedIds, debug)
        
        console.sendMessage(ColorUtil.translate("${ColorUtil.SUCCESS}Milestone configuration reloaded.", console))
    }

    private fun loadMilestones(debug: Boolean, debugLogs: MutableList<String>? = null) {
        val loader = MilestoneLoader(this)
        val milestonesDir = File(dataFolder, "milestones")
        val milestones = loader.loadAll(milestonesDir)
        
        for (milestone in milestones) {
            service.registerMilestone(milestone)
            if (debug) {
                val log = ColorUtil.translate("${ColorUtil.SECONDARY}[Milestone] Registered milestone: ${ColorUtil.HIGHLIGHT}${milestone.id} ${ColorUtil.SECONDARY}(Type: ${milestone.type})", server.consoleSender)
                if (debugLogs != null) {
                    debugLogs.add(log)
                } else {
                    server.consoleSender.sendMessage(log)
                }
            }
        }
        
        server.consoleSender.sendMessage(ColorUtil.translate("${ColorUtil.SUCCESS}Loaded ${milestones.size} milestones in total.", server.consoleSender))
    }
}
