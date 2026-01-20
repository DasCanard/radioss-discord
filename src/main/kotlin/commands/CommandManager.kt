package me.richy.radioss.commands

import me.richy.radioss.api.RadioBrowserAPI
import me.richy.radioss.bot.VoiceChannelManager
import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.handlers.SearchHandler
import me.richy.radioss.services.AdminService
import me.richy.radioss.services.FavoriteService
import me.richy.radioss.services.WebhookLogger
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory

class CommandManager(
    private val api: RadioBrowserAPI,
    private val audioHandler: AudioHandler,
    private val searchHandler: SearchHandler,
    private val favoriteService: FavoriteService,
    private val uiBuilder: UIBuilder,
    private val voiceChannelManager: VoiceChannelManager,
    private val webhookLogger: WebhookLogger,
    private val adminService: AdminService,
    private val jda: JDA?
) {
    private val logger = LoggerFactory.getLogger(CommandManager::class.java)
    
    private val commands: Map<String, Command> = createCommands()
    
    private fun createCommands(): Map<String, Command> {
        return mapOf(
            // Search Commands
            "search" to SearchCommand(api, searchHandler, favoriteService, uiBuilder),
            "top" to TopCommand(api, searchHandler, favoriteService, uiBuilder),
            "country" to CountryCommand(api, searchHandler, favoriteService, uiBuilder),
            "genre" to GenreCommand(api, searchHandler, favoriteService, uiBuilder),
            "random" to RandomCommand(api, audioHandler, uiBuilder, webhookLogger),
            "help" to HelpCommand(uiBuilder),
            
            // Audio Commands
            "play" to PlayCommand(audioHandler, uiBuilder, webhookLogger),
            "stop" to StopCommand(audioHandler, uiBuilder),
            "volume" to VolumeCommand(audioHandler, uiBuilder),
            "nowplaying" to NowPlayingCommand(audioHandler, favoriteService, uiBuilder),
            "247" to Command247(audioHandler, voiceChannelManager, uiBuilder),
            
            // Other Commands
            "favorites" to FavoritesCommand(favoriteService, searchHandler, uiBuilder),
            
            // Admin Commands
            "status" to StatusCommand(audioHandler, adminService, uiBuilder, jda)
        )
    }
    
    fun getAllCommands(): List<SlashCommandData> {
        // Alle Commands außer Admin-Commands
        return commands.filterKeys { it != "status" }.values.map { it.getCommandData() }
    }
    
    fun getAdminCommands(): List<SlashCommandData> {
        // Nur Admin-Commands
        return commands.filterKeys { it == "status" }.values.map { it.getCommandData() }
    }
    
    fun executeCommand(event: SlashCommandInteractionEvent) {
        val commandName = event.name
        val command = commands[commandName]
        
        if (command != null) {
            try {
                command.execute(event)
            } catch (e: Exception) {
                logger.error("Error executing command '$commandName'", e)
                throw e
            }
        } else {
            logger.warn("Unknown command: $commandName")
        }
    }
}

