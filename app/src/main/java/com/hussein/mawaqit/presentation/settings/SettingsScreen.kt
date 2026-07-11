package com.hussein.mawaqit.presentation.settings

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batoulapps.adhan2.CalculationMethod
import com.hussein.core.models.SavedLocation
import com.hussein.mawaqit.R
import com.hussein.mawaqit.infrastructure.location.CurrentLocationFetcher
import com.hussein.mawaqit.infrastructure.settings.AppColorScheme
import com.hussein.mawaqit.infrastructure.settings.AppSettings
import com.hussein.mawaqit.infrastructure.settings.AppTheme
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.presentation.shared.RootScreenWrapper
import com.hussein.mawaqit.presentation.shared.SettingNavigationRow
import com.hussein.mawaqit.presentation.shared.SettingPickerRow
import com.hussein.mawaqit.presentation.shared.SettingSectionHeader
import com.hussein.mawaqit.presentation.util.hasLocationPermission
import com.hussein.mawaqit.presentation.util.isLocationPermanentlyDenied
import com.hussein.mawaqit.presentation.util.openAppSettings
import com.hussein.mawaqit.ui.theme.MawaqitTheme
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onNavigateToNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val savedLocation by viewModel.savedLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var pendingAction by remember { mutableStateOf<LocationAction>(LocationAction.None) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchAndSaveLocation()
        } else {
            if (context.isLocationPermanentlyDenied()) {
                pendingAction = LocationAction.ShowSettingsDialog
            } else {
                pendingAction = LocationAction.None
            }
        }
    }

    fun onUpdateLocationTapped() {
        val activity = context as? ComponentActivity
        when {
            !CurrentLocationFetcher.isLocationEnabled(context) -> {
                pendingAction = LocationAction.ShowGpsDialog
            }

            context.hasLocationPermission() -> {
                viewModel.fetchAndSaveLocation()
            }

            activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                pendingAction = LocationAction.ShowRationaleDialog
            }

            else -> {
                // First time OR permanently denied
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    RootScreenWrapper(
        topAppBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Black) },
                scrollBehavior = topAppBarScrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                )
            )
        },
        content = {
            if (settings == null) {
                LoadingContent(modifier = Modifier.fillMaxSize())
            } else {
                SettingsContent(
                    settings = settings!!,
                    locationState = locationState,
                    savedLocation = savedLocation,
                    onUpdateLocationTapped = ::onUpdateLocationTapped,
                    onLocationErrorDismissed = { viewModel.resetLocationState() },
                    onMethodChanged = viewModel::onCalculationMethodChanged,
                    onThemeChanged = viewModel::onAppThemeChanged,
                    onColorSchemeChanged = viewModel::onAppColorSchemeChanged,
                    onNavigateToNotificationSettings = onNavigateToNotificationSettings,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                        .then(modifier)
                )
            }
        }
    )

    // Dialogs
    when (pendingAction) {
        LocationAction.ShowGpsDialog -> {
            AlertDialog(
                onDismissRequest = { pendingAction = LocationAction.None },
                title = { Text(stringResource(R.string.gps_is_disabled)) },
                text = { Text(stringResource(R.string.gps_off_description)) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingAction = LocationAction.None
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }) { Text(stringResource(R.string.open_settings)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingAction = LocationAction.None }) {
                        Text(
                            stringResource(R.string.cancel)
                        )
                    }
                }
            )
        }

        LocationAction.ShowRationaleDialog -> {
            AlertDialog(
                onDismissRequest = { pendingAction = LocationAction.None },
                title = { Text(stringResource(R.string.location_permission_needed)) },
                text = { Text(stringResource(R.string.location_permission_denied_description)) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingAction = LocationAction.None
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }) {
                        Text(stringResource(R.string.allow))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingAction = LocationAction.None }) {
                        Text(
                            stringResource(R.string.cancel)
                        )
                    }
                }
            )
        }

        LocationAction.ShowSettingsDialog -> {
            AlertDialog(
                onDismissRequest = { pendingAction = LocationAction.None },
                title = { Text(stringResource(R.string.location_permission_permenently_denied)) },
                text = { Text(stringResource(R.string.location_permission_permenently_denied_description)) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingAction = LocationAction.None
                        context.openAppSettings()
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingAction = LocationAction.None }) {
                        Text(
                            stringResource(R.string.cancel)
                        )
                    }
                }
            )
        }

        LocationAction.None -> Unit
    }
}


