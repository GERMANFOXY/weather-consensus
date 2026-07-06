package com.weatherconsensus.data.cache

import com.weatherconsensus.domain.model.GeoLocation
import com.weatherconsensus.domain.model.WeatherConsensusResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CachedWeatherEntry(
    val locationKey: String,
    val resultJson: String,
    val cachedAtEpochMs: Long,
)

class WeatherCache(
    private val ttlMs: Long = 15 * 60 * 1000L,
) {
    private val memoryCache = mutableMapOf<String, CachedWeatherEntry>()
    private val json = Json { ignoreUnknownKeys = true }

    fun get(location: GeoLocation): WeatherConsensusResult? {
        val key = locationKey(location)
        val entry = memoryCache[key] ?: return null
        if (System.currentTimeMillis() - entry.cachedAtEpochMs > ttlMs) {
            memoryCache.remove(key)
            return null
        }
        return runCatching {
            json.decodeFromString<WeatherConsensusResult>(entry.resultJson)
        }.getOrNull()
    }

    fun put(location: GeoLocation, result: WeatherConsensusResult) {
        val key = locationKey(location)
        val entry = CachedWeatherEntry(
            locationKey = key,
            resultJson = json.encodeToString(result),
            cachedAtEpochMs = System.currentTimeMillis(),
        )
        memoryCache[key] = entry
    }

    fun clear() {
        memoryCache.clear()
    }

    private fun locationKey(location: GeoLocation): String =
        "${location.latitude.format()},${location.longitude.format()}"

    private fun Double.format(): String = "%.4f".format(this)
}
