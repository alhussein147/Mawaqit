package com.hussein.mawaqit.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri

import com.hussein.mawaqit.data.infrastructure.prayer.PrayerSchedulerManager

/**
 * Single onboarding screen.
 *
 * All page switching is driven by [OnboardingViewModel].
 * [AnimatedContent] handles the slide transition between pages.
 * Permission launchers live here since they need a Compose context,
 * but their results are forwarded straight to the ViewModel.
 *
 * @param onFinished Called when the user completes or skips all onboarding steps.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current


    LaunchedEffect(state.page) {
        if (state.page == OnboardingPage.DONE) {
            // this should be the viewmodel responsibility
            PrayerSchedulerManager.enqueueImmediate(context)
            onFinished()
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
    ) { viewModel.onExactAlarmResult() }

    val batteryOptLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.onBatteryOptimizationResult() }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            StepIndicator(currentPage = state.page)

            Spacer(Modifier.height(32.dp))

            AnimatedContent(
                targetState = state.page,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "onboarding_page"
            ) { page ->
                when (page) {
                    OnboardingPage.WELCOME -> WelcomePage()
                    OnboardingPage.LOCATION -> LocationPage(errorMessage = state.errorMessage)
                    OnboardingPage.FETCHING_LOCATION -> FetchingLocationPage()
                    OnboardingPage.NOTIFICATION -> NotificationPage(
                        cityName = state.savedLocation?.cityName
                    )

                    OnboardingPage.DONE -> Unit
                    OnboardingPage.EXACT_ALARM -> ExactAlarmPage()
                    OnboardingPage.BATTERY_OPTIMIZATION -> BatteryOptimizationPage()
                }
            }
            OnboardingActions(
                page = state.page,
                onPrimaryClick = {
                    when (state.page) {
                        OnboardingPage.WELCOME -> viewModel.onGetStarted()
                        OnboardingPage.LOCATION -> locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )

                        OnboardingPage.NOTIFICATION -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.onNotificationPermissionResult(true)
                            }
                        }

                        OnboardingPage.EXACT_ALARM -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                exactAlarmLauncher.launch(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = "package:com.hussein.islamic".toUri()
                                    }
                                )
                            } else {
                                viewModel.onExactAlarmResult()
                            }
                        }

                        OnboardingPage.BATTERY_OPTIMIZATION -> {
                            batteryOptLauncher.launch(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = "package:com.hussein.islamic".toUri()
                                }
                            )
                        }

                        else -> Unit
                    }
                },
                onSkipClick = {
                    when (state.page) {
                        OnboardingPage.LOCATION -> viewModel.onSkipLocation()
                        OnboardingPage.NOTIFICATION -> viewModel.onSkipNotification()
                        OnboardingPage.EXACT_ALARM -> viewModel.onSkipExactAlarm()
                        OnboardingPage.BATTERY_OPTIMIZATION -> viewModel.onSkipBatteryOptimization()
                        else -> Unit

                    }
                }
            )
        }
    }
}


