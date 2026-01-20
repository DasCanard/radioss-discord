package me.richy.radioss.commands

import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.actionrow.ActionRow

class VoteCommand(
    private val uiBuilder: UIBuilder
) : Command {
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("vote", "Vote for the bot on top.gg")
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        val voteUrl = "https://top.gg/bot/1376904142412316812#reviews"
        
        val embed = net.dv8tion.jda.api.EmbedBuilder()
            .setTitle("🗳️ Vote for Radioss")
            .setDescription("**Thank you for your support!**\n\n" +
                    "Your vote helps us reach more people and improve the bot further.\n\n" +
                    "Click the button below to vote on top.gg and leave a review!")
            .setColor(java.awt.Color.CYAN)
            .setFooter("Radioss Discord Bot")
            .build()
        
        val button = Button.link(voteUrl, "🗳️ Vote on top.gg")
        
        event.replyEmbeds(embed)
            .setComponents(ActionRow.of(button))
            .setEphemeral(false)
            .queue()
    }
}
