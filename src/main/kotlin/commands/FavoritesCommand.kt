package me.richy.radioss.commands

import me.richy.radioss.handlers.SearchHandler
import me.richy.radioss.services.FavoriteService
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class FavoritesCommand(
    private val favoriteService: FavoriteService,
    private val searchHandler: SearchHandler,
    private val uiBuilder: UIBuilder
) : Command {
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("favorites", "Manage your favorite radio stations")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val userId = event.user.id
        event.deferReply().setEphemeral(true).queue()
        
        val favoriteStations = favoriteService.getFavoriteStations(userId)
        
        if (favoriteStations.isEmpty()) {
            val embed = uiBuilder.createErrorEmbed(
                "No Favorites",
                "You haven't favorited any stations yet. Use the favorite buttons in search results or now playing to add favorites!"
            )
            event.hook.editOriginalEmbeds(embed).queue()
            return
        }
        
        // Speichere Favoriten in SearchHandler für Pagination
        searchHandler.setUserSearchResults(userId, favoriteStations)
        searchHandler.setUserCurrentPage(userId, 1)
        searchHandler.setUserSearchTerm(userId, "favorites")
        
        val totalPages = (favoriteStations.size + SearchHandler.STATIONS_PER_PAGE - 1) / SearchHandler.STATIONS_PER_PAGE
        val currentPageStations = favoriteStations.take(SearchHandler.STATIONS_PER_PAGE)
        
        val embed = uiBuilder.createFavoritesListEmbed(
            currentPageStations, 1, totalPages
        )
        
        val buttons = uiBuilder.createFavoritesButtons(currentPageStations, 1, totalPages)
        
        event.hook.editOriginalEmbeds(embed)
            .setComponents(buttons)
            .queue()
    }
}

