package me.richy.radioss.ui

import Version
import me.richy.radioss.models.RadioStation
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import java.awt.Color

class UIBuilder {
    
    fun createStationListEmbed(
        stations: List<RadioStation>,
        currentPage: Int,
        totalPages: Int,
        title: String,
        searchTerm: String? = null
    ): MessageEmbed {
        val embed = EmbedBuilder()
            .setTitle("🎵 $title")
            .setColor(Color.CYAN)
        
        if (searchTerm != null) {
            embed.setDescription("**Searching:** `$searchTerm`")
        }
        
        if (stations.isEmpty()) {
            embed.addField("❌ No results", "No stations found.", false)
        } else {
            stations.forEachIndexed { index, station ->
                val number = (currentPage - 1) * 5 + index + 1
                val title = "$number. ${if (station.name.length > 30) station.name.take(30) + "..." else station.name}"
                val info = buildString {
                    if (station.country.isNotEmpty()) append("🌍 ${station.country}")
                    if (station.tags.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append("🎵 ${station.tags.split(",").take(2).joinToString(", ")}")
                    }
                    if (station.bitrate > 0) {
                        if (isNotEmpty()) append(" • ")
                        append("🎧 ${station.bitrate}kbps")
                    }
                    if (station.votes > 0) {
                        if (isNotEmpty()) append(" • ")
                        append("⭐ ${station.votes}")
                    }
                }
                embed.addField(title, info.ifEmpty { "No details available" }, false)
            }
        }
        
        if (totalPages > 1) {
            embed.setFooter("Page $currentPage of $totalPages • ${Version.FOOTER_TEXT}")
        } else {
            embed.setFooter(Version.FOOTER_TEXT)
        }
        
        return embed.build()
    }
    
    fun createPaginationButtons(currentPage: Int, totalPages: Int, hasStations: Boolean): List<ActionRow> {
        val rows = mutableListOf<ActionRow>()
        
        if (totalPages > 1) {
            val navButtons = listOf(
                Button.primary("first_page", "⏮️ First").withDisabled(currentPage == 1),
                Button.primary("prev_page", "◀️ Previous").withDisabled(currentPage == 1),
                Button.secondary("page_info", "Page $currentPage/$totalPages").asDisabled(),
                Button.primary("next_page", "▶️ Next").withDisabled(currentPage == totalPages),
                Button.primary("last_page", "⏭️ Last").withDisabled(currentPage == totalPages)
            )
            rows.add(ActionRow.of(navButtons))
        }
        
        if (hasStations) {
            val actionButtons = listOf(
                Button.success("play_1", "▶️ 1"),
                Button.success("play_2", "▶️ 2"),
                Button.success("play_3", "▶️ 3"),
                Button.success("play_4", "▶️ 4"),
                Button.success("play_5", "▶️ 5")
            )
            rows.add(ActionRow.of(actionButtons))
            
            val utilityButtons = listOf(
                Button.danger("stop_audio", "⏹️ Stop"),
                Button.primary("now_playing", "ℹ️ Info"),
                Button.primary("refresh_search", "🔄 Refresh")
            )
            rows.add(ActionRow.of(utilityButtons))
            
            val volumeMenu = createVolumeSelectMenu()
            rows.add(ActionRow.of(volumeMenu))
        }
        
        return rows
    }
    
    fun createVolumeSelectMenu(): StringSelectMenu {
        val menuBuilder = StringSelectMenu.create("volume_select")
            .setPlaceholder("🔊 Select Volume")
            .setRequiredRange(1, 1)
        
        for (volume in 5..100 step 5) {
            val emoji = when {
                volume <= 15 -> "🔈"
                volume <= 50 -> "🔉"
                else -> "🔊"
            }
            menuBuilder.addOption("$emoji ${volume}%", volume.toString(), "Set volume to $volume%")
        }
        
        return menuBuilder.build()
    }
    
