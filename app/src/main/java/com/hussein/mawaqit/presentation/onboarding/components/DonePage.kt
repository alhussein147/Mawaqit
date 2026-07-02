package com.hussein.mawaqit.presentation.onboarding.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun DonePage() {
    PageContent(
        iconRes = OnboardingPage.DONE.iconRes,
        title = stringResource(OnboardingPage.DONE.titleRes),
        subtitle = "You are all set! May your prayers be accepted and your journey with the Quran be blessed."
    )
}
