package me.richy.radioss.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.richy.radioss.api.RadioBrowserAPI
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory
import kotlin.random.Random

class RandomCommand(
    private val api: RadioBrowserAPI,
    private val uiBuilder: UIBuilder
) : Command {
    private val logger = LoggerFactory.getLogger(RandomCommand::class.java)
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("random", "Random radio station")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
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
}