@Composable
private fun SettingsContent(
    settings: AppSettings,
    savedLocation: SavedLocation?,
    locationState: SettingsViewModel.LocationUpdateState,
    onMethodChanged: (CalculationMethod) -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onColorSchemeChanged: (AppColorScheme) -> Unit,
    onUpdateLocationTapped: () -> Unit,
    onLocationErrorDismissed: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp , end = 16.dp, bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            SettingSectionHeader(stringResource(R.string.prayer_notifications))
            SettingNavigationRow(
                label = stringResource(R.string.notification_settings),
                subLabel = stringResource(R.string.control_sounds_and_specific_prayer_alerts),
                onClick = onNavigateToNotificationSettings,
                icon = ImageVector.vectorResource(R.drawable.ic_notification)
            )
        }

        item {
            SettingSectionHeader(stringResource(R.string.location))
            LocationRow(
                locationState = locationState,
                savedLocation = savedLocation,
                onUpdateTapped = onUpdateLocationTapped,
                onErrorDismissed = onLocationErrorDismissed
            )
        }

        item {
            SettingSectionHeader(stringResource(R.string.calculation_method))
            SettingPickerRow(
                label = stringResource(R.string.method),
                currentValue = settings.calculationMethod.displayName(),
                options = CalculationMethod.entries.map { it.displayName() },
                onOptionSelected = { name ->
                    val method = CalculationMethod.entries.first { it.displayName() == name }
                    onMethodChanged(method)
                }
            )
        }

        item {
            SettingSectionHeader(stringResource(R.string.appearance))
            SettingPickerRow(
                label = stringResource(R.string.theme),
                currentValue = settings.appTheme.displayName,
                options = AppTheme.entries.map { it.displayName },
                onOptionSelected = { name ->
                    onThemeChanged(AppTheme.entries.first { it.displayName == name })
                },
                shape = MawaqitTheme.listShapes.topItem            )
            SettingPickerRow(
                label = stringResource(R.string.color_scheme),
                currentValue = settings.appColorScheme.displayName,
                options = AppColorScheme.entries.map { it.displayName },
                onOptionSelected = { name ->
                    onColorSchemeChanged(AppColorScheme.entries.first { it.displayName == name })
                },
                shape = MawaqitTheme.listShapes.bottomItem            )
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
    val context = LocalContext.current
    val hasPermission = context.hasLocationPermission()
    val isGpsEnabled = CurrentLocationFetcher.isLocationEnabled(context)

    val displayLocation = when (locationState) {
        is SettingsViewModel.LocationUpdateState.Success -> locationState.location
        else -> savedLocation
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_location),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        if (!hasPermission || !isGpsEnabled) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                                border = BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                ),
                                content = {}
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = displayLocation?.cityName
                                ?: stringResource(R.string.no_location),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
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
                        } else {
                            Text(
                                text = stringResource(R.string.configure_location_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (locationState is SettingsViewModel.LocationUpdateState.Fetching) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                } else {
                    Button(
                        onClick = onUpdateTapped,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(text = stringResource(if (displayLocation != null) R.string.update else R.string.set))
                    }
                }
            }

            if (!hasPermission || !isGpsEnabled) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_error), // Or another icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                !hasPermission && !isGpsEnabled -> stringResource(R.string.permission_missing_and_gps_disabled)
                                !hasPermission -> stringResource(R.string.location_permission_not_granted)
                                else -> stringResource(R.string.gps_is_not_enabled)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (locationState is SettingsViewModel.LocationUpdateState.Error) {
                Spacer(Modifier.height(12.dp))
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
                    TextButton(onClick = onErrorDismissed) { Text(stringResource(R.string.dismiss)) }
                }
            }
        }
    }
}

private sealed interface LocationAction {
    data object None : LocationAction
    data object ShowGpsDialog : LocationAction
    data object ShowRationaleDialog : LocationAction
    data object ShowSettingsDialog : LocationAction
}


private fun CalculationMethod.displayName(): String = this.name.replaceFirstChar { it.uppercase() }

