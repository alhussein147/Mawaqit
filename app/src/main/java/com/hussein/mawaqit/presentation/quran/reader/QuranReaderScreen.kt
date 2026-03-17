package com.hussein.mawaqit.presentation.quran.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.quran.Ayah
import com.hussein.mawaqit.data.quran.QuranData
import com.hussein.mawaqit.data.quran.Surah
import com.hussein.mawaqit.data.recitation.Reciter
import com.hussein.mawaqit.presentation.quran.recitation.AyahReciterPickerSheetContent
import com.hussein.mawaqit.presentation.quran.tafsir.TafsirState
import com.hussein.mawaqit.presentation.shared.ErrorContent
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.ui.theme.quranFontFamily


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahIndex: Int,
    onBack: () -> Unit,
    viewModel: QuranViewModel = viewModel(),
) {
    val readerState by viewModel.readerState.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val quranTextAlignment by viewModel.textAlignment.collectAsStateWithLifecycle()
    val bookmark by viewModel.bookmark.collectAsStateWithLifecycle()

    val selectedAyah by viewModel.selectedAyah.collectAsStateWithLifecycle()
    val tafsirState by viewModel.tafsirState.collectAsStateWithLifecycle()

    val recitationState by viewModel.ayahRecitationState.collectAsStateWithLifecycle()
    val playingAyah by viewModel.playingAyah.collectAsStateWithLifecycle()
    val selectedReciter by viewModel.selectedReciter.collectAsStateWithLifecycle()
    val hasNetwork by viewModel.networkAvailable.collectAsStateWithLifecycle()


    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            if (viewModel.playingAyah.value != null) {
                viewModel.stopAyah()
            }
        }
    }

    LaunchedEffect(surahIndex) { viewModel.loadSurah(surahIndex) }

    selectedAyah?.let { ayah ->
        val isBookmarked = bookmark?.surahIndex == surahIndex &&
                bookmark?.ayahNumber == ayah.number
        AyahBottomSheet(
            ayah = ayah,
            tafsirState = tafsirState,
            isBookmarked = isBookmarked,
            onTafsir = { viewModel.fetchTafsir(surahIndex, ayah) },
            onBookmark = {
                if (isBookmarked) viewModel.clearBookmark()
                else viewModel.setBookmark(surahIndex, ayah.number)
            },
            onDismiss = { viewModel.dismissTafsir() },
            isPlaying = playingAyah == ayah.number &&
                    recitationState is RecitationState.Playing,
            onPlayPause = {
                if (playingAyah == ayah.number &&
                    recitationState !is RecitationState.Idle
                ) {
                    viewModel.stopAyah()
                } else {
                    viewModel.playAyah(surahIndex, ayah.number)
                }
            },
            playEnabled = hasNetwork,
            currentReciter = selectedReciter,
            onReciterSelect = { viewModel.selectReciter(it) },
        )
    }

    BackHandler(enabled = recitationState is RecitationState.Playing) {
        viewModel.stopAyah()
        onBack.invoke()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (readerState is QuranReaderUiState.Success) {
                        val surah = QuranData.surahs.getOrNull(surahIndex - 1)
                        Text(surah?.nameArabic ?: "")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (recitationState is RecitationState.Playing) {
                            viewModel.stopAyah()
                        }
                        onBack.invoke()
                    }) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Font size toggle
                    TextButton(onClick = {
                        viewModel.setFontSize(
                            when (fontSize) {
                                QuranFontSize.SMALL -> QuranFontSize.MEDIUM
                                QuranFontSize.MEDIUM -> QuranFontSize.LARGE
                                QuranFontSize.LARGE -> QuranFontSize.XLARGE
                                QuranFontSize.XLARGE -> QuranFontSize.SMALL
                            }
                        )
                    }) {
                        Text(
                            text = fontSize.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = {
                        viewModel.setTextAlignment(
                            when (quranTextAlignment) {
                                QuranTextAlignment.Start -> QuranTextAlignment.Center
                                QuranTextAlignment.Center -> QuranTextAlignment.End
                                QuranTextAlignment.End -> QuranTextAlignment.Start
                            }
                        )
                    }) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = ImageVector.vectorResource(
                                when (quranTextAlignment) {
                                    QuranTextAlignment.Start -> R.drawable.ic_align_end
                                    QuranTextAlignment.Center -> R.drawable.ic_align_center
                                    QuranTextAlignment.End -> R.drawable.ic_align_start
                                }
                            ), contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val state = readerState) {
            QuranReaderUiState.Idle,
            QuranReaderUiState.Loading -> {
                LoadingContent()
            }

            is QuranReaderUiState.Error -> {
                ErrorContent(state.message)
            }

            is QuranReaderUiState.Success -> {
                val surah = state.surah
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp)
                ) {
                    item {
                        SurahHeader(
                            surah = QuranData.surahs[surahIndex - 1],
                        )
                    }

                    // Group ayahs by juz, insert a divider between juz changes
                    val groups = surah.ayahs.groupByJuz(surah.juzMap)
                    groups.forEach { (juzNum, ayahs) ->
                        if (juzNum != null) {
                            item(key = "juz_$juzNum") { JuzDivider(juzNum) }
                        }
                        item(key = "block_$juzNum") {
                            FlowingAyahBlock(
                                ayahs = ayahs,
                                fontSize = fontSize,
                                quranTextAlignment = quranTextAlignment,
                                bookmark = bookmark,
                                surahIndex = surahIndex, onTap = { ayah ->
                                    viewModel.selectAyah(ayah)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowingAyahBlock(
    ayahs: List<Ayah>,
    fontSize: QuranFontSize,
    quranTextAlignment: QuranTextAlignment,
    bookmark: QuranBookmark?,
    surahIndex: Int,
    onTap: (Ayah) -> Unit
) {

    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val bookmarkColor = MaterialTheme.colorScheme.tertiary
    // Tag used in AnnotatedString to detect ayah taps
    val tag = "AYAH"

    val annotated = buildAnnotatedString {
        ayahs.forEach { ayah ->
            val isBookmarked = bookmark?.surahIndex == surahIndex &&
                    bookmark.ayahNumber == ayah.number
            // Ayah text
            pushStringAnnotation(tag, ayah.number.toString())
            withStyle(
                SpanStyle(
                    color = if (isBookmarked) bookmarkColor else onSurface,
                    fontSize = fontSize.sp.sp
                )
            ) {
                append(ayah.text)
            }
            // Ayah number marker ۝N
            withStyle(
                SpanStyle(
                    color = primary,
                    fontSize = (fontSize.sp * 0.75).sp,
                    fontWeight = FontWeight.Bold,
                )
            ) {
                append(ayahMarker(ayah.number))
            }
            pop()
        }
    }

    ClickableText(
        text = annotated,
        modifier = Modifier
            .fillMaxWidth()
            .padding(),
        style = TextStyle(
            fontSize = fontSize.sp.sp,
            lineHeight = (fontSize.sp * 2.0).sp,
            textAlign = when (quranTextAlignment) {
                QuranTextAlignment.Start -> TextAlign.Start
                QuranTextAlignment.Center -> TextAlign.Center
                QuranTextAlignment.End -> TextAlign.End
            },
            textDirection = TextDirection.Rtl, fontFamily = quranFontFamily
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(tag, offset, offset)
                .firstOrNull()?.let { ann ->
                    // skip showing
                    if (ann.item.toInt() == 0) return@ClickableText
                    val ayah = ayahs.find { it.number == ann.item.toInt() }
                    ayah?.let { onTap(it) }
                }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AyahBottomSheet(
    ayah: Ayah,
    tafsirState: TafsirState,
    isBookmarked: Boolean,
    onTafsir: () -> Unit,
    onBookmark: () -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onDismiss: () -> Unit,
    playEnabled: Boolean,
    currentReciter: Reciter,
    onReciterSelect: (Reciter) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        var showReciterOptions by remember { mutableStateOf(false) }
        AnimatedContent(showReciterOptions) { showReciterPicker ->
            if (!showReciterPicker) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.End
                ) {
                    // Ayah text preview
                    Text(
                        text = ayah.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Rtl,
                            lineHeight = 30.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    // Tafsir content
                    when (tafsirState) {
                        TafsirState.Idle -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    modifier = Modifier.weight(0.6f),
                                    onClick = onPlayPause,
                                    enabled = playEnabled
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) ImageVector.vectorResource(R.drawable.ic_stop)
                                        else ImageVector.vectorResource(R.drawable.ic_play),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isPlaying) "Stop" else "Play")
                                }
                                OutlinedButton(
                                    modifier = Modifier.weight(0.4f),
                                    onClick = { showReciterOptions = true },
                                    enabled = playEnabled
                                ) {
                                    Text(currentReciter.nameEnglish, modifier = Modifier.basicMarquee())
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = onBookmark) {
                                    Icon(
                                        if (isBookmarked) ImageVector.vectorResource(R.drawable.ic_bookmark_filled) else ImageVector.vectorResource(
                                            R.drawable.ic_bookmark
                                        ),
                                        contentDescription = if (isBookmarked) stringResource(R.string.remove_bookmark) else stringResource(
                                            R.string.bookmark
                                        )
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = onTafsir) {
                                    Text(stringResource(R.string.tafsir))
                                }
                            }

                        }
                        TafsirState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) { ContainedLoadingIndicator() }
                        }
                        TafsirState.NoNetwork -> {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_internet_connection),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is TafsirState.Error -> {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = tafsirState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is TafsirState.Success -> {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Text(
                                text = tafsirState.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.Rtl,
                                    lineHeight = 32.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

            } else {
                AyahReciterPickerSheetContent(
                    selectedReciter = currentReciter,
                    onDismiss = {
                        showReciterOptions = false
                    },
                    onSelect = { onReciterSelect(it); showReciterOptions = false }
                )
            }
        }
    }
}

@Composable
private fun SurahHeader(surah: Surah) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            fontFamily = quranFontFamily,
            text = surah.nameArabic,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = surah.nameTransliterated,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun JuzDivider(juzNumber: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = "Juz $juzNumber",
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.End
            )
        }
    }
}


// Extension to group ayahs by juz changes
private fun List<Ayah>.groupByJuz(juzMap: Map<Int, Int>): List<Pair<Int?, List<Ayah>>> {
    val result = mutableListOf<Pair<Int?, List<Ayah>>>()
    var current = mutableListOf<Ayah>()
    var lastJuz: Int? = null

    forEach { ayah ->
        val juz = juzMap[ayah.number]
        if (juz != lastJuz && current.isNotEmpty()) {
            result.add(lastJuz to current)
            current = mutableListOf()
        }
        lastJuz = juz
        current.add(ayah)
    }
    if (current.isNotEmpty()) result.add(lastJuz to current)
    return result
}

// Unicode circle numbers for ayah markers ١..٦٠٤ encoded as ۝ + arabic number
private fun ayahMarker(number: Int): String {
    val arabicNumber = number.toString().map { ch ->
        if (ch.isDigit()) '٠' + (ch - '0') else ch
    }.joinToString("")
    return " ﴿$arabicNumber﴾ "
}