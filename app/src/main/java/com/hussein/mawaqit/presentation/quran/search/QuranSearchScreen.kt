package com.hussein.mawaqit.presentation.quran.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.data.db.AyahEntity
import com.hussein.mawaqit.data.db.SurahEntity
import com.hussein.mawaqit.data.quran.QuranData
import org.koin.androidx.compose.koinViewModel
import kotlin.collections.isNotEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSearchScreen(
    onSurahSelected: (surahNumber: Int) -> Unit,
    onAyahSelected: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: QuranSearchViewModel = koinViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChanged(it) },
                        placeholder = { Text("Search") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearQuery() }) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(com.hussein.mawaqit.R.drawable.ic_close),
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = ImageVector.vectorResource(com.hussein.mawaqit.R.drawable.ic_arrow_back),
                            contentDescription = "back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            SearchState.Idle -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Search about ayah or surah",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SearchState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            SearchState.Empty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nothing is found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is SearchState.Results -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // ── Surah results ─────────────────────────────────────────
                    if (s.data.surahs.isNotEmpty()) {
                        item {
                            SearchSectionHeader("${s.data.surahs.size} Surah")
                        }
                        items(s.data.surahs, key = { "surah_${it.number}" }) { surah ->
                            SurahSearchRow(
                                surah = surah,
                                onClick = { onSurahSelected(surah.number) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    // ── Ayah results ──────────────────────────────────────────
                    if (s.data.ayahs.isNotEmpty()) {
                        item {
                            SearchSectionHeader("${s.data.ayahs.size} Ayah")
                        }
                        items(
                            s.data.ayahs,
                            key = { "ayah_${it.surahNumber}_${it.numberInSurah}" }) { ayah ->
                            AyahSearchRow(
                                ayah = ayah,
                                onClick = { onAyahSelected(ayah.surahNumber, ayah.numberInSurah) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section header
// ---------------------------------------------------------------------------

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        textAlign = TextAlign.End
    )
}


@Composable
private fun SurahSearchRow(
    surah: SurahEntity,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = surah.nameTransliterated,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = surah.nameArabic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
private fun AyahSearchRow(
    ayah: AyahEntity,
    onClick: () -> Unit
) {
    val surahName = QuranData.surahs.getOrNull(ayah.surahNumber - 1)?.nameArabic ?: ""

    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Surah + ayah reference
            Text(
                text = "$surahName • آية ${ayah.numberInSurah}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            // Ayah text — truncated to 2 lines
            Text(
                text = ayah.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDirection = TextDirection.Rtl
                ),
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
