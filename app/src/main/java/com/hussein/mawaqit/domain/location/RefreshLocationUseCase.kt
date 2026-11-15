package com.hussein.mawaqit.domain.location

import com.hussein.core.LocationRepository
import com.hussein.core.models.UserLocation
import com.hussein.mawaqit.infrastructure.location.CurrentLocationFetcher
import com.hussein.mawaqit.infrastructure.workers.prayer.PrayerSchedulerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RefreshLocationUseCase(
    private val locationFetcher: CurrentLocationFetcher,
    private val locationRepository: LocationRepository,
    private val prayerSchedulerManager: PrayerSchedulerManager
) {
    suspend fun execute(fallbackToIp: Boolean = false): Result<UserLocation> = withContext(Dispatchers.IO) {
        try {
            val userLocation = locationFetcher.fetch(fallbackToIp = fallbackToIp)
            if (userLocation != null) {
                val saved = locationRepository.saveLocation(
                    latitude = userLocation.latitude,
                    longitude = userLocation.longitude,
                    cityName = userLocation.cityName
                )
                // Reschedule prayer alarms for the new location
                prayerSchedulerManager.enqueueImmediate()
                Result.success(saved)
            } else {
                Result.failure(Exception("Could not determine location. Make sure GPS is enabled."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
