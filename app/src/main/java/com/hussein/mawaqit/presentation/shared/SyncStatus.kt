package com.hussein.mawaqit.presentation.shared

data class SyncStatus(
    val isSyncing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null
)
