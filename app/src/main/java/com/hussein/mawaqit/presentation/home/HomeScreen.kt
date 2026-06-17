package com.hussein.mawaqit.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.core.utils.HijriDateCalculator.toArabicDigits
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.db.models.AyahOfTheDay
import com.hussein.mawaqit.presentation.shared.ErrorContent
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.presentation.shared.ScreenWrapper
import com.hussein.mawaqit.presentation.util.formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

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

                else -> PrayerContent(
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
private fun PrayerContent(
    modifier: Modifier = Modifier,
    state: HomeUiState,
    countdownFlow: StateFlow<CountdownTime?>,
    onNavigateToAzkar: () -> Unit = {},
    onNavigateToRadio: () -> Unit = {},
    onNavigateToReader: (surahIndex: Int, ayahIndex: Int) -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeaderSection(state, countdownFlow = countdownFlow)
        TodaysPrayersSection(prayers = state.prayers)
        HomeQuickActionsSection(
            onNavigateToAzkar = onNavigateToAzkar,
            onNavigateToRadio = onNavigateToRadio,
        )

        AyahOfTheDayCard(
            ayah = state.ayahOfTheDay,
            onClick = onNavigateToReader,
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun HeaderSection(state: HomeUiState, countdownFlow: StateFlow<CountdownTime?>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.primary.copy(0.6f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (state.hijriDate.isNotBlank()) {
                Text(
                    text = state.hijriDate,
                    style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Rtl),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(4.dp))
            }
            state.nextPrayer?.let { next ->
                val label =
                    if (next.status == PrayerStatus.CURRENT) "Current Prayer" else "Next Prayer Is"

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = next.name,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (next.status == PrayerStatus.UPCOMING) {
                        // Pass the FLOW instead of the VALUE
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
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
    ) {
        Text(
            text = "in $time",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

}

@OptIn(ExperimentalTime::class)
@Composable
private fun TodaysPrayersSection(
    prayers: List<PrayerUiModel>,
    modifier: Modifier = Modifier
) {
    if (prayers.isEmpty()) return

    val completedCount = prayers.count { it.status == PrayerStatus.PASSED }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's prayers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$completedCount of ${prayers.size} completed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val activePrayer = prayers.firstOrNull { it.status == PrayerStatus.CURRENT }
                    ?: prayers.firstOrNull { it.status == PrayerStatus.UPCOMING }
                activePrayer?.let { prayer ->
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = prayer.time.formatTime(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
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

@OptIn(ExperimentalTime::class)
@Composable
private fun PrayerScheduleRow(
    prayer: PrayerUiModel,
    isFirst: Boolean,
    isLast: Boolean
) {
    val isCurrent = prayer.status == PrayerStatus.CURRENT
    val isPassed = prayer.status == PrayerStatus.PASSED
    val contentAlpha = if (isPassed) 0.48f else 1f
    val indicatorColor = when (prayer.status) {
        PrayerStatus.CURRENT -> MaterialTheme.colorScheme.primary
        PrayerStatus.PASSED -> MaterialTheme.colorScheme.outline
        PrayerStatus.UPCOMING -> MaterialTheme.colorScheme.secondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrayerStepIndicator(
            color = indicatorColor,
            isCurrent = isCurrent,
            isFirst = isFirst,
            isLast = isLast
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prayer.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCurrent) {
                    Text(
                        text = "Current prayer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = prayer.time.formatTime(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrayerStepIndicator(
    color: Color,
    isCurrent: Boolean,
    isFirst: Boolean,
    isLast: Boolean
) {
    val connectorColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier.size(width = 28.dp, height = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isFirst) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 2.dp, height = 28.dp)
                    .background(connectorColor, RoundedCornerShape(50.dp))
            )
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 2.dp, height = 28.dp)
                    .background(connectorColor, RoundedCornerShape(50.dp))
            )
        }

        Surface(
            modifier = Modifier.size(if (isCurrent) 20.dp else 14.dp),
            shape = CircleShape,
            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(2.dp, color)
        ) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .background(color, CircleShape)
                )
            }
        }
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
            .height(64.dp)
            .padding(horizontal = 16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (cityName.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            ) {

                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    text = "📍 $cityName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
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
        icon: Int
    ) {
        Surface(
            modifier = Modifier
                .height(88.dp)
                .then(modifier),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(icon),
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionTile(
            modifier = Modifier.weight(1f),
            onClick = onNavigateToAzkar,
            title = stringResource(R.string.azakr),
            subtitle = "Daily dhikr",
            icon = R.drawable.ic_placeholder
        )
        ActionTile(
            modifier = Modifier.weight(1f),
            onClick = onNavigateToRadio,
            title = "Radio",
            subtitle = "Quran audio",
            icon = R.drawable.ic_radio
        )
    }
}

@Composable
fun AyahOfTheDayCard(
    ayah: AyahOfTheDay?,
    modifier: Modifier = Modifier,
    onClick: (surahIndex: Int, ayahIndex: Int) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = {
            ayah?.let {
                onClick(it.surahIndex, it.numberInSurah)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Label
            Text(
                text = "آية اليوم",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(12.dp))

            when {
                ayah == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                else -> {
                    // Ayah text
                    Text(
                        text = ayah.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Rtl,
                            lineHeight = 32.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    // Surah name + ayah number
                    Text(
                        text = "${ayah.surahNameArabic} • آية ${ayah.numberInSurah.toArabicDigits()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

}

