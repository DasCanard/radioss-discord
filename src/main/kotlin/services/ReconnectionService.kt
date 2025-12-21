package me.richy.radioss.services

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.richy.radioss.database.ReconnectionDatabase
import me.richy.radioss.models.RadioStation
import net.dv8tion.jda.api.JDA
import org.slf4j.LoggerFactory

class ReconnectionService(
    private val database: ReconnectionDatabase
) {
    private val logger = LoggerFactory.getLogger(ReconnectionService::class.java)
    private var jda: JDA? = null
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    fun saveState(guildId: String, channelId: String, station: RadioStation, mode247Enabled: Boolean): Boolean {
        val stationData = try {
            json.encodeToString(station)
        } catch (e: Exception) {
            logger.error("Error serializing station data for reconnection state", e)
            return false
        }
        
        val result = database.saveState(guildId, channelId, stationData, mode247Enabled)
        if (result) {
            logger.debug("Saved reconnection state for guild $guildId, channel $channelId")
        } else {
            logger.warn("Failed to save reconnection state for guild $guildId")
        }
        return result
    }
    
    fun loadState(guildId: String): ReconnectionState? {
        val dbState = database.loadState(guildId) ?: return null
        
        val station = try {
            json.decodeFromString<RadioStation>(dbState.stationData)
        } catch (e: Exception) {
            logger.error("Error deserializing station data for guild $guildId", e)
            return null
        }
        
        return ReconnectionState(
            guildId = dbState.guildId,
            channelId = dbState.channelId,
            station = station,
            mode247Enabled = dbState.mode247Enabled
        )
    }
    
    fun loadAllStates(): List<ReconnectionState> {
        val dbStates = database.loadAllStates()
        val states = mutableListOf<ReconnectionState>()
        
        for (dbState in dbStates) {
            val station = try {
                json.decodeFromString<RadioStation>(dbState.stationData)
            } catch (e: Exception) {
                logger.error("Error deserializing station data for guild ${dbState.guildId}", e)
                continue
            }
            
            states.add(
                ReconnectionState(
                    guildId = dbState.guildId,
                    channelId = dbState.channelId,
                    station = station,
                    mode247Enabled = dbState.mode247Enabled
                )
            )
        }
        
        return states
    }
    
    fun deleteState(guildId: String): Boolean {
        val result = database.deleteState(guildId)
        if (result) {
            logger.debug("Deleted reconnection state for guild $guildId")
        }
        return result
    }
    
    fun cleanupInvalidStates(): Int {
        val allStates = database.loadAllStates()
        val invalidGuildIds = mutableListOf<String>()
        
        if (jda == null) {
            logger.warn("JDA is null, cannot cleanup invalid states")
            return 0
        }
        
        for (state in allStates) {
            try {
                val guild = jda.getGuildById(state.guildId)
                
                // Prüfe ob Guild noch existiert
                if (guild == null) {
                    logger.debug("Guild ${state.guildId} no longer exists, marking for cleanup")
                    invalidGuildIds.add(state.guildId)
                    continue
                }
                
                // Prüfe ob Channel noch existiert
                val channel = guild.getVoiceChannelById(state.channelId)
                if (channel == null) {
                    logger.debug("Channel ${state.channelId} in guild ${state.guildId} no longer exists, marking for cleanup")
                    invalidGuildIds.add(state.guildId)
                    continue
                }
                
                // Prüfe ob Bot noch Zugriff auf Channel hat
                val selfMember = guild.selfMember
                if (!selfMember.hasAccess(channel)) {
                    logger.debug("Bot no longer has access to channel ${state.channelId} in guild ${state.guildId}, marking for cleanup")
                    invalidGuildIds.add(state.guildId)
                    continue
                }
                
            } catch (e: Exception) {
                logger.error("Error checking validity of state for guild ${state.guildId}", e)
                invalidGuildIds.add(state.guildId)
            }
        }
        
        if (invalidGuildIds.isNotEmpty()) {
            val deletedCount = database.deleteStates(invalidGuildIds)
            logger.info("Cleaned up $deletedCount invalid reconnection states")
            return deletedCount
        }
        
        logger.debug("No invalid reconnection states found")
        return 0
    }
    
    fun updateJDA(jda: JDA?) {
        this.jda = jda
    }
    
    data class ReconnectionState(
        val guildId: String,
        val channelId: String,
        val station: RadioStation,
        val mode247Enabled: Boolean
    )
}

