package com.hussein.mawaqit.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.R
import com.hussein.mawaqit.presentation.util.formatTime
import com.hussein.mawaqit.presentation.home.components.PrayerArchStepper
import com.hussein.mawaqit.presentation.shared.ErrorContent
import com.hussein.mawaqit.presentation.shared.LoadingContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAzkar: () -> Unit = {},
    onNavigateToQuran: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    // Collect the main state. This doesn't change every second.
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.tick()
            delay(60_000)
        }
    }

    Scaffold(topBar = {
        HomeTopAppBar(
            cityName = state.cityName, onNavigateToSettings = onNavigateToSettings
        )
    }) { innerPadding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(innerPadding))
            state.error != null -> ErrorContent(state.error!!, Modifier.padding(innerPadding))
            else -> PrayerContent(
                state = state,
                countdownFlow = viewModel.countdown,
                modifier = Modifier.padding(innerPadding),
                onNavigateToAzkar = onNavigateToAzkar,
                onNavigateToQuran = onNavigateToQuran
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrayerContent(
    modifier: Modifier = Modifier,
    state: HomeUiState,
    countdownFlow: StateFlow<CountdownTime?>,
    onNavigateToAzkar: () -> Unit = {},
    onNavigateToQuran: () -> Unit = {}
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
        QuranAndAzkarSection(
            onNavigateToQuran = onNavigateToQuran,
            onNavigateToAzkar = onNavigateToAzkar
        )
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = "Today's Prayers",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(16.dp)
            )
        }

        state.prayers.forEach { prayer ->
            HomePrayerListItem(prayer)
        }

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

            PrayerArchStepper(
                prayers = state.prayers, modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
            )
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

@Composable
private fun HomeTopAppBar(
    modifier: Modifier = Modifier, cityName: String, onNavigateToSettings: () -> Unit
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
        IconButton(onClick = onNavigateToSettings) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
                contentDescription = "com.hussein.islamic.presentation.Settings"
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun HomePrayerListItem(prayer: PrayerUiModel) {
    val isCurrent = prayer.status == PrayerStatus.CURRENT
    val isPassed = prayer.status == PrayerStatus.PASSED


    val dotColor by animateColorAsState(
        targetValue = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(500),
        label = "dot"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isPassed) 0.4f else 1f)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier
                    .size(height = 24.dp, width = 6.dp)
                    .background(dotColor, CircleShape)
            )

            Text(
                prayer.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                prayer.time.formatTime(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}


@Composable
fun QuranAndAzkarSection(onNavigateToQuran: () -> Unit, onNavigateToAzkar: () -> Unit) {

    @Composable
    fun Section(modifier: Modifier = Modifier, onClick: () -> Unit, title: String) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.height(65.dp).then(modifier),
            shape = RoundedCornerShape(50),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(title)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Section(
            modifier = Modifier
                .weight(1f),
            onClick = onNavigateToQuran,
            title = stringResource(R.string.quran)
        )
        Section(
            modifier = Modifier
                .weight(1f),
            onClick = onNavigateToAzkar,
            title = stringResource(R.string.azakr)
        )
    }

}

@Composable
private fun QuranSection(onNavigateToQuran: () -> Unit) {
    Card(
        onClick = onNavigateToQuran,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quran",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "١١٤ سورة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}