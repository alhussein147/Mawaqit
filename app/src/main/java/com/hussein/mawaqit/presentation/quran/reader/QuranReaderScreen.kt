package com.hussein.mawaqit.presentation.quran.reader

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.hussein.mawaqit.data.db.entities.BookmarkEntity
import com.hussein.mawaqit.data.quran.QuranTextAlignment
import com.hussein.mawaqit.data.quran.recitation.Reciter
import com.hussein.mawaqit.domain.models.Ayah
import com.hussein.mawaqit.presentation.quran.components.AyahReciterPickerSheetContent
import com.hussein.mawaqit.presentation.shared.BackButton
import com.hussein.mawaqit.presentation.shared.ErrorContent
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.presentation.util.GlobalPlayerViewModel
import com.hussein.mawaqit.ui.theme.quranFontFamily
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahIndex: Int,
    onBack: () -> Unit,
    scrollToAyah: Int? = null,
    viewModel: QuranViewModel = koinViewModel(),
    globalMediaPlayerViewModel: GlobalPlayerViewModel = koinInject()
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

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var highlightedAyah by remember { mutableStateOf<Int?>(null) }

    val listState = rememberLazyListState()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var lastScrolledAyah by remember(surahIndex) { mutableStateOf<Int?>(null) }

    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(readerState, scrollToAyah, textLayoutResult) {
        if (scrollToAyah != null && scrollToAyah != lastScrolledAyah && readerState is QuranReaderUiState.Success && textLayoutResult != null) {
            val layout = textLayoutResult!!
            val annotatedString = layout.layoutInput.text
            val annotation = annotatedString.getStringAnnotations("AYAH", 0, annotatedString.length)
                .find { it.item == scrollToAyah.toString() }

            annotation?.let {
                val line = layout.getLineForOffset(it.start)
                val top = layout.getLineTop(line)
                listState.animateScrollToItem(index = 0, scrollOffset = top.toInt())
                lastScrolledAyah = scrollToAyah
            }
        }
    }

    LaunchedEffect(scrollToAyah) {
        if (scrollToAyah != null) {
            highlightedAyah = scrollToAyah
            delay(3000.milliseconds)
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
            isPlaying = playingAyah == ayah.numberInSurah && recitationState is AyahRecitationState.Playing,
            currentReciter = selectedReciter,
            onTafsir = { viewModel.fetchTafsir(surahIndex, ayah) },
            onBookmark = { viewModel.toggleBookmark(surahIndex, ayah.numberInSurah) },
            onPlayPause = {
                if (globalMediaPlayerViewModel.isPlaying.value) {
                    globalMediaPlayerViewModel.stop()
                }
                if (playingAyah == ayah.numberInSurah && recitationState !is AyahRecitationState.Idle) {
                    viewModel.stopAyah()
                } else {
                    viewModel.playAyah(surahIndex, ayah.numberInSurah)
                }
            },
            onReciterSelect = { viewModel.selectReciter(it) },
            onDismiss = { viewModel.dismissTafsir() },
            playEnabled = hasNetwork,
            onAyahCopy = {
                clipboardManager.setText(AnnotatedString(it))
            })
    }

    BackHandler(enabled = recitationState is AyahRecitationState.Playing) {
        viewModel.stopAyah()
        onBack.invoke()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (readerState is QuranReaderUiState.Success) {
                        val surah = (readerState as QuranReaderUiState.Success).surah
                        Text(
                            text = surah.nameArabic,
                            fontFamily = quranFontFamily,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    BackButton(onClick = {
                        if (recitationState is AyahRecitationState.Playing) viewModel.stopAyah()
                        onBack.invoke()
                    })
                },
                actions = {
                    IconButton(onClick = { showReadingOptionsDialog = true }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
                            contentDescription = "Reading Options"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = readerState) {
                is QuranReaderUiState.Success -> {
                    val surah = state.surah
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp, start = 12.dp, end = 12.dp),
                        state = listState
                    ) {
                        item {
                            FlowingAyahBlock(
                                ayahs = surah.ayahs,
                                fontSize = fontSize,
                                quranTextAlignment = quranTextAlignment,
                                bookmarks = bookmarks,
                                surahIndex = surahIndex,
                                onTap = { ayah -> viewModel.selectAyah(ayah) },
                                onTextLayout = { layout -> textLayoutResult = layout },
                                highlightedAyah = highlightedAyah
                            )
                        }
                    }

                    if (showReadingOptionsDialog) {
                        QuranReaderOptionsDialog(
                            selectedFontSize = fontSize,
                            selectedTextAlignment = quranTextAlignment,
                            onSelectedFontSize = { viewModel.setFontSize(it) },
                            onSelectedTextAlignment = { viewModel.setTextAlignment(it) },
                            onDismiss = { showReadingOptionsDialog = false }
                        )
                    }
                }

                QuranReaderUiState.Idle, QuranReaderUiState.Loading -> {
                    LoadingContent(
                        modifier = Modifier.fillMaxSize(),
                        backgroundColor = Color.Transparent
                    )
                }

                is QuranReaderUiState.Error -> {
                    ErrorContent(message = state.message, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun FlowingAyahBlock(
    ayahs: List<Ayah>,
    fontSize: Float,
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
    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    val tag = "AYAH"

    val annotated = buildAnnotatedString {
        ayahs.forEach { ayah ->
            val isBookmarked = bookmarks.any {
                it.surahNumber == surahIndex && it.ayahNumber == ayah.numberInSurah
            }
            val isHighlighted = ayah.numberInSurah == highlightedAyah
            
            pushStringAnnotation(tag, ayah.numberInSurah.toString())
            
            withStyle(
                SpanStyle(
                    color = if (isBookmarked) bookmarkColor else onSurface,
                    fontSize = fontSize.sp,
                    background = if (isHighlighted) highlightColor else Color.Transparent
                )
            ) {
                append(ayah.text)
            }
            
            withStyle(
                SpanStyle(
                    color = primary.copy(alpha = 0.8f),
                    fontSize = (fontSize * 0.7).sp,
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
            .padding(vertical = 8.dp),
        style = TextStyle(
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 2.2).sp,
            textAlign = when (quranTextAlignment) {
                QuranTextAlignment.Start -> TextAlign.Start
                QuranTextAlignment.Center -> TextAlign.Center
                QuranTextAlignment.End -> TextAlign.End
            },
            textDirection = TextDirection.Rtl,
            fontFamily = quranFontFamily
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(tag, offset, offset).firstOrNull()?.let { ann ->
                val ayah = ayahs.find { it.numberInSurah == ann.item.toInt() }
                ayah?.let { onTap(it) }
            }
        },
        onTextLayout = onTextLayout
    )
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
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LaunchedEffect(ayah) {
            onTafsir()
        }

        var showReciterOptions by remember { mutableStateOf(false) }

        AnimatedContent(
            targetState = showReciterOptions,
            label = "ReciterOptionsTransition"
        ) { showReciterPicker ->
            if (!showReciterPicker) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ayah ${ayah.numberInSurah}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                                contentDescription = "Close"
                            )
                        }
                    }

                    // Ayah Text Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp)
                        ) {
                            Text(
                                text = ayah.text,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    textDirection = TextDirection.Rtl,
                                    lineHeight = 38.sp,
                                    fontFamily = quranFontFamily,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Tafsir Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Tafsir",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            when (tafsirState) {
                                is TafsirState.Success -> {
                                    Text(
                                        text = tafsirState.text,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeight = 28.sp,
                                            textAlign = TextAlign.Justify
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                TafsirState.Loading -> {
                                    Box(
                                        Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LoadingIndicator()
                                    }
                                }
                                is TafsirState.Error -> {
                                    Text(
                                        text = tafsirState.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                TafsirState.Idle -> Unit
                            }
                        }
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BottomSheetButton(
                            modifier = Modifier.weight(1f),
                            icon = R.drawable.ic_copy,
                            title = "Copy",
                            onClick = {
                                onAyahCopy(ayah.text)
                                onDismiss()
                            }
                        )
                        BottomSheetButton(
                            modifier = Modifier.weight(1f),
                            icon = if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
                            title = if (isBookmarked) "Saved" else "Save",
                            selected = isBookmarked,
                            onClick = onBookmark
                        )
                    }

                    // Audio Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            onClick = { showReciterOptions = true },
                            enabled = playEnabled,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = currentReciter.nameEnglish,
                                modifier = Modifier.basicMarquee(),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            onClick = onPlayPause,
                            enabled = playEnabled,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play
                                ),
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isPlaying) "Stop" else "Play",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            } else {
                AyahReciterPickerSheetContent(
                    selectedReciter = currentReciter,
                    onDismiss = { showReciterOptions = false },
                    onSelect = {
                        onReciterSelect(it)
                        showReciterOptions = false
                    },
                    onBack = { showReciterOptions = false }
                )
            }
        }
    }
}

@Composable
private fun BottomSheetButton(
    modifier: Modifier,
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    title: String,
    selected: Boolean = false
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
