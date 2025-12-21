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
import me.richy.radioss.services.FavoriteService
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
    
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    
    private val voiceChannelManager = VoiceChannelManager(null, audioHandler, coroutineScope)
    private val commandManager = CommandManager(api, audioHandler, searchHandler, favoriteService, uiBuilder, voiceChannelManager)
    private val buttonHandler = ButtonHandler(searchHandler, audioHandler, favoriteService, uiBuilder)
    private val selectMenuHandler = SelectMenuHandler(audioHandler, uiBuilder)
    
    private val commandRegistrar = CommandRegistrar(commandManager)
    private val interactionDispatcher = InteractionEventDispatcher(commandManager, buttonHandler, selectMenuHandler, uiBuilder)
    private val readyEventHandler = ReadyEventHandler(commandRegistrar)
    
    private val lifecycleManager = BotLifecycleManager(token, favoriteService, voiceChannelManager)
    
    private var jda: JDA? = null
    
    fun start() {
        jda = lifecycleManager.start(this)
        voiceChannelManager.updateJDA(jda)
    }
    
    fun stop() {
        lifecycleManager.stop()
    }
    
    override fun onReady(event: ReadyEvent) {
        readyEventHandler.handleReady(event)
    }
    
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        interactionDispatcher.handleSlashCommand(event)
    }
    
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        interactionDispatcher.handleButtonInteraction(event)
    }
    
    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        interactionDispatcher.handleSelectMenuInteraction(event)
    }
    
    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        voiceChannelManager.handleVoiceUpdate(event)
    }
    
    fun getJDA(): JDA? = jda
}

