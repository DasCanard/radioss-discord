package me.richy.radioss.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

interface Command {
    fun getCommandData(): SlashCommandData
    fun execute(event: SlashCommandInteractionEvent)
}

