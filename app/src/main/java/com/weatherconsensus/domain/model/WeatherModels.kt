package com.weatherconsensus.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class WeatherProvider(
    val displayName: String,
    val userDisplayName: String,
    val requiresApiKey: Boolean,
) {
    OPENWEATHERMAP("OpenWeatherMap", "OpenWeather", true),
    WEATHERAPI("WeatherAPI.com", "WeatherAPI", true),
    TOMORROW_IO("Tomorrow.io", "Tomorrow", true),
    OPEN_METEO("Open-Meteo", "Open-Meteo", false),
    DWD("DWD", "Deutscher Wetterdienst", false);

    companion object {
        val all = entries.toList()
    }
}

@Serializable
enum class WeatherCondition(val labelDe: String) {
    CLEAR("Klar"),
    PARTLY_CLOUDY("Leicht bewölkt"),
    CLOUDY("Bewölkt"),
    FOG("Nebel"),
    DRIZZLE("Nieselregen"),
    RAIN("Regen"),
    SNOW("Schnee"),
    THUNDERSTORM("Gewitter"),
    UNKNOWN("Wechselhaft");

    companion object {
        fun fromLabel(label: String): WeatherCondition =
            entries.find { it.labelDe.equals(label, ignoreCase = true) } ?: UNKNOWN
    }
}

@Serializable
enum class ConfidenceLevel(val messageDe: String) {
    VERY_STABLE("Die Wetterdienste sind sich sehr einig."),
    SLIGHTLY_DEVIATING("Die Vorhersagen unterscheiden sich etwas."),
    UNCERTAIN("Die Wetterdienste unterscheiden sich heute stärker. Die Einschätzung ist daher weniger sicher.");

    companion object {
        fun fromScore(score: Double): ConfidenceLevel = when {
            score >= 0.8 -> VERY_STABLE
            score >= 0.5 -> SLIGHTLY_DEVIATING
            else -> UNCERTAIN
        }
    }
}

enum class ServiceTrustLevel(val labelDe: String, val weight: Float) {
    HIGH("Mehr vertrauen", 1.5f),
    NORMAL("Normal", 1.0f),
    LOW("Weniger berücksichtigen", 0.4f);

    companion object {
        fun fromWeight(weight: Float): ServiceTrustLevel = when {
            weight >= 1.25f -> HIGH
            weight <= 0.6f -> LOW
            else -> NORMAL
        }
    }
}

@Serializable
data class GeoLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val adminArea: String? = null,
) {
    val displayName: String
        get() = buildString {
            append(name)
            adminArea?.let { append(", $it") }
            country?.let { append(", $it") }
        }

    val shortName: String get() = name
}

@Serializable
data class WeatherDetails(
    val uvIndex: Double? = null,
    val pressureHpa: Double? = null,
    val visibilityKm: Double? = null,
    val sunriseEpochSeconds: Long? = null,
    val sunsetEpochSeconds: Long? = null,
    val moonPhase: String? = null,
    val moonIlluminationPercent: Int? = null,
    val airQualityIndex: Int? = null,
    val airQualityLabel: String? = null,
    val pollenLevel: String? = null,
)

@Serializable
data class NormalizedHourlyForecast(
    val timestampEpochSeconds: Long,
    val temperatureC: Double?,
    val feelsLikeC: Double? = null,
    val windKmh: Double?,
    val precipitationMm: Double?,
    val precipitationProbabilityPercent: Double?,
    val humidityPercent: Double?,
    val condition: WeatherCondition,
)

@Serializable
data class NormalizedDailyForecast(
    val dateEpochSeconds: Long,
    val minTempC: Double?,
    val maxTempC: Double?,
    val condition: WeatherCondition,
    val precipitationProbabilityPercent: Double?,
    val sunriseEpochSeconds: Long? = null,
    val sunsetEpochSeconds: Long? = null,
)

@Serializable
data class NormalizedWeatherSnapshot(
    val temperatureC: Double?,
    val feelsLikeC: Double? = null,
    val windKmh: Double?,
    val precipitationMm: Double?,
    val precipitationProbabilityPercent: Double?,
    val humidityPercent: Double?,
    val condition: WeatherCondition,
    val details: WeatherDetails = WeatherDetails(),
)

@Serializable
enum class WarningSeverity(val labelDe: String, val priority: Int) {
    HINWEIS("Hinweis", 1),
    WARNUNG("Amtliche Warnung", 2),
    UNWETTER("Unwetterwarnung", 3),
    EXTREME_UNWETTER("Extreme Unwetterwarnung", 4),
    UNKNOWN("Hinweis", 0);

    companion object {
        fun fromDwdLevel(level: Int?): WarningSeverity = when (level) {
            1 -> HINWEIS
            2 -> WARNUNG
            3 -> UNWETTER
            4 -> EXTREME_UNWETTER
            else -> UNKNOWN
        }
    }
}

