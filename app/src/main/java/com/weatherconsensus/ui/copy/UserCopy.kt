package com.weatherconsensus.ui.copy

import com.weatherconsensus.domain.model.ConfidenceLevel
import com.weatherconsensus.domain.model.WeatherProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object UserCopy {
    const val SEARCH_PLACEHOLDER = "Ort suchen"
    const val SELECT_LOCATION = "Ort wählen"
    const val MENU = "Menü"
    const val CURRENT = "Aktuell"
    const val TAB_WEATHER = "Wetter"
    const val TAB_WARNINGS = "Warnungen"
    const val TAB_SETTINGS = "Einstellungen"
    const val HOURLY_OVERVIEW = "Stundenübersicht"
    const val NOW = "Jetzt"
    const val SHOW_MORE = "Mehr anzeigen"
    const val REFRESH_WEATHER = "Wetter aktualisieren"
    const val RAIN_CHANCE_LABEL = "Regenchance"
    const val SETTINGS_TITLE = "Einstellungen"
    const val SERVICES_SETTINGS_TITLE = "Wetterdienste anpassen"
    const val SERVICES_SETTINGS_HINT = "Lege fest, welchen Wetterdiensten du mehr vertraust. Die Sicherheit der Vorhersage wird entsprechend angepasst."
    const val SAVE = "Speichern"
    const val RESET = "Alles auf Normal setzen"
    const val SAVED = "Gespeichert."
    const val RESET_DONE = "Alle Wetterdienste stehen wieder auf Normal."

    const val UPDATE_SECTION_TITLE = "App-Update"
    const val UPDATE_SECTION_HINT = "Prüft, ob eine neuere Testversion auf GitHub verfügbar ist."
    const val UPDATE_CHECK = "Nach Updates suchen"
    const val UPDATE_CHECKING = "Suche nach Updates …"
    const val UPDATE_UP_TO_DATE = "Du hast bereits die neueste Version."
    const val UPDATE_CHECK_FAILED = "Update-Prüfung fehlgeschlagen. Bitte später erneut versuchen."
    const val UPDATE_AVAILABLE_TITLE = "Neue Version verfügbar"
    const val UPDATE_DOWNLOAD_INSTALL = "Update herunterladen"
    const val UPDATE_LATER = "Später"
    const val UPDATE_DOWNLOADING = "Update wird heruntergeladen …"
    const val UPDATE_DOWNLOAD_FAILED = "Download fehlgeschlagen. Bitte erneut versuchen."
    const val UPDATE_INSTALL_PERMISSION = "Bitte erlaube die Installation unbekannter Apps für Wetter."
    const val UPDATE_GENERIC_ERROR = "Update konnte nicht gestartet werden."

    fun updateAvailableSubtitle(versionName: String): String =
        "Version $versionName steht zum Download bereit."

    fun installedVersionLabel(versionName: String): String =
        "Installierte Version: $versionName"

    const val EMPTY_TITLE = "Wohin soll die Reise gehen?"
    const val EMPTY_SUBTITLE = "Suche einen Ort oder nutze deinen Standort."
    const val LOADING = "Wetter wird geladen …"
    const val SEARCHING = "Orte werden gesucht …"

    const val ERROR_ALL_FAILED = "Gerade konnten wir leider kein Wetter abrufen."
    const val ERROR_NETWORK = "Keine Verbindung. Bitte prüfe dein Internet."
    const val ERROR_LOCATION_PERMISSION = "Standortzugriff ist erforderlich."
    const val ERROR_LOCATION = "Standort konnte nicht ermittelt werden."
    const val ERROR_GENERIC = "Etwas ist schiefgelaufen."

    const val RETRY = "Erneut versuchen"
    const val USE_LOCATION = "Standort nutzen"
    const val REFRESH = "Aktualisieren"

    const val WIDGET_EMPTY = "Wetter öffnen"
    const val FAVORITE_ADDED = "Zu Favoriten hinzugefügt."
    const val FAVORITE_REMOVED = "Aus Favoriten entfernt."
    const val ADD_FAVORITE = "Zu Favoriten hinzufügen"
    const val REMOVE_FAVORITE = "Aus Favoriten entfernen"
    const val OFFLINE_NO_DATA = "Kein Internet und keine gespeicherten Wetterdaten vorhanden."

    fun offlineDataMessage(fetchedAtEpochMs: Long, timezoneId: String?, weakNetwork: Boolean): String {
        val stand = formatDataTimestamp(fetchedAtEpochMs, timezoneId)
        return if (weakNetwork) {
            "Schwache Verbindung. Angezeigter Stand vom $stand."
        } else {
            "Kein Internet. Angezeigter Stand vom $stand."
        }
    }

    fun formatDataTimestamp(epochMs: Long, zoneId: String?): String {
        val zone = zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        return DateTimeFormatter.ofPattern("d. MMM · HH:mm", Locale.GERMAN)
            .format(Instant.ofEpochMilli(epochMs).atZone(zone))
    }

    const val FEELS_LIKE = "Gefühlt"
    const val RAIN = "Regen"
    const val HUMIDITY = "Luftfeuchtigkeit"
    const val WIND = "Wind"
    const val UV = "UV-Index"
    const val PRESSURE = "Luftdruck"
    const val VISIBILITY = "Sichtweite"

    const val HOURLY = "Nächste 24 Stunden"
    const val HOURLY_TITLE = HOURLY
    const val DAILY = "7-Tage-Vorhersage"
    const val COMPARISON_TITLE = "Wetterdienste im Vergleich"
    const val COMPARISON_SUBTITLE = "So ähnlich sehen die Vorhersagen aktuell aus."
    const val ENSEMBLE_TITLE = "Quellenübereinstimmung"
    const val RAIN_CHANCE = RAIN_CHANCE_LABEL
    const val SUN_MOON = "Sonne & Mond"
    const val SUNRISE = "Sonnenaufgang"
    const val SUNSET = "Sonnenuntergang"
    const val MOON = "Mondphase"
    const val AIR_QUALITY = "Luftqualität"
    const val POLLEN = "Pollenflug"
    const val NOT_AVAILABLE = "–"

    fun serviceUnavailable(provider: WeatherProvider): String = when (provider) {
        WeatherProvider.DWD -> "Der Deutsche Wetterdienst konnte gerade nicht erreicht werden."
        WeatherProvider.OPEN_METEO -> "Open-Meteo konnte gerade nicht erreicht werden."
        else -> "${provider.userDisplayName} ist gerade nicht erreichbar."
    }

    const val DWD_GERMANY_HINT = "Für Deutschland wird der Deutsche Wetterdienst besonders berücksichtigt."
    const val WARNINGS_TITLE = "Wetterwarnungen"
    const val WARNINGS_NONE = "Aktuell keine Wetterwarnungen für diesen Ort."
    const val WARNINGS_NO_LOCATION = "Wähle zuerst einen Ort, um Wetterwarnungen zu sehen."
    const val WARNINGS_GERMANY_ONLY = "Amtliche Wetterwarnungen sind nur für Orte in Deutschland verfügbar."
    const val WARNINGS_LOAD_ERROR = "Wetterwarnungen konnten gerade nicht geladen werden."
    const val WARNINGS_PERIOD = "Zeitraum"
    const val WARNINGS_RECOMMENDATION = "Empfehlung"
    const val WARNINGS_SHOW_DETAILS = "Details anzeigen"
    const val WARNINGS_HIDE_DETAILS = "Details ausblenden"
    const val WARNINGS_MORE = "Weitere Warnungen"

    fun formatWarningPeriod(fromEpoch: Long?, toEpoch: Long?, zoneId: String?): String {
        if (fromEpoch == null && toEpoch == null) return NOT_AVAILABLE
        val from = formatDateTimeShort(fromEpoch, zoneId)
        val to = formatDateTimeShort(toEpoch, zoneId)
        return if (fromEpoch != null && toEpoch != null) "$from – $to"
        else from.takeIf { fromEpoch != null } ?: to
    }

    private fun formatDateTimeShort(epochSeconds: Long?, zoneId: String?): String {
        if (epochSeconds == null) return NOT_AVAILABLE
        val zone = zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        return DateTimeFormatter.ofPattern("d. MMM, HH:mm", Locale.GERMAN)
            .format(Instant.ofEpochSecond(epochSeconds).atZone(zone))
    }

    fun serviceUnavailableShort(provider: WeatherProvider): String = "Gerade nicht verfügbar"

    fun confidenceMessage(level: ConfidenceLevel): String = level.messageDe

    fun safetyTitle(level: ConfidenceLevel): String = when (level) {
        ConfidenceLevel.VERY_STABLE -> "Hohe Sicherheit"
        ConfidenceLevel.SLIGHTLY_DEVIATING -> "Mittlere Sicherheit"
        ConfidenceLevel.UNCERTAIN -> "Niedrige Sicherheit"
    }

    fun safetyDescription(confidence: ConfidenceLevel, outlierCount: Int): String {
        val base = when (confidence) {
            ConfidenceLevel.VERY_STABLE -> "Die Quellen liegen nah beieinander."
            ConfidenceLevel.SLIGHTLY_DEVIATING -> "Die Vorhersagen weichen etwas voneinander ab."
            ConfidenceLevel.UNCERTAIN -> "Die Quellen unterscheiden sich deutlich."
        }
        val outlier = when (outlierCount) {
            0 -> ""
            1 -> " Ein Anbieter wurde als Ausreißer erkannt."
            else -> " $outlierCount Anbieter wurden als Ausreißer erkannt."
        }
        return base + outlier
    }

    fun ensembleCondition(agreeing: Int, total: Int, label: String): String =
        "$agreeing von $total Quellen: $label"

    fun ensembleRain(agreeing: Int, total: Int): String =
        "$agreeing von $total Quellen sehen Regen"

    fun formatTodayDate(epochMs: Long, zoneId: String?): String {
        val zone = zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN)
        val date = formatter.format(Instant.ofEpochMilli(epochMs).atZone(zone))
        return "Heute · $date"
    }

    fun formatDateTime(epochMs: Long, zoneId: String?): String {
        val zone = zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM · HH:mm", Locale.GERMAN)
        return formatter.format(Instant.ofEpochMilli(epochMs).atZone(zone))
    }

    fun formatTime(epochSeconds: Long?, zoneId: String?): String {
        if (epochSeconds == null) return NOT_AVAILABLE
        val zone = zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        return DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochSecond(epochSeconds).atZone(zone))
    }

    fun formatDay(epochSeconds: Long, zoneId: String?): String {
        val zone = zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        return DateTimeFormatter.ofPattern("EEE", Locale.GERMAN)
            .format(Instant.ofEpochSecond(epochSeconds).atZone(zone))
    }

    fun formatDailyLabel(epochSeconds: Long, zoneId: String?, isToday: Boolean): String {
        if (isToday) return "Heute"
        return formatDay(epochSeconds, zoneId).replaceFirstChar { it.uppercase(Locale.GERMAN) }
    }
}
