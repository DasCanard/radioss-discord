package me.richy.radioss.commands

import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.services.FavoriteService
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class NowPlayingCommand(
    private val audioHandler: AudioHandler,
    private val favoriteService: FavoriteService,
    private val uiBuilder: UIBuilder
) : Command {
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("nowplaying", "Show current station")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.id ?: return
        val userId = event.user.id
        val audioManager = audioHandler.getOrCreateAudioManager(guildId)
        
        val currentStation = audioManager.getCurrentStation()
        val volume = audioManager.getVolume()
        val isPlaying = audioManager.isPlaying()
        
        val isFavorite = if (currentStation != null) {
            favoriteService.isFavorite(userId, currentStation.stationUuid)
        } else {
            false
        }
        
        val embed = uiBuilder.createAudioStatusEmbed(currentStation, volume, isPlaying, isFavorite)
        
        val components = mutableListOf<ActionRow>()
        if (currentStation != null) {
            val favoriteButton = uiBuilder.createFavoriteButton(currentStation, isFavorite)
            components.add(ActionRow.of(favoriteButton))
        }
        
        if (components.isNotEmpty()) {
            event.replyEmbeds(embed).setComponents(components).setEphemeral(true).queue()
        } else {
            event.replyEmbeds(embed).setEphemeral(true).queue()
        }
    }
}

