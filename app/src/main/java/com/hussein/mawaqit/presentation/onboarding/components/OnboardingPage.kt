package com.hussein.mawaqit.presentation.onboarding.components

import com.hussein.mawaqit.R

enum class OnboardingPage(
    val titleRes: Int,
    val subtitleRes: Int? = null,
    val iconRes: Int
) {
    WELCOME(
        titleRes = R.string.onboarding_welcome_title,
        iconRes = R.drawable.ic_placeholder
    ),
    PERMISSIONS(
        titleRes = R.string.onboarding_permissions_title,
        iconRes = R.drawable.ic_notification
    ),
    OPTIONS(
        titleRes = R.string.onboarding_options_title,
        iconRes = R.drawable.ic_settings
    ),
    QURAN_SETUP(
        titleRes = R.string.onboarding_quran_title,
        iconRes = R.drawable.ic_cloud_download
    ),
    DONE(
        titleRes = R.string.onboarding_done_title,
        iconRes = R.drawable.ic_check
    );

    companion object {
        val pages = entries.toList()
    }
}
