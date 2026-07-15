package com.hussein.mawaqit.presentation.onboarding.components

import com.hussein.mawaqit.R

enum class OnboardingPage(
    val titleRes: Int,
    val subtitleRes: Int? = null,
    val iconRes: Int
) {
    WELCOME(
        titleRes = R.string.onboarding_welcome_title,
        iconRes = R.drawable.ic_new_logo
    ),
    PERMISSIONS(
        titleRes = R.string.onboarding_permissions_title,
        iconRes = R.drawable.ic_bell
    ),
    OPTIONS(
        titleRes = R.string.onboarding_options_title,
        iconRes = R.drawable.ic_settings
    ),
    DONE(
        titleRes = R.string.onboarding_done_title,
        iconRes = R.drawable.ic_check,
        subtitleRes = R.string.onboarding_done_page_subtitle
    ),

}
