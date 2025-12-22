package me.richy.radioss.bot

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class UptimeHeartbeat(
    private val coroutineScope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(UptimeHeartbeat::class.java)
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    
    private var heartbeatJob: Job? = null
    private val heartbeatUrl = System.getenv("UPTIME_HEARTBEAT_URL")
    
    fun start() {
        if (heartbeatUrl.isNullOrEmpty()) {
            logger.debug("UPTIME_HEARTBEAT_URL not set, heartbeat disabled")
            return
        }
        
        if (heartbeatJob?.isActive == true) {
            logger.warn("Uptime heartbeat is already running")
            return
        }
        
        logger.info("Starting uptime heartbeat (interval: 1 minute)")
        logger.info("Heartbeat URL: $heartbeatUrl")
        
        // Sende ersten Heartbeat sofort
        sendHeartbeat()
        
        heartbeatJob = coroutineScope.launch {
            while (isActive) {
                try {
                    delay(60_000) // 1 Minute = 60.000 Millisekunden
                    sendHeartbeat()
                } catch (e: CancellationException) {
                    logger.debug("Heartbeat scheduler cancelled")
                    break
                } catch (e: Exception) {
                    logger.error("Error during heartbeat", e)
                }
            }
        }
    }
    
    private fun sendHeartbeat() {
        if (heartbeatUrl.isNullOrEmpty()) {
            return
        }
        
        try {
            val request = Request.Builder()
                .url(heartbeatUrl)
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    logger.debug("Heartbeat sent successfully (${response.code})")
                } else {
                    logger.warn("Heartbeat request failed with status ${response.code}")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to send heartbeat", e)
        }
    }
    
    fun stop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        logger.info("Uptime heartbeat stopped")
    }
}

