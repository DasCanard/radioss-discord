package me.richy.radioss.commands

import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.models.RadioStation
import me.richy.radioss.services.WebhookLogger
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class PlayCommand(
    private val audioHandler: AudioHandler,
    private val uiBuilder: UIBuilder,
    private val webhookLogger: WebhookLogger
) : Command {
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("play", "Play a radio station")
            .addOption(OptionType.STRING, "station", "Name or URL of the radio station", true)
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.id ?: return
        val audioManager = audioHandler.getOrCreateAudioManager(guildId)
        val stationInput = event.getOption("station")?.asString ?: ""
        
        val member = event.member ?: return
        val voiceState = member.voiceState ?: return
        
        if (!voiceState.inAudioChannel()) {
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Not in Voice Channel",
                "You must be in a voice channel to listen to radio!"
            )
            event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            return
        }
        
        val guild = event.guild ?: return
        val audioChannel = voiceState.channel ?: return
        
        guild.audioManager.openAudioConnection(audioChannel)
        guild.audioManager.sendingHandler = audioManager.getSendHandler()
        
        event.deferReply().queue()
        
        if (stationInput.startsWith("http")) {
            val station = RadioStation(
                name = "Custom Stream",
                url = stationInput,
                urlResolved = stationInput
            )
            audioManager.playTrack(stationInput, station)
            
            // Webhook-Logging
            val user = event.user
            val guildName = guild.name
            webhookLogger.logPlayCommand(
                userId = user.id,
                userName = user.name,
                guildId = guildId,
                guildName = guildName,
                stationName = station.name,
                stationUrl = stationInput
            )
            
            val successEmbed = uiBuilder.createSuccessEmbed(
                "Stream Started",
                "Playing Custom Stream"
            )
            event.hook.editOriginalEmbeds(successEmbed).queue()
        } else {
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Not Implemented",
                "Search by station name not yet implemented. Use a direct URL."
            )
            event.hook.editOriginalEmbeds(errorEmbed).queue()
        }
    }
}

