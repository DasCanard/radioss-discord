package me.richy.radioss.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.richy.radioss.api.RadioBrowserAPI
import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory
import kotlin.random.Random

class RandomCommand(
    private val api: RadioBrowserAPI,
    private val audioHandler: AudioHandler,
    private val uiBuilder: UIBuilder
) : Command {
    private val logger = LoggerFactory.getLogger(RandomCommand::class.java)
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("random", "Random radio station")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.id ?: return
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
        
        event.deferReply().queue()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stations = api.getTopStations(500)
                if (stations.isNotEmpty()) {
                    val randomStation = stations[Random.nextInt(stations.size)]
                    
                    val guild = event.guild ?: return@launch
                    val audioChannel = voiceState.channel ?: return@launch
                    
                    try {
                        guild.audioManager.openAudioConnection(audioChannel)
                        logger.info("Bot connected to voice channel: ${audioChannel.name}")
                        
                        val guildAudioManager = audioHandler.getOrCreateAudioManager(guildId)
                        guild.audioManager.sendingHandler = guildAudioManager.getSendHandler()
                        logger.info("Audio send handler set for guild $guildId")
                        
                        audioHandler.playStation(guildId, randomStation)
                        logger.info("Playing random station '${randomStation.name}' (URL: ${randomStation.url}) for user ${event.user.id} in guild $guildId")
                        
                        val successEmbed = uiBuilder.createSuccessEmbed(
                            "🎵 Now Playing",
                            "**${randomStation.name}** is now playing!"
                        )
                        
                        event.hook.editOriginalEmbeds(successEmbed).queue()
                    } catch (e: Exception) {
                        logger.error("Error playing random station '${randomStation.name}'", e)
                        
                        val errorEmbed = uiBuilder.createErrorEmbed(
                            "Playback Error",
                            "Error playing station: ${e.message}"
                        )
                        
                        event.hook.editOriginalEmbeds(errorEmbed).queue()
                    }
                } else {
                    val errorEmbed = uiBuilder.createErrorEmbed(
                        "No Stations", 
                        "No stations available"
                    )
                    event.hook.editOriginalEmbeds(errorEmbed).queue()
                }
            } catch (e: Exception) {
                logger.error("Error loading random station", e)
                val errorEmbed = uiBuilder.createErrorEmbed(
                    "Loading Error", 
                    "Error loading station: ${e.message}"
                )
                event.hook.editOriginalEmbeds(errorEmbed).queue()
            }
        }
    }
}

