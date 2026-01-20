package me.richy.radioss.ui

import me.richy.radioss.Version
import me.richy.radioss.models.RadioStation
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import java.awt.Color

class UIBuilder {
    
    fun createStationListEmbed(
        stations: List<RadioStation>,
        currentPage: Int,
        totalPages: Int,
        title: String,
        searchTerm: String? = null,
        favoriteStatus: Map<String, Boolean> = emptyMap()
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
                val isFavorite = favoriteStatus[station.stationUuid] ?: false
                val favoriteIcon = if (isFavorite) " ❤️" else ""
                val title = "$number. ${if (station.name.length > 30) station.name.take(30) + "..." else station.name}$favoriteIcon"
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
    
    fun createPaginationButtons(
        currentPage: Int, 
        totalPages: Int, 
        hasStations: Boolean,
        stations: List<RadioStation> = emptyList(),
        favoriteStatus: Map<String, Boolean> = emptyMap()
    ): List<ActionRow> {
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
            // Play-Buttons dynamisch basierend auf Anzahl der Stationen erstellen
            if (stations.isNotEmpty()) {
                val playButtons = stations.mapIndexed { index, _ ->
                    Button.success("play_${index + 1}", "▶️ ${index + 1}")
                }
                rows.add(ActionRow.of(playButtons))
                
                // Favoriten-Buttons für die aktuellen Stationen
                val favoriteButtons = stations.mapIndexed { index, station ->
                    val isFavorite = favoriteStatus[station.stationUuid] ?: false
                    val buttonId = if (isFavorite) "unfavorite_${station.stationUuid}" else "favorite_${station.stationUuid}"
                    val buttonLabel = if (isFavorite) "❤️ ${index + 1}" else "🤍 ${index + 1}"
                    Button.secondary(buttonId, buttonLabel)
                }
                rows.add(ActionRow.of(favoriteButtons))
            } else {
                // Fallback: Wenn keine Stationen übergeben wurden, aber hasStations true ist
                val actionButtons = listOf(
                    Button.success("play_1", "▶️ 1"),
                    Button.success("play_2", "▶️ 2"),
                    Button.success("play_3", "▶️ 3"),
                    Button.success("play_4", "▶️ 4"),
                    Button.success("play_5", "▶️ 5")
                )
                rows.add(ActionRow.of(actionButtons))
            }
            
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
        
        // Add 1-4% steps
        for (volume in 1..4) {
            menuBuilder.addOption("🔈 ${volume}%", volume.toString(), "Set volume to $volume%")
        }
        
        // Add 5% steps from 5 to 100
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
        isPlaying: Boolean,
        isFavorite: Boolean = false
    ): MessageEmbed {
        val embed = EmbedBuilder()
            .setColor(if (isPlaying) Color.GREEN else Color.RED)
        
        if (isPlaying && station != null) {
            val favoriteIcon = if (isFavorite) " ❤️" else ""
            embed.setTitle("🎵 Now Playing$favoriteIcon")
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
    
    fun createFavoriteButton(station: RadioStation, isFavorite: Boolean): Button {
        val buttonId = if (isFavorite) "unfavorite_${station.stationUuid}" else "favorite_${station.stationUuid}"
        val buttonLabel = if (isFavorite) "❤️ Remove Favorite" else "🤍 Add Favorite"
        return Button.secondary(buttonId, buttonLabel)
    }
    
    fun createFavoritesListEmbed(
        stations: List<RadioStation>,
        currentPage: Int,
        totalPages: Int
    ): MessageEmbed {
        val embed = EmbedBuilder()
            .setTitle("❤️ Your Favorites")
            .setColor(Color.MAGENTA)
        
        if (stations.isEmpty()) {
            embed.addField("❌ No Favorites", "You haven't favorited any stations yet. Use the favorite buttons in search results or now playing to add favorites!", false)
        } else {
            stations.forEachIndexed { index, station ->
                val number = (currentPage - 1) * 5 + index + 1
                val title = "$number. ${if (station.name.length > 30) station.name.take(30) + "..." else station.name} ❤️"
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
    
    fun createFavoritesButtons(
        stations: List<RadioStation>,
        currentPage: Int,
        totalPages: Int
    ): List<ActionRow> {
        val rows = mutableListOf<ActionRow>()
        
        if (totalPages > 1) {
            val navButtons = listOf(
                Button.primary("fav_first_page", "⏮️ First").withDisabled(currentPage == 1),
                Button.primary("fav_prev_page", "◀️ Previous").withDisabled(currentPage == 1),
                Button.secondary("fav_page_info", "Page $currentPage/$totalPages").asDisabled(),
                Button.primary("fav_next_page", "▶️ Next").withDisabled(currentPage == totalPages),
                Button.primary("fav_last_page", "⏭️ Last").withDisabled(currentPage == totalPages)
            )
            rows.add(ActionRow.of(navButtons))
        }
        
        if (stations.isNotEmpty()) {
            val playButtons = stations.take(5).mapIndexed { index, _ ->
                Button.success("fav_play_${index + 1}", "▶️ ${index + 1}")
            }
            rows.add(ActionRow.of(playButtons))
            
            val unfavoriteButtons = stations.take(5).mapIndexed { index, station ->
                Button.danger("unfavorite_${station.stationUuid}", "❤️ Remove ${index + 1}")
            }
            rows.add(ActionRow.of(unfavoriteButtons))
        }
        
        return rows
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
                "`/play <station>` - Play direct stream url (.mp3, .m3u for example)\n" +
                "`/stop` - Stop playback\n" +
                "`/volume <value>` - Change volume\n" +
                "`/247` - Enable 24/7 mode\n" +
                "`/nowplaying` - Show current station", false)
            .addField("❤️ Favorites", 
                "`/favorites` - Manage favorites", false)
            .addField("🗳️ Support", 
                "`/vote` - Vote for the bot on top.gg\n" +
                "`/feedback` - Show Discord server for updates and feedback", false)
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