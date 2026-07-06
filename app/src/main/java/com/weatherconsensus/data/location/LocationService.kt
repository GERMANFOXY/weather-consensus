package com.weatherconsensus.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.weatherconsensus.data.client.GeocodingClient
import com.weatherconsensus.domain.model.GeoLocation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationService(
    private val context: Context,
    private val geocodingClient: GeocodingClient,
) {
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun getCurrentLocation(): GeoLocation = suspendCancellableCoroutine { cont ->
        if (!hasLocationPermission()) {
            cont.resumeWithException(SecurityException("Standortberechtigung nicht erteilt"))
            return@suspendCancellableCoroutine
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationToken = CancellationTokenSource()

        cont.invokeOnCancellation { cancellationToken.cancel() }

        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location: Location? ->
                if (location == null) {
                    cont.resumeWithException(IllegalStateException("Standort konnte nicht ermittelt werden"))
                } else {
                    cont.resume(
                        GeoLocation(
                            name = "Aktueller Standort",
                            latitude = location.latitude,
                            longitude = location.longitude,
                        )
                    )
                }
            }
            .addOnFailureListener { error ->
                cont.resumeWithException(error)
            }
    }

    suspend fun resolveLocationName(location: GeoLocation): GeoLocation {
        if (location.name != "Aktueller Standort") return location
        val results = geocodingClient.searchCities("${location.latitude},${location.longitude}")
        return results.firstOrNull()?.copy(
            latitude = location.latitude,
            longitude = location.longitude,
        ) ?: location
    }
}
