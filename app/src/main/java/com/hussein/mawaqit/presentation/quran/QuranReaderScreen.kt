package com.hussein.mawaqit.presentation.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.quran.Ayah
import com.hussein.mawaqit.data.quran.QuranData
import com.hussein.mawaqit.data.quran.Surah
import com.hussein.mawaqit.presentation.quran.tafsir.TafsirState
import com.hussein.mawaqit.presentation.shared.ErrorContent
import com.hussein.mawaqit.presentation.shared.LoadingContent
import com.hussein.mawaqit.ui.theme.quranFontFamily


// Al-Fatihah (1) already contains Bismillah; At-Tawbah (9) has none
private fun showBismillah(surahIndex: Int) = surahIndex != 1 && surahIndex != 9

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahIndex: Int,
    onBack: () -> Unit,
    viewModel: QuranViewModel = viewModel()
) {
    val readerState by viewModel.readerState.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val quranTextAlignment by viewModel.quranTextAlignment.collectAsStateWithLifecycle()
    val bookmark by viewModel.bookmark.collectAsStateWithLifecycle()

    val selectedAyah by viewModel.selectedAyah.collectAsStateWithLifecycle()
    val tafsirState by viewModel.tafsirState.collectAsStateWithLifecycle()
    val isNetworkAvail by viewModel.networkAvailable.collectAsStateWithLifecycle()

    LaunchedEffect(surahIndex) { viewModel.loadSurah(surahIndex) }

    // Fetch tafsir when an ayah is selected
    LaunchedEffect(selectedAyah) {
        selectedAyah?.let { viewModel.fetchTafsir(surahIndex, it) }
    }

    // Bottom sheet
    if (selectedAyah != null) {
        ModalBottomSheet(onDismissRequest = { viewModel.dismissTafsir() }) {
            TafsirBottomSheet(
                ayah = selectedAyah!!,
                tafsirState = tafsirState,
                isNetwork = isNetworkAvail
            )
        }
    }

    LaunchedEffect(surahIndex) { viewModel.loadSurah(surahIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (readerState is QuranReaderState.Success) {
                        val surah = QuranData.surahs.getOrNull(surahIndex - 1)
                        Text(surah?.nameArabic ?: "")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            QuranReaderState.Idle,
            QuranReaderState.Loading -> {
                LoadingContent()
            }

            is QuranReaderState.Error -> {
                ErrorContent(state.message)
            }

            is QuranReaderState.Success -> {
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
                                surahIndex = surahIndex,
                                onBookmark = { ayahNumber ->
                                    val isBookmarked = bookmark?.surahIndex == surahIndex &&
                                            bookmark?.ayahNumber == ayahNumber
                                    if (isBookmarked) viewModel.clearBookmark()
                                    else viewModel.setBookmark(surahIndex, ayahNumber)
                                }, onTap = {
                                    ayah -> viewModel.fetchTafsir(surahIndex, ayah)
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

@Composable
private fun FlowingAyahBlock(
    ayahs: List<Ayah>,
    fontSize: QuranFontSize,
    quranTextAlignment: QuranTextAlignment,
    bookmark: QuranBookmark?,
    surahIndex: Int,
    onBookmark: (Int) -> Unit,
    onTap: (Ayah) -> Unit,

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
                    val ayah = ayahs.find { it.number == ann.item.toInt() }
                    ayah?.let { onTap(it) }
                }
        }
    )
}
// ---------------------------------------------------------------------------
// Tafsir bottom sheet
// ---------------------------------------------------------------------------


@Composable
private fun TafsirBottomSheet(
    ayah: Ayah,
    tafsirState: TafsirState,
    isNetwork: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        // Ayah number header
        Text(
            text = "تفسير الآية ${ayah.number}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        when (tafsirState) {
            TafsirState.Idle,
            TafsirState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            TafsirState.NoNetwork -> {
                Text(
                    text = "لا يوجد اتصال بالإنترنت",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            is TafsirState.Error -> {
                Text(
                    text = tafsirState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            is TafsirState.Success -> {
                Text(
                    text = tafsirState.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 32.sp,
                        textDirection = TextDirection.Rtl
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TafsirBottomSheet(
    ayah: Ayah?,
    tafsirState: TafsirState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Ayah number header
            ayah?.let {
                Text(
                    text = "الآية ${it.number}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDirection = TextDirection.Rtl,
                        lineHeight = 28.sp
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
            }

            // Tafsir content
            when (tafsirState) {
                TafsirState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                TafsirState.NoNetwork -> {
                    Text(
                        text = "لا يوجد اتصال بالإنترنت",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is TafsirState.Error -> {
                    Text(
                        text = tafsirState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is TafsirState.Success -> {
                    Text(
                        text = tafsirState.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Rtl,
                            lineHeight = 32.sp
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TafsirState.Idle -> Unit
            }
        }
    }
}