package me.richy.radioss.commands

import me.richy.radioss.Version
import me.richy.radioss.handlers.AudioHandler
import me.richy.radioss.services.AdminService
import me.richy.radioss.ui.UIBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import java.lang.management.ManagementFactory
import java.time.Duration
import java.time.Instant

class StatusCommand(
    private val audioHandler: AudioHandler,
    private val adminService: AdminService,
    private val uiBuilder: UIBuilder,
    private val jda: JDA?
) : Command {
    
    private val startTime = Instant.now()
    
    override fun getCommandData(): SlashCommandData {
        return Commands.slash("status", "Zeige Bot-Status und Statistiken")
            .addSubcommands(
                SubcommandData("player", "Zeige laufende Player und Player-Statistiken"),
                SubcommandData("guild", "Zeige Guild-Statistiken"),
                SubcommandData("system", "Zeige System-Informationen")
            )
    }
    
    override fun execute(event: SlashCommandInteractionEvent) {
        // Prüfe Admin-Berechtigung
        if (!adminService.isAdmin(event.user.id)) {
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Keine Berechtigung",
                "Du hast keine Berechtigung, diesen Command auszuführen."
            )
            event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            return
        }
        
        val subcommand = event.subcommandName
        
        when (subcommand) {
            "player" -> handlePlayerSubcommand(event)
            "guild" -> handleGuildSubcommand(event)
            "system" -> handleSystemSubcommand(event)
            else -> {
                val errorEmbed = uiBuilder.createErrorEmbed(
                    "Ungültiger Subcommand",
                    "Bitte verwende einen gültigen Subcommand: `player`, `guild` oder `system`"
                )
                event.replyEmbeds(errorEmbed).setEphemeral(true).queue()
            }
        }
    }
    
    private fun handlePlayerSubcommand(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        
        // Verwende Reflection oder eine Methode, um auf audioManagers zuzugreifen
        // Da audioManagers private ist, müssen wir eine Methode in AudioHandler hinzufügen
        val activePlayers = getActivePlayers()
        val totalPlayers = activePlayers.size
        val playingCount = activePlayers.count { it.isPlaying }
        
        val embed = net.dv8tion.jda.api.EmbedBuilder()
            .setTitle("🎵 Player Status")
            .setColor(java.awt.Color.CYAN)
            .addField("Aktive Player", "$totalPlayers", true)
            .addField("Laufende Streams", "$playingCount", true)
        
        if (activePlayers.isNotEmpty()) {
            val playerList = activePlayers.take(10).joinToString("\n") { player ->
                val guildId = player.guildId
                val station = player.currentStation
                val volume = player.volume
                val status = if (player.isPlaying) "▶️" else "⏸️"
                val stationName = station?.name ?: "Unbekannt"
                val guildName = jda?.getGuildById(guildId)?.name ?: "Unbekannt"
                "$status **$guildName** - $stationName (🔊 $volume%)"
            }
            
            if (activePlayers.size > 10) {
                embed.addField("Aktive Player (Top 10)", playerList, false)
                embed.setFooter("Zeige ${activePlayers.size - 10} weitere...")
            } else {
                embed.addField("Aktive Player", playerList, false)
            }
            
            // Durchschnittliche Lautstärke
            val avgVolume = activePlayers.map { it.volume }.average().toInt()
            embed.addField("Durchschnittliche Lautstärke", "$avgVolume%", true)
        } else {
            embed.addField("Keine aktiven Player", "Es laufen derzeit keine Streams.", false)
        }
        
        embed.setFooter(Version.FOOTER_TEXT)
        event.hook.editOriginalEmbeds(embed.build()).queue()
    }
    
    private fun handleGuildSubcommand(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        
        val jdaInstance = jda ?: run {
            val errorEmbed = uiBuilder.createErrorEmbed(
                "Fehler",
                "JDA-Instanz nicht verfügbar"
            )
            event.hook.editOriginalEmbeds(errorEmbed).queue()
            return
        }
        
        val guilds = jdaInstance.guilds
        val totalGuilds = guilds.size
        val totalMembers = guilds.sumOf { it.memberCount }
        val uniqueUsers = jdaInstance.users.size
        
        val embed = net.dv8tion.jda.api.EmbedBuilder()
            .setTitle("🏰 Guild Status")
            .setColor(java.awt.Color.GREEN)
            .addField("Guilds", "$totalGuilds", true)
            .addField("Gesamt User", "$totalMembers", true)
            .addField("Unique User", "$uniqueUsers", true)
        
        // Top 5 Guilds nach Mitgliederzahl
        val topGuilds = guilds.sortedByDescending { it.memberCount }.take(5)
        if (topGuilds.isNotEmpty()) {
            val topGuildsList = topGuilds.joinToString("\n") { guild ->
                "**${guild.name}** - ${guild.memberCount} Mitglieder"
            }
            embed.addField("Top 5 Guilds", topGuildsList, false)
        }
        
        embed.setFooter(Version.FOOTER_TEXT)
        event.hook.editOriginalEmbeds(embed.build()).queue()
    }
    
    private fun handleSystemSubcommand(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        
        val uptime = Duration.between(startTime, Instant.now())
        val uptimeFormatted = formatDuration(uptime)
        
        val runtime = ManagementFactory.getRuntimeMXBean()
        val jvmUptime = Duration.ofMillis(runtime.uptime)
        val jvmUptimeFormatted = formatDuration(jvmUptime)
        
        val memoryBean = ManagementFactory.getMemoryMXBean()
        val heapUsage = memoryBean.heapMemoryUsage
        val usedMemory = heapUsage.used / 1024 / 1024 // MB
        val maxMemory = heapUsage.max / 1024 / 1024 // MB
        val memoryPercent = (usedMemory * 100 / maxMemory).toInt()
        
        val javaVersion = System.getProperty("java.version")
        val javaVendor = System.getProperty("java.vendor")
        
        val embed = net.dv8tion.jda.api.EmbedBuilder()
            .setTitle("⚙️ System Status")
            .setColor(java.awt.Color.ORANGE)
            .addField("Bot Version", Version.FULL_VERSION, true)
            .addField("Bot Uptime", uptimeFormatted, true)
            .addField("JVM Uptime", jvmUptimeFormatted, true)
            .addField("Java Version", "$javaVersion\n$javaVendor", false)
            .addField("Memory", "${usedMemory}MB / ${maxMemory}MB ($memoryPercent%)", false)
            .addField("Admin User", "${adminService.getAdminCount()}", true)
        
        embed.setFooter(Version.FOOTER_TEXT)
        event.hook.editOriginalEmbeds(embed.build()).queue()
    }
    
    private fun formatDuration(duration: Duration): String {
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60
        
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m ${seconds}s"
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
    
    // Hilfsstruktur für Player-Informationen
    private data class PlayerInfo(
        val guildId: String,
        val isPlaying: Boolean,
        val currentStation: me.richy.radioss.models.RadioStation?,
        val volume: Int
    )
    
    // Methode zum Abrufen aktiver Player
    private fun getActivePlayers(): List<PlayerInfo> {
        val activePlayers = audioHandler.getAllActivePlayers()
        return activePlayers.map { (guildId, manager) ->
            PlayerInfo(
                guildId = guildId,
                isPlaying = manager.isPlaying(),
                currentStation = manager.getCurrentStation(),
                volume = manager.getVolume()
            )
        }
    }
}
