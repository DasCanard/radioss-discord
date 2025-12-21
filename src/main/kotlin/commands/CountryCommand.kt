package me.richy.radioss.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.richy.radioss.api.RadioBrowserAPI
import me.richy.radioss.handlers.SearchHandler
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory

class CountryCommand(
    private val api: RadioBrowserAPI,
    private val searchHandler: SearchHandler,
    private val uiBuilder: UIBuilder
) : Command {
    private val logger = LoggerFactory.getLogger(CountryCommand::class.java)
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("country", "Search for radio stations by country")
            .addOption(OptionType.STRING, "country", "Country (e.g. Germany, Austria)", true)
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val country = event.getOption("country")?.asString ?: ""
        event.deferReply().queue()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stations = api.searchByCountry(country, SearchHandler.MAX_STATIONS)
                val userId = event.user.id
                
                searchHandler.setUserSearchResults(userId, stations)
                searchHandler.setUserCurrentPage(userId, 1)
                searchHandler.setUserSearchTerm(userId, country)
                
                val totalPages = (stations.size + SearchHandler.STATIONS_PER_PAGE - 1) / SearchHandler.STATIONS_PER_PAGE
                val currentPageStations = stations.take(SearchHandler.STATIONS_PER_PAGE)
                
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
}

