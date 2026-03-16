package com.hussein.mawaqit.presentation.quran.tafsir

sealed interface TafsirState {
        data object Idle : TafsirState
        data object Loading : TafsirState
        data class Success(val text: String) : TafsirState
        data class Error(val message: String) : TafsirState
        data object NoNetwork : TafsirState
    }