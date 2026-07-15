package com.hussein.mawaqit.presentation.radio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.db.entities.AudioSourceEntity
import com.hussein.mawaqit.infrastructure.services.PlaybackSource
import com.hussein.mawaqit.presentation.shared.BackButton
import com.hussein.mawaqit.presentation.shared.SyncStatus
import com.hussein.mawaqit.presentation.util.GlobalPlayerViewModel
import com.hussein.mawaqit.ui.theme.MawaqitTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RadioChannelListScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    radioViewModel: RadioViewModel = koinViewModel(),
    globalPlayerViewModel: GlobalPlayerViewModel = koinInject()
) {
    val radioUiState by radioViewModel.uiState.collectAsStateWithLifecycle()
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
            CenterAlignedTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { 
                    Text(
                        "Radio Channels",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    BackButton(onClick = onBack)
                },
                actions = {
                    if (playbackState.source is PlaybackSource.Radio) {
                        IconButton(
                            onClick = globalPlayerViewModel::stop,
                            colors = IconButtonDefaults.filledTonalIconButtonColors()
                        ) {
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
        if (radioUiState.channels.isEmpty()) {
            RadioEmptyState(
                syncStatus = radioUiState.syncStatus,
                onSyncClick = { radioViewModel.syncAudioSources() },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(radioUiState.channels, key = { it.id }) { channel ->
                    val isSelected = channel.radioStreamUrl == activeRadioUrl
                    val isPlaying = isSelected && playbackState.isPlaying
                    val isBuffering = isSelected && playbackState.isBuffering

                    RadioChannelRow(
                        channel = channel,
                        isSelected = isSelected,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        onPlayPause = {
                            val streamUrl = channel.radioStreamUrl ?: return@RadioChannelRow
                            when {
                                isPlaying || isSelected -> globalPlayerViewModel.togglePlayPause()
                                else -> globalPlayerViewModel.playRadio(
                                    stationUrl = streamUrl,
                                    title = channel.name
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RadioEmptyState(
    syncStatus: SyncStatus,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MawaqitTheme.appShapes.large,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = MawaqitTheme.appShapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_radio),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "No Radio Stations",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Sync audio sources to enjoy live radio stations and recitations from around the world.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(48.dp))

                if (syncStatus.isSyncing) {
                    SyncProgressBanner(
                        progress = syncStatus.progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Button(
                        onClick = onSyncClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = MawaqitTheme.appShapes.medium,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_download),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Sync Audio Sources",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (syncStatus.error != null) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = syncStatus.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncProgressBanner(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MawaqitTheme.appShapes.small,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Syncing Audio Sources...",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun RadioChannelRow(
    channel: AudioSourceEntity,
    isSelected: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit
) {
    ElevatedCard(
        onClick = onPlayPause,
        shape = MawaqitTheme.appShapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            supportingContent = {
                Text(
                    text = if (isPlaying) "Live now" else "Quran radio",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            leadingContent = {
                RadioAvatar(channel = channel, isSelected = isSelected)
            },
            trailingContent = {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    IconButton(
                        onClick = onPlayPause,
                        colors = if (isSelected) IconButtonDefaults.filledTonalIconButtonColors() else IconButtonDefaults.iconButtonColors()
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                when {
                                    isPlaying -> R.drawable.ic_pause
                                    isSelected -> R.drawable.ic_resume
                                    else -> R.drawable.ic_play
                                }
                            ),
                            contentDescription = when {
                                isPlaying -> "Pause ${channel.name}"
                                isSelected -> "Resume ${channel.name}"
                                else -> "Play ${channel.name}"
                            },
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun RadioAvatar(channel: AudioSourceEntity, isSelected: Boolean) {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = MawaqitTheme.appShapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = channel.id.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

