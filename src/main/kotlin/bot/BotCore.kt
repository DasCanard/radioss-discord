package me.richy.radioss.bot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.richy.radioss.api.RadioBrowserAPI
import me.richy.radioss.commands.CommandManager
import me.richy.radioss.database.FavoriteDatabase
import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.handlers.ButtonHandler
import me.richy.radioss.handlers.SearchHandler
import me.richy.radioss.handlers.SelectMenuHandler
import me.richy.radioss.services.AdminService
import me.richy.radioss.services.FavoriteService
import me.richy.radioss.services.WebhookLogger
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class BotCore(private val token: String) : ListenerAdapter() {
    private val api = RadioBrowserAPI()
    private val uiBuilder = UIBuilder()
    
    private val favoriteDatabase = FavoriteDatabase()
    private val favoriteService = FavoriteService(favoriteDatabase)
    
    private val searchHandler = SearchHandler()
    private val audioHandler = AudioHandler()
    
    private val webhookLogger = WebhookLogger()
    private val adminService = AdminService()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    
    private val voiceChannelManager = VoiceChannelManager(null, audioHandler, coroutineScope)
    private var commandManager: CommandManager? = null
    private var buttonHandler: ButtonHandler? = null
    private val selectMenuHandler = SelectMenuHandler(audioHandler, uiBuilder)
    
    private val reconnectionModule = ReconnectionModule(audioHandler, voiceChannelManager, coroutineScope)
    private val uptimeHeartbeat = UptimeHeartbeat(coroutineScope)
    
    private var readyEventHandler: ReadyEventHandler? = null
    private var interactionDispatcher: InteractionEventDispatcher? = null
    
    private val lifecycleManager = BotLifecycleManager(token, favoriteService, voiceChannelManager)
    
    private var jda: JDA? = null
    
    fun start() {
        jda = lifecycleManager.start(this)
        voiceChannelManager.updateJDA(jda)
        audioHandler.updateJDA(jda)
        
        // Initialisiere ButtonHandler mit WebhookLogger
        buttonHandler = ButtonHandler(searchHandler, audioHandler, favoriteService, uiBuilder, webhookLogger)
        
        // Initialisiere CommandManager mit JDA
        commandManager = CommandManager(
            api, 
            audioHandler, 
            searchHandler, 
            favoriteService, 
            uiBuilder, 
            voiceChannelManager,
            webhookLogger,
            adminService,
            jda
        )
        
        // Aktualisiere CommandRegistrar und InteractionDispatcher mit neuem CommandManager
        val updatedCommandRegistrar = CommandRegistrar(commandManager!!, adminService)
        interactionDispatcher = InteractionEventDispatcher(commandManager!!, buttonHandler!!, selectMenuHandler, uiBuilder)
        
        // Initialisiere Reconnection-Modul
        if (jda != null) {
            reconnectionModule.initialize(jda!!)
        }
        
        // Erstelle ReadyEventHandler mit Reconnection-Modul
        readyEventHandler = ReadyEventHandler(
            updatedCommandRegistrar,
            reconnectionModule.getReconnectionManager(),
            reconnectionModule.getCleanupScheduler(),
            uptimeHeartbeat
        )
    }
    
    fun stop() {
        uptimeHeartbeat.stop()
        webhookLogger.shutdown()
        reconnectionModule.shutdown()
        lifecycleManager.stop()
    }
    
    override fun onReady(event: ReadyEvent) {
        readyEventHandler?.handleReady(event)
    }
    
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        interactionDispatcher?.handleSlashCommand(event)
    }
    
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        buttonHandler?.let {
            interactionDispatcher?.handleButtonInteraction(event)
        }
    }
    
    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        interactionDispatcher?.handleSelectMenuInteraction(event)
    }
    
    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        voiceChannelManager.handleVoiceUpdate(event)
    }
    
    fun getJDA(): JDA? = jda
}

