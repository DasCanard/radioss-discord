package me.richy.radioss.commands

import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class VolumeCommand(
    private val audioHandler: AudioHandler,
    private val uiBuilder: UIBuilder
) : Command {
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("volume", "Set the volume")
            .addOption(OptionType.INTEGER, "value", "Volume (0-100)", true)
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.id ?: return
        val volume = event.getOption("value")?.asInt ?: 5
        
        if (!audioHandler.isPlaying(guildId)) {
            val errorEmbed = uiBuilder.createErrorEmbed(
                "No Music Playing",
                "Volume can only be changed while music is playing. Start playing a radio station first!"
            )
            event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            return
        }
        
        val audioManager = audioHandler.getOrCreateAudioManager(guildId)
        val safeVolume = volume.coerceIn(0, 100)
        audioManager.setVolume(safeVolume)
        
        val embed = uiBuilder.createSuccessEmbed(
            "Volume Changed",
            "Volume set to $safeVolume%"
        )
        event.replyEmbeds(embed).setEphemeral(true).queue()
    }
}

