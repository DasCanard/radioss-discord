package me.richy.radioss.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RadioStation(
    @SerialName("stationuuid") val stationUuid: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("url_resolved") val urlResolved: String = "",
    @SerialName("homepage") val homepage: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("tags") val tags: String = "",
    @SerialName("bitrate") val bitrate: Int = 0,
    @SerialName("votes") val votes: Int = 0,
    @SerialName("codec") val codec: String = "",
    @SerialName("language") val language: String = ""
) 