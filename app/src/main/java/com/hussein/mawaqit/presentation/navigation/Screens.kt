package com.hussein.mawaqit.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey

@Serializable
data object Main : Screen

@Serializable
data object Onboarding : Screen

@Serializable
data object Home : Screen

@Serializable
data object Settings : Screen

@Serializable
data object NotificationSettings : Screen

@Serializable
data object Initializing : Screen

@Serializable
data object AzkarCategories : Screen

@Serializable
data class AzkarList(val categoryId: Int) : Screen

@Serializable
data object QuranSurahList : Screen

@Serializable
data object QuranSearch : Screen

@Serializable
data class QuranReading(val surahIndex: Int, val scrollToAyah: Int? = null) : Screen

@Serializable
data object RadioChannels : Screen

@Serializable
data object QuranReaderSettings : Screen

@Serializable
data object QuranBookmarks : Screen

@Serializable
data class QuranReaderWithTafsir(val surahIndex: Int, val surahName: String) : Screen
