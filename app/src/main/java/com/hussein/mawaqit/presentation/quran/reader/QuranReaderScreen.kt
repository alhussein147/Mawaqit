package com.hussein.mawaqit.presentation.quran.reader

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.db.BookmarkEntity
import com.hussein.mawaqit.data.db.models.Ayah
import com.hussein.mawaqit.data.quran.QuranData
import com.hussein.mawaqit.data.quran.QuranFontSize
import com.hussein.mawaqit.data.quran.QuranTextAlignment
import com.hussein.mawaqit.data.quran.Surah
import com.hussein.mawaqit.data.quran.recitation.Reciter
import com.hussein.mawaqit.presentation.quran.components.AyahReciterPickerSheetContent
import com.hussein.mawaqit.presentation.shared.ErrorContent
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.ui.theme.quranFontFamily
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahIndex: Int,
    onBack: () -> Unit,
    scrollToAyah: Int? = null,
    viewModel: QuranViewModel = koinViewModel(),
) {
    val readerState by viewModel.readerState.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val quranTextAlignment by viewModel.textAlignment.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    val selectedAyah by viewModel.selectedAyah.collectAsStateWithLifecycle()
    val tafsirState by viewModel.tafsirState.collectAsStateWithLifecycle()

    val recitationState by viewModel.ayahRecitationState.collectAsStateWithLifecycle()
    val playingAyah by viewModel.playingAyah.collectAsStateWithLifecycle()
    val selectedReciter by viewModel.selectedReciter.collectAsStateWithLifecycle()
    val hasNetwork by viewModel.networkAvailable.collectAsStateWithLifecycle()

    var showReadingOptionsDialog by remember { mutableStateOf(false) }

    val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var highlightedAyah by remember { mutableStateOf<Int?>(null) }

    val listState = rememberLazyListState()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var lastScrolledAyah by remember(surahIndex) { mutableStateOf<Int?>(null) }

    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(readerState, scrollToAyah, textLayoutResult) {
        if (scrollToAyah != null &&
            scrollToAyah != lastScrolledAyah &&
            readerState is QuranReaderUiState.Success &&
            textLayoutResult != null
        ) {
            val layout = textLayoutResult!!
            val annotatedString = layout.layoutInput.text
            val annotation = annotatedString.getStringAnnotations("AYAH", 0, annotatedString.length)
                .find { it.item == scrollToAyah.toString() }

            annotation?.let {
                val line = layout.getLineForOffset(it.start)
                val top = layout.getLineTop(line)
                // index 1 is FlowingAyahBlock, scrollOffset puts the specific line at the top
                listState.animateScrollToItem(index = 1, scrollOffset = top.toInt())
                lastScrolledAyah = scrollToAyah
            }
        }
    }

    LaunchedEffect(scrollToAyah) {
        if (scrollToAyah != null) {
            highlightedAyah = scrollToAyah
            delay(3000)
            highlightedAyah = null
        }
    }

    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            if (viewModel.playingAyah.value != null) {
                viewModel.stopAyah()
            }
        }
    }

    LaunchedEffect(surahIndex) { viewModel.loadSurah(surahIndex) }

    selectedAyah?.let { ayah ->
        val isBookmarked = bookmarks.any {
            it.surahNumber == surahIndex && it.ayahNumber == ayah.numberInSurah
        }
        AyahBottomSheet(
            ayah = ayah,
            tafsirState = tafsirState,
            isBookmarked = isBookmarked,
            isPlaying = playingAyah == ayah.numberInSurah &&
                    recitationState is AyahRecitationState.Playing,
            currentReciter = selectedReciter,
            onTafsir = { viewModel.fetchTafsir(surahIndex, ayah) },
            onBookmark = { viewModel.toggleBookmark(surahIndex, ayah.numberInSurah) },
            onPlayPause = {
                if (playingAyah == ayah.numberInSurah &&
                    recitationState !is AyahRecitationState.Idle
                ) {
                    viewModel.stopAyah()
                } else {
                    viewModel.playAyah(surahIndex, ayah.numberInSurah)
                }
            },
            onReciterSelect = { viewModel.selectReciter(it) },
            onDismiss = { viewModel.dismissTafsir() },
            playEnabled = hasNetwork, onAyahCopy = {
                clipboardManager.setText(AnnotatedString(it))
            }
        )
    }

    BackHandler(enabled = recitationState is AyahRecitationState.Playing) {
        viewModel.stopAyah()
        onBack.invoke()
    }
    Scaffold(
        modifier = Modifier,
        topBar = {
            if (readerState is QuranReaderUiState.Success) {
                TopAppBar(
                    scrollBehavior = topAppBarScrollBehavior,
                    title = {
                        val surah = QuranData.surahs.getOrNull(surahIndex - 1)
                        Text(surah?.nameArabic ?: "")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (recitationState is AyahRecitationState.Playing) {
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
                        IconButton(onClick = {
                            showReadingOptionsDialog = true
                        }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        when (val state = readerState) {
            QuranReaderUiState.Idle,
            QuranReaderUiState.Loading -> {
                LoadingContent(modifier = Modifier.fillMaxSize())
            }

            is QuranReaderUiState.Error -> {
                ErrorContent(state.message, modifier = Modifier.fillMaxSize())
            }

            is QuranReaderUiState.Success -> {
                if (showReadingOptionsDialog) {
                    QuranReaderOptionsDialog(
                        selectedFontSize = fontSize,
                        selectedTextAlignment = quranTextAlignment,
                        onSelectedFontSize = {
                            viewModel.setFontSize(it)
                        },
                        onSelectedTextAlignment = {
                            viewModel.setTextAlignment(it)
                        },
                        onDismiss = {
                            showReadingOptionsDialog = false
                        }
                    )
                }
                val surah = state.surah

                LazyColumn(
                    modifier = Modifier
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 32.dp, start = 4.dp, end = 4.dp),
                    state = listState
                ) {
                    item {
                        SurahHeader(
                            surah = QuranData.surahs[surahIndex - 1],
                        )
                    }
                    item {
                        FlowingAyahBlock(
                            ayahs = surah.ayahs,
                            fontSize = fontSize,
                            quranTextAlignment = quranTextAlignment,
                            bookmarks = bookmarks,
                            surahIndex = surahIndex,
                            onTap = { ayah ->
                                viewModel.selectAyah(ayah)
                            }, onTextLayout = { layout -> textLayoutResult = layout },
                            highlightedAyah = highlightedAyah
                        )
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
    bookmarks: List<BookmarkEntity>,
    surahIndex: Int,
    onTap: (Ayah) -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    highlightedAyah: Int? = null,
) {

    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val bookmarkColor = MaterialTheme.colorScheme.tertiary
    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val tag = "AYAH"

    val annotated = buildAnnotatedString {
        ayahs.forEach { ayah ->
            val isBookmarked = bookmarks.any {
                it.surahNumber == surahIndex && it.ayahNumber == ayah.numberInSurah
            }// Ayah text
            val isHighlighted = ayah.numberInSurah == highlightedAyah
            pushStringAnnotation(tag, ayah.numberInSurah.toString())
            withStyle(
                SpanStyle(
                    color = if (isBookmarked) bookmarkColor else onSurface,
                    fontSize = fontSize.sp.sp,
                    background = if (isHighlighted) highlightColor else Color.Transparent // Apply background
                )
            ) {
                append(ayah.text)
            }
            withStyle(
                SpanStyle(
                    color = primary,
                    fontSize = (fontSize.sp * 0.75).sp,
                    fontWeight = FontWeight.Bold,
                )
            ) {
                append(ayahMarker(ayah.numberInSurah))
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
                    val ayah = ayahs.find { it.numberInSurah == ann.item.toInt() }
                    ayah?.let { onTap(it) }
                }
        },
        onTextLayout = onTextLayout
    )
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

private fun ayahMarker(number: Int): String {
    val arabicNumber = number.toString().map { ch ->
        if (ch.isDigit()) '٠' + (ch - '0') else ch
    }.joinToString("")
    return " ﴿$arabicNumber﴾ "
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
    onAyahCopy: (String) -> Unit

) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {

        @Composable
        fun BottomSheetButton(
            modifier: Modifier,
            onClick: () -> Unit,
            @DrawableRes icon: Int,
            title: String
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .then(modifier)
                    .height(96.dp), shape = RoundedCornerShape(24.dp),
                onClick = onClick
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = ImageVector.vectorResource(icon), contentDescription = null)
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // copy button
                                BottomSheetButton(
                                    modifier = Modifier.weight(1f),
                                    icon = R.drawable.ic_copy,
                                    title = "Copy Ayah",
                                    onClick = {
                                        onAyahCopy(ayah.text)
                                        onDismiss()
                                    }
                                )
                                // bookmark button
                                BottomSheetButton(
                                    modifier = Modifier.weight(1f),
                                    icon = if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
                                    title = if (isBookmarked) "Remove bookmark" else "Bookmark",
                                    onClick = onBookmark
                                )
                                // tafsir button
                                BottomSheetButton(
                                    modifier = Modifier.weight(1f),
                                    icon = R.drawable.ic_placeholder,
                                    title = "Tafsir", onClick = onTafsir
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // reciter button
                                OutlinedButton(
                                    modifier = Modifier.weight(0.4f),
                                    onClick = { showReciterOptions = true },
                                    enabled = playEnabled
                                ) {
                                    Text(
                                        currentReciter.nameEnglish,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                                // ayah recitation play pause button
                                Button(
                                    modifier = Modifier.weight(0.6f),
                                    onClick = onPlayPause,
                                    enabled = playEnabled
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(
                                            if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isPlaying) "Stop" else "Play")
                                }
                            }
                        }

                        TafsirState.Loading -> {
                            LoadingContent(modifier = Modifier.size(100.dp))
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
                            ErrorContent(
                                message = tafsirState.message,
                                modifier = Modifier.size(100.dp)
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
                    onSelect = { onReciterSelect(it); showReciterOptions = false },
                    onBack = { showReciterOptions = false }
                )
            }
        }
    }
}
