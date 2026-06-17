package com.hussein.mawaqit.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.presentation.azkar.AzkarScreen
import com.hussein.mawaqit.presentation.azkar.categories.AzkarCategoryScreen
import com.hussein.mawaqit.presentation.home.HomeScreen
import com.hussein.mawaqit.presentation.onboarding.OnboardingScreen
import com.hussein.mawaqit.presentation.quran.list_screen.SurahListScreen
import com.hussein.mawaqit.presentation.quran.reader.QuranReaderScreen
import com.hussein.mawaqit.presentation.quran.search.QuranSearchScreen
import com.hussein.mawaqit.presentation.radio.RadioChannelListScreen
import com.hussein.mawaqit.presentation.settings.SettingsScreen
import com.hussein.mawaqit.presentation.shared.LoadingContent
import kotlinx.serialization.Serializable
import org.koin.core.annotation.KoinExperimentalAPI

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
data object QuranSearch : Screen

@Serializable
data class QuranReader(val surahIndex: Int, val scrollToAyah: Int? = null) : Screen

@Serializable
data object RadioChannels : Screen

private data class TopLevelDestination(
    val screen: Screen,
    val label: String,
    val icon: Int
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Home, "Home", R.drawable.ic_home),
    TopLevelDestination(QuranSurahList, "Quran", R.drawable.ic_placeholder),
    TopLevelDestination(Settings, "Settings", R.drawable.ic_settings)
)

private fun NavKey.topLevelIndex(): Int = topLevelDestinations.indexOfFirst { it.screen == this }

private fun NavKey.navBarIndex(): Int = when (this) {
    Home -> 0
    QuranSurahList, is QuranReader, QuranSearch -> 1
    Settings -> 2
    else -> -1
}


@OptIn(KoinExperimentalAPI::class)
@Composable
fun AppNavigation(settingsRepository: SettingsRepository) {

    val backStack = rememberNavBackStack(Initializing)
    val navDirection = remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        if (settingsRepository.isOnboardingDone()) {
            backStack.removeLastOrNull()
            backStack.add(Home)
        } else {
            backStack.removeLastOrNull()
            backStack.add(Onboarding)
        }
    }

    val currentScreen = backStack.lastOrNull()
    val selectedTopLevel = currentScreen?.topLevelIndex()?.takeIf { it >= 0 }

    fun navigateToTopLevel(destination: Screen) {
        val current = backStack.lastOrNull()
        if (current == destination) return

        val currentIndex = current?.navBarIndex()?.takeIf { it >= 0 } ?: Home.navBarIndex()
        val destinationIndex = destination.topLevelIndex()
        navDirection.intValue = destinationIndex.compareTo(currentIndex).takeIf { it != 0 } ?: 1

        backStack.clear()
        backStack.add(destination)
    }

    Box(modifier = Modifier.fillMaxSize()) {


        NavDisplay(
            transitionSpec = {
                enterTransition(navDirection.intValue) togetherWith exitTransition(navDirection.intValue)
            },
            popTransitionSpec = {
                popEnterTransition() togetherWith popExitTransition()
            },
            predictivePopTransitionSpec = {
                popEnterTransition() togetherWith popExitTransition()
            },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            backStack = backStack,
            onBack = {
                if (backStack.lastOrNull()?.topLevelIndex()?.takeIf { it >= 0 } != null) {
                    navigateToTopLevel(Home)
                } else {
                    backStack.removeLastOrNull()
                }
            },
            entryProvider = entryProvider {

                entry<Home> {
                    HomeScreen(
                        modifier = Modifier.padding(bottom = 80.dp),
                        onNavigateToAzkar = {
                            backStack.add(
                                AzkarCategories
                            )
                        },
                        onNavigateToRadio = { backStack.add(RadioChannels) },
                        onNavigateToReader = { index, ayahIndex ->
                            backStack.add(QuranReader(index, ayahIndex))
                        },
                    )
                }

                entry<Settings> {
                    SettingsScreen(modifier = Modifier.padding(bottom = 80.dp))
                }

                entry<QuranSurahList> {
                    SurahListScreen(
                        modifier = Modifier.padding(bottom = 80.dp),
                        onSurahSelected = { index, scrollToAyah ->
                            backStack.add(QuranReader(index, scrollToAyah))
                        },
                        onNavigateToSearch = {
                            backStack.add(QuranSearch)
                        }
                    )
                }

                entry<QuranReader> { key ->
                    QuranReaderScreen(
                        scrollToAyah = key.scrollToAyah,
                        surahIndex = key.surahIndex,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<QuranSearch> {
                    QuranSearchScreen(
                        onSurahSelected = { surahIndex ->
                            backStack.add(QuranReader(surahIndex))
                        },
                        onAyahSelected = { surahIndex, ayahNumber ->
                            backStack.add(QuranReader(surahIndex, ayahNumber))
                        },
                        onBack = { backStack.removeLastOrNull() },

                        )
                }

                entry<AzkarCategories> {
                    AzkarCategoryScreen(
                        onCategorySelected = { index -> backStack.add(AzkarList(index)) },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<AzkarList> { key ->
                    AzkarScreen(
                        categoryIndex = key.categoryIndex,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }



                entry<RadioChannels> {
                    RadioChannelListScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<Initializing> {
                    LoadingContent(modifier = Modifier.fillMaxSize())
                }

                entry<Onboarding> {
                    OnboardingScreen(
                        onFinished = {
                            backStack.clear()
                            backStack.add(Home)
                        }
                    )
                }

            }
        )

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = selectedTopLevel != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            if (selectedTopLevel != null) {
                MawaqitNavigationBar(
                    selectedIndex = selectedTopLevel,
                    onDestinationSelected = { navigateToTopLevel(topLevelDestinations[it].screen) }
                )
            }
        }

    }


}

@Composable
private fun MawaqitNavigationBar(
    selectedIndex: Int,
    onDestinationSelected: (Int) -> Unit
) {
    NavigationBar {
        topLevelDestinations.forEachIndexed { index, destination ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onDestinationSelected(index) },
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(destination.icon),
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}

