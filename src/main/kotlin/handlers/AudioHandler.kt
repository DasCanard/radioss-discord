package me.richy.radioss.handlers

import me.richy.radioss.audio.GuildAudioManager
import me.richy.radioss.models.RadioStation
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class AudioHandler {
    private val logger = LoggerFactory.getLogger(AudioHandler::class.java)
    private val audioManagers = ConcurrentHashMap<String, GuildAudioManager>()
    
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
    
}