package me.richy.radioss.database

import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class ReconnectionDatabase {
    private val logger = LoggerFactory.getLogger(ReconnectionDatabase::class.java)
    private var connection: Connection? = null
    
    init {
        initializeDatabase()
    }
    
    private fun initializeDatabase() {
        try {
            // Erstelle data-Verzeichnis falls es nicht existiert
            val dataDir = File("data")
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }
            
            val dbPath = "data/favorites.db"
            val url = "jdbc:sqlite:$dbPath"
            
            connection = DriverManager.getConnection(url)
            logger.info("SQLite database connected for reconnection: $dbPath")
            
            createTable()
        } catch (e: SQLException) {
            logger.error("Error initializing reconnection database", e)
            throw RuntimeException("Failed to initialize reconnection database", e)
        }
    }
    
    private fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS reconnection_state (
                guild_id TEXT NOT NULL PRIMARY KEY,
                channel_id TEXT NOT NULL,
                station_data TEXT NOT NULL,
                mode247_enabled INTEGER NOT NULL DEFAULT 0,
                last_updated INTEGER NOT NULL
            )
        """.trimIndent()
        
        try {
            connection?.createStatement()?.execute(sql)
            logger.info("Reconnection state table created or already exists")
        } catch (e: SQLException) {
            logger.error("Error creating reconnection_state table", e)
            throw RuntimeException("Failed to create reconnection_state table", e)
        }
    }
    
    fun saveState(guildId: String, channelId: String, stationData: String, mode247Enabled: Boolean): Boolean {
        val sql = """
            INSERT OR REPLACE INTO reconnection_state 
            (guild_id, channel_id, station_data, mode247_enabled, last_updated) 
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setString(1, guildId)
            stmt?.setString(2, channelId)
            stmt?.setString(3, stationData)
            stmt?.setInt(4, if (mode247Enabled) 1 else 0)
            stmt?.setLong(5, System.currentTimeMillis())
            val result = stmt?.executeUpdate() ?: 0
            stmt?.close()
            result > 0
        } catch (e: SQLException) {
            logger.error("Error saving reconnection state for guild $guildId", e)
            false
        }
    }
    
    fun loadState(guildId: String): ReconnectionState? {
        val sql = "SELECT channel_id, station_data, mode247_enabled, last_updated FROM reconnection_state WHERE guild_id = ? LIMIT 1"
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setString(1, guildId)
            val rs = stmt?.executeQuery()
            
            if (rs?.next() == true) {
                val state = ReconnectionState(
                    guildId = guildId,
                    channelId = rs.getString("channel_id"),
                    stationData = rs.getString("station_data"),
                    mode247Enabled = rs.getInt("mode247_enabled") == 1,
                    lastUpdated = rs.getLong("last_updated")
                )
                rs.close()
                stmt?.close()
                state
            } else {
                rs?.close()
                stmt?.close()
                null
            }
        } catch (e: SQLException) {
            logger.error("Error loading reconnection state for guild $guildId", e)
            null
        }
    }
    
    fun loadAllStates(): List<ReconnectionState> {
        val sql = "SELECT guild_id, channel_id, station_data, mode247_enabled, last_updated FROM reconnection_state"
        val states = mutableListOf<ReconnectionState>()
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            val rs = stmt?.executeQuery()
            
            while (rs?.next() == true) {
                states.add(
                    ReconnectionState(
                        guildId = rs.getString("guild_id"),
                        channelId = rs.getString("channel_id"),
                        stationData = rs.getString("station_data"),
                        mode247Enabled = rs.getInt("mode247_enabled") == 1,
                        lastUpdated = rs.getLong("last_updated")
                    )
                )
            }
            
            rs?.close()
            stmt?.close()
            states
        } catch (e: SQLException) {
            logger.error("Error loading all reconnection states", e)
            emptyList()
        }
    }
    
    fun deleteState(guildId: String): Boolean {
        val sql = "DELETE FROM reconnection_state WHERE guild_id = ?"
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setString(1, guildId)
            val result = stmt?.executeUpdate() ?: 0
            stmt?.close()
            result > 0
        } catch (e: SQLException) {
            logger.error("Error deleting reconnection state for guild $guildId", e)
            false
        }
    }
    
    fun deleteStates(guildIds: List<String>): Int {
        if (guildIds.isEmpty()) return 0
        
        val placeholders = guildIds.joinToString(",") { "?" }
        val sql = "DELETE FROM reconnection_state WHERE guild_id IN ($placeholders)"
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            guildIds.forEachIndexed { index, guildId ->
                stmt?.setString(index + 1, guildId)
            }
            val result = stmt?.executeUpdate() ?: 0
            stmt?.close()
            result
        } catch (e: SQLException) {
            logger.error("Error deleting reconnection states for multiple guilds", e)
            0
        }
    }
    
    fun close() {
        try {
            connection?.close()
            logger.info("Reconnection database connection closed")
        } catch (e: SQLException) {
            logger.error("Error closing reconnection database connection", e)
        }
    }
    
    data class ReconnectionState(
        val guildId: String,
        val channelId: String,
        val stationData: String,
        val mode247Enabled: Boolean,
        val lastUpdated: Long
    )
}

