package me.richy.radioss.handlers

import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.slf4j.LoggerFactory

class SelectMenuHandler(
    private val audioHandler: AudioHandler,
    private val uiBuilder: UIBuilder
) {
    private val logger = LoggerFactory.getLogger(SelectMenuHandler::class.java)
    
    fun handleSelectMenuInteraction(event: StringSelectInteractionEvent) {
        val menuId = event.componentId
        val guildId = event.guild?.id ?: return
        
        when (menuId) {
            "volume_select" -> handleVolumeSelect(event, guildId)
            else -> {
                logger.warn("Unknown select menu: $menuId")
            }
        }
    }
    
    private fun handleVolumeSelect(event: StringSelectInteractionEvent, guildId: String) {
        event.deferEdit().queue()
        
        val selectedValue = event.values.firstOrNull() ?: return
        
        if (!audioHandler.isPlaying(guildId)) {
            val errorEmbed = uiBuilder.createErrorEmbed(
                "No Music Playing",
                "Volume can only be changed while music is playing. Start playing a radio station first!"
            )
            
            event.hook.sendMessageEmbeds(errorEmbed)
                .setEphemeral(true)
                .queue()
                
            logger.info("Volume change denied - no music playing in guild $guildId by user ${event.user.id}")
            return
        }
        
        try {
            val volume = selectedValue.toInt()
            audioHandler.setVolume(guildId, volume)
            
            val embed = uiBuilder.createSuccessEmbed(
                "🔊 Volume Changed",
                "Volume set to $volume%"
            )
            
            event.hook.sendMessageEmbeds(embed)
                .setEphemeral(true)
                .queue()
                
            logger.info("Volume changed to $volume% in guild $guildId by user ${event.user.id}")
            
        } catch (e: NumberFormatException) {
            logger.error("Invalid volume value: $selectedValue", e)
            
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Invalid Volume",
                "Invalid volume value selected"
            )
            
            event.hook.sendMessageEmbeds(errorEmbed)
                .setEphemeral(true)
                .queue()
        } catch (e: Exception) {
            logger.error("Error changing volume", e)
            
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Volume Error",
                "Error changing volume: ${e.message}"
            )
            
            event.hook.sendMessageEmbeds(errorEmbed)
                .setEphemeral(true)
                .queue()
        }
    }
}