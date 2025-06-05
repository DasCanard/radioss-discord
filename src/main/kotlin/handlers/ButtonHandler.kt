package me.richy.radioss.handlers

import me.richy.radioss.models.RadioStation
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import org.slf4j.LoggerFactory

class ButtonHandler(
    private val searchHandler: SearchHandler,
    private val audioHandler: AudioHandler,
    private val uiBuilder: UIBuilder
) {
    private val logger = LoggerFactory.getLogger(ButtonHandler::class.java)
    
    fun handleButtonInteraction(event: ButtonInteractionEvent) {
        val buttonId = event.componentId
        val userId = event.user.id
        val guildId = event.guild?.id ?: return
        
        when {
            buttonId.startsWith("play_") -> handlePlayButton(event, buttonId, userId, guildId)
            buttonId == "stop_audio" -> handleStopButton(event, guildId)
            buttonId == "now_playing" -> handleNowPlayingButton(event, guildId)
            buttonId == "refresh_search" -> handleRefreshButton(event, userId)
            buttonId == "first_page" -> handlePageNavigation(event, userId, 1)
            buttonId == "prev_page" -> handlePreviousPage(event, userId)
            buttonId == "next_page" -> handleNextPage(event, userId)
            buttonId == "last_page" -> handleLastPage(event, userId)
            buttonId == "page_info" -> {
                event.deferEdit().queue()
            }
        }
    }
    
    private fun handlePlayButton(event: ButtonInteractionEvent, buttonId: String, userId: String, guildId: String) {
        val member = event.member ?: return
        val voiceState = member.voiceState ?: return
        
        if (!voiceState.inAudioChannel()) {
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Not in Voice Channel",
                "You must be in a voice channel to listen to radio!"
            )
            event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            return
        }
        
        val stationIndex = try {
            buttonId.removePrefix("play_").toInt() - 1
        } catch (e: NumberFormatException) {
            logger.error("Ungültige Button ID: $buttonId", e)
            return
        }
        
        val (searchResults, currentPage, _) = searchHandler.getPaginationData(userId)
        val startIndex = (currentPage - 1) * SearchHandler.STATIONS_PER_PAGE + stationIndex
        
        if (startIndex >= 0 && startIndex < searchResults.size) {
            val station = searchResults[startIndex]
            
            val guild = event.guild ?: return
            val audioChannel = voiceState.channel ?: return
            
            event.deferEdit().queue()
            
            try {
                guild.audioManager.openAudioConnection(audioChannel)
                logger.info("Bot connected to voice channel: ${audioChannel.name}")
                
                val guildAudioManager = audioHandler.getOrCreateAudioManager(guildId)
                guild.audioManager.sendingHandler = guildAudioManager.getSendHandler()
                logger.info("Audio send handler set for guild $guildId")
                
                audioHandler.playStation(guildId, station)
                logger.info("Playing station '${station.name}' (URL: ${station.url}) for user $userId in guild $guildId")
                
                val successEmbed = uiBuilder.createSuccessEmbed(
                    "🎵 Now Playing",
                    "**${station.name}** is now playing!"
                )
                
                event.hook.sendMessageEmbeds(successEmbed)
                    .setEphemeral(true)
                    .queue()
                
            } catch (e: Exception) {
                logger.error("Error playing station '${station.name}'", e)
                
                val errorEmbed = uiBuilder.createErrorEmbed(
                    "Playback Error",
                    "Error playing station: ${e.message}"
                )
                
                event.hook.sendMessageEmbeds(errorEmbed)
                    .setEphemeral(true)
                    .queue()
            }
        } else {
            logger.warn("Invalid station index: $startIndex for user $userId")
            event.deferEdit().queue()
        }
    }
    
    private fun handleStopButton(event: ButtonInteractionEvent, guildId: String) {
        event.deferEdit().queue()
        
        try {
            audioHandler.stopAudio(guildId)
            
            val guild = event.guild
            guild?.audioManager?.closeAudioConnection()
            
            val successEmbed = uiBuilder.createSuccessEmbed(
                "⏹️ Stopped",
                "Playback has been stopped"
            )
            
            event.hook.sendMessageEmbeds(successEmbed)
                .setEphemeral(true)
                .queue()
                
            logger.info("Audio stopped in guild $guildId by user ${event.user.id}")
            
        } catch (e: Exception) {
            logger.error("Error stopping playback", e)
            
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Error",
                "Error stopping playback: ${e.message}"
            )
            
            event.hook.sendMessageEmbeds(errorEmbed)
                .setEphemeral(true)
                .queue()
        }
    }
    
    private fun handleNowPlayingButton(event: ButtonInteractionEvent, guildId: String) {
        val currentStation = audioHandler.getCurrentStation(guildId)
        val volume = audioHandler.getCurrentVolume(guildId)
        val isPlaying = audioHandler.isPlaying(guildId)
        
        val embed = uiBuilder.createAudioStatusEmbed(currentStation, volume, isPlaying)
        
        event.replyEmbeds(embed).setEphemeral(true).queue()
    }
    
    private fun handleRefreshButton(event: ButtonInteractionEvent, userId: String) {
        event.deferEdit().queue()
        
        val (searchResults, currentPage, searchTerm) = searchHandler.getPaginationData(userId)
        
        if (searchResults.isNotEmpty()) {
            val totalPages = (searchResults.size + SearchHandler.STATIONS_PER_PAGE - 1) / SearchHandler.STATIONS_PER_PAGE
            val startIndex = (currentPage - 1) * SearchHandler.STATIONS_PER_PAGE
            val endIndex = (startIndex + SearchHandler.STATIONS_PER_PAGE).coerceAtMost(searchResults.size)
            val currentPageStations = searchResults.subList(startIndex, endIndex)
            
            val embed = uiBuilder.createStationListEmbed(
                currentPageStations, currentPage, totalPages, "Search Results (Refreshed)", searchTerm
            )
            
            val buttons = uiBuilder.createPaginationButtons(currentPage, totalPages, searchResults.isNotEmpty())
            
            event.hook.editOriginalEmbeds(embed)
                .setComponents(buttons)
                .queue()
        }
    }
    
    private fun handlePageNavigation(event: ButtonInteractionEvent, userId: String, targetPage: Int) {
        event.deferEdit().queue()
        
        val (searchResults, _, searchTerm) = searchHandler.getPaginationData(userId)
        val totalPages = (searchResults.size + SearchHandler.STATIONS_PER_PAGE - 1) / SearchHandler.STATIONS_PER_PAGE
        
        val validPage = targetPage.coerceIn(1, totalPages)
        searchHandler.setUserCurrentPage(userId, validPage)
        
        updatePaginationMessage(event, searchResults, validPage, totalPages, searchTerm)
    }
    
    private fun handlePreviousPage(event: ButtonInteractionEvent, userId: String) {
        val currentPage = searchHandler.getUserCurrentPage(userId)
        handlePageNavigation(event, userId, currentPage - 1)
    }
    
    private fun handleNextPage(event: ButtonInteractionEvent, userId: String) {
        val currentPage = searchHandler.getUserCurrentPage(userId)
        handlePageNavigation(event, userId, currentPage + 1)
    }
    
    private fun handleLastPage(event: ButtonInteractionEvent, userId: String) {
        val searchResults = searchHandler.getUserSearchResults(userId)
        val totalPages = (searchResults.size + SearchHandler.STATIONS_PER_PAGE - 1) / SearchHandler.STATIONS_PER_PAGE
        handlePageNavigation(event, userId, totalPages)
    }
    
    private fun updatePaginationMessage(
        event: ButtonInteractionEvent,
        searchResults: List<RadioStation>,
        currentPage: Int,
        totalPages: Int,
        searchTerm: String
    ) {
        val startIndex = (currentPage - 1) * SearchHandler.STATIONS_PER_PAGE
        val endIndex = (startIndex + SearchHandler.STATIONS_PER_PAGE).coerceAtMost(searchResults.size)
        val currentPageStations = searchResults.subList(startIndex, endIndex)
        
        val embed = uiBuilder.createStationListEmbed(
            currentPageStations, currentPage, totalPages, "Search Results", searchTerm
        )
        
        val buttons = uiBuilder.createPaginationButtons(currentPage, totalPages, searchResults.isNotEmpty())
        
        event.hook.editOriginalEmbeds(embed)
            .setComponents(buttons)
            .queue()
    }
}