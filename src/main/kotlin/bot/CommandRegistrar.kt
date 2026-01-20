package me.richy.radioss.bot

import me.richy.radioss.commands.CommandManager
import me.richy.radioss.services.AdminService
import net.dv8tion.jda.api.JDA
import org.slf4j.LoggerFactory

class CommandRegistrar(
    private val commandManager: CommandManager,
    private val adminService: AdminService
) {
    private val logger = LoggerFactory.getLogger(CommandRegistrar::class.java)
    private var commandsRegistered = false
    
    fun registerCommands(jda: JDA) {
        // Verhindere mehrfache Registrierung
        if (commandsRegistered) {
            logger.debug("Commands already registered, skipping...")
            return
        }
        
        logger.info("Registering slash commands...")
        
        val commands = commandManager.getAllCommands()
        val adminCommands = commandManager.getAdminCommands()

        // Globale Registrierung für normale Commands
        jda.updateCommands().addCommands(commands).queue(
            { 
                logger.info("${commands.size} slash commands registered globally")
                commands.forEach { cmd ->
                    logger.debug("   - /${cmd.name}: ${cmd.description}")
                }
                
                // Registriere Admin-Commands nur in Guilds, wo ein Admin ist
                if (adminCommands.isNotEmpty()) {
                    registerAdminCommands(jda, adminCommands)
                } else {
                    commandsRegistered = true
                }
            },
            { error -> 
                logger.error("Error registering global commands", error)
            }
        )
    }
    
    private fun registerAdminCommands(jda: JDA, adminCommands: List<net.dv8tion.jda.api.interactions.commands.build.SlashCommandData>) {
        val adminGuildIds = adminService.getAdminGuildIds()
        
        if (adminGuildIds.isEmpty()) {
            commandsRegistered = true
            logger.info("No admin guilds configured, admin commands not registered")
            return
        }
        
        val guilds = jda.guilds.filter { guild ->
            adminService.isAdminGuild(guild.id)
        }
        
        if (guilds.isEmpty()) {
            commandsRegistered = true
            logger.info("No matching admin guilds found, admin commands not registered")
            return
        }
        
        var registeredCount = 0
        val totalGuilds = guilds.size
        
        guilds.forEach { guild ->
            guild.updateCommands().addCommands(adminCommands).queue(
                {
                    registeredCount++
                    logger.info("Admin commands registered in guild: ${guild.name} (${guild.id})")
                    if (registeredCount == totalGuilds) {
                        commandsRegistered = true
                        logger.info("Admin commands registered in $registeredCount guild(s)")
                    }
                },
                { error ->
                    logger.warn("Error registering admin commands in guild ${guild.name}: ${error.message}")
                    registeredCount++
                    if (registeredCount == totalGuilds) {
                        commandsRegistered = true
                    }
                }
            )
        }
    }
    
    fun registerAdminCommandsForGuild(guild: net.dv8tion.jda.api.entities.Guild) {
        val adminCommands = commandManager.getAdminCommands()
        if (adminCommands.isEmpty()) return
        
        // Prüfe ob diese Guild in der Admin-Guild-Liste ist
        if (adminService.isAdminGuild(guild.id)) {
            guild.updateCommands().addCommands(adminCommands).queue(
                {
                    logger.info("Admin commands registered in guild: ${guild.name} (${guild.id})")
                },
                { error ->
                    logger.warn("Error registering admin commands in guild ${guild.name}: ${error.message}")
                }
            )
        }
    }
}

