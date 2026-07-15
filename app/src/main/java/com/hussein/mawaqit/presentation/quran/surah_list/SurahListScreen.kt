package com.hussein.mawaqit.presentation.quran.surah_list

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.R
import com.hussein.mawaqit.domain.models.Surah
import com.hussein.mawaqit.presentation.quran.components.SurahReciterPickerSheet
import com.hussein.mawaqit.presentation.quran.surah_list.components.QuranEmptyState
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.presentation.shared.RootScreenWrapper
import com.hussein.mawaqit.presentation.util.GlobalPlayerViewModel
import com.hussein.mawaqit.presentation.util.SurahItemState
import com.hussein.mawaqit.ui.theme.MawaqitTheme
import com.hussein.mawaqit.ui.theme.quranFontFamily
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SurahListScreen(
    modifier: Modifier = Modifier,
    onSurahSelected: (surahIndex: Int, scrollToAyah: Int?) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    surahListViewModel: SurahListViewModel = koinViewModel(),
    globalPlayerViewModel: GlobalPlayerViewModel = koinInject()
) {

    val uiState by surahListViewModel.uiState.collectAsStateWithLifecycle()
    val surahStates by globalPlayerViewModel.surahStates.collectAsStateWithLifecycle()
    val selectedReciter by globalPlayerViewModel.selectedReciter.collectAsStateWithLifecycle()
    val playbackState by globalPlayerViewModel.playbackState.collectAsStateWithLifecycle()

    var showReciterPicker by remember { mutableStateOf(false) }
    var surahToDownload by remember { mutableStateOf<Int?>(null) }

    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (showReciterPicker) {
        SurahReciterPickerSheet(
            current = selectedReciter,
            available = playbackState.availableReciters,
            onSelect = { reciter ->
                globalPlayerViewModel.selectReciter(reciter)
                surahToDownload?.let { surahNumber ->
                    globalPlayerViewModel.downloadSurah(surahNumber)
                    surahToDownload = null
                }
                showReciterPicker = false
            },
            onDismiss = {
                showReciterPicker = false
                surahToDownload = null
            })
    }


    RootScreenWrapper(
        modifier = Modifier.navigationBarsPadding(),
        topAppBar = {
            if (!uiState.isLoading && uiState.surahs.isNotEmpty()) {
                LargeTopAppBar(
                    scrollBehavior = topAppBarScrollBehavior,
                    title = {
                        Text(
                            stringResource(R.string.alQuran),
                            fontWeight = FontWeight.Black
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToBookmarks) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_bookmark),
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                                contentDescription = null
                            )
                        }
                    })
            }
        },
        content = {
            when {
                uiState.isLoading -> {
                    LoadingContent(Modifier.fillMaxSize())
                }

                uiState.surahs.isEmpty() -> {
                    QuranEmptyState(
                        syncStatus = uiState.syncStatus,
                        availableTafsirSources = uiState.availableTafsirSources,
                        selectedTafsirSource = uiState.selectedTafsirSource,
                        onTafsirSourceSelected = { surahListViewModel.selectTafsirSource(it) },
                        onSyncClick = { surahListViewModel.syncQuranData() },
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                            .then(modifier),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 24.dp
                        )
                    ) {
                        uiState.lastRead?.let { last ->
                            item(key = "continue_reading") {
                                ContinueReadingCard(
                                    lastRead = last,
                                    onClick = {
                                        onSurahSelected(
                                            last.surahNumber,
                                            last.ayahNumber
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                            }
                        }

                        items(
                            items = uiState.surahs,
                            key = { it.number }
                        ) { surah ->
                            val itemState =
                                surahStates[surah.number] ?: SurahItemState.NotDownloaded
                            SurahRow(
                                modifier = Modifier.fillMaxWidth(),
                                surah = surah,
                                itemState = itemState,
                                onPlayPause = {
                                    when (itemState) {
                                        SurahItemState.Playing,
                                        SurahItemState.Paused -> globalPlayerViewModel.togglePlayPause()

                                        SurahItemState.Downloaded -> globalPlayerViewModel.playSurah(
                                            surah.number
                                        )

                                        else -> Unit
                                    }
                                },
                                onDownload = {
                                    surahToDownload = surah.number
                                    showReciterPicker = true
                                },
                                onCancel = { workId ->
                                    globalPlayerViewModel.cancelDownload(
                                        workId
                                    )
                                },
                                onClick = { onSurahSelected(surah.number, null) },
                            )
                        }
                    }
                }
            }
        }
    )
}


@Composable
private fun SurahRow(
    modifier: Modifier = Modifier,
    surah: Surah,
    itemState: SurahItemState,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onCancel: (UUID) -> Unit,
    onPlayPause: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MawaqitTheme.appShapes.medium,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Surah number badge
            Surface(
                shape = MawaqitTheme.appShapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = surah.number.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Names
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = surah.nameArabic,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLargeEmphasized.copy(
                        textAlign = TextAlign.End,
                        fontFamily = quranFontFamily
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = surah.nameTransliterated,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            ActionIcon(
                itemState = itemState,
                onDownload = onDownload,
                onCancel = onCancel,
                onPlayPause = onPlayPause
            )
        }
    }
}

@Composable
private fun ActionIcon(
    itemState: SurahItemState,
    onDownload: () -> Unit,
    onCancel: (UUID) -> Unit,
    onPlayPause: () -> Unit,
) {
    when (itemState) {
        SurahItemState.NotDownloaded -> {
            IconButton(
                onClick = onDownload,
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                )
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_download),
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        is SurahItemState.Downloading -> {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { itemState.progress },
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                IconButton(
                    onClick = { onCancel(itemState.workId) },
                    modifier = Modifier.size(IconButtonDefaults.smallContainerSize())
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                        contentDescription = "Cancel",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        SurahItemState.Downloaded -> {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                )
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_play),
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        SurahItemState.Playing -> {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_pause),
                    contentDescription = "Pause",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        SurahItemState.Paused -> {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    CircleShape
                )
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_resume),
                    contentDescription = "Resume",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(
    lastRead: LastRead,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MawaqitTheme.appShapes.medium,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.continue_reading),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${lastRead.surahName} • Ayah ${lastRead.ayahNumber}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = quranFontFamily
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize())
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                    )
                }
            }
        }
    }
}


