package com.hussein.mawaqit.presentation.settings

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.batoulapps.adhan2.CalculationMethod
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.infrastructure.location.CurrentLocationFetcher
import com.hussein.mawaqit.data.infrastructure.location.SavedLocation
import com.hussein.mawaqit.data.infrastructure.settings.AppSettings
import com.hussein.mawaqit.data.infrastructure.settings.AppTheme
import com.hussein.mawaqit.data.infrastructure.settings.NotificationSound
import com.hussein.mawaqit.data.infrastructure.settings.PrayerNotificationSettings
import com.hussein.mawaqit.presentation.ext.getPrayersDisplayNames
import com.hussein.mawaqit.presentation.ext.hasLocationPermission
import com.hussein.mawaqit.presentation.ext.isLocationPermanentlyDenied
import com.hussein.mawaqit.presentation.ext.openAppSettings
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.ui.listShapes


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val savedLocation by viewModel.savedLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingAction by remember { mutableStateOf<LocationAction>(LocationAction.None) }

    // ── Permission launcher ───────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchAndSaveLocation()
        } else {
            // After denial,
            // shouldShowRationale flips to false = permanently denied
            pendingAction = LocationAction.None
        }
    }

    // ── Kick off the right flow when user taps "Update Location" ─────────────
    fun onUpdateLocationTapped() {
        when {
            // GPS disabled — send to system location settings
            !CurrentLocationFetcher.isLocationEnabled(context) -> {
                pendingAction = LocationAction.ShowGpsDialog
            }

            // Permission already granted — fetch immediately
            context.hasLocationPermission() -> {
                viewModel.fetchAndSaveLocation()
            }

            // Permission denied once before — show rationale first
            else -> {
                pendingAction = LocationAction.ShowRationaleDialog
            }
        }
    }

    LaunchedEffect(locationState) {
        if (locationState is SettingsViewModel.LocationUpdateState.Error) {
            Toast.makeText(context, "Error updating location", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") }, navigationIcon = {
                FilledTonalIconButton(onClick = onBack) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back"
                    )
                }
            })
        }) { innerPadding ->
        if (settings == null) {
            LoadingContent()
        } else {
            SettingsContent(
                settings = settings!!,
                locationState = locationState,
                savedLocation = savedLocation,
                onUpdateLocationTapped = ::onUpdateLocationTapped,
                onLocationErrorDismissed = { viewModel.resetLocationState() },
                onPrayerToggled = viewModel::onPrayerNotificationToggled,
                onMethodChanged = viewModel::onCalculationMethodChanged,
                onSoundChanged = viewModel::onNotificationSoundChanged,
                onThemeChanged = viewModel::onAppThemeChanged,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
    when (pendingAction) {

        LocationAction.ShowGpsDialog -> {
            AlertDialog(
                onDismissRequest = { pendingAction = LocationAction.None },
                title = { Text("GPS is Disabled") },
                text = { Text("Location services are turned off. Please enable GPS to update your prayer location.") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingAction = LocationAction.None
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }) { Text("Open com.hussein.islamic.presentation.Settings") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingAction = LocationAction.None }) {
                        Text("Cancel")
                    }
                }
            )
        }

        LocationAction.ShowRationaleDialog -> {
            AlertDialog(
                onDismissRequest = { pendingAction = LocationAction.None },
                title = { Text("Location Permission Needed") },
                text = { Text("Prayer times are calculated from your coordinates. Your location is stored only on this device and never shared.") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingAction = LocationAction.None
                        // If permanently denied, shouldShowRationale is false — go to app settings
                        if (context.isLocationPermanentlyDenied()) {
                            context.openAppSettings()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }) {
                        Text(if (context.isLocationPermanentlyDenied()) "Open com.hussein.islamic.presentation.Settings" else "Allow")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingAction = LocationAction.None }) {
                        Text("Cancel")
                    }
                }
            )
        }

        LocationAction.None -> Unit
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsContent(
    settings: AppSettings,
    savedLocation: SavedLocation?,
    locationState: SettingsViewModel.LocationUpdateState,
    onPrayerToggled: (String, Boolean) -> Unit,
    onMethodChanged: (CalculationMethod) -> Unit,
    onSoundChanged: (NotificationSound) -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onUpdateLocationTapped: () -> Unit,
    onLocationErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        item {
            SectionHeader("Prayer Notifications")
            val enabled = settings.notificationSound != NotificationSound.NONE

            PickerRow(
                label = "Sound",
                currentValue = settings.notificationSound.displayName,
                options = NotificationSound.entries.map { it.displayName },
                onOptionSelected = { name ->
                    onSoundChanged(NotificationSound.entries.first { it.displayName == name })
                })

            AnimatedVisibility(
                enabled,
                enter = expandVertically () + fadeIn(), exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    getPrayersDisplayNames().forEachIndexed { index, prayer ->

                        val enabled = settings.prayerNotifications.isEnabledFor(prayer)

                        ToggleRow(
                            label = prayer,
                            checked = enabled,
                            onCheckedChange = { onPrayerToggled(prayer, it) },
                            shape = when (index) {
                                0 -> listShapes.topItem
                                4 -> listShapes.bottomItem
                                else -> listShapes.midItem
                            }
                        )
                    }
                }
            }

        }

        item {
            SectionHeader("Location")
            LocationRow(
                locationState = locationState,
                savedLocation = savedLocation,
                onUpdateTapped = onUpdateLocationTapped,
                onErrorDismissed = onLocationErrorDismissed
            )
            Spacer(Modifier.height(8.dp))
        }

        item {
            SectionHeader("Calculation Method")
            PickerRow(
                label = "Method",
                currentValue = settings.calculationMethod.displayName(),
                options = CalculationMethod.entries.map { it.displayName() },
                onOptionSelected = { name ->
                    val method = CalculationMethod.entries.first { it.displayName() == name }
                    onMethodChanged(method)
                })
        }

        item {
            SectionHeader("Appearance")
            PickerRow(
                label = "Theme",
                currentValue = settings.appTheme.displayName,
                options = AppTheme.entries.map { it.displayName },
                onOptionSelected = { name ->
                    onThemeChanged(AppTheme.entries.first { it.displayName == name })
                })
        }
    }
}


