package com.hussein.mawaqit.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.hussein.mawaqit.R
import com.hussein.mawaqit.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.presentation.azkar.AzkarScreen
import com.hussein.mawaqit.presentation.azkar.AzkarCategoriesScreen
import com.hussein.mawaqit.presentation.home.HomeScreen
import com.hussein.mawaqit.presentation.navigation.components.BottomBarNestedScrollConnection
import com.hussein.mawaqit.presentation.navigation.components.FloatingNavBar
import com.hussein.mawaqit.presentation.navigation.components.rememberBottomBarState
import com.hussein.mawaqit.presentation.onboarding.OnboardingScreen
import com.hussein.mawaqit.presentation.quran.bookmarks.BookmarksScreen
import com.hussein.mawaqit.presentation.quran.reading.reading_settings.QuranReadingSettingsScreen
import com.hussein.mawaqit.presentation.quran.reading.QuranReadingScreen
import com.hussein.mawaqit.presentation.quran.search.QuranSearchScreen
import com.hussein.mawaqit.presentation.quran.surah_list.SurahListScreen
import com.hussein.mawaqit.presentation.quran.tafsir.QuranReaderWithTafsirScreen
import com.hussein.mawaqit.presentation.radio.RadioChannelListScreen
import com.hussein.mawaqit.presentation.settings.NotificationSettingsScreen
import com.hussein.mawaqit.presentation.settings.SettingsScreen
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.ui.enterTransition
import com.hussein.mawaqit.ui.exitTransition
import com.hussein.mawaqit.ui.popEnterTransition
import com.hussein.mawaqit.ui.popExitTransition
import org.koin.core.annotation.KoinExperimentalAPI

data class TopLevelDestination(
    val screen: Screen,
    val label: String,
    val icon: Int
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Home, "Home", R.drawable.ic_home),
    TopLevelDestination(QuranSurahList, "Quran", R.drawable.ic_quran2),
    TopLevelDestination(Settings, "Settings", R.drawable.ic_settings)
)

