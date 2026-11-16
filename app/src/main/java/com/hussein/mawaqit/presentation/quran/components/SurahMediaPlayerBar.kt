package com.hussein.mawaqit.presentation.quran.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.db.entities.AudioSourceEntity
import com.hussein.mawaqit.infrastructure.services.PlaybackSource
import com.hussein.mawaqit.presentation.util.GlobalPlaybackUiState
import com.hussein.mawaqit.ui.theme.MawaqitTheme
import com.hussein.mawaqit.ui.theme.quranFontFamily

@Composable
fun SurahMediaPlayerBar(
    modifier: Modifier = Modifier,
    playbackState: GlobalPlaybackUiState,
    selectedReciter: AudioSourceEntity?,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onClickSurah: ((Int) -> Unit)? = null
) {
    val surah = playbackState.currentSurah
    val isVisible = playbackState.source is PlaybackSource.Surah && surah != null

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (surah == null) return@AnimatedVisibility

        var isDragging by remember { mutableStateOf(false) }
        var dragPosition by remember { mutableFloatStateOf(0f) }

        val currentPos = if (isDragging) dragPosition.toLong() else playbackState.currentPositionMs
        val duration = playbackState.durationMs

        Surface(
            shape = MawaqitTheme.appShapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Header Row: Surah Info & Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Surah details
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (onClickSurah != null) {
                                    Modifier.clickable { onClickSurah(surah.number) }
                                } else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MawaqitTheme.appShapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = surah.number.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = surah.nameArabic,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = quranFontFamily
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                                Text(
                                    text = " • ${surah.nameTransliterated}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            selectedReciter?.let { reciter ->
                                Text(
                                    text = reciter.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Media Control Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onPrevious != null && surah.number > 1) {
                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                                    contentDescription = "Previous Surah",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onPlayPause,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (playbackState.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = ImageVector.vectorResource(
                                        if (playbackState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                    ),
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (onNext != null && surah.number < 114) {
                            IconButton(
                                onClick = onNext,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right),
                                    contentDescription = "Next Surah",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                                contentDescription = "Close player",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Progress Bar & Time Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(currentPos),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val sliderMax = maxOf(1f, duration.toFloat())
                    Slider(
                        value = currentPos.toFloat().coerceIn(0f, sliderMax),
                        onValueChange = {
                            isDragging = true
                            dragPosition = it
                        },
                        onValueChangeFinished = {
                            isDragging = false
                            onSeek(dragPosition.toLong())
                        },
                        valueRange = 0f..sliderMax,
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}
