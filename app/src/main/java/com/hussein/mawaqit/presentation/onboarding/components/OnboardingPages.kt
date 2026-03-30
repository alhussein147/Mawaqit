package com.hussein.mawaqit.presentation.onboarding.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussein.mawaqit.R

enum class OnboardingPage { WELCOME, LOCATION, FETCHING_LOCATION, NOTIFICATION, EXACT_ALARM, BATTERY_OPTIMIZATION, QURAN_SETUP, DONE }


@Composable
fun WelcomePage() {
    PageContent(
        iconRes = R.drawable.ic_placeholder,
        title = stringResource(R.string.onboarding_welcome_title),
        subtitle = "Stay connected to your daily prayers. " +
                "We'll need a couple of quick permissions to get you set up."
    )
}

@Composable
fun LocationPage(errorMessage: String?) {
    PageContent(
        iconRes = R.drawable.ic_location,
        title = stringResource(R.string.onboarding_location_title),
        subtitle = stringResource(R.string.onboarding_location_description),
        errorMessage = errorMessage
    )
}

@Composable
fun FetchingLocationPage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
        Spacer(Modifier.height(24.dp))
        Text("Finding your location…", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This only takes a moment",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NotificationPage(cityName: String?) {
    PageContent(
        iconRes = R.drawable.ic_notification,
        title = stringResource(R.string.onboarding_notification_title),
        subtitle = stringResource(R.string.onboarding_notification_description),
        badge = cityName?.let { "📍 $it" }
    )
}

@Composable
fun ExactAlarmPage() {
    PageContent(
        iconRes = R.drawable.ic_alarm,
        title = stringResource(R.string.onboarding_exact_alarm_title),
        subtitle = "To ensure prayer alarms fire at the exact right time — " +
                "even when your phone is in deep sleep — we need permission " +
                "to schedule exact alarms."
    )
}

@Composable
fun BatteryOptimizationPage() {
    PageContent(
        iconRes = R.drawable.ic_battery,
        title = stringResource(R.string.onboarding_battery_optimization_title),
        subtitle = "Android's battery optimization can delay or block prayer " +
                "notifications. Exempting this app ensures you never miss " +
                "a prayer time."
    )
}

@Composable
fun DonePage() {
    PageContent(
        iconRes = R.drawable.ic_check,
        title = "All Set",
        subtitle = ""
    )
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuranSetupPage(
    progress: Float,
    currentSurah: Int,
    failed: Boolean,
    onRetry: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "quran_setup_progress"
    )

    PageContent(
        iconRes = R.drawable.ic_placeholder,
        title = "Loading quran data",
        subtitle = "Loaded $currentSurah of 114",
        badge = null,
        errorMessage = if (failed) "Error happed while loading data" else "",
        progress = animatedProgress
    )

}

@Composable
fun PageContent(
    iconRes: Int,
    title: String,
    subtitle: String,
    badge: String? = null,
    errorMessage: String? = null,
    progress: Float? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )

        progress?.let {
            if (it > 0f) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { it },
                    modifier = Modifier,
                    color = ProgressIndicatorDefaults.linearColor,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }
        }

        badge?.let {
            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        errorMessage?.let { msg ->
            msg.ifBlank { return@let }
            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun OnboardingActions(
    page: OnboardingPage,
    onPrimaryClick: () -> Unit,
    onSkipClick: () -> Unit,
    primaryButtonEnabled: Boolean = true
) {
    val primaryLabel = when (page) {
        OnboardingPage.WELCOME -> stringResource(R.string.get_started)
        OnboardingPage.LOCATION -> stringResource(R.string.allow_location)
        OnboardingPage.NOTIFICATION -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            stringResource(R.string.allow_notifications) else stringResource(R.string.continue_)

        OnboardingPage.EXACT_ALARM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            stringResource(R.string.allow_exact_alarms) else stringResource(R.string.continue_)

        OnboardingPage.BATTERY_OPTIMIZATION -> stringResource(R.string.disable_optimization)
        OnboardingPage.QURAN_SETUP -> "Load quran data"
        OnboardingPage.DONE -> "Finish !"
        else -> null
    }
    val showSkip = page in listOf(
        OnboardingPage.LOCATION,
        OnboardingPage.NOTIFICATION,
        OnboardingPage.EXACT_ALARM,
        OnboardingPage.BATTERY_OPTIMIZATION
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        primaryLabel?.let { label ->
            Button(
                onClick = onPrimaryClick,
                enabled = primaryButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(label, fontSize = 16.sp)
            }
        }

        if (showSkip) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkipClick) {
                Text(
                    stringResource(R.string.skip_for_now),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StepIndicator(currentPage: OnboardingPage) {
    val visiblePages = listOf(
        OnboardingPage.WELCOME,
        OnboardingPage.LOCATION,
        OnboardingPage.NOTIFICATION,
        OnboardingPage.EXACT_ALARM,
        OnboardingPage.BATTERY_OPTIMIZATION
    )
    // Clamp FETCHING_LOCATION to show the LOCATION dot as active
    val activeIndex = when (currentPage) {
        OnboardingPage.FETCHING_LOCATION -> 1
        else -> visiblePages.indexOf(currentPage).coerceAtLeast(0)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visiblePages.forEachIndexed { index, _ ->
            val isActive = index == activeIndex
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (isActive) 24.dp else 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}