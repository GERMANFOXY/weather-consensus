package com.weatherconsensus.data.network

import android.util.Log
import com.weatherconsensus.domain.model.WeatherProvider

private const val TAG = "WeatherConsensus"

object WeatherLogger {
    fun logRequest(provider: WeatherProvider, url: String) {
        Log.d(TAG, "${provider.name}: GET $url")
    }

    fun logSuccess(provider: WeatherProvider, detail: String) {
        Log.d(TAG, "${provider.name}: OK – $detail")
    }

    fun logError(provider: WeatherProvider, stage: String, error: Throwable) {
        Log.w(TAG, "${provider.name} [$stage]: ${error.javaClass.simpleName} – ${error.message}", error)
    }
}
