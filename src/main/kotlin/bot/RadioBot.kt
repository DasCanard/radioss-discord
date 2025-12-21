package me.richy.radioss.bot

import kotlinx.coroutines.*
import me.richy.radioss.commands.CommandManager
import me.richy.radioss.database.FavoriteDatabase
import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.handlers.ButtonHandler
import me.richy.radioss.handlers.SearchHandler
import me.richy.radioss.handlers.SelectMenuHandler
import me.richy.radioss.services.FavoriteService
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class RadioBot(private val token: String) : ListenerAdapter() {
    private val logger = LoggerFactory.getLogger(RadioBot::class.java)
    
    private val api = me.richy.radioss.api.RadioBrowserAPI()
    private val uiBuilder = UIBuilder()
    
    private val favoriteDatabase = FavoriteDatabase()
    private val favoriteService = FavoriteService(favoriteDatabase)
    
    private val searchHandler = SearchHandler()
    private val audioHandler = AudioHandler()
    private val commandManager = CommandManager(api, audioHandler, searchHandler, favoriteService, uiBuilder, this)
    private val buttonHandler = ButtonHandler(searchHandler, audioHandler, favoriteService, uiBuilder)
    private val selectMenuHandler = SelectMenuHandler(audioHandler, uiBuilder)
    
    private var jda: JDA? = null
    
    private val disconnectTimers = ConcurrentHashMap<String, Job>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val mode247Enabled = ConcurrentHashMap<String, Boolean>()
    
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
    
    fun stop() {
        logger.info("Stopping Radioss...")
        
        disconnectTimers.values.forEach { it.cancel() }
        disconnectTimers.clear()
        
        coroutineScope.cancel()
        
        favoriteService.close()
        
        jda?.shutdown()
        logger.info("Bot stopped")
    }
    
    override fun onReady(event: ReadyEvent) {
        val jda = event.jda
        logger.info("Bot '${jda.selfUser.name}' is ready!")
        logger.info("Bot is active on ${jda.guilds.size} servers")
        
        registerCommands(jda)
    }
    
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val commandName = event.name
        logger.info("Command '$commandName' from ${event.user.name} (${event.user.id})")
        
        try {
            commandManager.executeCommand(event)
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

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val guild = event.guild
        val guildId = guild.id
        val botMember = guild.selfMember
        val botVoiceState = botMember.voiceState
        
        if (botVoiceState == null || !botVoiceState.inAudioChannel()) {
            return
        }
        
        val botChannel = botVoiceState.channel
        if (botChannel == null) {
            return
        }
        
        val humanMembersCount = botChannel.members.count { member ->
            !member.user.isBot
        }
        
        logger.debug("Voice channel '${botChannel.name}' in guild '${guild.name}' has $humanMembersCount human members")
        
        if (humanMembersCount == 0) {
            // Nur Disconnect-Timer starten, wenn 24/7-Modus nicht aktiviert ist
            if (!is247ModeEnabled(guildId)) {
                startDisconnectTimer(guildId)
            } else {
                logger.debug("24/7-Modus aktiviert, kein Disconnect-Timer für guild '${guild.name}'")
            }
        } else {
            cancelDisconnectTimer(guildId)
        }
    }
    
    private fun startDisconnectTimer(guildId: String) {
        cancelDisconnectTimer(guildId)
        
        logger.info("Starting 30-second disconnect timer for guild $guildId")
        
        val timer = coroutineScope.launch {
            try {
                delay(30_000)
                
                val guild = jda?.getGuildById(guildId)
                if (guild != null) {
                    val botVoiceState = guild.selfMember.voiceState
                    
                    if (botVoiceState != null && botVoiceState.inAudioChannel()) {
                        val channel = botVoiceState.channel
                        val humanMembers = channel?.members?.count { !it.user.isBot } ?: 0
                        
                        if (humanMembers == 0) {
                            logger.info("Disconnecting from empty voice channel in guild '${guild.name}'")
                            
                            audioHandler.stopAudio(guildId)
                            guild.audioManager.closeAudioConnection()
                        } else {
                            logger.debug("Channel no longer empty, cancelling disconnect for guild '${guild.name}'")
                        }
                    }
                }
            } catch (e: CancellationException) {
                logger.debug("Disconnect timer cancelled for guild $guildId")
            } catch (e: Exception) {
                logger.error("Error in disconnect timer for guild $guildId", e)
            } finally {
                disconnectTimers.remove(guildId)
            }
        }
        
        disconnectTimers[guildId] = timer
    }
    
    private fun cancelDisconnectTimer(guildId: String) {
        disconnectTimers[guildId]?.let { timer ->
            timer.cancel()
            disconnectTimers.remove(guildId)
            logger.debug("Cancelled disconnect timer for guild $guildId")
        }
    }

    private fun registerCommands(jda: JDA) {
        logger.info("Registering slash commands...")
        
        val commands = commandManager.getAllCommands()

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
    
    fun set247Mode(guildId: String, enabled: Boolean) {
        if (enabled) {
            mode247Enabled[guildId] = true
            cancelDisconnectTimer(guildId)
            logger.info("24/7-Modus aktiviert für guild $guildId")
        } else {
            mode247Enabled.remove(guildId)
            logger.info("24/7-Modus deaktiviert für guild $guildId")
        }
    }
    
    fun is247ModeEnabled(guildId: String): Boolean {
        return mode247Enabled[guildId] ?: false
    }

    fun getJDA(): JDA? = jda
}