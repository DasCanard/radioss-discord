package me.richy.radioss.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import org.slf4j.LoggerFactory

class RadioTrackScheduler(private val player: AudioPlayer) : AudioEventAdapter() {
    private val logger = LoggerFactory.getLogger(RadioTrackScheduler::class.java)
    
    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        logger.info("Track started: ${track.info.title}")
        logger.info("   URI: ${track.info.uri}")
        logger.info("   Length: ${if (track.info.isStream) "Live Stream" else "${track.info.length}ms"}")
    }
    
    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        logger.info("Track ended: ${track.info.title}")
        logger.info("   Reason: $endReason")
        
        when (endReason) {
            AudioTrackEndReason.FINISHED -> {
                logger.info("Track finished normally")
            }
            AudioTrackEndReason.LOAD_FAILED -> {
                logger.error("Track could not be loaded")
            }
            AudioTrackEndReason.STOPPED -> {
                logger.info("Track was stopped")
            }
            AudioTrackEndReason.REPLACED -> {
                logger.info("Track was replaced")
            }
            AudioTrackEndReason.CLEANUP -> {
                logger.info("Track cleanup")
            }
        }
    }
    
    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: com.sedmelluq.discord.lavaplayer.tools.FriendlyException) {
        logger.error("Track exception for '${track.info.title}': ${exception.message}", exception)
    }
    
    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        logger.warn("Track stuck '${track.info.title}' for ${thresholdMs}ms")
    }
    
    fun queue(track: AudioTrack) {
        if (!player.startTrack(track, false)) {
            logger.warn("Player was already busy, track will not be played")
        }
    }
}