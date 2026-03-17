package com.hussein.mawaqit.presentation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.presentation.azkar.AzkarCategoryScreen
import com.hussein.mawaqit.presentation.azkar.AzkarListScreen
import com.hussein.mawaqit.presentation.home.HomeScreen
import com.hussein.mawaqit.presentation.onboarding.OnboardingScreen
import com.hussein.mawaqit.presentation.quran.list_screen.SurahListScreen
import com.hussein.mawaqit.presentation.quran.reader.QuranReaderScreen
import com.hussein.mawaqit.presentation.settings.SettingsScreen
import com.hussein.mawaqit.presentation.shared.LoadingContent
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey

@Serializable
data object Onboarding : Screen

@Serializable
data object Home : Screen

@Serializable
data object Settings : Screen

@Serializable
data object Initializing : Screen

@Serializable
data object AzkarCategories : Screen

@Serializable
data class AzkarList(val categoryIndex: Int) : Screen

@Serializable
data object QuranSurahList : Screen

@Serializable
data class QuranReader(val surahIndex: Int) : Screen


@Composable
fun AppNavigation(settingsRepository: SettingsRepository) {

    val slideSpec = tween<IntOffset>(durationMillis = 300, easing = FastOutLinearInEasing)

    val backStack = rememberNavBackStack(Initializing)

    LaunchedEffect(Unit) {
        if (settingsRepository.isOnboardingDone()) {
            backStack.removeLastOrNull()
            backStack.add(Home)
        } else {
            backStack.removeLastOrNull()
            backStack.add(Onboarding)
        }
    }


    NavDisplay(
        transitionSpec = {
            // Slide in from right when navigating forward
            slideInHorizontally(slideSpec) { it }togetherWith
                    slideOutHorizontally(slideSpec) { -it }
        },
        popTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(slideSpec) { -it }  togetherWith
                    slideOutHorizontally(slideSpec) { it }
        },
        predictivePopTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(slideSpec) { -it }  togetherWith
                    slideOutHorizontally(slideSpec) { it }
        },
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            entry<Initializing> {
                LoadingContent()
            }

            entry<Onboarding> {
                OnboardingScreen(
                    onFinished = {
                        backStack.clear()
                        backStack.add(Home)
                    }
                )
            }

            entry<Home> {
                HomeScreen(
                    onNavigateToSettings = { backStack.add(Settings) },
                    onNavigateToAzkar = {
                        backStack.add(
                            AzkarCategories
                        )
                    }, onNavigateToQuran = { backStack.add(QuranSurahList) }
                )
            }

            entry<AzkarCategories> {
                AzkarCategoryScreen(
                    onCategorySelected = { index -> backStack.add(AzkarList(index)) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            entry<AzkarList> { key ->
                AzkarListScreen(
                    categoryIndex = key.categoryIndex,
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            entry<QuranSurahList> {
                SurahListScreen(
                    onSurahSelected = { index -> backStack.add(QuranReader(index)) },
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<QuranReader> { key ->
                QuranReaderScreen(
                    surahIndex = key.surahIndex,
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<Settings> {
                SettingsScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}