    fun createStationInfoEmbed(station: RadioStation): MessageEmbed {
        return EmbedBuilder()
            .setTitle("📻 ${station.name}")
            .setDescription("**Detailed Station Information**")
            .setColor(Color.GREEN)
            .addField("🌍 Country", station.country.ifEmpty { "Unknown" }, true)
            .addField("🎵 Genre", station.tags.ifEmpty { "Not specified" }, true)
            .addField("🎧 Bitrate", if (station.bitrate > 0) "${station.bitrate} kbps" else "Unknown", true)
            .addField("📊 Codec", station.codec.ifEmpty { "Unknown" }, true)
            .addField("⭐ Rating", station.votes.toString(), true)
            .addField("🌐 Language", station.language.ifEmpty { "Unknown" }, true)
            .addField("🔗 Homepage", if (station.homepage.isNotEmpty()) station.homepage else "No homepage", false)
            .addField("📡 Stream URL", "```${station.urlResolved.ifEmpty { station.url }}```", false)
            .setTimestamp(java.time.Instant.now())
            .setFooter(Version.FOOTER_TEXT)
            .build()
    }
    
    fun createAudioStatusEmbed(
        station: RadioStation?,
        volume: Int,
        isPlaying: Boolean
    ): MessageEmbed {
        val embed = EmbedBuilder()
            .setColor(if (isPlaying) Color.GREEN else Color.RED)
        
        if (isPlaying && station != null) {
            embed.setTitle("🎵 Now Playing")
                .setDescription("**${station.name}**")
                .addField("🌍 Country", station.country.ifEmpty { "Unknown" }, true)
                .addField("🎵 Genre", station.tags.split(",").take(2).joinToString(", ").ifEmpty { "Not specified" }, true)
                .addField("🔊 Volume", "$volume%", true)
                .addField("🎧 Bitrate", if (station.bitrate > 0) "${station.bitrate} kbps" else "Unknown", true)
                .addField("📊 Codec", station.codec.ifEmpty { "Unknown" }, true)
                .addField("⭐ Rating", station.votes.toString(), true)
        } else {
            embed.setTitle("⏹️ Not Active")
                .setDescription("No music is currently playing")
                .addField("🔊 Volume", "$volume%", true)
        }
        
        embed.setFooter(Version.FOOTER_TEXT)
        return embed.build()
    }
    
    fun createHelpEmbed(): MessageEmbed {
        return EmbedBuilder()
            .setTitle("🎵 Radioss Help")
            .setDescription("**Available Commands:**")
            .setColor(Color.BLUE)
            .addField("🔍 Search", 
                "`/search <name>` - Search by station name\n" +
                "`/country <country>` - Search by country\n" +
                "`/genre <genre>` - Search by genre/tag\n" +
                "`/top [count]` - Top stations\n" +
                "`/random` - Random station", false)
            .addField("🎵 Audio", 
                "`/play <station>` - Play station\n" +
                "`/stop` - Stop playback\n" +
                "`/volume <value>` - Change volume\n" +
                "`/nowplaying` - Show current station", false)
            .addField("❤️ Favorites", 
                "`/favorites` - Manage favorites", false)
            .addField("ℹ️ Info", 
                "`/help` - Show this help", false)
            .addField("🎮 Usage", 
                "Use the buttons below search results for easy playback and navigation!", false)
            .setFooter(Version.FOOTER_TEXT)
            .build()
    }
    
    fun createErrorEmbed(title: String, message: String): MessageEmbed {
        return EmbedBuilder()
            .setTitle("❌ $title")
            .setDescription(message)
            .setColor(Color.RED)
            .setTimestamp(java.time.Instant.now())
            .setFooter(Version.FOOTER_TEXT)
            .build()
    }
    
    fun createSuccessEmbed(title: String, message: String): MessageEmbed {
        return EmbedBuilder()
            .setTitle("✅ $title")
            .setDescription(message)
            .setColor(Color.GREEN)
            .setTimestamp(java.time.Instant.now())
            .setFooter(Version.FOOTER_TEXT)
            .build()
    }
} 