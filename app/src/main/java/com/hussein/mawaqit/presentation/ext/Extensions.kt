package com.hussein.mawaqit.presentation.ext

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.batoulapps.adhan2.Prayer
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun Instant.formatTime(): String {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 = if (local.hour % 12 == 0) 12 else local.hour % 12
    return "%d:%02d %s".format(hour12, local.minute, if (local.hour < 12) "AM" else "PM")
}

fun getPrayersDisplayNames(): List<String> {
    val prayers = Prayer.entries
        .filter { it != Prayer.NONE && it != Prayer.SUNRISE }.map { prayer ->
            prayer.name.lowercase().replaceFirstChar { name -> name.uppercase() }
        }
    return prayers
}


/**
 * Returns true if the user has permanently denied location permission
 * (i.e. shouldShowRationale is false AND permission is not granted).
 * Must be called from an Activity context — works correctly inside Compose
 * via [LocalContext] since ComponentActivity implements ActivityCompat.
 */
fun Context.isLocationPermanentlyDenied(): Boolean {
    if (hasLocationPermission()) return false
    val activity = this as? ComponentActivity ?: return false
    return !ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.ACCESS_FINE_LOCATION
    )
}

fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    })
}

fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
