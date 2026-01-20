package me.richy.radioss.handlers

import me.richy.radioss.audio.GuildAudioManager
import me.richy.radioss.models.RadioStation
import me.richy.radioss.services.ReconnectionService
import net.dv8tion.jda.api.JDA
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class AudioHandler {
    private val logger = LoggerFactory.getLogger(AudioHandler::class.java)
    private val audioManagers = ConcurrentHashMap<String, GuildAudioManager>()
    private var reconnectionService: ReconnectionService? = null
    private var jda: JDA? = null
    
    fun setReconnectionService(reconnectionService: ReconnectionService) {
        this.reconnectionService = reconnectionService
    }
    
    fun updateJDA(jda: JDA?) {
        this.jda = jda
        reconnectionService?.updateJDA(jda)
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
            
            // Speichere Reconnection-State
            saveReconnectionState(guildId, station)
        } else {
            logger.error("No valid URL for station '${station.name}'")
        }
    }
    
    private fun saveReconnectionState(guildId: String, station: RadioStation) {
        val service = reconnectionService ?: return
        val jdaInstance = jda ?: return
        
        try {
            val guild = jdaInstance.getGuildById(guildId) ?: return
            val audioManager = guild.audioManager
            
            // Check if bot is in a channel
            val connectedChannel = audioManager.connectedChannel
            if (connectedChannel != null) {
                val mode247Enabled = false // Will be updated later by VoiceChannelManager
                service.saveState(guildId, connectedChannel.id, station, mode247Enabled)
                logger.debug("Saved reconnection state for guild $guildId, channel ${connectedChannel.id}")
            }
        } catch (e: Exception) {
            logger.error("Error saving reconnection state for guild $guildId", e)
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
    
    fun getAllActivePlayers(): Map<String, GuildAudioManager> {
        return audioManagers.toMap()
    }
    
    fun getActivePlayerCount(): Int {
        return audioManagers.size
    }
    
    fun getPlayingCount(): Int {
        return audioManagers.values.count { it.isPlaying() }
    }
    
}