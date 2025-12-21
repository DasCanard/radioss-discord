package me.richy.radioss.handlers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.richy.radioss.audio.GuildAudioManager
import me.richy.radioss.bot.RadioBot
import me.richy.radioss.models.RadioStation
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class AudioHandler(
    private val uiBuilder: UIBuilder
) {
    private var radioBot: RadioBot? = null
    
    fun setRadioBot(bot: RadioBot) {
        this.radioBot = bot
    }
    private val logger = LoggerFactory.getLogger(AudioHandler::class.java)
    private val audioManagers = ConcurrentHashMap<String, GuildAudioManager>()
    
    fun getAudioCommands(): List<SlashCommandData> {
        return listOf(
            Commands.slash("play", "Play a radio station")
                .addOption(OptionType.STRING, "station", "Name or URL of the radio station", true),
            
            Commands.slash("stop", "Stop playback"),
            
            Commands.slash("volume", "Set the volume")
                .addOption(OptionType.INTEGER, "value", "Volume (0-100)", true),
            
            Commands.slash("nowplaying", "Show current station"),
            
            Commands.slash("247", "Aktiviere 24/7-Modus - Bot bleibt im Channel")
        )
    }
    
    fun handleAudioCommand(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.id ?: return
        val audioManager = getOrCreateAudioManager(guildId)
        
        when (event.name) {
            "play" -> {
                val stationInput = event.getOption("station")?.asString ?: ""
                handlePlayCommand(event, audioManager, stationInput)
            }
            
            "stop" -> {
                handleStopCommand(event, audioManager)
            }
            
            "volume" -> {
                val volume = event.getOption("value")?.asInt ?: 5
                handleVolumeCommand(event, audioManager, volume)
            }
            
            "nowplaying" -> {
                handleNowPlayingCommand(event, audioManager)
            }
            
            "247" -> {
                handle247Command(event, audioManager)
            }
        }
    }
    
    fun playStation(guildId: String, station: RadioStation) {
        val audioManager = getOrCreateAudioManager(guildId)
        val streamUrl = station.urlResolved.ifEmpty { station.url }
        
        logger.info("Attempting to play station '${station.name}'")
        logger.info("Original URL: ${station.url}")
        logger.info("Resolved URL: ${station.urlResolved}")
        logger.info("Using URL: $streamUrl")
        
        if (streamUrl.isNotEmpty()) {
            audioManager.playTrack(streamUrl, station)
            logger.info("Track loading started for station '${station.name}' in guild $guildId")
        } else {
            logger.error("No valid URL for station '${station.name}'")
        }
    }
    
    fun stopAudio(guildId: String) {
        audioManagers[guildId]?.stop()
    }
    
    fun setVolume(guildId: String, volume: Int) {
        audioManagers[guildId]?.setVolume(volume)
    }
    
    fun getCurrentStation(guildId: String): RadioStation? {
        return audioManagers[guildId]?.getCurrentStation()
    }
    
    fun getCurrentVolume(guildId: String): Int {
        return audioManagers[guildId]?.getVolume() ?: 5
    }
    
    fun isPlaying(guildId: String): Boolean {
        return audioManagers[guildId]?.isPlaying() ?: false
    }
    
    fun getOrCreateAudioManager(guildId: String): GuildAudioManager {
        return audioManagers.computeIfAbsent(guildId) { 
            GuildAudioManager()
        }
    }
    
    private fun handlePlayCommand(
        event: SlashCommandInteractionEvent, 
        audioManager: GuildAudioManager, 
        stationInput: String
    ) {
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
    
    private fun handleStopCommand(event: SlashCommandInteractionEvent, audioManager: GuildAudioManager) {
        audioManager.stop()
        
        val guild = event.guild
        guild?.audioManager?.closeAudioConnection()
        
        val successEmbed = uiBuilder.createSuccessEmbed(
            "Playback Stopped",
            "Audio stopped and left voice channel"
        )
        event.replyEmbeds(successEmbed).setEphemeral(true).queue()
    }
    
    private fun handleVolumeCommand(
        event: SlashCommandInteractionEvent, 
        audioManager: GuildAudioManager, 
        volume: Int
    ) {
        val guildId = event.guild?.id ?: return
        
        if (!isPlaying(guildId)) {
            val errorEmbed = uiBuilder.createErrorEmbed(
                "No Music Playing",
                "Volume can only be changed while music is playing. Start playing a radio station first!"
            )
            event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            return
        }
        
        val safeVolume = volume.coerceIn(0, 100)
        audioManager.setVolume(safeVolume)
        
        val embed = uiBuilder.createSuccessEmbed(
            "Volume Changed",
            "Volume set to $safeVolume%"
        )
        event.replyEmbeds(embed).setEphemeral(true).queue()
    }
    
    private fun handleNowPlayingCommand(event: SlashCommandInteractionEvent, audioManager: GuildAudioManager) {
        val currentStation = audioManager.getCurrentStation()
        val volume = audioManager.getVolume()
        val isPlaying = audioManager.isPlaying()
        
        val embed = uiBuilder.createAudioStatusEmbed(currentStation, volume, isPlaying)
        event.replyEmbeds(embed).setEphemeral(true).queue()
    }
    
    private fun handle247Command(event: SlashCommandInteractionEvent, audioManager: GuildAudioManager) {
        val guildId = event.guild?.id ?: return
        
        if (!isPlaying(guildId)) {
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Kein Stream aktiv",
                "Es muss ein Stream laufen, um den 24/7-Modus zu aktivieren!"
            )
            event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            return
        }
        
        val isCurrentlyEnabled = radioBot?.is247ModeEnabled(guildId) ?: false
        
        if (isCurrentlyEnabled) {
            // Deaktivieren
            radioBot?.set247Mode(guildId, false)
            val embed = uiBuilder.createSuccessEmbed(
                "24/7-Modus deaktiviert",
                "Der Bot wird den Channel wieder verlassen, wenn niemand mehr drin ist."
            )
            event.replyEmbeds(embed).setEphemeral(true).queue()
        } else {
            // Aktivieren
            radioBot?.set247Mode(guildId, true)
            val embed = uiBuilder.createSuccessEmbed(
                "24/7-Modus aktiviert",
                "Der Bot bleibt jetzt 24/7 im Channel, auch wenn niemand mehr drin ist!"
            )
            event.replyEmbeds(embed).setEphemeral(true).queue()
        }
    }
}