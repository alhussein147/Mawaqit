package com.hussein.mawaqit.presentation.onboarding

import com.hussein.mawaqit.data.infrastructure.location.SavedLocation

data class OnboardingUiState(
    val page: OnboardingPage = OnboardingPage.WELCOME,
    val isLoadingLocation: Boolean = false,
    val notificationGranted: Boolean = false,
    val savedLocation: SavedLocation? = null,
    val errorMessage: String? = null
)