package com.weatherconsensus.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weatherconsensus.domain.model.WeatherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.accuracyDataStore: DataStore<Preferences> by preferencesDataStore(name = "provider_accuracy")

/**
 * Tracks per-provider accuracy multipliers learned from past consensus agreement.
 * Multipliers drift slowly toward 0.5–1.5 based on whether a provider agrees with the ensemble.
 */
class ProviderAccuracyStore(private val context: Context) {

    val multipliersFlow: Flow<Map<WeatherProvider, Float>> = context.accuracyDataStore.data.map { prefs ->
        WeatherProvider.all.associateWith { provider ->
            prefs[floatPreferencesKey(accuracyKey(provider))] ?: NEUTRAL_MULTIPLIER
        }
    }

    suspend fun currentMultipliers(): Map<WeatherProvider, Float> = multipliersFlow.first()

    suspend fun applyConsensusFeedback(
        statisticalOutliers: Set<WeatherProvider>,
        agreeingProviders: Set<WeatherProvider>,
    ) {
        if (statisticalOutliers.isEmpty() && agreeingProviders.isEmpty()) return

        context.accuracyDataStore.edit { prefs ->
            statisticalOutliers.forEach { provider ->
                val key = floatPreferencesKey(accuracyKey(provider))
                val current = prefs[key] ?: NEUTRAL_MULTIPLIER
                prefs[key] = (current * OUTLIER_PENALTY).coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER)
            }
            agreeingProviders.forEach { provider ->
                if (provider in statisticalOutliers) return@forEach
                val key = floatPreferencesKey(accuracyKey(provider))
                val current = prefs[key] ?: NEUTRAL_MULTIPLIER
                prefs[key] = (current * AGREEMENT_REWARD).coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER)
            }
        }
    }

    suspend fun reset() {
        context.accuracyDataStore.edit { prefs ->
            WeatherProvider.all.forEach { provider ->
                prefs[floatPreferencesKey(accuracyKey(provider))] = NEUTRAL_MULTIPLIER
            }
        }
    }

    private fun accuracyKey(provider: WeatherProvider): String = "accuracy_${provider.name}"

    companion object {
        const val NEUTRAL_MULTIPLIER = 1f
        private const val MIN_MULTIPLIER = 0.5f
        private const val MAX_MULTIPLIER = 1.5f
        private const val OUTLIER_PENALTY = 0.98f
        private const val AGREEMENT_REWARD = 1.005f
    }
}
