package com.hussein.mawaqit.presentation.onboarding.components.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hussein.mawaqit.presentation.onboarding.components.OnboardingPage
import com.hussein.mawaqit.presentation.onboarding.components.PageContent

@Composable
fun WelcomePage() {
    PageContent(
        iconRes = OnboardingPage.WELCOME.iconRes,
        title = stringResource(OnboardingPage.WELCOME.titleRes),
        subtitle = "Stay connected to your daily prayers. We'll need a couple of quick permissions to get you set up."
    )
}
