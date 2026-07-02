package com.hussein.mawaqit.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
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
import com.hussein.mawaqit.presentation.quran.bookmarks.BookmarksScreen
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

private fun NavKey.navBarIndex(): Int = when (this) {
    Home -> 0
    QuranSurahList -> 1
    Settings -> 2
    else -> -1
}

val LocalBottomBarHeight = staticCompositionLocalOf<Dp> { 0.dp }

@OptIn(KoinExperimentalAPI::class)
@Composable
fun AppNavigation(settingsRepository: SettingsRepository) {

    val backStack: NavBackStack<NavKey> = rememberNavBackStack(Initializing)

    LaunchedEffect(Unit) {
        if (settingsRepository.isOnboardingDone()) {
            if (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1)
            backStack.add(Main)
        } else {
            if (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1)
            backStack.add(Onboarding)
        }
    }

    NavDisplay(
        transitionSpec = {
            enterTransition() togetherWith exitTransition()
        },
        popTransitionSpec = {
            popEnterTransition() togetherWith popExitTransition()
        },
        predictivePopTransitionSpec = {
            enterTransition() togetherWith exitTransition()
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
        entryProvider = entryProvider {

            entry<Main> {
                MainNavigation(rootBackStack = backStack)
            }

            entry<QuranReader> { key ->
                QuranReaderScreen(
                    scrollToAyah = key.scrollToAyah,
                    surahIndex = key.surahIndex,
                    onBack = { backStack.removeAt(backStack.size - 1) },
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
                    onBack = { backStack.removeAt(backStack.size - 1) },
                )
            }

            entry<QuranBookmarks> {
                BookmarksScreen(
                    onNavigateToAyah = { surahIndex, ayahNumber ->
                        backStack.add(QuranReader(surahIndex, ayahNumber))
                    },
                    onBack = { backStack.removeAt(backStack.size - 1) }
                )
            }

            entry<AzkarCategories> {
                AzkarCategoryScreen(
                    onCategorySelected = { index -> backStack.add(AzkarList(index)) },
                    onBack = { backStack.removeAt(backStack.size - 1) },
                )
            }

            entry<AzkarList> { key ->
                AzkarScreen(
                    categoryIndex = key.categoryIndex,
                    onBack = { backStack.removeAt(backStack.size - 1) }
                )
            }

            entry<RadioChannels> {
                RadioChannelListScreen(
                    onBack = { backStack.removeAt(backStack.size - 1) }
                )
            }

            entry<Initializing> {
                LoadingContent(modifier = Modifier.fillMaxSize())
            }

            entry<Onboarding> {
                OnboardingScreen(
                    onFinished = {
                        backStack.clear()
                        backStack.add(Main)
                    }
                )
            }
        }
    )
}

@OptIn(KoinExperimentalAPI::class)
@Composable
private fun MainNavigation(rootBackStack: NavBackStack<NavKey>) {
    val mainBackStack: NavBackStack<NavKey> = rememberNavBackStack(Home)
    val navDirection = remember { mutableIntStateOf(1) }
    var bottomPadding by remember { mutableStateOf(0.dp) }
    val showNavBar = remember { mutableStateOf(true) }

    val currentScreen = mainBackStack.lastOrNull()
    val selectedTopLevel = currentScreen?.navBarIndex()?.takeIf { it >= 0 } ?: 0

    fun navigateToTopLevel(destination: Screen) {
        val current = mainBackStack.lastOrNull()
        if (current == destination) return

        val currentIndex = current?.navBarIndex()?.takeIf { it >= 0 } ?: 0
        val destinationIndex = destination.navBarIndex()
        navDirection.intValue = destinationIndex.compareTo(currentIndex).takeIf { it != 0 } ?: 1

        mainBackStack.clear()
        mainBackStack.add(destination)
    }

    BackHandler(enabled = currentScreen != Home && currentScreen != null) {
        navigateToTopLevel(Home)
    }

    val density = LocalDensity.current

    CompositionLocalProvider(LocalBottomBarHeight provides bottomPadding) {

        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                transitionSpec = {
                    enterTransition(navDirection.intValue) togetherWith exitTransition(navDirection.intValue)
                },
                popTransitionSpec = {
                    popEnterTransition() togetherWith popExitTransition()
                },
                predictivePopTransitionSpec = {
                    enterTransition() togetherWith exitTransition()
                },
                modifier = Modifier.fillMaxSize(),
                backStack = mainBackStack,
                onBack = {
                    if (mainBackStack.lastOrNull() != Home) {
                        navigateToTopLevel(Home)
                    }
                },
                entryProvider = entryProvider {
                    entry<Home> {
                        HomeScreen(
                            onNavigateToAzkar = { rootBackStack.add(AzkarCategories) },
                            onNavigateToRadio = { rootBackStack.add(RadioChannels) },
                            onNavigateToReader = { index, ayahIndex ->
                                rootBackStack.add(QuranReader(index, ayahIndex))
                            }
                        )
                    }

                    entry<Settings> {
                        SettingsScreen()
                    }

                    entry<QuranSurahList> {
                        SurahListScreen(
                            onSurahSelected = { index, scrollToAyah ->
                                rootBackStack.add(QuranReader(index, scrollToAyah))
                            },
                            onNavigateToSearch = {
                                rootBackStack.add(QuranSearch)
                            },
                            onNavigateToBookmarks = {
                                rootBackStack.add(QuranBookmarks)
                            },
                            toggleNavBar = { showNavBar.value = it }
                        )
                    }
                }
            )

            FloatingNavBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .onSizeChanged {
                        bottomPadding = with(density) { it.height.toDp() }
                    },
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
