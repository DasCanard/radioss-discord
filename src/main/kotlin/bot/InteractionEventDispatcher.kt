package me.richy.radioss.bot

import me.richy.radioss.commands.CommandManager
import me.richy.radioss.handlers.ButtonHandler
import me.richy.radioss.handlers.SelectMenuHandler
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.slf4j.LoggerFactory

class InteractionEventDispatcher(
    private val commandManager: CommandManager,
    private val buttonHandler: ButtonHandler,
    private val selectMenuHandler: SelectMenuHandler,
    private val uiBuilder: UIBuilder
) {
    private val logger = LoggerFactory.getLogger(InteractionEventDispatcher::class.java)
    
    fun handleSlashCommand(event: SlashCommandInteractionEvent) {
        val commandName = event.name
        logger.info("Command '$commandName' from ${event.user.name} (${event.user.id})")
        
        try {
            commandManager.executeCommand(event)
        } catch (e: Exception) {
            logger.error("Error in command '$commandName'", e)
            
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Command Error",
                "An unexpected error occurred: ${e.message}"
            )
            
            if (event.isAcknowledged) {
                event.hook.editOriginalEmbeds(errorEmbed).queue()
            } else {
                event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            }
        }
    }
    
    fun handleButtonInteraction(event: ButtonInteractionEvent) {
        val buttonId = event.componentId
        val userId = event.user.id
        
        logger.debug("Button '$buttonId' from user $userId")
        
        try {
            buttonHandler.handleButtonInteraction(event)
        } catch (e: Exception) {
            logger.error("Error with button '$buttonId'", e)
            
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Button Error",
                "Error with button action: ${e.message}"
            )
            
            if (event.isAcknowledged) {
                event.hook.sendMessageEmbeds(errorEmbed).setEphemeral(true).queue()
            } else {
                event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            }
        }
    }
    
    fun handleSelectMenuInteraction(event: StringSelectInteractionEvent) {
        val menuId = event.componentId
        val userId = event.user.id
        
        logger.debug("Select menu '$menuId' from user $userId")
        
        try {
            selectMenuHandler.handleSelectMenuInteraction(event)
        } catch (e: Exception) {
            logger.error("Error with select menu '$menuId'", e)
            
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Select Menu Error",
                "Error with select menu action: ${e.message}"
            )
            
            if (event.isAcknowledged) {
                event.hook.sendMessageEmbeds(errorEmbed).setEphemeral(true).queue()
            } else {
                event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            }
        }
    }
}

