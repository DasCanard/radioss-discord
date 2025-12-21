package me.richy.radioss.bot

import net.dv8tion.jda.api.events.session.ReadyEvent
import org.slf4j.LoggerFactory

class ReadyEventHandler(
    private val commandRegistrar: CommandRegistrar
) {
    private val logger = LoggerFactory.getLogger(ReadyEventHandler::class.java)
    
    fun handleReady(event: ReadyEvent) {
        val jda = event.jda
        logger.info("Bot '${jda.selfUser.name}' is ready!")
        logger.info("Bot is active on ${jda.guilds.size} servers")
        
        commandRegistrar.registerCommands(jda)
    }
}

