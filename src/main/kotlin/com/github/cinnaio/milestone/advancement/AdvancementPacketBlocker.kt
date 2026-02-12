package com.github.cinnaio.milestone.advancement

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.plugin.Plugin
import com.github.cinnaio.milestone.service.MilestoneService
import java.util.HashMap

class AdvancementPacketBlocker(private val plugin: Plugin, private val service: MilestoneService) {

    private var mode: VanillaBlocker.Mode = VanillaBlocker.Mode.HYBRID
    private var blockedNamespaces: List<String> = emptyList()
    private var blockedIds: List<String> = emptyList()
    private var debug: Boolean = false

    fun init(mode: VanillaBlocker.Mode, blockedNamespaces: List<String>, blockedIds: List<String>, debug: Boolean) {
        this.mode = mode
        this.blockedNamespaces = blockedNamespaces
        this.blockedIds = blockedIds
        this.debug = debug

        if (mode == VanillaBlocker.Mode.VANILLA_ONLY) return

        val manager = ProtocolLibrary.getProtocolManager()

        // Block UI Packets
        manager.addPacketListener(
            object : PacketAdapter(plugin, PacketType.Play.Server.ADVANCEMENTS) {
                override fun onPacketSending(event: PacketEvent) {
                    try {
                        handleAdvancementPacket(event)
                    } catch (e: Exception) {
                        if (debug) {
                            plugin.logger.warning("[PacketBlocker] Error handling advancement packet: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            }
        )
        
        // Block Chat Announcements
        manager.addPacketListener(
            object : PacketAdapter(plugin, PacketType.Play.Server.SYSTEM_CHAT) {
                override fun onPacketSending(event: PacketEvent) {
                    handleChatPacket(event)
                }
            }
        )
    }

    private fun handleAdvancementPacket(event: PacketEvent) {
        val structure = event.packet.modifier
        
        // 1: List<AdvancementHolder> (Added) - Controls L Menu (1.20.5+)
        // or Map<MinecraftKey, Advancement> (Older versions)
        // 3: Map<MinecraftKey, AdvancementProgress> (Progress Update) - Controls Toasts
        
        val originalAdded = structure.read(1)
        val originalProgressMap = if (structure.size() > 3) structure.read(3) as? Map<*, *> else null

        // --- Filter Added (Hides from L menu) ---
        if (originalAdded != null) {
            if (originalAdded is Collection<*>) {
                // Handle List<AdvancementHolder>
                if (originalAdded.isNotEmpty()) {
                    val newAddedList = ArrayList<Any>()
                    var filteredCount = 0
                    
                    for (item in originalAdded) {
                        if (item != null) {
                            val id = getAdvancementId(item)
                            if (!shouldBlockId(id)) {
                                newAddedList.add(item)
                            } else {
                                filteredCount++
                            }
                        }
                    }
                    
                    if (filteredCount > 0) {
                        structure.write(1, newAddedList)
                        if (debug) plugin.logger.info("[PacketBlocker] Filtered $filteredCount advancements from ADD list.")
                    }
                }
            } else if (originalAdded is Map<*, *>) {
                // Handle Map<ResourceLocation, Advancement> (Legacy fallback)
                if (originalAdded.isNotEmpty()) {
                    val toRemove = ArrayList<Any>()
                    for (keyObj in originalAdded.keys) {
                        if (keyObj != null && shouldBlock(keyObj)) {
                            toRemove.add(keyObj)
                        }
                    }
                    
                    if (toRemove.isNotEmpty()) {
                        val newMap = HashMap(originalAdded)
                        for (key in toRemove) {
                            newMap.remove(key)
                        }
                        structure.write(1, newMap)
                        if (debug) plugin.logger.info("[PacketBlocker] Filtered ${toRemove.size} advancements from ADD map.")
                    }
                }
            }
        }

        // --- Filter Progress Map (Hides Toasts) ---
        if (originalProgressMap != null && originalProgressMap.isNotEmpty()) {
            val toRemove = ArrayList<Any>()
            for (keyObj in originalProgressMap.keys) {
                if (keyObj != null && shouldBlock(keyObj)) {
                    toRemove.add(keyObj)
                }
            }
            
            if (toRemove.isNotEmpty()) {
                // Create a mutable copy using explicit java.util.HashMap
                val newMap = HashMap(originalProgressMap)
                for (key in toRemove) {
                    newMap.remove(key)
                }
                // Write the NEW map back to the packet
                structure.write(3, newMap)
                
                if (debug) plugin.logger.info("[PacketBlocker] Filtered ${toRemove.size} advancements from PROGRESS map.")
            }
        }
    }

    private fun getAdvancementId(obj: Any): String {
        // Try to get ID from AdvancementHolder (Record)
        try {
            // Try id() method (Record accessor)
            val method = obj.javaClass.getMethod("id")
            return method.invoke(obj).toString()
        } catch (e: Exception) {
            // Fallback: Parse toString() which usually is "AdvancementHolder[id=..., value=...]"
            val str = obj.toString()
            if (str.startsWith("AdvancementHolder")) {
                val match = Regex("id=([^,]+)").find(str)
                if (match != null) {
                    return match.groupValues[1]
                }
            }
            return str
        }
    }

    private fun shouldBlock(keyObj: Any): Boolean {
        return shouldBlockId(keyObj.toString())
    }

    private fun shouldBlockId(keyStr: String): Boolean {
        if (debug && Math.random() < 0.001) { 
             plugin.logger.info("[PacketBlocker-DEBUG] Inspecting Key: $keyStr")
        }

        val parts = keyStr.split(":")
        val namespace = if (parts.size > 1) parts[0] else "minecraft"
        
        if (namespace == "milestone") return false

        return (mode == VanillaBlocker.Mode.DISABLE_VANILLA) ||
               blockedNamespaces.contains(namespace) ||
               blockedIds.contains(keyStr)
    }

    private fun handleChatPacket(event: PacketEvent) {
        try {
            val components = event.packet.chatComponents
            if (components.size() == 0) return
            
            val wrapped = components.read(0)
            if (wrapped == null) return
            
            val json = wrapped.json
            
            if (json != null && json.contains("chat.type.advancement")) {
                val milestones = service.getAllMilestones()
                var isMilestone = false
                for (m in milestones) {
                    if (json.contains(m.title)) {
                        isMilestone = true
                        break
                    }
                }
                
                if (!isMilestone) {
                     var shouldBlock = false
                     
                     if (mode == VanillaBlocker.Mode.DISABLE_VANILLA) {
                         shouldBlock = true
                     } else if (mode == VanillaBlocker.Mode.HYBRID) {
                         // Check for Vanilla translation keys
                         // Vanilla keys usually look like "advancements.story.root.title"
                         // If "minecraft" is blocked, we block all vanilla-like keys
                         val blockAllVanilla = blockedNamespaces.contains("minecraft")
                         
                         if (blockAllVanilla && json.contains("\"translate\":\"advancements.")) {
                             shouldBlock = true
                         } else {
                             // Check specific categories/namespaces
                             for (ns in blockedNamespaces) {
                                 // Check for "advancements.story." etc.
                                 if (json.contains("advancements.$ns.")) {
                                     shouldBlock = true
                                     break
                                 }
                                 // Check for resource location style just in case (e.g. hover event)
                                 if (json.contains("\"minecraft:$ns/")) {
                                     shouldBlock = true
                                     break
                                 }
                             }
                         }
                     }

                     if (shouldBlock) {
                         event.isCancelled = true
                         if (debug) plugin.logger.info("[PacketBlocker] Blocked advancement chat: $json")
                     }
                }
            }
        } catch (e: Exception) {
            // Ignore errors
        }
    }
}
