package me.richy.radioss.bot

import me.richy.radioss.services.FavoriteService
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory

class BotLifecycleManager(
    private val token: String,
    private val favoriteService: FavoriteService,
    private val voiceChannelManager: VoiceChannelManager
) {
    private val logger = LoggerFactory.getLogger(BotLifecycleManager::class.java)
    private var jda: JDA? = null
    
    fun start(eventListener: Any): JDA {
        try {
            logger.info("Starting Radioss...")
            
            jda = JDABuilder.createDefault(token)
                .addEventListeners(eventListener)
                .enableIntents(
                    GatewayIntent.GUILD_VOICE_STATES,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT
                )
                .setActivity(Activity.listening("Radio Streams 🎵"))
                .build()
                
            logger.info("Bot started successfully!")
            return jda!!
            
        } catch (e: Exception) {
            logger.error("Error starting bot", e)
            throw e
        }
    }
    
    fun stop() {
        logger.info("Stopping Radioss...")
        
        voiceChannelManager.cancelAllTimers()
        
        favoriteService.close()
        
        jda?.shutdown()
        logger.info("Bot stopped")
    }
    
    fun getJDA(): JDA? = jda
}

