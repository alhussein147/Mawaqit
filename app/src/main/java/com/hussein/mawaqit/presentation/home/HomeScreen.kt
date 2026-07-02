package com.hussein.mawaqit.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.core.utils.HijriDateCalculator.toArabicDigits
import com.hussein.mawaqit.R
import com.hussein.mawaqit.domain.models.AyahOfTheDay
import com.hussein.mawaqit.presentation.navigation.LocalBottomBarHeight
import com.hussein.mawaqit.presentation.shared.ErrorContent
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.presentation.shared.ScreenWrapper
import com.hussein.mawaqit.presentation.util.formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToAzkar: () -> Unit = {},
    onNavigateToRadio: () -> Unit = {},
    onNavigateToReader: (surahIndex: Int, ayahIndex: Int) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.tick()
            delay(60_000.milliseconds)
        }
    }

    ScreenWrapper(
        modifier = modifier,
        topAppBar = {
            HomeTopAppBar(
                cityName = state.cityName,
            )
        },
        content = {
            when {
                state.isLoading -> LoadingContent(
                    Modifier.fillMaxSize()
                )

                state.error != null -> ErrorContent(
                    modifier = Modifier.fillMaxSize(),
                    message = state.error!!
                )

                else -> HomeScreenContent(
                    state = state,
                    countdownFlow = viewModel.countdown,
                    onNavigateToAzkar = onNavigateToAzkar,
                    onNavigateToRadio = onNavigateToRadio,
                    onNavigateToReader = onNavigateToReader
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeScreenContent(
    modifier: Modifier = Modifier,
    state: HomeUiState,
    countdownFlow: StateFlow<CountdownTime?>,
    onNavigateToAzkar: () -> Unit = {},
    onNavigateToRadio: () -> Unit = {},
    onNavigateToReader: (surahIndex: Int, ayahIndex: Int) -> Unit,
) {
    val bottomBarHeight = LocalBottomBarHeight.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .then(modifier),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomBarHeight + 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderSection(state, countdownFlow = countdownFlow)
        }
        item {
            TodayPrayersSection(prayers = state.prayers)
        }
        item {
            HomeQuickActionsSection(
                onNavigateToAzkar = onNavigateToAzkar,
                onNavigateToRadio = onNavigateToRadio,
            )
        }
        item {
            state.ayahOfTheDay?.let {
                AyahOfTheDayCard(
                    ayah = it,
                    onClick = onNavigateToReader,
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(state: HomeUiState, countdownFlow: StateFlow<CountdownTime?>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (state.hijriDate.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = state.hijriDate,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            state.nextPrayer?.let { next ->
                val label =
                    if (next.status == PrayerStatus.CURRENT) "Current Prayer" else "Next Prayer"

                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = next.name,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (next.status == PrayerStatus.UPCOMING) {
                        CountdownDisplay(countdownFlow = countdownFlow)
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownDisplay(countdownFlow: StateFlow<CountdownTime?>) {
    val countdown by countdownFlow.collectAsStateWithLifecycle()
    val time = countdown ?: return

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 4.dp
    ) {
        Text(
            text = "in $time",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis
        )
    }
}

@Composable
private fun TodayPrayersSection(
    prayers: List<PrayerUiModel>,
    modifier: Modifier = Modifier
) {
    if (prayers.isEmpty()) return
    val completedCount = prayers.count { it.status == PrayerStatus.PASSED }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$completedCount of ${prayers.size} prayers completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                val activePrayer = prayers.firstOrNull { it.status == PrayerStatus.CURRENT }
                    ?: prayers.firstOrNull { it.status == PrayerStatus.UPCOMING }
                activePrayer?.let { prayer ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = prayer.time.formatTime(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }


            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Column() {
                prayers.forEachIndexed { index, prayer ->
                    PrayerScheduleRow(
                        prayer = prayer,
                        isFirst = index == 0,
                        isLast = index == prayers.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerScheduleRow(
    prayer: PrayerUiModel,
    isFirst: Boolean,
    isLast: Boolean
) {
    val isCurrent = prayer.status == PrayerStatus.CURRENT
    val isPassed = prayer.status == PrayerStatus.PASSED
    val contentAlpha = if (isPassed) 0.5f else 1f

    val backgroundColor = when (prayer.status) {
        PrayerStatus.CURRENT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrayerStepIndicator(
            status = prayer.status,
            isFirst = isFirst,
            isLast = isLast
        )

        Spacer(Modifier.width(16.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = prayer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isCurrent) {
                    Text(
                        text = "Current prayer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Text(
                text = prayer.time.formatTime(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrayerStepIndicator(
    status: PrayerStatus,
    isFirst: Boolean,
    isLast: Boolean
) {
    val indicatorColor = when (status) {
        PrayerStatus.CURRENT -> MaterialTheme.colorScheme.primary
        PrayerStatus.PASSED -> MaterialTheme.colorScheme.outline
        PrayerStatus.UPCOMING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    }

    val connectorColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier.size(width = 24.dp, height = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isFirst) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 2.dp, height = 32.dp)
                    .background(connectorColor)
            )
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 2.dp, height = 32.dp)
                    .background(connectorColor)
            )
        }

        Surface(
            modifier = Modifier.size(if (status == PrayerStatus.CURRENT) 20.dp else 12.dp),
            shape = CircleShape,
            color = if (status == PrayerStatus.CURRENT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            border = BorderStroke(3.dp, indicatorColor)
        ) {}
    }
}

@Composable
private fun HomeTopAppBar(
    modifier: Modifier = Modifier,
    cityName: String
) {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (cityName.isNotBlank()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                onClick = {}
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_location),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = cityName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun HomeQuickActionsSection(
    onNavigateToAzkar: () -> Unit,
    onNavigateToRadio: () -> Unit,
) {
    @Composable
    fun ActionTile(
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
        title: String,
        subtitle: String,
        icon: Int,
        containerColor: Color
    ) {
        Surface(
            modifier = Modifier
                .height(100.dp)
                .then(modifier),
            shape = RoundedCornerShape(28.dp),
            color = containerColor,
            onClick = onClick
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(icon),
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionTile(
            modifier = Modifier.weight(1f),
            onClick = onNavigateToAzkar,
            title = stringResource(R.string.azakr),
            subtitle = "Daily dhikr",
            icon = R.drawable.ic_placeholder,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        )
        ActionTile(
            modifier = Modifier.weight(1f),
            onClick = onNavigateToRadio,
            title = "Radio",
            subtitle = "Quran audio",
            icon = R.drawable.ic_radio,
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun AyahOfTheDayCard(
    ayah: AyahOfTheDay?,
    modifier: Modifier = Modifier,
    onClick: (surahIndex: Int, ayahIndex: Int) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = {
            ayah?.let {
                onClick(it.surahIndex, it.numberInSurah)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_bookmark),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = "آية اليوم",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            if (ayah == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 4.dp)
                }
            } else {
                Text(

                    overflow = TextOverflow.Ellipsis,
                    text = ayah.text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        textDirection = TextDirection.Rtl,
                        lineHeight = 44.sp,
                        textAlign = TextAlign.Center
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6
                )

                Spacer(Modifier.height(20.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "${ayah.surahNameArabic} • آية ${ayah.numberInSurah.toArabicDigits()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
