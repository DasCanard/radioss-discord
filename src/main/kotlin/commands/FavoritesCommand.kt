package me.richy.radioss.commands

import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class FavoritesCommand(
    private val uiBuilder: UIBuilder
) : Command {
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("favorites", "Manage your favorite radio stations")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val embed = uiBuilder.createErrorEmbed(
            "Not Yet Available",
            "The favorites function will be implemented in a future version."
        )
        event.replyEmbeds(embed).setEphemeral(true).queue()
    }
}

