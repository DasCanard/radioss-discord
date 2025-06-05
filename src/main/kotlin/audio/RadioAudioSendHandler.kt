package me.richy.radioss.audio

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class RadioAudioSendHandler(private val audioPlayer: AudioPlayer) : AudioSendHandler {
    private val buffer = ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())
    private val frame = MutableAudioFrame()
    
    init {
        frame.setBuffer(buffer)
    }
    
    override fun canProvide(): Boolean {
        return audioPlayer.provide(frame)
    }
    
    override fun provide20MsAudio(): ByteBuffer {
        buffer.flip()
        return buffer
    }
    
    override fun isOpus(): Boolean = true
} 