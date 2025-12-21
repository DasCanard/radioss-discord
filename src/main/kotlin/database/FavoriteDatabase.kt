package me.richy.radioss.database

import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class FavoriteDatabase {
    private val logger = LoggerFactory.getLogger(FavoriteDatabase::class.java)
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
            logger.info("SQLite database connected: $dbPath")
            
            createTable()
        } catch (e: SQLException) {
            logger.error("Error initializing database", e)
            throw RuntimeException("Failed to initialize database", e)
        }
    }
    
    private fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS favorites (
                user_id TEXT NOT NULL,
                station_uuid TEXT NOT NULL,
                station_data TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                PRIMARY KEY (user_id, station_uuid)
            )
        """.trimIndent()
        
        try {
            connection?.createStatement()?.execute(sql)
            logger.info("Favorites table created or already exists")
            
            // Migration: Füge station_data Spalte hinzu, falls sie nicht existiert
            try {
                connection?.createStatement()?.execute("ALTER TABLE favorites ADD COLUMN station_data TEXT")
                logger.info("Added station_data column to favorites table")
            } catch (e: SQLException) {
                // Spalte existiert bereits, das ist OK
                logger.debug("station_data column already exists or migration not needed")
            }
        } catch (e: SQLException) {
            logger.error("Error creating favorites table", e)
            throw RuntimeException("Failed to create favorites table", e)
        }
    }
    
    fun addFavorite(userId: String, stationUuid: String, stationData: String): Boolean {
        val sql = "INSERT OR REPLACE INTO favorites (user_id, station_uuid, station_data, created_at) VALUES (?, ?, ?, ?)"
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setString(1, userId)
            stmt?.setString(2, stationUuid)
            stmt?.setString(3, stationData)
            stmt?.setLong(4, System.currentTimeMillis())
            val result = stmt?.executeUpdate() ?: 0
            stmt?.close()
            result > 0
        } catch (e: SQLException) {
            logger.error("Error adding favorite for user $userId, station $stationUuid", e)
            false
        }
    }
    
    fun getFavoriteStationData(userId: String, stationUuid: String): String? {
        val sql = "SELECT station_data FROM favorites WHERE user_id = ? AND station_uuid = ? LIMIT 1"
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setString(1, userId)
            stmt?.setString(2, stationUuid)
            val rs = stmt?.executeQuery()
            val result = if (rs?.next() == true) rs.getString("station_data") else null
            rs?.close()
            stmt?.close()
            result
        } catch (e: SQLException) {
            logger.error("Error getting favorite station data for user $userId, station $stationUuid", e)
            null
        }
    }
    
    fun removeFavorite(userId: String, stationUuid: String): Boolean {
        val sql = "DELETE FROM favorites WHERE user_id = ? AND station_uuid = ?"
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setString(1, userId)
            stmt?.setString(2, stationUuid)
            val result = stmt?.executeUpdate() ?: 0
            stmt?.close()
            result > 0
        } catch (e: SQLException) {
            logger.error("Error removing favorite for user $userId, station $stationUuid", e)
            false
        }
    }
    
    fun isFavorite(userId: String, stationUuid: String): Boolean {
        val sql = "SELECT 1 FROM favorites WHERE user_id = ? AND station_uuid = ? LIMIT 1"
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setString(1, userId)
            stmt?.setString(2, stationUuid)
            val rs = stmt?.executeQuery()
            val result = rs?.next() ?: false
            rs?.close()
            stmt?.close()
            result
        } catch (e: SQLException) {
            logger.error("Error checking favorite for user $userId, station $stationUuid", e)
            false
        }
    }
    
    fun getFavorites(userId: String): List<String> {
        val sql = "SELECT station_uuid FROM favorites WHERE user_id = ? ORDER BY created_at DESC"
        val favorites = mutableListOf<String>()
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setString(1, userId)
            val rs = stmt?.executeQuery()
            
            while (rs?.next() == true) {
                favorites.add(rs.getString("station_uuid"))
            }
            
            rs?.close()
            stmt?.close()
            favorites
        } catch (e: SQLException) {
            logger.error("Error getting favorites for user $userId", e)
            emptyList()
        }
    }
    
    fun getFavoriteStationsData(userId: String): List<String> {
        val sql = "SELECT station_data FROM favorites WHERE user_id = ? ORDER BY created_at DESC"
        val stationsData = mutableListOf<String>()
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setString(1, userId)
            val rs = stmt?.executeQuery()
            
            while (rs?.next() == true) {
                val data = rs.getString("station_data")
                if (data.isNotEmpty()) {
                    stationsData.add(data)
                }
            }
            
            rs?.close()
            stmt?.close()
            stationsData
        } catch (e: SQLException) {
            logger.error("Error getting favorite stations data for user $userId", e)
            emptyList()
        }
    }
    
    fun close() {
        try {
            connection?.close()
            logger.info("Database connection closed")
        } catch (e: SQLException) {
            logger.error("Error closing database connection", e)
        }
    }
}

