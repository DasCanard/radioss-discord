package me.richy.radioss.commands

import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class FeedbackCommand(
    private val uiBuilder: UIBuilder
) : Command {
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("feedback", "Show feedback information and updates")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val discordLink = "https://discord.gg/zvVT5hA8Me"
        
        val message = "**📢 Radioss Discord Server**\n\n" +
                "For updates, changelogs, status updates and feedback, visit our Discord server:\n" +
                "**$discordLink**\n\n" +
                "There you'll find all current information and can also give direct feedback!"
        
        event.reply(message).setEphemeral(false).queue()
    }
}
