package com.weatherconsensus.domain.location

import com.weatherconsensus.domain.model.GeoLocation

private val GERMANY_COUNTRY_NAMES = setOf(
    "de",
    "deu",
    "deutschland",
    "germany",
    "bundesrepublik deutschland",
)

fun GeoLocation.isInGermany(): Boolean {
    country?.trim()?.lowercase()?.let { normalized ->
        if (normalized in GERMANY_COUNTRY_NAMES) return true
    }
    return latitude in 47.2..55.1 && longitude in 5.8..15.2
}
