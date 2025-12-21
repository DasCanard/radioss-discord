package me.richy.radioss.bot

import kotlinx.coroutines.*
import me.richy.radioss.services.ReconnectionService
import org.slf4j.LoggerFactory

class CleanupScheduler(
    private val reconnectionService: ReconnectionService,
    private val coroutineScope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(CleanupScheduler::class.java)
    private var cleanupJob: Job? = null
    
    fun startPeriodicCleanup(intervalHours: Long = 24) {
        if (cleanupJob?.isActive == true) {
            logger.warn("Cleanup scheduler is already running")
            return
        }
        
        logger.info("Starting periodic cleanup scheduler (interval: ${intervalHours}h)")
        
        cleanupJob = coroutineScope.launch {
            while (isActive) {
                try {
                    delay(intervalHours * 3600_000) // Konvertiere Stunden zu Millisekunden
                    logger.info("Running periodic cleanup of reconnection states...")
                    val cleanedCount = reconnectionService.cleanupInvalidStates()
                    logger.info("Periodic cleanup completed, removed $cleanedCount invalid state(s)")
                } catch (e: CancellationException) {
                    logger.debug("Cleanup scheduler cancelled")
                    break
                } catch (e: Exception) {
                    logger.error("Error during periodic cleanup", e)
                }
            }
        }
    }
    
    fun stop() {
        cleanupJob?.cancel()
        cleanupJob = null
        logger.info("Cleanup scheduler stopped")
    }
    
    fun runCleanupNow(): Int {
        logger.info("Running immediate cleanup of reconnection states...")
        return try {
            val cleanedCount = reconnectionService.cleanupInvalidStates()
            logger.info("Immediate cleanup completed, removed $cleanedCount invalid state(s)")
            cleanedCount
        } catch (e: Exception) {
            logger.error("Error during immediate cleanup", e)
            0
        }
    }
}

