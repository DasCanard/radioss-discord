package me.richy.radioss.bot

import me.richy.radioss.commands.CommandManager
import net.dv8tion.jda.api.JDA
import org.slf4j.LoggerFactory

class CommandRegistrar(
    private val commandManager: CommandManager
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

        // Nur globale Registrierung
        jda.updateCommands().addCommands(commands).queue(
            { 
                commandsRegistered = true
                logger.info("${commands.size} slash commands registered globally")
                commands.forEach { cmd ->
                    logger.debug("   - /${cmd.name}: ${cmd.description}")
                }
            },
            { error -> 
                logger.error("Error registering global commands", error)
            }
        )
    }
}

