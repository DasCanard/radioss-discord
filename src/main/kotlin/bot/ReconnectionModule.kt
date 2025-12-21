package me.richy.radioss.bot

import kotlinx.coroutines.CoroutineScope
import me.richy.radioss.database.ReconnectionDatabase
import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.services.ReconnectionService
import net.dv8tion.jda.api.JDA
import org.slf4j.LoggerFactory

class ReconnectionModule(
    private val audioHandler: AudioHandler,
    private val voiceChannelManager: VoiceChannelManager,
    private val coroutineScope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(ReconnectionModule::class.java)
    
    private val reconnectionDatabase = ReconnectionDatabase()
    private val reconnectionService = ReconnectionService(reconnectionDatabase)
    private var reconnectionManager: ReconnectionManager? = null
    private val cleanupScheduler = CleanupScheduler(reconnectionService, coroutineScope)
    
    fun initialize(jda: JDA) {
        logger.info("Initializing ReconnectionModule...")
        
        reconnectionService.updateJDA(jda)
        audioHandler.setReconnectionService(reconnectionService)
        audioHandler.updateJDA(jda)
        voiceChannelManager.setReconnectionService(reconnectionService)
        
        reconnectionManager = ReconnectionManager(
            reconnectionService,
            audioHandler,
            voiceChannelManager,
            jda,
            coroutineScope
        )
        
        logger.info("ReconnectionModule initialized")
    }
    
    fun getReconnectionManager(): ReconnectionManager? = reconnectionManager
    
    fun getCleanupScheduler(): CleanupScheduler = cleanupScheduler
    
    fun shutdown() {
        logger.info("Shutting down ReconnectionModule...")
        cleanupScheduler.stop()
        reconnectionDatabase.close()
        logger.info("ReconnectionModule shut down")
    }
}

