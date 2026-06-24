package com.hussein.mawaqit.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.hussein.mawaqit.R
import com.hussein.mawaqit.infrastructure.settings.SettingsRepository
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
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.core.annotation.KoinExperimentalAPI

data class TopLevelDestination(
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

    val showNavBar = remember { mutableStateOf(true) }

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

    LaunchedEffect(currentScreen) {
        if (currentScreen == Home) {
            showNavBar.value = true
        }
    }

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
                        onNavigateToAzkar = {
                            backStack.add(
                                AzkarCategories
                            )
                        },
                        onNavigateToRadio = { backStack.add(RadioChannels) },
                        onNavigateToReader = { index, ayahIndex ->
                            backStack.add(QuranReader(index, ayahIndex))
                        }
                    )
                }

                entry<Settings> {
                    SettingsScreen()
                }

                entry<QuranSurahList> {
                    SurahListScreen(
                        onSurahSelected = { index, scrollToAyah ->
                            backStack.add(QuranReader(index, scrollToAyah))
                        },
                        onNavigateToSearch = {
                            backStack.add(QuranSearch)
                        }, toggleNavBar = {
                            showNavBar.value = it
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

        if (selectedTopLevel != null) {

            BackHandler(enabled = selectedTopLevel > 0 , onBack = {
                navigateToTopLevel(Home)
            })

            FloatingNavBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                items = topLevelDestinations,
                selectedIndex = selectedTopLevel,
                onSelect = { navigateToTopLevel(topLevelDestinations[it].screen) },
                show = showNavBar.value
            )
        }
    }


}

@Composable
fun ScrollObserver(
    onToggleNavBar: (Boolean) -> Unit,
    listState: LazyListState
) {
    val currentOnToggleNavBar = rememberUpdatedState(onToggleNavBar)

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        var isNavBarVisible = true

        snapshotFlow {
            val layoutInfo = listState.layoutInfo

            ScrollSnapshot(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0,
                layoutInfo.totalItemsCount
            )
        }.distinctUntilChanged().collect { scrollSnapshot ->
            val index = scrollSnapshot.firstVisibleItemIndex
            val offset = scrollSnapshot.firstVisibleItemScrollOffset

            val scrollingDown =
                index > previousIndex ||
                        (index == previousIndex && offset > previousOffset)

            val scrollingUp =
                index < previousIndex ||
                        (index == previousIndex && offset < previousOffset)

            val isAtEnd =
                scrollSnapshot.totalItemsCount > 0 &&
                        scrollSnapshot.lastVisibleItemIndex >= scrollSnapshot.totalItemsCount - 1

            val shouldShowNavBar = when {
                isAtEnd -> true
                scrollingDown -> false
                scrollingUp -> true
                else -> isNavBarVisible
            }

            if (shouldShowNavBar != isNavBarVisible) {
                isNavBarVisible = shouldShowNavBar
                currentOnToggleNavBar.value(shouldShowNavBar)
            }

            previousIndex = index
            previousOffset = offset
        }
    }
}

private data class ScrollSnapshot(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val lastVisibleItemIndex: Int,
    val totalItemsCount: Int
)
