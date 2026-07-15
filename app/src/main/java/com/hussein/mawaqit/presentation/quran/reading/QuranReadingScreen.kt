package com.hussein.mawaqit.presentation.quran.reading

import android.content.ClipData
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import com.hussein.mawaqit.data.db.entities.AudioSourceEntity
import com.hussein.mawaqit.domain.models.Ayah
import com.hussein.mawaqit.domain.models.Bookmark
import com.hussein.mawaqit.infrastructure.settings.QuranTextAlignment
import com.hussein.mawaqit.presentation.shared.BackButton
import com.hussein.mawaqit.presentation.shared.ErrorContent
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.ui.theme.MawaqitTheme
import com.hussein.mawaqit.ui.theme.quranFontFamily
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun QuranReadingScreen(
    surahIndex: Int,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTafsir: (String) -> Unit,
    scrollToAyah: Int? = null,
    viewModel: QuranReadingViewModel = koinViewModel()) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val readerState = uiState.readerState
    val fontSize = uiState.fontSize
    val quranTextAlignment = uiState.textAlignment
    val bookmarks = uiState.bookmarks
    val selectedAyah = uiState.selectedAyah
    val tafsirState = uiState.tafsirState
    val recitationState = uiState.recitationState
    val playingAyah = uiState.playingAyah
    val selectedReciter = uiState.selectedReciter
    val hasNetwork = uiState.isNetworkAvailable

    var highlightedAyah by remember { mutableStateOf<Int?>(null) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var lastScrolledAyah by remember(surahIndex) { mutableStateOf<Int?>(null) }
    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    val surahName = if (readerState is QuranReaderUiState.Success) {
        readerState.surah.nameArabic
    } else ""


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
            if (viewModel.uiState.value.playingAyah != null) {
                viewModel.stopAyah()
            }
        }
    }

    LaunchedEffect(surahIndex) { viewModel.loadSurah(surahIndex) }

    val density = LocalDensity.current
    // Track center ayah for "continue reading" feature
    LaunchedEffect(listState, textLayoutResult) {
        snapshotFlow {
            val layout = textLayoutResult ?: return@snapshotFlow null
            val viewportHeight = listState.layoutInfo.viewportSize.height
            if (viewportHeight <= 0) return@snapshotFlow null

            val scrollOffset = listState.firstVisibleItemScrollOffset
            val paddingPx = with(density) { 8.dp.toPx() }
            val centerY = scrollOffset + (viewportHeight / 2f) - paddingPx

            if (centerY < 0 || centerY > layout.size.height) return@snapshotFlow null

            val offset = try {
                layout.getOffsetForPosition(Offset(layout.size.width / 2f, centerY))
            } catch (e: Exception) {
                return@snapshotFlow null
            }

            layout.layoutInput.text.getStringAnnotations("AYAH", offset, offset)
                .firstOrNull()?.item?.toIntOrNull()
        }
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(500.milliseconds)
            .collect { ayahIndex ->
                if(!listState.isScrollInProgress){
                    viewModel.updateLastRead(surahIndex, ayahIndex)
                }
            }
    }

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
            availableReciters = uiState.availableReciters,
            onTafsir = { viewModel.fetchTafsir(surahIndex, ayah) },
            onBookmark = {
                viewModel.toggleBookmark(
                    surahNumber = surahIndex,
                    ayahNumber = ayah.numberInSurah,
                    surahName = surahName
                )
            },
            onPlayPause = {
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
                coroutineScope.launch {
                    val clipData = ClipData.newPlainText("Ayah ${ayah.numberInSurah}", ayah.text)
                    clipboardManager.setClipEntry(ClipEntry(clipData))
                }

            })
    }

    BackHandler(enabled = recitationState is AyahRecitationState.Playing) {
        viewModel.stopAyah()
        onBack.invoke()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    IconButton(onClick = { onNavigateToTafsir(surahName) }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_placeholder),
                            contentDescription = "Tafsir"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
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
        }
    ) { paddingValues ->
        when (val state = readerState) {

            is QuranReaderUiState.Success -> {
                val surah = state.surah
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 32.dp, start = 6.dp, end = 6.dp),
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

@Composable
private fun FlowingAyahBlock(
    ayahs: List<Ayah>,
    fontSize: Float,
    quranTextAlignment: QuranTextAlignment,
    bookmarks: List<Bookmark>,
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
    currentReciter: AudioSourceEntity?,
    availableReciters: List<AudioSourceEntity>,
    onReciterSelect: (AudioSourceEntity) -> Unit,
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.ayah_number, ayah.numberInSurah),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, overflow = TextOverflow.MiddleEllipsis
                        )

                        IconButton(
                            onClick = onBookmark,
                            modifier = Modifier.background(
                                color = if (isBookmarked) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                MawaqitTheme.appShapes.medium
                            )
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                                contentDescription = stringResource(if (isBookmarked) R.string.remove_bookmark else R.string.bookmark)
                            )
                        }

                        IconButton(
                            onClick = { onAyahCopy(ayah.text) },
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MawaqitTheme.appShapes.medium
                            )
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_copy),
                                contentDescription = stringResource(R.string.copy_ayah)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MawaqitTheme.appShapes.medium
                            )
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    }

                    // Ayah Text Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MawaqitTheme.appShapes.medium
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
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
                        shape = MawaqitTheme.appShapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tafsir),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            when (tafsirState) {
                                is TafsirState.Success -> {
                                    Text(
                                        text = tafsirState.text,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            textDirection = TextDirection.Rtl,
                                            lineHeight = 38.sp,
                                            textAlign = TextAlign.Center,
                                            fontFamily = quranFontFamily
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
                            shape = MawaqitTheme.appShapes.medium
                        ) {
                            Text(
                                text = currentReciter?.name ?: stringResource(R.string.pick_a_reciter),
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
                            shape = MawaqitTheme.appShapes.medium
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play
                                ),
                                contentDescription = stringResource(if (isPlaying) R.string.stop else R.string.play),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(if (isPlaying) R.string.stop else R.string.play),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            4.dp,
                            alignment = Alignment.Start
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showReciterOptions = false }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                                contentDescription = null
                            )
                        }

                        Text(
                            text = stringResource(R.string.pick_a_reciter),
                            style = MaterialTheme.typography.titleMedium,
                        )

                    }

                    availableReciters.forEach { reciter ->
                        val isSelected = reciter.id == currentReciter?.id
                        Surface(
                            shape = MawaqitTheme.appShapes.small,
                            onClick = {
                                onReciterSelect(reciter)
                                showReciterOptions = false;
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = reciter.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontFamily = quranFontFamily
                                    )
                                    reciter.language?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}


fun ayahMarker(number: Int): String {
    val arabicNumber = number.toString().map { ch ->
        if (ch.isDigit()) '٠' + (ch - '0') else ch
    }.joinToString("")
    return " ﴿$arabicNumber﴾ "
}