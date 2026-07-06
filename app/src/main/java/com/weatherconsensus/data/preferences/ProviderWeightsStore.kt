package com.weatherconsensus.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weatherconsensus.domain.model.ProviderWeights
import com.weatherconsensus.domain.model.WeatherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weightsDataStore: DataStore<Preferences> by preferencesDataStore(name = "provider_weights")

class ProviderWeightsStore(private val context: Context) {

    val weightsFlow: Flow<ProviderWeights> = context.weightsDataStore.data.map { prefs ->
        val map = WeatherProvider.all.associateWith { provider ->
            val key = floatPreferencesKey(provider.name)
            prefs[key] ?: ProviderWeights.DEFAULT_WEIGHT
        }
        ProviderWeights(map)
    }

    suspend fun saveWeights(weights: ProviderWeights) {
        context.weightsDataStore.edit { prefs ->
            weights.weights.forEach { (provider, value) ->
                prefs[floatPreferencesKey(provider.name)] = value.coerceIn(0f, 2f)
            }
        }
    }

    suspend fun resetToDefaults() {
        saveWeights(ProviderWeights.default)
    }
}
