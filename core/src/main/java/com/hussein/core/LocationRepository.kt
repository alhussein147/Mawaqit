package com.hussein.core

import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hussein.core.models.SavedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private val Context.locationDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "location")


class LocationRepository(private val context: Context) {

    companion object {
        private val KEY_LATITUDE  = doublePreferencesKey("latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("longitude")
        private val KEY_CITY_NAME = stringPreferencesKey("city_name")
    }


    val locationFlow: Flow<SavedLocation?> = context.locationDataStore.data.map { prefs ->
        val lat = prefs[KEY_LATITUDE]  ?: return@map null
        val lng = prefs[KEY_LONGITUDE] ?: return@map null
        SavedLocation(
            latitude = lat,
            longitude = lng,
            cityName = prefs[KEY_CITY_NAME] ?: "Unknown"
        )
    }

    /** One-shot check — returns true if a location has been saved. */
    suspend fun hasLocation(): Boolean =
        context.locationDataStore.data.firstOrNull()?.get(KEY_LATITUDE) != null

    /** One-shot read — returns the saved [SavedLocation], or null. */
    suspend fun getSavedLocation(): SavedLocation? = locationFlow.firstOrNull()

    /**
     * Persists [latitude] and [longitude], reverse-geocodes a city name,
     * and returns the saved [SavedLocation].
     */
    suspend fun saveLocation(latitude: Double, longitude: Double): SavedLocation =
        withContext(Dispatchers.IO) {
            val cityName = resolveCityName(latitude, longitude)
            context.locationDataStore.edit { prefs ->
                prefs[KEY_LATITUDE]  = latitude
                prefs[KEY_LONGITUDE] = longitude
                prefs[KEY_CITY_NAME] = cityName
            }
            SavedLocation(latitude, longitude, cityName)
        }

    /** Clears all stored location data (e.g. when user resets the app). */
    suspend fun clearLocation() {
        context.locationDataStore.edit { it.clear() }
    }

    // ---------------------------------------------------------------------------
    // Geocoding
    // ---------------------------------------------------------------------------

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

