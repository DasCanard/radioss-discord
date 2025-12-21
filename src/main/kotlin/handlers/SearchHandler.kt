package me.richy.radioss.handlers

import me.richy.radioss.models.RadioStation
import java.util.concurrent.ConcurrentHashMap

class SearchHandler {
    
    private val userSearchResults = ConcurrentHashMap<String, List<RadioStation>>()
    private val userCurrentPage = ConcurrentHashMap<String, Int>()
    private val userSearchTerms = ConcurrentHashMap<String, String>()
    
    companion object {
        const val STATIONS_PER_PAGE = 5
        const val MAX_STATIONS = 50
    }
    
    fun setUserSearchResults(userId: String, stations: List<RadioStation>) {
        userSearchResults[userId] = stations
    }
    
    fun setUserSearchTerm(userId: String, term: String) {
        userSearchTerms[userId] = term
    }
    
    fun getUserSearchResults(userId: String): List<RadioStation> {
        return userSearchResults[userId] ?: emptyList()
    }
    
    fun getUserCurrentPage(userId: String): Int {
        return userCurrentPage[userId] ?: 1
    }
    
    fun getUserSearchTerm(userId: String): String {
        return userSearchTerms[userId] ?: ""
    }
    
    fun setUserCurrentPage(userId: String, page: Int) {
        userCurrentPage[userId] = page
    }
    
    fun getPaginationData(userId: String): Triple<List<RadioStation>, Int, String> {
        return Triple(
            getUserSearchResults(userId),
            getUserCurrentPage(userId),
            getUserSearchTerm(userId)
        )
    }
}