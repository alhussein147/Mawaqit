package com.hussein.mawaqit.presentation.onboarding.components.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hussein.mawaqit.R
import com.hussein.mawaqit.presentation.onboarding.components.OnboardingPage
import com.hussein.mawaqit.presentation.onboarding.components.PageContent

@Composable
fun DonePage() {
    PageContent(
        iconRes = OnboardingPage.DONE.iconRes,
        title = stringResource(OnboardingPage.DONE.titleRes),
        subtitle = stringResource(R.string.onboarding_done_page_subtitle)
    )
}
