package com.weatherconsensus

import android.app.Application
import com.weatherconsensus.data.cache.WeatherCache
import com.weatherconsensus.data.client.GeocodingClient
import com.weatherconsensus.data.client.SupplementaryWeatherClient
import com.weatherconsensus.data.client.WeatherProviderClient
import com.weatherconsensus.data.location.LocationService
import com.weatherconsensus.data.network.NetworkModule
import com.weatherconsensus.data.preferences.ProviderAccuracyStore
import com.weatherconsensus.data.network.NetworkMonitor
import com.weatherconsensus.data.preferences.FavoriteLocationStore
import com.weatherconsensus.data.preferences.LastLocationStore
import com.weatherconsensus.data.preferences.ProviderWeightsStore
import com.weatherconsensus.data.repository.WeatherRepository
import com.weatherconsensus.data.update.AppUpdateRepository
import com.weatherconsensus.domain.consensus.ConsensusEngine

class WeatherConsensusApp : Application() {

    lateinit var repository: WeatherRepository
        private set

    lateinit var locationService: LocationService
        private set

    lateinit var weightsStore: ProviderWeightsStore
        private set

    lateinit var lastLocationStore: LastLocationStore
        private set

    lateinit var favoriteLocationStore: FavoriteLocationStore
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    lateinit var accuracyStore: ProviderAccuracyStore
        private set

    lateinit var appUpdateRepository: AppUpdateRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val geocodingClient = GeocodingClient(
            NetworkModule.openMeteoGeocodingApi,
            NetworkModule.openWeatherMapApi,
        )
        val providerClient = WeatherProviderClient(
            NetworkModule.openWeatherMapApi,
            NetworkModule.weatherApiComApi,
            NetworkModule.tomorrowIoApi,
            NetworkModule.openMeteoForecastApi,
            NetworkModule.brightSkyApi,
        )

        val supplementaryClient = SupplementaryWeatherClient(
            NetworkModule.openMeteoAirQualityApi,
            NetworkModule.openMeteoPollenApi,
        )

        accuracyStore = ProviderAccuracyStore(this)

        repository = WeatherRepository(
            geocodingClient = geocodingClient,
            providerClient = providerClient,
            supplementaryClient = supplementaryClient,
            consensusEngine = ConsensusEngine(),
            cache = WeatherCache(this),
            accuracyStore = accuracyStore,
        )
        locationService = LocationService(this, geocodingClient)
        weightsStore = ProviderWeightsStore(this)
        lastLocationStore = LastLocationStore(this)
        favoriteLocationStore = FavoriteLocationStore(this)
        networkMonitor = NetworkMonitor(this)
        appUpdateRepository = AppUpdateRepository(this, NetworkModule.okHttpClient)
    }
}
