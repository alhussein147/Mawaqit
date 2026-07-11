package com.hussein.mawaqit.presentation.onboarding.components

enum class PermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED;

    val isGranted: Boolean get() = this == GRANTED
    val isDenied: Boolean get() = this == DENIED
    val isPermanentlyDenied: Boolean get() = this == PERMANENTLY_DENIED
    val isNotRequested: Boolean get() = this == NOT_REQUESTED
}
