package com.hussein.mawaqit.presentation.onboarding.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hussein.mawaqit.R

@Composable
fun WelcomePage() {
    PageContent(
        iconRes = OnboardingPage.WELCOME.iconRes,
        title = stringResource(OnboardingPage.WELCOME.titleRes),
        subtitle = "Stay connected to your daily prayers. We'll need a couple of quick permissions to get you set up."
    )
}
