package me.richy.radioss.commands

import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class NowPlayingCommand(
    private val audioHandler: AudioHandler,
    private val uiBuilder: UIBuilder
) : Command {
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("nowplaying", "Show current station")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.id ?: return
        val audioManager = audioHandler.getOrCreateAudioManager(guildId)
        
        val currentStation = audioManager.getCurrentStation()
        val volume = audioManager.getVolume()
        val isPlaying = audioManager.isPlaying()
        
        val embed = uiBuilder.createAudioStatusEmbed(currentStation, volume, isPlaying)
        event.replyEmbeds(embed).setEphemeral(true).queue()
    }
}

