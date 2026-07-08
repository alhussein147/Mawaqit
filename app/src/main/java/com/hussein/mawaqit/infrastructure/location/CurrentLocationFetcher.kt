package com.hussein.mawaqit.infrastructure.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class UserLocation(
    @SerialName("lat")
    val latitude: Double,
    @SerialName("lon")
    val longitude: Double,
    @SerialName("city")
    val city: String
)

class CurrentLocationFetcher(private val context: Context) {

    companion object {
        private const val TAG = "CurrentLocationFetcher"
        private const val TIMEOUT_MS = 15_000L
        private const val IP_API_URL = "http://ip-api.com/json/"

        fun isLocationEnabled(context: Context): Boolean {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        }

        fun hasLocationPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val httpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }
    }

    /**
     * Tries to fetch location using GPS/Network if permissions are granted and enabled.
     * Falls back to IP-based location if:
     * 1. Permissions are denied.
     * 2. Location services are disabled.
     * 3. GPS/Network fix fails or times out.
     */
    suspend fun fetch(): UserLocation? {
        Log.d(TAG, "Fetching current location...")

        if (hasLocationPermission(context) && isLocationEnabled(context)) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)

            Log.d(TAG, "Attempting fresh GPS/Network fix...")
            val fresh = withTimeoutOrNull(TIMEOUT_MS.milliseconds) {
                requestFreshLocation(fusedClient)
            }
            if (fresh != null) {
                Log.d(TAG, "Fresh location obtained: $fresh")
                val cityName = resolveCityName(
                    lat = fresh.first,
                    lng = fresh.second
                )
                return UserLocation(
                    latitude = fresh.first,
                    longitude = fresh.second,
                    city = cityName
                )
            }

            Log.d(TAG, "Fresh location timed out, trying last known location...")
            val lastKnown = getLastKnownLocation(fusedClient)
            if (lastKnown != null) {
                Log.d(TAG, "Last known location obtained: $lastKnown")
                val cityName = resolveCityName(
                    lat = lastKnown.first,
                    lng = lastKnown.second
                )
                return UserLocation(
                    latitude = lastKnown.first,
                    longitude = lastKnown.second,
                    city = cityName
                )
            }
        } else {
            Log.d(TAG, "Location permission missing or service disabled. Skipping GPS/Network.")
        }

        Log.d(TAG, "Falling back to IP-based location...")
        val ipLocation = fetchIpLocation()
        if (ipLocation != null) {
            Log.d(TAG, "IP-based location obtained: $ipLocation")
            val cityName = resolveCityName(
                lat = ipLocation.first,
                lng = ipLocation.second
            )
            return UserLocation(
                latitude = ipLocation.first,
                longitude = ipLocation.second,
                city = cityName
            )
        }

        Log.e(TAG, "All location acquisition methods failed.")
        return null
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(
        fusedClient: FusedLocationProviderClient
    ): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMaxUpdates(1)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedClient.removeLocationUpdates(this)
                val loc = result.lastLocation
                if (cont.isActive) {
                    cont.resume(loc?.let { it.latitude to it.longitude })
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener {
                fusedClient.removeLocationUpdates(callback)
                if (cont.isActive) cont.resume(null)
            }

        cont.invokeOnCancellation {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(
        fusedClient: FusedLocationProviderClient
    ): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (cont.isActive) cont.resume(loc?.let { it.latitude to it.longitude })
        }.addOnFailureListener {
            if (cont.isActive) cont.resume(null)
        }
    }

    private suspend fun fetchIpLocation(): Pair<Double, Double>? {
        return try {
            val response: UserLocation = httpClient.get(IP_API_URL).body()
            response.latitude to response.longitude
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching IP location: ${e.message}")
            null
        }
    }

    private val geocoder = Geocoder(context)

    private suspend fun resolveCityName(lat: Double, lng: Double): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        cont.resume(
                            addresses.firstOrNull()?.locality
                                ?: addresses.firstOrNull()?.adminArea
                                ?: "Unknown"
                        )
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                addresses?.firstOrNull()?.locality
                    ?: addresses?.firstOrNull()?.adminArea
                    ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
