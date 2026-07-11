package com.weatherconsensus.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weatherconsensus.domain.model.GeoLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "favorite_locations",
)

@Serializable
data class FavoriteLocation(
    val location: GeoLocation,
)

class FavoriteLocationStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val favoritesFlow: Flow<List<FavoriteLocation>> = context.favoritesDataStore.data.map { prefs ->
        val raw = prefs[KEY_FAVORITES_JSON] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<FavoriteLocation>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun addFavorite(location: GeoLocation) {
        context.favoritesDataStore.edit { prefs ->
            val current = readFavorites(prefs[KEY_FAVORITES_JSON])
            if (current.any { samePlace(it.location, location) }) return@edit
            val updated = current + FavoriteLocation(location)
            prefs[KEY_FAVORITES_JSON] = json.encodeToString(updated.take(MAX_FAVORITES))
        }
    }

    suspend fun removeFavorite(location: GeoLocation) {
        context.favoritesDataStore.edit { prefs ->
            val updated = readFavorites(prefs[KEY_FAVORITES_JSON])
                .filterNot { samePlace(it.location, location) }
            prefs[KEY_FAVORITES_JSON] = json.encodeToString(updated)
        }
    }

    fun isFavorite(location: GeoLocation, favorites: List<FavoriteLocation>): Boolean =
        favorites.any { samePlace(it.location, location) }

    private fun readFavorites(raw: String?): List<FavoriteLocation> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<FavoriteLocation>>(raw) }.getOrDefault(emptyList())
    }

    private fun samePlace(a: GeoLocation, b: GeoLocation): Boolean =
        "%.4f".format(a.latitude) == "%.4f".format(b.latitude) &&
            "%.4f".format(a.longitude) == "%.4f".format(b.longitude)

    private companion object {
        val KEY_FAVORITES_JSON = stringPreferencesKey("favorites_json")
        const val MAX_FAVORITES = 8
    }
}
