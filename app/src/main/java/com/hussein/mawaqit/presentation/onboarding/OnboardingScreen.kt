package com.hussein.mawaqit.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.presentation.onboarding.components.OnboardingActions
import com.hussein.mawaqit.presentation.onboarding.components.OnboardingPage
import com.hussein.mawaqit.presentation.onboarding.components.StepIndicator
import com.hussein.mawaqit.presentation.onboarding.components.pages.DonePage
import com.hussein.mawaqit.presentation.onboarding.components.pages.OptionsPage
import com.hussein.mawaqit.presentation.onboarding.components.pages.PermissionsPage
import com.hussein.mawaqit.presentation.onboarding.components.pages.WelcomePage
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { OnboardingPage.entries.size })
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(state.page) {
        if (pagerState.currentPage != state.page.ordinal) {
            pagerState.animateScrollToPage(state.page.ordinal)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != state.page.ordinal) {
            viewModel.onPageSwiped(OnboardingPage.entries[pagerState.currentPage])
        }
    }


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStatuses(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.onExactAlarmResult(context) }

    val batteryOptLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.onBatteryOptimizationResult(context) }

    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            StepIndicator(
                currentPage = state.page,
                modifier = Modifier.padding(top = 16.dp)
            )
            BackHandler(enabled = pagerState.currentPage != 0) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !state.isQuranPopulating,
                modifier = Modifier.weight(1f),
                key = { OnboardingPage.entries[it].name },
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) { pageIndex ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (OnboardingPage.entries[pageIndex]) {
                        OnboardingPage.WELCOME -> WelcomePage()
                        OnboardingPage.PERMISSIONS -> PermissionsPage(
                            isLocationGranted = state.isLocationGranted,
                            isNotificationGranted = state.isNotificationGranted,
                            isExactAlarmGranted = state.isExactAlarmGranted,
                            isBatteryOptimizationIgnored = state.isBatteryOptimizationIgnored,
                            onLocationClick = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            onNotificationClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.onNotificationPermissionResult(true)
                                }
                            },
                            onExactAlarmClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    exactAlarmLauncher.launch(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = "package:${context.packageName}".toUri()
                                        }
                                    )
                                }
                            },
                            onBatteryClick = {
                                batteryOptLauncher.launch(
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = "package:${context.packageName}".toUri()
                                    }
                                )
                            },
                            isLoadingLocation = state.isLoadingLocation
                        )
                        OnboardingPage.OPTIONS -> OptionsPage(
                            settings = state.settings,
                            onCalculationMethodChanged = viewModel::onCalculationMethodChanged,
                            onNotificationSoundChanged = viewModel::onNotificationSoundChanged,
                            onAppThemeChanged = viewModel::onAppThemeChanged
                        )
                        OnboardingPage.DONE -> DonePage()
                    }
                }
            }

            OnboardingActions(
                page = state.page,
                onPrimaryClick = {
                    when (state.page) {
                        OnboardingPage.WELCOME -> viewModel.onGetStarted()
                        OnboardingPage.PERMISSIONS -> viewModel.onPermissionsContinue()
                        OnboardingPage.OPTIONS -> viewModel.onPermissionsContinue()
                        OnboardingPage.DONE -> {
                            viewModel.onOnboardingComplete(); onFinished()
                        }
                    }
                },
                onSkipClick = viewModel::onSkipQuranSetup,
                primaryButtonEnabled = !state.isLoadingLocation && !state.isQuranPopulating &&
                        (!state.isOffline),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
