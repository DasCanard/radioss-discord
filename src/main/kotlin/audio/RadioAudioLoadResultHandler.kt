package me.richy.radioss.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.richy.radioss.models.RadioStation
import org.slf4j.LoggerFactory

class RadioAudioLoadResultHandler(
    private val scheduler: RadioTrackScheduler,
    private val station: RadioStation
) : AudioLoadResultHandler {
    
    private val logger = LoggerFactory.getLogger(RadioAudioLoadResultHandler::class.java)
    
    override fun trackLoaded(track: AudioTrack) {
        logger.info("Track loaded successfully: ${track.info.title}")
        logger.info("   Station: ${station.name}")
        logger.info("   URL: ${track.info.uri}")
        logger.info("   Stream: ${track.info.isStream}")
        
        scheduler.queue(track)
    }
    
    override fun playlistLoaded(playlist: AudioPlaylist) {
        logger.info("Playlist loaded: ${playlist.name}")
        
        val firstTrack = playlist.tracks.firstOrNull()
        if (firstTrack != null) {
            logger.info("Playing first track from playlist: ${firstTrack.info.title}")
            scheduler.queue(firstTrack)
        } else {
            logger.warn("Playlist is empty")
        }
    }
    
    override fun noMatches() {
        logger.warn("No matches for station '${station.name}' (${station.url})")
    }
    
    override fun loadFailed(exception: FriendlyException) {
        logger.error("Loading failed for station '${station.name}': ${exception.message}", exception)
        logger.error("   URL: ${station.url}")
        logger.error("   Resolved URL: ${station.urlResolved}")
        logger.error("   Exception Severity: ${exception.severity}")
    }
} 