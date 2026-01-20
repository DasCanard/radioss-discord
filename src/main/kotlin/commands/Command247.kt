package me.richy.radioss.commands

import me.richy.radioss.bot.VoiceChannelManager
import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class Command247(
    private val audioHandler: AudioHandler,
    private val voiceChannelManager: VoiceChannelManager,
    private val uiBuilder: UIBuilder
) : Command {
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("247", "Enable 24/7 mode - Bot stays in channel")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.id ?: return
        
        if (!audioHandler.isPlaying(guildId)) {
            val errorEmbed = uiBuilder.createErrorEmbed(
                "No active stream",
                "A stream must be playing to activate 24/7 mode!"
            )
            event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            return
        }
        
        val isCurrentlyEnabled = voiceChannelManager.is247ModeEnabled(guildId)
        
        if (isCurrentlyEnabled) {
            // Disable
            voiceChannelManager.set247Mode(guildId, false)
            val embed = uiBuilder.createSuccessEmbed(
                "24/7 mode disabled",
                "The bot will leave the channel again when no one is in it."
            )
            event.replyEmbeds(embed).setEphemeral(true).queue()
        } else {
            // Enable
            voiceChannelManager.set247Mode(guildId, true)
            val embed = uiBuilder.createSuccessEmbed(
                "24/7 mode enabled",
                "The bot will now stay 24/7 in the channel, even when no one is in it!"
            )
            event.replyEmbeds(embed).setEphemeral(true).queue()
        }
    }
}

