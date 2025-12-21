package me.richy.radioss.bot

import kotlinx.coroutines.*
import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.services.ReconnectionService
import net.dv8tion.jda.api.JDA
import org.slf4j.LoggerFactory

class ReconnectionManager(
    private val reconnectionService: ReconnectionService,
    private val audioHandler: AudioHandler,
    private val voiceChannelManager: VoiceChannelManager,
    private val jda: JDA,
    private val coroutineScope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(ReconnectionManager::class.java)
    
    suspend fun reconnect() {
        logger.info("Starting reconnection process...")
        
        val states = reconnectionService.loadAllStates()
        if (states.isEmpty()) {
            logger.info("No reconnection states found")
            return
        }
        
        logger.info("Found ${states.size} reconnection state(s) to restore")
        
        states.forEachIndexed { index, state ->
            try {
                logger.info("Processing reconnection ${index + 1}/${states.size} for guild ${state.guildId}")
                reconnectGuild(state)
                
                // Verzögerung zwischen Reconnections, um Rate Limits zu vermeiden
                if (index < states.size - 1) {
                    val delayMs = 1000L + (index * 200L) // 1s, 1.2s, 1.4s, etc.
                    logger.debug("Waiting ${delayMs}ms before next reconnection...")
                    delay(delayMs)
                }
            } catch (e: Exception) {
                logger.error("Error reconnecting guild ${state.guildId}", e)
                // Lösche State bei Fehler, um zukünftige fehlgeschlagene Versuche zu vermeiden
                reconnectionService.deleteState(state.guildId)
            }
        }
        
        logger.info("Reconnection process completed")
    }
    
    private suspend fun reconnectGuild(state: ReconnectionService.ReconnectionState) {
        try {
            val guild = jda.getGuildById(state.guildId)
            if (guild == null) {
                logger.warn("Guild ${state.guildId} no longer exists, skipping reconnection")
                reconnectionService.deleteState(state.guildId)
                return
            }
            
            val channel = guild.getVoiceChannelById(state.channelId)
            if (channel == null) {
                logger.warn("Channel ${state.channelId} in guild ${guild.name} no longer exists, skipping reconnection")
                reconnectionService.deleteState(state.guildId)
                return
            }
            
            // Prüfe ob Bot Zugriff auf Channel hat
            val selfMember = guild.selfMember
            if (!selfMember.hasAccess(channel)) {
                logger.warn("Bot no longer has access to channel ${channel.name} in guild ${guild.name}, skipping reconnection")
                reconnectionService.deleteState(state.guildId)
                return
            }
            
            logger.info("Reconnecting to channel '${channel.name}' in guild '${guild.name}'")
            
            // Joine in Channel
            try {
                guild.audioManager.openAudioConnection(channel)
                logger.info("Connection request sent to voice channel '${channel.name}'")
                
                // Warte kurz, damit die Verbindung hergestellt werden kann
                delay(500)
                
                // Prüfe ob Verbindung erfolgreich war
                val connectedChannel = guild.audioManager.connectedChannel
                if (connectedChannel == null || connectedChannel.id != channel.id) {
                    logger.warn("Failed to verify connection to channel '${channel.name}' in guild '${guild.name}'")
                    reconnectionService.deleteState(state.guildId)
                    return
                }
                
                logger.info("Successfully connected to voice channel '${channel.name}'")
            } catch (e: Exception) {
                logger.error("Failed to connect to voice channel '${channel.name}' in guild '${guild.name}'", e)
                reconnectionService.deleteState(state.guildId)
                return
            }
            
            // Warte kurz vor dem Setzen des Audio Handlers
            delay(300)
            
            // Setze Audio Handler
            val guildAudioManager = audioHandler.getOrCreateAudioManager(state.guildId)
            guild.audioManager.sendingHandler = guildAudioManager.getSendHandler()
            logger.debug("Audio send handler set for guild '${guild.name}'")
            
            // Warte kurz vor dem Starten der Station
            delay(300)
            
            // Starte Radio-Station
            audioHandler.playStation(state.guildId, state.station)
            logger.info("Started playing station '${state.station.name}' in guild '${guild.name}'")
            
            // Stelle 24/7-Modus wieder her
            if (state.mode247Enabled) {
                voiceChannelManager.set247Mode(state.guildId, true)
                logger.info("Restored 24/7 mode for guild '${guild.name}'")
            }
            
            logger.info("Successfully reconnected guild '${guild.name}' (${state.guildId})")
            
        } catch (e: Exception) {
            logger.error("Error during reconnection for guild ${state.guildId}", e)
            // Lösche State bei Fehler, um zukünftige fehlgeschlagene Versuche zu vermeiden
            reconnectionService.deleteState(state.guildId)
        }
    }
}

