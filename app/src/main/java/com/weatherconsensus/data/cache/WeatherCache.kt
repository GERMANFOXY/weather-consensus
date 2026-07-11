package com.weatherconsensus.data.cache

import android.content.Context
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

@Serializable
private data class WeatherCachePayload(
    val entries: Map<String, CachedWeatherEntry> = emptyMap(),
    val lastLocationJson: String? = null,
)

class WeatherCache(
    context: Context,
    private val freshTtlMs: Long = 15 * 60 * 1000L,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private var payload: WeatherCachePayload = loadPayload()

    fun getFresh(location: GeoLocation): WeatherConsensusResult? =
        getEntry(location)?.takeIf { !isExpired(it) }?.let { decodeResult(it) }

    fun getStale(location: GeoLocation): WeatherConsensusResult? =
        getEntry(location)?.let { decodeResult(it) }

    fun getLastSnapshot(): Pair<GeoLocation, WeatherConsensusResult>? {
        val locationJson = payload.lastLocationJson ?: return null
        val location = runCatching { json.decodeFromString<GeoLocation>(locationJson) }.getOrNull()
            ?: return null
        val entry = getEntry(location) ?: return null
        val result = decodeResult(entry) ?: return null
        return location to result
    }

    fun put(location: GeoLocation, result: WeatherConsensusResult) {
        val key = locationKey(location)
        val entry = CachedWeatherEntry(
            locationKey = key,
            resultJson = json.encodeToString(result),
            cachedAtEpochMs = System.currentTimeMillis(),
        )
        payload = payload.copy(
            entries = payload.entries + (key to entry),
            lastLocationJson = json.encodeToString(location),
        )
        savePayload()
    }

    fun clear() {
        payload = WeatherCachePayload()
        savePayload()
    }

    private fun getEntry(location: GeoLocation): CachedWeatherEntry? =
        payload.entries[locationKey(location)]

    private fun isExpired(entry: CachedWeatherEntry): Boolean =
        System.currentTimeMillis() - entry.cachedAtEpochMs > freshTtlMs

    private fun decodeResult(entry: CachedWeatherEntry): WeatherConsensusResult? =
        runCatching { json.decodeFromString<WeatherConsensusResult>(entry.resultJson) }.getOrNull()

    private fun loadPayload(): WeatherCachePayload {
        val raw = prefs.getString(KEY_PAYLOAD, null) ?: return WeatherCachePayload()
        return runCatching { json.decodeFromString<WeatherCachePayload>(raw) }.getOrDefault(WeatherCachePayload())
    }

    private fun savePayload() {
        prefs.edit().putString(KEY_PAYLOAD, json.encodeToString(payload)).apply()
    }

    private fun locationKey(location: GeoLocation): String =
        "${location.latitude.format()},${location.longitude.format()}"

    private fun Double.format(): String = "%.4f".format(this)

    private companion object {
        const val PREFS_NAME = "weather_cache"
        const val KEY_PAYLOAD = "payload"
    }
}