private fun NavKey.navBarIndex(): Int = when (this) {
    Home -> 0
    QuranSurahList -> 1
    Settings -> 2
    else -> -1
}


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
            mainRootEnterTransition(
                fromRoute = initialState.key as? NavKey,
                toRoute = targetState.key as? NavKey,
                fallback = enterTransition(),
                isPop = false
            ) togetherWith
                    mainRootExitTransition(
                        fromRoute = initialState.key as? NavKey,
                        toRoute = targetState.key as? NavKey,
                        fallback = exitTransition(),
                        isPop = false
                    )
        },
        popTransitionSpec = {
            mainRootEnterTransition(
                fromRoute = initialState.key as? NavKey,
                toRoute = targetState.key as? NavKey,
                fallback = popEnterTransition(),
                isPop = true
            ) togetherWith
                    mainRootExitTransition(
                        fromRoute = initialState.key as? NavKey,
                        toRoute = targetState.key as? NavKey,
                        fallback = popExitTransition(),
                        isPop = true
                    )
        },
        predictivePopTransitionSpec = {
            mainRootEnterTransition(
                fromRoute = initialState.key as? NavKey,
                toRoute = targetState.key as? NavKey,
                fallback = popEnterTransition(),
                isPop = true
            ) togetherWith
                    mainRootExitTransition(
                        fromRoute = initialState.key as? NavKey,
                        toRoute = targetState.key as? NavKey,
                        fallback = popExitTransition(),
                        isPop = true
                    )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
        entryProvider = entryProvider {

            entry<Main> {
                TopLevelNavigation(rootBackStack = backStack)
            }

            entry<NotificationSettings> {
                NotificationSettingsScreen(
                    onBack = { backStack.removeAt(backStack.size - 1) }
                )
            }

            entry<QuranReading> { key ->
                QuranReadingScreen(
                    scrollToAyah = key.scrollToAyah,
                    surahIndex = key.surahIndex,
                    onBack = { backStack.removeAt(backStack.size - 1) },
                    onNavigateToSettings = { backStack.add(QuranReaderSettings) },
                    onNavigateToTafsir = { surahName ->
                        backStack.add(QuranReaderWithTafsir(key.surahIndex, surahName))
                    }
                )
            }

            entry<QuranReaderWithTafsir> { key ->
                QuranReaderWithTafsirScreen(
                    surahNumber = key.surahIndex,
                    surahName = key.surahName,
                    onBack = { backStack.removeAt(backStack.size - 1) }
                )
            }

            entry<QuranReaderSettings> {
                QuranReadingSettingsScreen(
                    onBack = { backStack.removeAt(backStack.size - 1) }
                )
            }

            entry<QuranSearch> {
                QuranSearchScreen(
                    onSurahSelected = { surahIndex ->
                        backStack.add(QuranReading(surahIndex))
                    },
                    onAyahSelected = { surahIndex, ayahNumber ->
                        backStack.add(QuranReading(surahIndex, ayahNumber))
                    },
                    onBack = { backStack.removeAt(backStack.size - 1) },
                )
            }

            entry<QuranBookmarks> {
                BookmarksScreen(
                    onNavigateToAyah = { surahIndex, ayahNumber ->
                        backStack.add(QuranReading(surahIndex, ayahNumber))
                    },
                    onBack = { backStack.removeAt(backStack.size - 1) }
                )
            }

            entry<AzkarCategories> {
                AzkarCategoriesScreen(
                    onCategorySelected = { id -> backStack.add(AzkarList(id)) },
                    onBack = { backStack.removeAt(backStack.size - 1) },
                )
            }

            entry<AzkarList> { key ->
                AzkarScreen(
                    categoryId = key.categoryId,
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
private fun TopLevelNavigation(
    rootBackStack: NavBackStack<NavKey>
) {
    val mainBackStack: NavBackStack<NavKey> = rememberNavBackStack(Home)

    val navDirection = remember { mutableIntStateOf(1) }

    val bottomBarVisible = rememberBottomBarState()

    val scrollConnection = remember {
        BottomBarNestedScrollConnection(
            onHide = { bottomBarVisible.value = false },
            onShow = { bottomBarVisible.value = true }
        )
    }

    val currentScreen = mainBackStack.lastOrNull()
    val selectedTopLevel = currentScreen?.navBarIndex()?.takeIf { it >= 0 } ?: 0

    fun navigateToTopLevel(destination: Screen) {
        val current = mainBackStack.lastOrNull()
        if (current == destination) return

        val currentIndex = current?.navBarIndex()?.takeIf { it >= 0 } ?: 0
        val destinationIndex = destination.navBarIndex()

        navDirection.intValue = destinationIndex.compareTo(currentIndex).takeIf { it != 0 } ?: 1

        if (mainBackStack.isNotEmpty()) {
            mainBackStack[0] = destination
        } else {
            mainBackStack.add(destination)
        }
    }

    BackHandler(enabled = currentScreen != Home && currentScreen != null) {
        navigateToTopLevel(Home)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val direction = if (navDirection.intValue >= 0) MainRootDirection.FORWARD else MainRootDirection.BACKWARD

        NavDisplay(
            transitionSpec = {
                mainRootEnterTransition(direction, enterTransition()) togetherWith
                        mainRootExitTransition(direction, exitTransition())
            },
            popTransitionSpec = {
                mainRootEnterTransition(direction, popEnterTransition()) togetherWith
                        mainRootExitTransition(direction, popExitTransition())
            },
            predictivePopTransitionSpec = {
                mainRootEnterTransition(direction, popEnterTransition()) togetherWith
                        mainRootExitTransition(direction, popExitTransition())
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
                        modifier = Modifier.nestedScroll(scrollConnection),
                        onNavigateToAzkar = { rootBackStack.add(AzkarCategories) },
                        onNavigateToRadio = { rootBackStack.add(RadioChannels) },
                        onNavigateToReader = { index, ayahIndex ->
                            rootBackStack.add(QuranReading(index, ayahIndex))
                        },
                        onNavigateToSettings = { navigateToTopLevel(Settings) }
                    )
                }

                entry<Settings> {
                    SettingsScreen(
                        modifier = Modifier.nestedScroll(scrollConnection),
                        onNavigateToNotificationSettings = { rootBackStack.add(NotificationSettings) }
                    )
                }

                entry<QuranSurahList> {
                    SurahListScreen(
                        modifier = Modifier.nestedScroll(scrollConnection),
                        onSurahSelected = { index, scrollToAyah ->
                            rootBackStack.add(QuranReading(index, scrollToAyah))
                        },
                        onNavigateToSearch = {
                            rootBackStack.add(QuranSearch)
                        },
                        onNavigateToBookmarks = {
                            rootBackStack.add(QuranBookmarks)
                        }
                    )
                }
            }
        )

        FloatingNavBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            items = topLevelDestinations,
            selectedIndex = selectedTopLevel,
            onSelect = { navigateToTopLevel(topLevelDestinations[it].screen) },
            show = bottomBarVisible.value
        )
    }
}

private enum class MainRootDirection {
    FORWARD,
    BACKWARD
}

// Base duration for bottom-nav switches at 1x — at 0.5x system scale = ~190 ms.
private const val BOTTOM_NAV_TRANSITION_DURATION = 380

// MD3 Expressive easing for bottom-nav switches
private val BottomNavEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private val MAIN_ROOT_TRANSITION_SPEC =
    tween<IntOffset>(durationMillis = BOTTOM_NAV_TRANSITION_DURATION, easing = BottomNavEasing)

private val MAIN_ROOT_FADE_SPEC =
    tween<Float>(durationMillis = BOTTOM_NAV_TRANSITION_DURATION / 2, easing = BottomNavEasing)

private fun mainRootDirection(
    fromRoute: NavKey?,
    toRoute: NavKey?,
    isPop: Boolean = false
): MainRootDirection? {
    if (fromRoute == toRoute) return null

    val fromIndex = fromRoute?.navBarIndex() ?: -1
    val toIndex = toRoute?.navBarIndex() ?: -1

    if (fromIndex != -1 && toIndex != -1) {
        if (fromIndex == toIndex) return null
        return if (toIndex > fromIndex) MainRootDirection.FORWARD else MainRootDirection.BACKWARD
    }

    return if (isPop) MainRootDirection.BACKWARD else MainRootDirection.FORWARD
}

private fun mainRootEnterTransition(
    direction: MainRootDirection?,
    fallback: EnterTransition
): EnterTransition = when (direction) {
    MainRootDirection.FORWARD -> {
        slideInHorizontally(
            animationSpec = MAIN_ROOT_TRANSITION_SPEC,
            initialOffsetX = { (it * 0.5f).toInt() }
        ) + fadeIn(animationSpec = MAIN_ROOT_FADE_SPEC)
    }
    MainRootDirection.BACKWARD -> {
        slideInHorizontally(
            animationSpec = MAIN_ROOT_TRANSITION_SPEC,
            initialOffsetX = { -(it * 0.5f).toInt() }
        ) + fadeIn(animationSpec = MAIN_ROOT_FADE_SPEC)
    }
    null -> fallback
}

private fun mainRootEnterTransition(
    fromRoute: NavKey?,
    toRoute: NavKey?,
    fallback: EnterTransition,
    isPop: Boolean = false
): EnterTransition = mainRootEnterTransition(mainRootDirection(fromRoute, toRoute, isPop), fallback)

private fun mainRootExitTransition(
    direction: MainRootDirection?,
    fallback: ExitTransition
): ExitTransition = when (direction) {
    MainRootDirection.FORWARD -> {
        slideOutHorizontally(
            animationSpec = MAIN_ROOT_TRANSITION_SPEC,
            targetOffsetX = { -(it * 0.5f).toInt() }
        ) + fadeOut(animationSpec = MAIN_ROOT_FADE_SPEC)
    }
    MainRootDirection.BACKWARD -> {
        slideOutHorizontally(
            animationSpec = MAIN_ROOT_TRANSITION_SPEC,
            targetOffsetX = { (it * 0.5f).toInt() }
        ) + fadeOut(animationSpec = MAIN_ROOT_FADE_SPEC)
    }
    null -> fallback
}

private fun mainRootExitTransition(
    fromRoute: NavKey?,
    toRoute: NavKey?,
    fallback: ExitTransition,
    isPop: Boolean = false
): ExitTransition = mainRootExitTransition(mainRootDirection(fromRoute, toRoute, isPop), fallback)
