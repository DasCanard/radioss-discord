import me.richy.radioss.bot.RadioBot
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Main")

fun main() {
    logger.info("=== ${Version.FULL_VERSION} ===")
    
    val token = System.getenv("DISCORD_BOT_TOKEN")
    
    if (token.isNullOrEmpty()) {
        logger.error("DISCORD_BOT_TOKEN environment variable not set!")
        logger.info("Set the environment variable: export DISCORD_BOT_TOKEN=\"your_bot_token_here\"")
        System.exit(1)
    }
    
    try {
        val bot = RadioBot(token)
        
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutdown signal received...")
            bot.stop()
        })
        
        bot.start()
        
        logger.info("Bot is running! Press Ctrl+C to stop.")
        
    } catch (e: Exception) {
        logger.error("Critical error starting bot", e)
        System.exit(1)
    }
}