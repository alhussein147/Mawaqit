package com.hussein.mawaqit.presentation.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.hussein.mawaqit.R

@Composable
fun QuranSetupPage(
    progress: Float,
    failed: Boolean,
    isOffline: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "quran_setup_progress"
    )

    PageContent(
        iconRes = OnboardingPage.QURAN_SETUP.iconRes,
        title = stringResource(OnboardingPage.QURAN_SETUP.titleRes),
        subtitle = if (isOffline) "Please connect to the internet to load Quran data." else "We are preparing the Quran database for you. This will allow you to read and listen to the Quran offline.",
        errorMessage = when {
            isOffline -> "Internet connection is required."
            failed -> stringResource(R.string.onboarding_quran_error)
            else -> null
        },
        progress = if (isOffline) null else animatedProgress
    )
}
