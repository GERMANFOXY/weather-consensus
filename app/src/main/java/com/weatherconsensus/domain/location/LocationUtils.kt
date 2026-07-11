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

private val ALPINE_COUNTRY_NAMES = setOf(
    "at",
    "aut",
    "austria",
    "österreich",
    "ch",
    "che",
    "switzerland",
    "schweiz",
    "li",
    "lie",
    "liechtenstein",
)

fun GeoLocation.isInAlpineRegion(): Boolean {
    country?.trim()?.lowercase()?.let { normalized ->
        if (normalized in ALPINE_COUNTRY_NAMES) return true
    }
    return !isInGermany() && latitude in 45.8..47.8 && longitude in 5.9..16.2
}

private val UK_COUNTRY_NAMES = setOf(
    "gb",
    "gbr",
    "uk",
    "united kingdom",
    "great britain",
    "england",
    "scotland",
    "wales",
)

fun GeoLocation.isInUnitedKingdom(): Boolean {
    country?.trim()?.lowercase()?.let { normalized ->
        if (normalized in UK_COUNTRY_NAMES) return true
    }
    return latitude in 49.5..61.0 && longitude in (-8.5)..2.0
}
