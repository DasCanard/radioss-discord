package me.richy.radioss.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.richy.radioss.api.RadioBrowserAPI
import me.richy.radioss.handlers.SearchHandler
import me.richy.radioss.services.FavoriteService
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory

class GenreCommand(
    private val api: RadioBrowserAPI,
    private val searchHandler: SearchHandler,
    private val favoriteService: FavoriteService,
    private val uiBuilder: UIBuilder
) : Command {
    private val logger = LoggerFactory.getLogger(GenreCommand::class.java)
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("genre", "Search for radio stations by genre")
            .addOption(OptionType.STRING, "genre", "Genre/Tag (e.g. rock, pop, jazz)", true)
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val genre = event.getOption("genre")?.asString ?: ""
        event.deferReply().queue()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stations = api.searchByTag(genre, SearchHandler.MAX_STATIONS)
                val userId = event.user.id
                
                searchHandler.setUserSearchResults(userId, stations)
                searchHandler.setUserCurrentPage(userId, 1)
                searchHandler.setUserSearchTerm(userId, genre)
                
                val totalPages = (stations.size + SearchHandler.STATIONS_PER_PAGE - 1) / SearchHandler.STATIONS_PER_PAGE
                val currentPageStations = stations.take(SearchHandler.STATIONS_PER_PAGE)
                
                val favoriteStatus = currentPageStations.associate { station ->
                    station.stationUuid to favoriteService.isFavorite(userId, station.stationUuid)
                }
                
                val embed = uiBuilder.createStationListEmbed(
                    currentPageStations, 1, totalPages, "Genre: $genre", genre, favoriteStatus
                )
                
                val buttons = uiBuilder.createPaginationButtons(
                    1, totalPages, stations.isNotEmpty(), currentPageStations, favoriteStatus
                )
                
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
}

