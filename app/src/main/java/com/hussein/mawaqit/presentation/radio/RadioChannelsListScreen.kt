package com.hussein.mawaqit.presentation.radio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.R
import com.hussein.mawaqit.infrastructure.services.PlaybackSource
import com.hussein.mawaqit.presentation.shared.BackButton
import com.hussein.mawaqit.presentation.util.GlobalPlayerViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RadioChannelListScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    globalPlayerViewModel: GlobalPlayerViewModel = koinInject()
) {
    val playbackState by globalPlayerViewModel.playbackState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val activeRadioUrl = (playbackState.source as? PlaybackSource.Radio)?.stationUrl

    LaunchedEffect(playbackState.errorMessage) {
        playbackState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            globalPlayerViewModel.clearPlaybackError()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text("Radio") },
                navigationIcon = {
                    BackButton(onClick = onBack)
                },
                actions = {
                    if (playbackState.source is PlaybackSource.Radio) {
                        IconButton(onClick = globalPlayerViewModel::stop) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_stop),
                                contentDescription = "Stop radio"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(RadioChannel.entries, key = { it.id }) { channel ->
                val isSelected = channel.streamUrl == activeRadioUrl
                val isPlaying = isSelected && playbackState.isPlaying
                val isBuffering = isSelected && playbackState.isBuffering

                RadioChannelRow(
                    channel = channel,
                    isSelected = isSelected,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    onPlayPause = {
                        when {
                            isPlaying || isSelected -> globalPlayerViewModel.togglePlayPause()
                            else -> globalPlayerViewModel.playRadio(stationUrl = channel.streamUrl, title = channel.displayName)
                        }
                    }
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
private fun RadioChannelRow(
    channel: RadioChannel,
    isSelected: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit
) {
    Surface(
        onClick = onPlayPause,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RadioAvatar(channel = channel, isSelected = isSelected)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = channel.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (isPlaying) "Live now" else "Quran radio",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        strokeWidth = 3.dp
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            when {
                                isPlaying -> R.drawable.ic_pause
                                isSelected -> R.drawable.ic_resume
                                else -> R.drawable.ic_play
                            }
                        ),
                        contentDescription = when {
                            isPlaying -> "Pause ${channel.displayName}"
                            isSelected -> "Resume ${channel.displayName}"
                            else -> "Play ${channel.displayName}"
                        },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioAvatar(channel: RadioChannel, isSelected: Boolean) {
    Surface(
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = channel.id.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