@Serializable
data class WeatherWarning(
    val hazardType: String,
    val severity: WarningSeverity,
    val description: String,
    val instruction: String? = null,
    val effectiveEpochSeconds: Long? = null,
    val expiresEpochSeconds: Long? = null,
) {
    val severityLabel: String get() = severity.labelDe

    fun isActive(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val afterStart = effectiveEpochSeconds?.let { nowEpochSeconds >= it } ?: true
        val beforeEnd = expiresEpochSeconds?.let { nowEpochSeconds <= it } ?: true
        return afterStart && beforeEnd
    }
}

@Serializable
data class ProviderWeatherResult(
    val provider: WeatherProvider,
    val location: GeoLocation,
    val current: NormalizedWeatherSnapshot?,
    val hourlyForecast: List<NormalizedHourlyForecast> = emptyList(),
    val dailyForecast: List<NormalizedDailyForecast> = emptyList(),
    val timezoneId: String? = null,
    val weatherWarnings: List<WeatherWarning> = emptyList(),
    val warningsLoadFailed: Boolean = false,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = errorMessage == null && current != null
}

data class ProviderValue<T>(
    val provider: WeatherProvider,
    val value: T,
)

@Serializable
data class EnsembleAgreement(
    val agreeingProviders: Int,
    val totalProviders: Int,
    val type: String,
    val label: String? = null,
) {
    companion object {
        const val TYPE_CONDITION = "condition"
        const val TYPE_RAIN = "rain"
    }
}

@Serializable
data class ConsensusSnapshot(
    val temperatureC: Double?,
    val feelsLikeC: Double? = null,
    val windKmh: Double?,
    val precipitationMm: Double?,
    val precipitationProbabilityPercent: Double?,
    val humidityPercent: Double?,
    val condition: WeatherCondition,
    val confidence: ConfidenceLevel,
    val confidenceScore: Double,
    val details: WeatherDetails = WeatherDetails(),
    val providerContributions: Map<WeatherProvider, NormalizedWeatherSnapshot>,
    val excludedProviders: Map<WeatherProvider, String> = emptyMap(),
    val statisticalOutliers: Map<WeatherProvider, String> = emptyMap(),
    val ensembleHints: List<EnsembleAgreement> = emptyList(),
)

@Serializable
data class ConsensusHourlyForecast(
    val timestampEpochSeconds: Long,
    val temperatureC: Double?,
    val feelsLikeC: Double? = null,
    val windKmh: Double?,
    val precipitationMm: Double?,
    val precipitationProbabilityPercent: Double?,
    val humidityPercent: Double?,
    val condition: WeatherCondition,
    val confidence: ConfidenceLevel,
    val providerContributions: Map<WeatherProvider, NormalizedHourlyForecast>,
)

@Serializable
data class ConsensusDailyForecast(
    val dateEpochSeconds: Long,
    val minTempC: Double?,
    val maxTempC: Double?,
    val condition: WeatherCondition,
    val precipitationProbabilityPercent: Double?,
    val sunriseEpochSeconds: Long? = null,
    val sunsetEpochSeconds: Long? = null,
)

@Serializable
data class WeatherConsensusResult(
    val location: GeoLocation,
    val current: ConsensusSnapshot,
    val hourlyForecast: List<ConsensusHourlyForecast>,
    val dailyForecast: List<ConsensusDailyForecast>,
    val providerResults: List<ProviderWeatherResult>,
    val missingApiKeys: List<WeatherProvider>,
    val fetchedAtEpochMs: Long,
    val timezoneId: String? = null,
    val weatherWarnings: List<WeatherWarning> = emptyList(),
    val warningsLoadError: String? = null,
    val isInGermany: Boolean = false,
) {
    val isNight: Boolean
        get() {
            val now = fetchedAtEpochMs / 1000
            val sunrise = current.details.sunriseEpochSeconds
            val sunset = current.details.sunsetEpochSeconds
            if (sunrise != null && sunset != null) {
                return now < sunrise || now > sunset
            }
            val hour = java.time.Instant.ofEpochMilli(fetchedAtEpochMs)
                .atZone(java.time.ZoneId.systemDefault()).hour
            return hour < 6 || hour >= 20
        }
}

data class ProviderWeights(
    val weights: Map<WeatherProvider, Float>,
) {
    fun weightFor(provider: WeatherProvider): Float =
        weights[provider]?.coerceIn(0f, 2f) ?: DEFAULT_WEIGHT

    fun trustLevelFor(provider: WeatherProvider): ServiceTrustLevel =
        ServiceTrustLevel.fromWeight(weightFor(provider))

    fun withTrustLevel(provider: WeatherProvider, level: ServiceTrustLevel): ProviderWeights {
        val updated = weights.toMutableMap()
        updated[provider] = level.weight
        return copy(weights = updated)
    }

    fun withWeight(provider: WeatherProvider, weight: Float): ProviderWeights {
        val updated = weights.toMutableMap()
        updated[provider] = weight.coerceIn(0f, 2f)
        return copy(weights = updated)
    }

    companion object {
        const val DEFAULT_WEIGHT = 1.0f
        val default = ProviderWeights(WeatherProvider.all.associateWith { DEFAULT_WEIGHT })
    }
}

data class ApiKeyStatus(
    val provider: WeatherProvider,
    val isConfigured: Boolean,
)
