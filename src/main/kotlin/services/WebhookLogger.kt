package me.richy.radioss.services

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.time.Instant

class WebhookLogger {
    private val logger = LoggerFactory.getLogger(WebhookLogger::class.java)
    private val httpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val webhookUrl: String? = System.getenv("LOG_WEBHOOK_URL")?.takeIf { it.isNotBlank() }
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        if (webhookUrl == null) {
            logger.debug("LOG_WEBHOOK_URL not set, webhook logging disabled")
        } else {
            logger.info("Webhook logging enabled")
        }
    }
    
    fun logPlayCommand(userId: String, userName: String, guildId: String, guildName: String, stationName: String, stationUrl: String) {
        if (webhookUrl == null) return
        
        val embed = WebhookEmbed(
            title = "🎵 Play Command",
            description = "**$userName** hat einen Stream gestartet",
            color = 0x00FF00, // Grün
            fields = listOf(
                WebhookEmbedField("User", "$userName\n`$userId`", true),
                WebhookEmbedField("Guild", "$guildName\n`$guildId`", true),
                WebhookEmbedField("Station", stationName, false),
                WebhookEmbedField("URL", "[Link]($stationUrl)", false)
            ),
            timestamp = Instant.now().atOffset(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_DATE_TIME)
        )
        
        sendWebhook(embed)
    }
    
    fun logError(message: String, exception: Exception? = null) {
        if (webhookUrl == null) return
        
        val errorDetails = exception?.let { 
            "${it.javaClass.simpleName}: ${it.message}\n```\n${it.stackTrace.take(5).joinToString("\n")}\n```"
        } ?: message
        
        val embed = WebhookEmbed(
            title = "❌ Error",
            description = message,
            color = 0xFF0000, // Rot
            fields = listOf(
                WebhookEmbedField("Details", errorDetails.take(1024), false)
            ),
            timestamp = Instant.now().atOffset(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_DATE_TIME)
        )
        
        sendWebhook(embed)
    }
    
    fun logInfo(message: String) {
        if (webhookUrl == null) return
        
        val embed = WebhookEmbed(
            title = "ℹ️ Info",
            description = message,
            color = 0x0099FF, // Blau
            timestamp = Instant.now().atOffset(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_DATE_TIME)
        )
        
        sendWebhook(embed)
    }
    
    private fun sendWebhook(embed: WebhookEmbed) {
        if (webhookUrl == null) return
        
        coroutineScope.launch {
            try {
                val webhookPayload = WebhookPayload(
                    embeds = listOf(embed)
                )
                
                val jsonPayload = json.encodeToString(webhookPayload)
                val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(requestBody)
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    val errorBody = try {
                        response.body?.string() ?: "No error body"
                    } catch (e: Exception) {
                        "Error reading error body: ${e.message}"
                    }
                    logger.warn("Webhook request failed with code ${response.code}: $errorBody")
                    logger.debug("Sent payload: $jsonPayload")
                } else {
                    logger.debug("Webhook sent successfully")
                }
                
                response.close()
            } catch (e: Exception) {
                logger.error("Error sending webhook", e)
            }
        }
    }
    
    fun shutdown() {
        coroutineScope.cancel()
    }
}

@Serializable
private data class WebhookPayload(
    val embeds: List<WebhookEmbed>
)

@Serializable
private data class WebhookEmbed(
    val title: String,
    val description: String? = null,
    val color: Int? = null,
    val fields: List<WebhookEmbedField>? = null,
    val timestamp: String? = null
)

@Serializable
private data class WebhookEmbedField(
    val name: String,
    val value: String,
    val inline: Boolean = false
)
