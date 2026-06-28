package com.hussein.mawaqit.presentation.settings

import CurrentLocationFetcher
import android.Manifest
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batoulapps.adhan2.CalculationMethod
import com.hussein.core.models.SavedLocation
import com.hussein.mawaqit.R
import com.hussein.mawaqit.infrastructure.settings.AppColorScheme
import com.hussein.mawaqit.infrastructure.settings.AppSettings
import com.hussein.mawaqit.infrastructure.settings.AppTheme
import com.hussein.mawaqit.infrastructure.settings.NotificationSound
import com.hussein.mawaqit.infrastructure.settings.PrayerNotificationSettings
import com.hussein.mawaqit.presentation.navigation.LocalBottomBarHeight
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.presentation.shared.ScreenWrapper
import com.hussein.mawaqit.presentation.util.getPrayersDisplayNames
import com.hussein.mawaqit.presentation.util.hasLocationPermission
import com.hussein.mawaqit.presentation.util.isLocationPermanentlyDenied
import com.hussein.mawaqit.presentation.util.openAppSettings
import com.hussein.mawaqit.ui.listShapes
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val savedLocation by viewModel.savedLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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

    ScreenWrapper(
        modifier = modifier,
        topAppBar = { LargeTopAppBar(title = { Text("Settings") }, scrollBehavior = topAppBarScrollBehavior) },
        content = {
            if (settings == null) {
            LoadingContent(
                modifier = Modifier
                    .fillMaxSize()
            )
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
                onColorSchemeChanged = viewModel::onAppColorSchemeChanged,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
            )
        }
        }
    )
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
                    }) { Text("Open Settings") }
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
                        Text(if (context.isLocationPermanentlyDenied()) "Open App Settings" else "Allow")
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
    onColorSchemeChanged: (AppColorScheme) -> Unit,
    onUpdateLocationTapped: () -> Unit,
    onLocationErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomBarHeight = LocalBottomBarHeight.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomBarHeight + 16.dp),
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
                enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()
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
                options = CalculationMethod.entries.filter { it != CalculationMethod.OTHER }
                    .map { it.displayName() },
                onOptionSelected = { name ->
                    val method = CalculationMethod.entries.first { it.displayName() == name }
                    onMethodChanged(method)
                })
        }

        item {
            SectionHeader("Appearance")
            PickerRow(
                label = "Theme",
                shape = RoundedCornerShape(
                    bottomStart = 2.dp,
                    bottomEnd = 2.dp,
                    topEnd = 16.dp,
                    topStart = 16.dp
                ),
                currentValue = settings.appTheme.displayName,
                options = AppTheme.entries.map { it.displayName },
                onOptionSelected = { name ->
                    onThemeChanged(AppTheme.entries.first { it.displayName == name })
                })
            Spacer(Modifier.height(1.dp))
            PickerRow(
                label = "Color Scheme",
                shape = RoundedCornerShape(
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp,
                    topEnd = 2.dp,
                    topStart = 2.dp
                ),
                currentValue = settings.appColorScheme.displayName,
                options = AppColorScheme.entries.map { it.displayName },
                onOptionSelected = { name ->
                    onColorSchemeChanged(AppColorScheme.entries.first { it.displayName == name })
                })
        }
    }
}


@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: RoundedCornerShape
) {
    SettingOptionContainer(
        onClick = { onCheckedChange(!checked) },
        shape = shape,
        modifier = Modifier.padding(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerRow(
    label: String,
    currentValue: String,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    SettingOptionContainer(onClick = { showSheet = true }, shape = shape) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(20.dp)
            )
        }

    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false }, sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                options.forEach { option ->
                    val isSelected = option == currentValue
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        onClick = {
                            onOptionSelected(option)
                            showSheet = false
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(20.dp)
                                )
                            }
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
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (displayLocation != null) {
                            Text(
                                text = "%.4f°, %.4f°".format(
                                    displayLocation.latitude,
                                    displayLocation.longitude
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
    CalculationMethod.OTHER -> ""
}
