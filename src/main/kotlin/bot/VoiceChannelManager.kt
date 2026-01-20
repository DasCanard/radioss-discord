package me.richy.radioss.bot

import kotlinx.coroutines.*
import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.services.ReconnectionService
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class VoiceChannelManager(
    private var jda: JDA?,
    private val audioHandler: AudioHandler,
    private val coroutineScope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(VoiceChannelManager::class.java)
    
    private val disconnectTimers = ConcurrentHashMap<String, Job>()
    private val mode247Enabled = ConcurrentHashMap<String, Boolean>()
    private var reconnectionService: ReconnectionService? = null
    
    fun setReconnectionService(reconnectionService: ReconnectionService) {
        this.reconnectionService = reconnectionService
    }
    
    fun updateJDA(jda: JDA?) {
        this.jda = jda
    }
    
    fun handleVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val guild = event.guild
        val guildId = guild.id
        val botMember = guild.selfMember
        val botVoiceState = botMember.voiceState
        
        if (botVoiceState == null || !botVoiceState.inAudioChannel()) {
            return
        }
        
        val botChannel = botVoiceState.channel
        if (botChannel == null) {
            return
        }
        
        val humanMembersCount = botChannel.members.count { member ->
            !member.user.isBot
        }
        
        logger.debug("Voice channel '${botChannel.name}' in guild '${guild.name}' has $humanMembersCount human members")
        
        if (humanMembersCount == 0) {
            // Only start disconnect timer if 24/7 mode is not enabled
            if (!is247ModeEnabled(guildId)) {
                startDisconnectTimer(guildId)
            } else {
                logger.debug("24/7 mode enabled, no disconnect timer for guild '${guild.name}'")
            }
        } else {
            cancelDisconnectTimer(guildId)
        }
    }
    
    private fun startDisconnectTimer(guildId: String) {
        cancelDisconnectTimer(guildId)
        
        logger.info("Starting 30-second disconnect timer for guild $guildId")
        
        val timer = coroutineScope.launch {
            try {
                delay(30_000)
                
                val guild = jda?.getGuildById(guildId)
                if (guild != null) {
                    val botVoiceState = guild.selfMember.voiceState
                    
                    if (botVoiceState != null && botVoiceState.inAudioChannel()) {
                        val channel = botVoiceState.channel
                        val humanMembers = channel?.members?.count { !it.user.isBot } ?: 0
                        
                        if (humanMembers == 0) {
                            logger.info("Disconnecting from empty voice channel in guild '${guild.name}'")
                            
                            audioHandler.stopAudio(guildId)
                            guild.audioManager.closeAudioConnection()
                        } else {
                            logger.debug("Channel no longer empty, cancelling disconnect for guild '${guild.name}'")
                        }
                    }
                }
            } catch (e: CancellationException) {
                logger.debug("Disconnect timer cancelled for guild $guildId")
            } catch (e: Exception) {
                logger.error("Error in disconnect timer for guild $guildId", e)
            } finally {
                disconnectTimers.remove(guildId)
            }
        }
        
        disconnectTimers[guildId] = timer
    }
    
    private fun cancelDisconnectTimer(guildId: String) {
        disconnectTimers[guildId]?.let { timer ->
            timer.cancel()
            disconnectTimers.remove(guildId)
            logger.debug("Cancelled disconnect timer for guild $guildId")
        }
    }
    
    fun set247Mode(guildId: String, enabled: Boolean) {
        if (enabled) {
            mode247Enabled[guildId] = true
            cancelDisconnectTimer(guildId)
            logger.info("24/7 mode enabled for guild $guildId")
        } else {
            mode247Enabled.remove(guildId)
            logger.info("24/7 mode disabled for guild $guildId")
        }
        
        // Update reconnection state
        updateReconnectionState(guildId, enabled)
    }
    
    private fun updateReconnectionState(guildId: String, mode247Enabled: Boolean) {
        val service = reconnectionService ?: return
        val jdaInstance = jda ?: return
        
        try {
            val guild = jdaInstance.getGuildById(guildId) ?: return
            val audioManager = guild.audioManager
            val connectedChannel = audioManager.connectedChannel ?: return
            
            val currentStation = audioHandler.getCurrentStation(guildId) ?: return
            
            service.saveState(guildId, connectedChannel.id, currentStation, mode247Enabled)
            logger.debug("Updated reconnection state for guild $guildId with 24/7 mode: $mode247Enabled")
        } catch (e: Exception) {
            logger.error("Error updating reconnection state for guild $guildId", e)
        }
    }
    
    fun is247ModeEnabled(guildId: String): Boolean {
        return mode247Enabled[guildId] ?: false
    }
    
    fun cancelAllTimers() {
        disconnectTimers.values.forEach { it.cancel() }
        disconnectTimers.clear()
    }
}

