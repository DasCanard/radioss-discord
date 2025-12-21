package me.richy.radioss.commands

import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class HelpCommand(
    private val uiBuilder: UIBuilder
) : Command {
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("help", "Show help and available commands")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val helpEmbed = uiBuilder.createHelpEmbed()
        event.replyEmbeds(helpEmbed).setEphemeral(true).queue()
    }
}

