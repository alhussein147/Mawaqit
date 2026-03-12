package com.hussein.mawaqit.data.infrastructure.location


import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import androidx.core.location.LocationManagerCompat.isLocationEnabled
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.hussein.mawaqit.data.infrastructure.location.CurrentLocationFetcher.Companion.TIMEOUT_MS
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * A one-shot location fetcher using the Fused Location Provider (Google Play Services).
 *
 * Called once during onboarding — the result is stored in [LocationRepository] and
 * reused forever (prayer times don't need live location, only city-level accuracy).
 *
 * Falls back to the last known location before requesting a fresh fix, which is
 * faster and saves battery.
 *
 * Requires: ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION already granted.
 */

class CurrentLocationFetcher(val context: Context) {

    companion object{
        private const val TIMEOUT_MS = 15_000L
        fun isLocationEnabled(context: Context): Boolean {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        }

    }

    /**
     * Always requests a fresh GPS/network fix.
     * Falls back to lastLocation only if no fix arrives within [TIMEOUT_MS].
     *
     * Requires: ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION already granted.
     */
    @SuppressLint("MissingPermission")
    suspend fun fetch(): Pair<Double, Double>? {
        if (!isLocationEnabled(context)) return null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        // Always try a fresh fix first — lastLocation can be hours old
        val fresh = withTimeoutOrNull(TIMEOUT_MS) {
            requestFreshLocation(fusedClient)
        }
        if (fresh != null) return fresh

        // Fallback: last known location if fresh fix timed out
        return getLastKnownLocation(fusedClient)
    }

    // ---------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(
        fusedClient: FusedLocationProviderClient
    ): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->

        val request =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L).setMaxUpdates(1)
                .setWaitForAccurateLocation(false).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedClient.removeLocationUpdates(this)
                val loc = result.lastLocation
                cont.resume(if (loc != null) Pair(loc.latitude, loc.longitude) else null)
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        cont.invokeOnCancellation {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(
        fusedClient: FusedLocationProviderClient
    ): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
        fusedClient.lastLocation.addOnSuccessListener { loc ->
                cont.resume(if (loc != null) Pair(loc.latitude, loc.longitude) else null)
            }.addOnFailureListener { cont.resume(null) }
    }


}