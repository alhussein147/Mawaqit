package com.hussein.mawaqit.presentation.quran.list_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.quran.QuranData
import com.hussein.mawaqit.data.quran.Surah
import com.hussein.mawaqit.presentation.quran.components.SurahReciterPickerSheet
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    onSurahSelected: (surahIndex: Int, scrollToAyah: Int?) -> Unit,
    onNavigateToSearch: () -> Unit,
    onBack: () -> Unit,
    surahListViewModel: SurahListViewModel = koinViewModel()
) {

    val surahStates by surahListViewModel.surahStates.collectAsStateWithLifecycle()
    val selectedReciter by surahListViewModel.selectedReciter.collectAsStateWithLifecycle()
    val playingSurah by surahListViewModel.playingSurah.collectAsStateWithLifecycle()
    val bookmarks by surahListViewModel.bookmarks.collectAsStateWithLifecycle()
    var showReciterPicker by remember { mutableStateOf(false) }
    var surahToDownload by remember { mutableStateOf<Int?>(null) }

    var showBookmarksDialog by remember { mutableStateOf(false) }

    if (showReciterPicker) {
        SurahReciterPickerSheet(
            current = selectedReciter,
            onSelect = { reciter ->
                surahListViewModel.selectedReciter(reciter)
                surahToDownload?.let { surahNumber ->
                    surahListViewModel.downloadSurah(surahNumber)
                    surahToDownload = null
                }
                showReciterPicker = false
            },
            onDismiss = {
                showReciterPicker = false
                surahToDownload = null
            })
    }

    if (showBookmarksDialog) {
        BookmarksDialog(
            bookmarks = bookmarks,
            onNavigate = { surahNumber, ayahNumber -> onSurahSelected(surahNumber, ayahNumber) },
            onDelete = { surahNumber, ayahNumber ->
                surahListViewModel.deleteBookmark(surahNumber, ayahNumber)
            },
            onDismiss = { showBookmarksDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Quran") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                        contentDescription = null
                    )
                }
            }, actions = {

                IconButton(onClick = { showBookmarksDialog = true }) {
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
        }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            itemsIndexed(QuranData.surahs, key = { _, surah -> surah.number }) { _, surah ->
                val itemState = surahStates[surah.number] ?: SurahItemState.NotDownloaded
                SurahRow(
                    surah = surah,
                    onClick = { onSurahSelected(surah.number, null) },
                    onPlayPause = {
                        if (playingSurah == surah.number) {
                            surahListViewModel.togglePlayPause()
                        } else {
                            surahListViewModel.playSurah(surah.number)
                        }
                    },
                    itemState = itemState,
                    onDownload = {
                        surahToDownload = surah.number
                        showReciterPicker = true
                    },
                    onCancel = { workId -> surahListViewModel.cancelDownload(workId) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun SurahRow(
    surah: Surah,
    itemState: SurahItemState,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onCancel: (UUID) -> Unit,
    onPlayPause: () -> Unit
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Surah number badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = surah.number.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Names
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameTransliterated,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Pages ${surah.startPage} – ${QuranData.endPageOf(surah)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Arabic name + bookmark indicator
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = surah.nameArabic,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(8.dp))
            when (itemState) {
                SurahItemState.NotDownloaded -> {
                    IconButton(onClick = onDownload) {
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
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        IconButton(
                            onClick = { onCancel(itemState.workId) },
                            modifier = Modifier.size(32.dp)
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
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_play),
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                SurahItemState.Playing -> {
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_pause),
                            contentDescription = "Pause",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                SurahItemState.Paused -> {
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_resume),
                            contentDescription = "Resume",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}