@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: RoundedCornerShape
) {
    SettingOptionContainer(clickEnabled = false, shape = shape, modifier = Modifier.padding(1.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerRow(
    label: String, currentValue: String, options: List<String>, onOptionSelected: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    SettingOptionContainer(onClick = { showSheet = true }) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false }, sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 16.dp , end = 16.dp , bottom = 32.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                options.forEach { option ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        onClick = {
                            onOptionSelected(option)
                            showSheet = false
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (option == currentValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(text = option, style = MaterialTheme.typography.bodyLarge)
                        }

                    }
                }
            }
        }
    }
}

@Composable
private fun LocationRow(
    locationState: SettingsViewModel.LocationUpdateState,
    savedLocation: SavedLocation?,
    onUpdateTapped: () -> Unit,
    onErrorDismissed: () -> Unit
) {
    // Resolve what to display — Success overrides the persisted savedLocation
    val displayLocation = when (locationState) {
        is SettingsViewModel.LocationUpdateState.Success -> locationState.location
        else -> savedLocation
    }

    SettingOptionContainer(
        clickEnabled = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_location),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = displayLocation?.cityName ?: "No location set",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (displayLocation != null) {
                            Text(
                                text = "%.4f°, %.4f°".format(
                                    displayLocation.latitude,
                                    displayLocation.longitude
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                when (locationState) {
                    SettingsViewModel.LocationUpdateState.Fetching -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    else -> {
                        TextButton(onClick = onUpdateTapped) {
                            Text(
                                text = if (displayLocation != null) "Update" else "Set",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Inline error
            if (locationState is SettingsViewModel.LocationUpdateState.Error) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = locationState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onErrorDismissed) { Text("Dismiss") }
                }
            }
        }
    }

}

@Composable
private fun SettingOptionContainer(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    clickEnabled: Boolean = true,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(clickEnabled, onClick = onClick)
            .then(modifier),
        shape = shape,
        color = color
    ) {
        content()
    }
}

private sealed interface LocationAction {
    data object None : LocationAction
    data object ShowGpsDialog : LocationAction
    data object ShowRationaleDialog : LocationAction
}

private fun PrayerNotificationSettings.isEnabledFor(prayer: String): Boolean = when (prayer) {
    "Fajr" -> fajr
    "Dhuhr" -> dhuhr
    "Asr" -> asr
    "Maghrib" -> maghrib
    "Isha" -> isha
    else -> true
}

private fun CalculationMethod.displayName(): String = when (this) {
    CalculationMethod.MUSLIM_WORLD_LEAGUE -> "Muslim World League"
    CalculationMethod.EGYPTIAN -> "Egyptian"
    CalculationMethod.KARACHI -> "Karachi"
    CalculationMethod.UMM_AL_QURA -> "Umm Al-Qura"
    CalculationMethod.DUBAI -> "Dubai"
    CalculationMethod.MOON_SIGHTING_COMMITTEE -> "Moon Sighting Committee"
    CalculationMethod.NORTH_AMERICA -> "North America (ISNA)"
    CalculationMethod.KUWAIT -> "Kuwait"
    CalculationMethod.QATAR -> "Qatar"
    CalculationMethod.SINGAPORE -> "Singapore"
    CalculationMethod.TURKEY -> "Turkey"
    else -> name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}