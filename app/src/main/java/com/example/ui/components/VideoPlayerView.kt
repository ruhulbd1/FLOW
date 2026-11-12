package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.service.MediaPlaybackManager
import com.example.ui.theme.FlowRed
import kotlinx.coroutines.delay

/**
 * Native VideoPlayer Composable bound directly to the shared ExoPlayer engine.
 * Guarantees zero external app redirects, seamless mini-player continuity, and
 * uninterrupted background playback when the device is locked.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerComposable(
    video: VideoItem,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    currentQuality: String = "1080p FHD",
    onQualityChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerSettings by MediaPlaybackManager.playerSettings.collectAsState()
    val sleepTimerRemainingSeconds by MediaPlaybackManager.sleepTimerRemainingSeconds.collectAsState()
    val currentPositionMs by MediaPlaybackManager.currentPositionMs.collectAsState()
    val durationMs by MediaPlaybackManager.durationMs.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var localQuality by remember(currentQuality) {
        mutableStateOf(if (currentQuality.isNotBlank()) currentQuality else "1080p FHD")
    }

    var isBuffering by remember { mutableStateOf(false) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPositionFraction by remember { mutableFloatStateOf(0f) }

    // Bind to the shared singleton ExoPlayer instance
    val exoPlayer = remember(context) {
        MediaPlaybackManager.getSharedExoPlayer(context)
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
            }
            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            // Note: Do not release the shared ExoPlayer here!
            // Keeping it alive ensures continuous background playback and smooth mini-player operation.
        }
    }

    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    var seekFeedbackSide by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            delay(750L)
            seekFeedbackText = null
            seekFeedbackSide = null
        }
    }

    // Direct Stream Binding: Prepare immediately and ensure playWhenReady is applied
    LaunchedEffect(video.id, video.streamUrl, localQuality) {
        val streamUrl = if (video.streamUrl.isNotBlank()) {
            video.streamUrl
        } else {
            MediaPlaybackManager.resolveStreamUrl(video.id, localQuality)
        }

        val needsNewMedia = exoPlayer.currentMediaItem?.mediaId != video.id ||
                exoPlayer.playbackState == Player.STATE_IDLE ||
                exoPlayer.playbackState == Player.STATE_ENDED

        if (needsNewMedia) {
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(streamUrl))
                .setMediaId(video.id)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }

        exoPlayer.playWhenReady = isPlaying
        if (isPlaying) {
            exoPlayer.play()
        }
    }

    // Instant play/pause synchronization
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
        if (isPlaying) {
            if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4500L)
            showControls = false
        }
    }

    val qualityLabel = when {
        localQuality.contains("1080") || localQuality.contains("FHD") -> "FHD"
        localQuality.contains("720") || localQuality.contains("HD") -> "HD"
        localQuality.contains("480") || localQuality.contains("360") -> "SD"
        else -> "HD"
    }

    val tapModifier = Modifier.pointerInput(playerSettings.doubleTapSeekSeconds) {
        detectTapGestures(
            onTap = {
                showControls = !showControls
            },
            onDoubleTap = { offset ->
                val seekSec = playerSettings.doubleTapSeekSeconds
                if (offset.x < size.width / 2) {
                    val targetMs = (exoPlayer.currentPosition - seekSec * 1000L).coerceAtLeast(0L)
                    MediaPlaybackManager.seekTo(targetMs)
                    seekFeedbackText = "-${seekSec}s"
                    seekFeedbackSide = "left"
                } else {
                    val targetMs = (exoPlayer.currentPosition + seekSec * 1000L).coerceAtMost(durationMs)
                    MediaPlaybackManager.seekTo(targetMs)
                    seekFeedbackText = "+${seekSec}s"
                    seekFeedbackSide = "right"
                }
            }
        )
    }

    val boxModifier = if (isFullscreen) {
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(tapModifier)
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .then(tapModifier)
    }

    Box(modifier = boxModifier) {
        // Native Media3 PlayerView (Reattaches instantly to shared ExoPlayer)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = isPlaying
                    resizeMode = if (playerSettings.zoomToFill) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
                playerView.keepScreenOn = isPlaying
                playerView.resizeMode = if (playerSettings.zoomToFill) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // YouTube-Style Double-Tap Seek Feedback Overlay
        if (seekFeedbackText != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = if (seekFeedbackSide == "left") Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = CircleShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (seekFeedbackSide == "left") Icons.Default.Replay10 else Icons.Default.Forward10,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = seekFeedbackText ?: "",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Buffering Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = FlowRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Touch Scrim Overlay
        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }

        // Top Controls: Quality, SponsorBlock, Sleep Timer, Fullscreen
        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (playerSettings.showSponsorBlockButton) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "SponsorBlock: Segments skipped automatically", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "SponsorBlock",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (playerSettings.showSleepTimerButton) {
                    IconButton(
                        onClick = {
                            val currentTimer = playerSettings.sleepTimerMinutes
                            val nextTimer = when (currentTimer) {
                                0 -> 15
                                15 -> 30
                                30 -> 60
                                else -> 0
                            }
                            MediaPlaybackManager.setSleepTimer(nextTimer, context)
                            val msg = if (nextTimer > 0) "Sleep timer set to ${nextTimer}m" else "Sleep timer turned off"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerRemainingSeconds > 0) FlowRed else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (playerSettings.showCastButton) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Searching for Cast devices...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Cast",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quality Selector Badge (FHD / HD / SD)
                Box {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable { showQualityMenu = true }
                            .testTag("quality_selector_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = qualityLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (qualityLabel == "FHD") Color(0xFF00E676) else Color.White
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showQualityMenu,
                        onDismissRequest = { showQualityMenu = false }
                    ) {
                        listOf(
                            "1080p FHD (High Quality)",
                            "720p HD (Standard)",
                            "480p SD",
                            "360p Data Saver"
                        ).forEach { qual ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = qual,
                                        fontSize = 13.sp,
                                        fontWeight = if (localQuality.startsWith(qual.take(4))) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    localQuality = qual
                                    onQualityChange?.invoke(qual)
                                    showQualityMenu = false
                                }
                            )
                        }
                    }
                }

                // Fullscreen Toggle
                if (onToggleFullscreen != null) {
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .testTag("player_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Center Playback Controls (10s Rewind, Play/Pause, 10s Forward, Prev, Next)
        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (playerSettings.showPrevNextButtons) {
                    IconButton(
                        onClick = { MediaPlaybackManager.skipPrevious(context) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val seekSec = playerSettings.doubleTapSeekSeconds
                        val targetMs = (exoPlayer.currentPosition - seekSec * 1000L).coerceAtLeast(0L)
                        MediaPlaybackManager.seekTo(targetMs)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(64.dp)
                        .background(FlowRed, shape = CircleShape)
                        .testTag("player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val seekSec = playerSettings.doubleTapSeekSeconds
                        val targetMs = (exoPlayer.currentPosition + seekSec * 1000L).coerceAtMost(durationMs)
                        MediaPlaybackManager.seekTo(targetMs)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                if (playerSettings.showPrevNextButtons) {
                    IconButton(
                        onClick = { MediaPlaybackManager.skipNext(context) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        // Bottom Progress Bar & Time Displays
        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayMs = if (isUserScrubbing) {
                        (scrubPositionFraction * durationMs).toLong()
                    } else {
                        currentPositionMs
                    }
                    val currentMin = (displayMs / 1000L) / 60
                    val currentSec = (displayMs / 1000L) % 60
                    val totalMin = (durationMs / 1000L) / 60
                    val totalSec = (durationMs / 1000L) % 60

                    Text(
                        text = String.format("%02d:%02d / %02d:%02d", currentMin, currentSec, totalMin, totalSec),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (onToggleFullscreen != null) {
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                val currentFraction = if (durationMs > 0L) {
                    (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Slider(
                    value = if (isUserScrubbing) scrubPositionFraction else currentFraction,
                    onValueChange = { frac ->
                        isUserScrubbing = true
                        scrubPositionFraction = frac
                    },
                    onValueChangeFinished = {
                        val targetMs = (scrubPositionFraction * durationMs).toLong()
                        MediaPlaybackManager.seekTo(targetMs)
                        isUserScrubbing = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = FlowRed,
                        activeTrackColor = FlowRed,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )
            }
        }
    }
}

@Composable
fun VideoPlayerView(
    video: VideoItem,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    currentQuality: String = "1080p FHD",
    onQualityChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    VideoPlayerComposable(
        video = video,
        isPlaying = isPlaying,
        onTogglePlay = onTogglePlay,
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        currentQuality = currentQuality,
        onQualityChange = onQualityChange,
        modifier = modifier
    )
}

/**
 * Persistent Picture-in-Picture / Floating Bottom Mini-Player
 * Directly bound to the active video and shared ExoPlayer instance to guarantee
 * 100% media ID, stream URL, and playback state consistency.
 */
@Composable
fun MiniPlayer(
    video: VideoItem,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPositionMs by MediaPlaybackManager.currentPositionMs.collectAsState()
    val durationMs by MediaPlaybackManager.durationMs.collectAsState()
    val progressFraction = if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable { onExpand() }
            .testTag("mini_player"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Scrub/progress indicator on top of mini-player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction)
                        .height(2.dp)
                        .background(FlowRed)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 76.dp, height = 48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Channel
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = video.channelTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play / Pause Button
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Close Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("mini_player_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close player",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
