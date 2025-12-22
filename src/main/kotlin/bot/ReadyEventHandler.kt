package me.richy.radioss.bot

import kotlinx.coroutines.*
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.slf4j.LoggerFactory

class ReadyEventHandler(
    private val commandRegistrar: CommandRegistrar,
    private val reconnectionManager: ReconnectionManager?,
    private val cleanupScheduler: CleanupScheduler?,
    private val uptimeHeartbeat: UptimeHeartbeat
) {
    private val logger = LoggerFactory.getLogger(ReadyEventHandler::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    
    fun handleReady(event: ReadyEvent) {
        val jda = event.jda
        logger.info("Bot '${jda.selfUser.name}' is ready!")
        logger.info("Bot is active on ${jda.guilds.size} servers")
        
        commandRegistrar.registerCommands(jda)
        
        // Prüfe ob Reconnection aktiviert ist
        val enableReconnection = System.getenv("ENABLE_RECONNECTION")?.toBoolean() ?: false
        
        if (enableReconnection) {
            logger.info("Reconnection feature is enabled")
            
            // Führe Cleanup beim Start aus
            cleanupScheduler?.runCleanupNow()
            
            // Starte Reconnection in Coroutine
            coroutineScope.launch {
                try {
                    reconnectionManager?.reconnect()
                } catch (e: Exception) {
                    logger.error("Error during reconnection", e)
                }
            }
            
            // Starte periodische Cleanup
            cleanupScheduler?.startPeriodicCleanup()
        } else {
            logger.debug("Reconnection feature is disabled (ENABLE_RECONNECTION not set or false)")
        }
        
        // Starte Uptime Heartbeat
        uptimeHeartbeat.start()
    }
}

