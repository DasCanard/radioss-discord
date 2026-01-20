package me.richy.radioss.services

import org.slf4j.LoggerFactory

class AdminService {
    private val logger = LoggerFactory.getLogger(AdminService::class.java)
    private val adminUserIds: Set<String> = loadAdminUserIds()
    private val adminGuildIds: Set<String> = loadAdminGuildIds()
    
    private fun loadAdminUserIds(): Set<String> {
        val adminIdsEnv = System.getenv("ADMIN_USER_IDS")?.takeIf { it.isNotBlank() }
        
        if (adminIdsEnv == null) {
            logger.debug("ADMIN_USER_IDS not set, no admin users configured")
            return emptySet()
        }
        
        val ids = adminIdsEnv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        
        if (ids.isNotEmpty()) {
            logger.info("Loaded ${ids.size} admin user IDs: ${ids.joinToString(", ")}")
        } else {
            logger.warn("ADMIN_USER_IDS is set but contains no valid IDs")
        }
        
        return ids
    }
    
    private fun loadAdminGuildIds(): Set<String> {
        val adminGuildIdsEnv = System.getenv("ADMIN_GUILD_IDS")?.takeIf { it.isNotBlank() }
        
        if (adminGuildIdsEnv == null) {
            logger.debug("ADMIN_GUILD_IDS not set, no admin guilds configured")
            return emptySet()
        }
        
        val ids = adminGuildIdsEnv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        
        if (ids.isNotEmpty()) {
            logger.info("Loaded ${ids.size} admin guild IDs: ${ids.joinToString(", ")}")
        } else {
            logger.warn("ADMIN_GUILD_IDS is set but contains no valid IDs")
        }
        
        return ids
    }
    
    fun isAdmin(userId: String): Boolean {
        if (adminUserIds.isEmpty()) {
            logger.debug("No admin users configured, denying access for user $userId")
            return false
        }
        
        val isAdmin = adminUserIds.contains(userId)
        if (isAdmin) {
            logger.debug("User $userId is an admin")
        } else {
            logger.debug("User $userId is not an admin")
        }
        
        return isAdmin
    }
    
    fun getAdminCount(): Int {
        return adminUserIds.size
    }
    
    fun isAdminGuild(guildId: String): Boolean {
        if (adminGuildIds.isEmpty()) {
            logger.debug("No admin guilds configured, denying access for guild $guildId")
            return false
        }
        
        val isAdminGuild = adminGuildIds.contains(guildId)
        if (isAdminGuild) {
            logger.debug("Guild $guildId is an admin guild")
        } else {
            logger.debug("Guild $guildId is not an admin guild")
        }
        
        return isAdminGuild
    }
    
    fun getAdminGuildIds(): Set<String> {
        return adminGuildIds
    }
}
