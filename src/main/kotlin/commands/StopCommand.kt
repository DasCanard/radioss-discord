package me.richy.radioss.commands

import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class StopCommand(
    private val audioHandler: AudioHandler,
    private val uiBuilder: UIBuilder
) : Command {
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("stop", "Stop playback")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.id ?: return
        val audioManager = audioHandler.getOrCreateAudioManager(guildId)
        
        audioManager.stop()
        
        val guild = event.guild
        guild?.audioManager?.closeAudioConnection()
        
        val successEmbed = uiBuilder.createSuccessEmbed(
            "Playback Stopped",
            "Audio stopped and left voice channel"
        )
        event.replyEmbeds(successEmbed).setEphemeral(true).queue()
    }
}

