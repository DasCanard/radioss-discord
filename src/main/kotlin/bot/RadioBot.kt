package me.richy.radioss.bot

import me.richy.radioss.api.RadioBrowserAPI
import me.richy.radioss.audio.GuildAudioManager
import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.handlers.ButtonHandler
import me.richy.radioss.handlers.SearchHandler
import me.richy.radioss.handlers.SelectMenuHandler
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory

class RadioBot(private val token: String) : ListenerAdapter() {
    private val logger = LoggerFactory.getLogger(RadioBot::class.java)
    
    // Core Components
    private val api = RadioBrowserAPI()
    private val uiBuilder = UIBuilder()
    
    // Handler Classes
    private val searchHandler = SearchHandler(api, uiBuilder)
    private val audioHandler = AudioHandler(uiBuilder)
    private val buttonHandler = ButtonHandler(searchHandler, audioHandler, uiBuilder)
    private val selectMenuHandler = SelectMenuHandler(audioHandler, uiBuilder)
    
    private var jda: JDA? = null
    
    /**
     * Start bot
     */
    fun start() {
        try {
            logger.info("Starting Radioss...")
            
            jda = JDABuilder.createDefault(token)
                .addEventListeners(this)
                .enableIntents(
                    GatewayIntent.GUILD_VOICE_STATES,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT
                )
                .setActivity(Activity.listening("Radio Streams 🎵"))
                .build()
                
            logger.info("Bot started successfully!")
            
        } catch (e: Exception) {
            logger.error("Error starting bot", e)
            throw e
        }
    }
    
    /**
     * Stop bot
     */
    fun stop() {
        logger.info("Stopping Radioss...")
        jda?.shutdown()
        logger.info("Bot stopped")
    }
    
    /**
     * Bot Ready Event
     */
    override fun onReady(event: ReadyEvent) {
        val jda = event.jda
        logger.info("Bot '${jda.selfUser.name}' is ready!")
        logger.info("Bot is active on ${jda.guilds.size} servers")
        
        // Slash Commands registrieren
        registerCommands(jda)
    }
    
    /**
     * Slash Command Event
     */
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val commandName = event.name
        logger.info("Command '$commandName' from ${event.user.name} (${event.user.id})")
        
        try {
            when {
                isSearchCommand(commandName) -> {
                    searchHandler.handleSearchCommand(event)
                }
                
                isAudioCommand(commandName) -> {
                    audioHandler.handleAudioCommand(event)
                }
                
                commandName == "favorites" -> {
                    handleFavoritesCommand(event)
                }
                
                else -> {
                    logger.warn("Unknown command: $commandName")
                }
            }
        } catch (e: Exception) {
            logger.error("Error in command '$commandName'", e)
            
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Command Error",
                "An unexpected error occurred: ${e.message}"
            )
            
            if (event.isAcknowledged) {
                event.hook.editOriginalEmbeds(errorEmbed).queue()
            } else {
                event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            }
        }
    }
    
    /**
     * Button Interaction Event
     */
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val buttonId = event.componentId
        val userId = event.user.id
        
        logger.debug("Button '$buttonId' from user $userId")
        
        try {
            buttonHandler.handleButtonInteraction(event)
        } catch (e: Exception) {
            logger.error("Error with button '$buttonId'", e)
            
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Button Error",
                "Error with button action: ${e.message}"
            )
            
            if (event.isAcknowledged) {
                event.hook.sendMessageEmbeds(errorEmbed).setEphemeral(true).queue()
            } else {
                event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            }
        }
    }
    
    /**
     * String Select Menu Interaction Event
     */
    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        val menuId = event.componentId
        val userId = event.user.id
        
        logger.debug("Select menu '$menuId' from user $userId")
        
        try {
            selectMenuHandler.handleSelectMenuInteraction(event)
        } catch (e: Exception) {
            logger.error("Error with select menu '$menuId'", e)
            
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Select Menu Error",
                "Error with select menu action: ${e.message}"
            )
            
            if (event.isAcknowledged) {
                event.hook.sendMessageEmbeds(errorEmbed).setEphemeral(true).queue()
            } else {
                event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            }
        }
    }
    
    /**
     * Register Slash Commands
     */
    private fun registerCommands(jda: JDA) {
        logger.info("Registering slash commands...")
        
        val commands = mutableListOf<SlashCommandData>()

        commands.addAll(searchHandler.getSearchCommands())

        commands.addAll(audioHandler.getAudioCommands())
        
        // Add favorites command (temporarily disabled)
        // commands.add(Commands.slash("favorites", "Manage your favorite radio stations"))

        jda.updateCommands().addCommands(commands).queue(
            { 
                logger.info("${commands.size} slash commands registered globally")
                commands.forEach { cmd ->
                    logger.debug("   - /${cmd.name}: ${cmd.description}")
                }
            },
            { error -> 
                logger.error("Error registering global commands", error)
            }
        )

        jda.guilds.forEach { guild ->
            guild.updateCommands().addCommands(commands).queue(
                {
                    logger.debug("Commands registered for guild '${guild.name}'")
                },
                { error ->
                    logger.warn("Error registering for guild '${guild.name}': ${error.message}")
                }
            )
        }
    }

    private fun isSearchCommand(commandName: String): Boolean {
        return commandName in listOf(
            "search", "top", "country", 
            "genre", "random", "help"
        )
    }

    private fun isAudioCommand(commandName: String): Boolean {
        return commandName in listOf(
            "play", "stop", "volume", "nowplaying"
        )
    }

    private fun handleFavoritesCommand(event: SlashCommandInteractionEvent) {
        val embed = uiBuilder.createErrorEmbed(
            "Not Yet Available",
            "The favorites function will be implemented in a future version."
        )
        event.replyEmbeds(embed).setEphemeral(true).queue()
    }

    fun getJDA(): JDA? = jda
} 