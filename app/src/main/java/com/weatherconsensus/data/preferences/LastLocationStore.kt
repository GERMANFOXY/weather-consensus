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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.lastLocationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "last_location",
)

class LastLocationStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val lastLocationFlow: Flow<GeoLocation?> = context.lastLocationDataStore.data.map { prefs ->
        val raw = prefs[KEY_LOCATION_JSON] ?: return@map null
        runCatching { json.decodeFromString<GeoLocation>(raw) }.getOrNull()
    }

    suspend fun saveLocation(location: GeoLocation) {
        context.lastLocationDataStore.edit { prefs ->
            prefs[KEY_LOCATION_JSON] = json.encodeToString(location)
        }
    }

    suspend fun clearLocation() {
        context.lastLocationDataStore.edit { prefs ->
            prefs.remove(KEY_LOCATION_JSON)
        }
    }

    private companion object {
        val KEY_LOCATION_JSON = stringPreferencesKey("location_json")
    }
}
