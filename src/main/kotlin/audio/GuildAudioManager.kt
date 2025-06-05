package me.richy.radioss.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import me.richy.radioss.models.RadioStation
import net.dv8tion.jda.api.audio.AudioSendHandler
import org.slf4j.LoggerFactory

class GuildAudioManager {
    private val logger = LoggerFactory.getLogger(GuildAudioManager::class.java)
    
    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val audioPlayer = playerManager.createPlayer()
    private val scheduler = RadioTrackScheduler(audioPlayer)
    private val sendHandler = RadioAudioSendHandler(audioPlayer)
    
    private var currentStation: RadioStation? = null
    
    init {
        playerManager.registerSourceManager(HttpAudioSourceManager())
        audioPlayer.addListener(scheduler)
        audioPlayer.volume = 5
        logger.debug("Default volume set to 5%")
    }
    
    fun playTrack(url: String, station: RadioStation) {
        logger.info("Loading track: $url")
        currentStation = station
        
        val resultHandler = RadioAudioLoadResultHandler(scheduler, station)
        playerManager.loadItem(url, resultHandler)
    }
    
    fun stop() {
        audioPlayer.stopTrack()
        currentStation = null
        logger.info("Playback stopped")
    }
    
    fun setVolume(volume: Int) {
        val safeVolume = volume.coerceIn(0, 100)
        audioPlayer.volume = safeVolume
        logger.debug("Volume set to: $safeVolume%")
    }
    
    fun getVolume(): Int {
        return audioPlayer.volume
    }
    
    fun isPlaying(): Boolean {
        return audioPlayer.playingTrack != null && !audioPlayer.isPaused
    }
    
    fun getCurrentStation(): RadioStation? {
        return currentStation
    }
    
    fun getSendHandler(): AudioSendHandler {
        return sendHandler
    }
}