package me.richy.radioss.api

import kotlinx.serialization.json.Json
import me.richy.radioss.models.RadioStation
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class RadioBrowserAPI {
    private val logger = LoggerFactory.getLogger(RadioBrowserAPI::class.java)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }
    
    private val baseUrl = "http://stations.richy.sh/json"

    suspend fun searchStations(searchTerm: String, limit: Int = 10): List<RadioStation> {
        return searchByName(searchTerm, limit)
    }

    suspend fun searchByName(name: String, limit: Int = 50): List<RadioStation> {
        val url = "$baseUrl/stations/byname/$name?limit=$limit"
        logger.info("Suche Stationen nach Namen: $name")
        return makeRequest(url)
    }

    suspend fun getTopStations(limit: Int = 50): List<RadioStation> {
        val url = "$baseUrl/stations/topvote/$limit"
        logger.info("Lade Top $limit Stationen")
        return makeRequest(url)
    }

    suspend fun searchByCountry(country: String, limit: Int = 50): List<RadioStation> {
        val url = "$baseUrl/stations/bycountry/$country?limit=$limit"
        logger.info("Suche Stationen nach Land: $country")
        return makeRequest(url)
    }

    suspend fun searchByTag(tag: String, limit: Int = 50): List<RadioStation> {
        val url = "$baseUrl/stations/bytag/$tag?limit=$limit"
        logger.info("Suche Stationen nach Tag: $tag")
        return makeRequest(url)
    }

    suspend fun checkServerStatus(): String {
        return try {
            val url = "$baseUrl/stats"
            val request = Request.Builder()
                .url(url)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    "✅ Server erreichbar (${response.code})"
                } else {
                    "❌ Server Fehler: ${response.code}"
                }
            }
        } catch (e: Exception) {
            logger.error("Server Status Prüfung fehlgeschlagen", e)
            "❌ Server nicht erreichbar: ${e.message}"
        }
    }

    private suspend fun makeRequest(url: String): List<RadioStation> {
        return try {
            val request = Request.Builder()
                .url(url)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.error("HTTP Fehler: ${response.code} für URL: $url")
                    return emptyList()
                }
                
                val jsonResponse = response.body?.string() ?: ""
                if (jsonResponse.isEmpty()) {
                    logger.warn("Leere Antwort von Server")
                    return emptyList()
                }
                
                logger.debug("API Antwort erhalten: ${jsonResponse.take(200)}...")
                
                try {
                    val stations = json.decodeFromString<List<RadioStation>>(jsonResponse)
                    logger.info("${stations.size} Stationen erfolgreich geladen")
                    stations
                } catch (e: Exception) {
                    logger.error("JSON Parsing Fehler für URL: $url", e)
                    logger.debug("Problematische JSON Antwort: $jsonResponse")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("Request Fehler für URL: $url", e)
            emptyList()
        }
    }
}