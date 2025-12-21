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
        return Commands.slash("247", "Aktiviere 24/7-Modus - Bot bleibt im Channel")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.id ?: return
        
        if (!audioHandler.isPlaying(guildId)) {
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Kein Stream aktiv",
                "Es muss ein Stream laufen, um den 24/7-Modus zu aktivieren!"
            )
            event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            return
        }
        
        val isCurrentlyEnabled = voiceChannelManager.is247ModeEnabled(guildId)
        
        if (isCurrentlyEnabled) {
            // Deaktivieren
            voiceChannelManager.set247Mode(guildId, false)
            val embed = uiBuilder.createSuccessEmbed(
                "24/7-Modus deaktiviert",
                "Der Bot wird den Channel wieder verlassen, wenn niemand mehr drin ist."
            )
            event.replyEmbeds(embed).setEphemeral(true).queue()
        } else {
            // Aktivieren
            voiceChannelManager.set247Mode(guildId, true)
            val embed = uiBuilder.createSuccessEmbed(
                "24/7-Modus aktiviert",
                "Der Bot bleibt jetzt 24/7 im Channel, auch wenn niemand mehr drin ist!"
            )
            event.replyEmbeds(embed).setEphemeral(true).queue()
        }
    }
}

