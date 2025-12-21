package me.richy.radioss.services

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.richy.radioss.database.FavoriteDatabase
import me.richy.radioss.models.RadioStation
import org.slf4j.LoggerFactory

class FavoriteService(private val database: FavoriteDatabase) {
    private val logger = LoggerFactory.getLogger(FavoriteService::class.java)
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }
    
    @Synchronized
    fun addFavorite(userId: String, station: RadioStation): Boolean {
        if (station.stationUuid.isEmpty()) {
            logger.warn("Attempted to add favorite with empty station UUID for user $userId")
            return false
        }
        
        val stationData = try {
            json.encodeToString(station)
        } catch (e: Exception) {
            logger.error("Error serializing station data", e)
            return false
        }
        
        val result = database.addFavorite(userId, station.stationUuid, stationData)
        if (result) {
            logger.info("Added favorite: user $userId, station ${station.stationUuid}")
        } else {
            logger.debug("Favorite already exists: user $userId, station ${station.stationUuid}")
        }
        return result
    }
    
    @Synchronized
    fun removeFavorite(userId: String, stationUuid: String): Boolean {
        if (stationUuid.isEmpty()) {
            logger.warn("Attempted to remove favorite with empty station UUID for user $userId")
            return false
        }
        
        val result = database.removeFavorite(userId, stationUuid)
        if (result) {
            logger.info("Removed favorite: user $userId, station $stationUuid")
        }
        return result
    }
    
    @Synchronized
    fun isFavorite(userId: String, stationUuid: String): Boolean {
        if (stationUuid.isEmpty()) {
            return false
        }
        return database.isFavorite(userId, stationUuid)
    }
    
    @Synchronized
    fun getFavorites(userId: String): List<String> {
        return database.getFavorites(userId)
    }
    
    @Synchronized
    fun getFavoriteStations(userId: String): List<RadioStation> {
        val stationsData = database.getFavoriteStationsData(userId)
        return stationsData.mapNotNull { data ->
            try {
                json.decodeFromString<RadioStation>(data)
            } catch (e: Exception) {
                logger.error("Error deserializing station data", e)
                null
            }
        }
    }
    
    @Synchronized
    fun toggleFavorite(userId: String, station: RadioStation): Boolean {
        return if (isFavorite(userId, station.stationUuid)) {
            removeFavorite(userId, station.stationUuid)
            false
        } else {
            addFavorite(userId, station)
            true
        }
    }
    
    fun close() {
        database.close()
    }
}

