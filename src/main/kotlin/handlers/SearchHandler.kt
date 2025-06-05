package me.richy.radioss.handlers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.richy.radioss.api.RadioBrowserAPI
import me.richy.radioss.models.RadioStation
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class SearchHandler(
    private val api: RadioBrowserAPI,
    private val uiBuilder: UIBuilder
) {
    private val logger = LoggerFactory.getLogger(SearchHandler::class.java)
    
    private val userSearchResults = ConcurrentHashMap<String, List<RadioStation>>()
    private val userCurrentPage = ConcurrentHashMap<String, Int>()
    private val userSearchTerms = ConcurrentHashMap<String, String>()
    
    companion object {
        const val STATIONS_PER_PAGE = 5
        const val MAX_STATIONS = 50
    }
    
    fun getSearchCommands(): List<SlashCommandData> {
        return listOf(
            Commands.slash("search", "Search for radio stations by name")
                .addOption(OptionType.STRING, "name", "Name of the radio station", true),
            
            Commands.slash("top", "Show the most popular radio stations")
                .addOption(OptionType.INTEGER, "count", "Number of stations (max 50)", false),
            
            Commands.slash("country", "Search for radio stations by country")
                .addOption(OptionType.STRING, "country", "Country (e.g. Germany, Austria)", true),
            
            Commands.slash("genre", "Search for radio stations by genre")
                .addOption(OptionType.STRING, "genre", "Genre/Tag (e.g. rock, pop, jazz)", true),
            
            Commands.slash("random", "Random radio station"),
            
            Commands.slash("help", "Show help and available commands")
        )
    }
    
    fun handleSearchCommand(event: SlashCommandInteractionEvent) {
        val commandName = event.name
        val userId = event.user.id
        
        when (commandName) {
            "search" -> {
                val searchTerm = event.getOption("name")?.asString ?: ""
                handleSearch(event, searchTerm, "Search Results")
            }
            
            "top" -> {
                val count = event.getOption("count")?.asInt ?: 20
                val safeCount = count.coerceIn(1, MAX_STATIONS)
                handleTopStations(event, safeCount)
            }
            
            "country" -> {
                val country = event.getOption("country")?.asString ?: ""
                handleCountrySearch(event, country)
            }
            
            "genre" -> {
                val genre = event.getOption("genre")?.asString ?: ""
                handleGenreSearch(event, genre)
            }
            
            "random" -> {
                handleRandomStation(event)
            }
            
            "help" -> {
                handleHelp(event)
            }
        }
    }
    
    private fun handleSearch(event: SlashCommandInteractionEvent, searchTerm: String, title: String) {
        event.deferReply().queue()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stations = api.searchByName(searchTerm, MAX_STATIONS)
                val userId = event.user.id
                
                userSearchResults[userId] = stations
                userCurrentPage[userId] = 1
                userSearchTerms[userId] = searchTerm
                
                val totalPages = (stations.size + STATIONS_PER_PAGE - 1) / STATIONS_PER_PAGE
                val currentPageStations = stations.take(STATIONS_PER_PAGE)
                
                val embed = uiBuilder.createStationListEmbed(
                    currentPageStations, 1, totalPages, title, searchTerm
                )
                
                val buttons = uiBuilder.createPaginationButtons(1, totalPages, stations.isNotEmpty())
                
                event.hook.editOriginalEmbeds(embed)
                    .setComponents(buttons)
                    .queue()
                    
            } catch (e: Exception) {
                logger.error("Error searching for '$searchTerm'", e)
                val errorEmbed = uiBuilder.createErrorEmbed(
                    "Search Error", 
                    "Error during search: ${e.message}"
                )
                event.hook.editOriginalEmbeds(errorEmbed).queue()
            }
        }
    }
    
    private fun handleTopStations(event: SlashCommandInteractionEvent, count: Int) {
        event.deferReply().queue()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stations = api.getTopStations(count)
                val userId = event.user.id
                
                userSearchResults[userId] = stations
                userCurrentPage[userId] = 1
                userSearchTerms[userId] = "Top $count Stations"
                
                val totalPages = (stations.size + STATIONS_PER_PAGE - 1) / STATIONS_PER_PAGE
                val currentPageStations = stations.take(STATIONS_PER_PAGE)
                
                val embed = uiBuilder.createStationListEmbed(
                    currentPageStations, 1, totalPages, "Top $count Stations"
                )
                
                val buttons = uiBuilder.createPaginationButtons(1, totalPages, stations.isNotEmpty())
                
                event.hook.editOriginalEmbeds(embed)
                    .setComponents(buttons)
                    .queue()
                    
            } catch (e: Exception) {
                logger.error("Error loading top stations", e)
                val errorEmbed = uiBuilder.createErrorEmbed(
                    "Loading Error", 
                    "Error loading stations: ${e.message}"
                )
                event.hook.editOriginalEmbeds(errorEmbed).queue()
            }
        }
    }
    
    private fun handleCountrySearch(event: SlashCommandInteractionEvent, country: String) {
        event.deferReply().queue()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stations = api.searchByCountry(country, MAX_STATIONS)
                val userId = event.user.id
                
                userSearchResults[userId] = stations
                userCurrentPage[userId] = 1
                userSearchTerms[userId] = country
                
                val totalPages = (stations.size + STATIONS_PER_PAGE - 1) / STATIONS_PER_PAGE
                val currentPageStations = stations.take(STATIONS_PER_PAGE)
                
                val embed = uiBuilder.createStationListEmbed(
                    currentPageStations, 1, totalPages, "Stations from $country", country
                )
                
                val buttons = uiBuilder.createPaginationButtons(1, totalPages, stations.isNotEmpty())
                
                event.hook.editOriginalEmbeds(embed)
                    .setComponents(buttons)
                    .queue()
                    
            } catch (e: Exception) {
                logger.error("Error searching by country '$country'", e)
                val errorEmbed = uiBuilder.createErrorEmbed(
                    "Search Error", 
                    "Error during country search: ${e.message}"
                )
                event.hook.editOriginalEmbeds(errorEmbed).queue()
            }
        }
    }
    
    private fun handleGenreSearch(event: SlashCommandInteractionEvent, genre: String) {
        event.deferReply().queue()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stations = api.searchByTag(genre, MAX_STATIONS)
                val userId = event.user.id
                
                userSearchResults[userId] = stations
                userCurrentPage[userId] = 1
                userSearchTerms[userId] = genre
                
                val totalPages = (stations.size + STATIONS_PER_PAGE - 1) / STATIONS_PER_PAGE
                val currentPageStations = stations.take(STATIONS_PER_PAGE)
                
                val embed = uiBuilder.createStationListEmbed(
                    currentPageStations, 1, totalPages, "Genre: $genre", genre
                )
                
                val buttons = uiBuilder.createPaginationButtons(1, totalPages, stations.isNotEmpty())
                
                event.hook.editOriginalEmbeds(embed)
                    .setComponents(buttons)
                    .queue()
                    
            } catch (e: Exception) {
                logger.error("Error searching by genre '$genre'", e)
                val errorEmbed = uiBuilder.createErrorEmbed(
                    "Search Error", 
                    "Error during genre search: ${e.message}"
                )
                event.hook.editOriginalEmbeds(errorEmbed).queue()
            }
        }
    }
    
    private fun handleRandomStation(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stations = api.getTopStations(500)
                if (stations.isNotEmpty()) {
                    val randomStation = stations[Random.nextInt(stations.size)]
                    val embed = uiBuilder.createStationInfoEmbed(randomStation)
                    
                    event.hook.editOriginalEmbeds(embed).queue()
                } else {
                    val errorEmbed = uiBuilder.createErrorEmbed(
                        "No Stations", 
                        "No stations available"
                    )
                    event.hook.editOriginalEmbeds(errorEmbed).queue()
                }
            } catch (e: Exception) {
                logger.error("Error loading random station", e)
                val errorEmbed = uiBuilder.createErrorEmbed(
                    "Loading Error", 
                    "Error loading station: ${e.message}"
                )
                event.hook.editOriginalEmbeds(errorEmbed).queue()
            }
        }
    }
    
    private fun handleHelp(event: SlashCommandInteractionEvent) {
        val helpEmbed = uiBuilder.createHelpEmbed()
        event.replyEmbeds(helpEmbed).setEphemeral(true).queue()
    }
    
    fun getUserSearchResults(userId: String): List<RadioStation> {
        return userSearchResults[userId] ?: emptyList()
    }
    
    fun getUserCurrentPage(userId: String): Int {
        return userCurrentPage[userId] ?: 1
    }
    
    fun getUserSearchTerm(userId: String): String {
        return userSearchTerms[userId] ?: ""
    }
    
    fun setUserCurrentPage(userId: String, page: Int) {
        userCurrentPage[userId] = page
    }
    
    fun getPaginationData(userId: String): Triple<List<RadioStation>, Int, String> {
        return Triple(
            getUserSearchResults(userId),
            getUserCurrentPage(userId),
            getUserSearchTerm(userId)
        )
    }
}