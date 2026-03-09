package com.hussein.mawaqit.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.presentation.azkar.AzkarCategoryScreen
import com.hussein.mawaqit.presentation.azkar.AzkarListScreen
import com.hussein.mawaqit.presentation.home.HomeScreen
import com.hussein.mawaqit.presentation.onboarding.OnboardingScreen
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
data class  AzkarList(val categoryIndex: Int) : Screen


@Composable
fun AppNavigation(settingsRepository: SettingsRepository) {


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

    // Hold rendering until we know the start destination

    NavDisplay(
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
                    onNavigateToSettings = { backStack.add(Settings) } , onNavigateToAzkar = {
                        backStack.add(
                            AzkarCategories
                        )
                    }
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

            entry<Settings> {
                SettingsScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}