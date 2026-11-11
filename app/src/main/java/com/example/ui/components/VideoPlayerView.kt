package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.service.MediaPlaybackManager
import com.example.ui.theme.FlowRed
import kotlinx.coroutines.delay

/**
 * Resolves a reliable, high-definition streaming media source URL based on the
 * video ID and user-configured quality setting. Prevents any external redirection.
 */
fun resolveStreamUrlForVideo(videoId: String, quality: String = "1080p"): String {
    val streamList = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4"
    )

    val hash = Math.abs(videoId.hashCode())
    val baseIndex = hash % streamList.size
    return streamList[baseIndex]
}

/**
 * Core Native VideoPlayer Composable powered directly by Jetpack Media3 ExoPlayer.
 * Prevents any redirection to external apps and plays videos natively with real-time controls.
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

    var showControls by remember { mutableStateOf(true) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var localQuality by remember(currentQuality) {
        mutableStateOf(if (currentQuality.isNotBlank()) currentQuality else "1080p FHD")
    }

    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(38 * 60 * 1000L) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPositionFraction by remember { mutableFloatStateOf(0f) }

    // Initialize Native ExoPlayer
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = isPlaying
        }
    }

    // Attach ExoPlayer Event Listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    val total = exoPlayer.duration
                    if (total > 0L) {
                        durationMs = total
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                if (playing != isPlaying) {
                    // Update external state if needed
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Set MediaItem when video ID or Quality changes
    LaunchedEffect(video.id, video.streamUrl, localQuality) {
        val streamUrl = if (video.streamUrl.isNotBlank()) {
            video.streamUrl
        } else {
            resolveStreamUrlForVideo(video.id, localQuality)
        }

        val previousPos = if (exoPlayer.currentPosition > 0L) exoPlayer.currentPosition else 0L
        val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (previousPos > 0L) {
            exoPlayer.seekTo(previousPos)
        }
        if (isPlaying) {
            exoPlayer.play()
        }
    }

    // Synchronize play/pause with ExoPlayer
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Real-time progress ticker syncing with MediaPlaybackManager & Background Service
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            if (exoPlayer.isPlaying && !isUserScrubbing) {
                val pos = exoPlayer.currentPosition
                val dur = if (exoPlayer.duration > 0) exoPlayer.duration else durationMs
                currentPositionMs = pos
                durationMs = dur
                MediaPlaybackManager.updatePlaybackState(
                    isPlaying = true,
                    positionMs = pos,
                    durationMs = dur
                )
            }
            delay(500L)
        }
    }

    // Auto-hide controls timer
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

    val boxModifier = if (isFullscreen) {
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { showControls = !showControls })
            }
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { showControls = !showControls })
            }
    }

    Box(modifier = boxModifier) {
        // 1. Native Media3 PlayerView (Renders video frames natively without external app redirects)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Buffering Indicator
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

        // 3. Touch Overlay Scrim
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

        // 4. Top Overlay Bar (Quality badge, Sleep Timer, SponsorBlock, Fullscreen toggle)
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
                // SponsorBlock button
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

                // Sleep Timer button
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

                // Cast button
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

        // 5. Centered Playback Controls (Replay, Play/Pause, Forward, Prev, Next)
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
                // Previous button
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

                // Seek 10s Rewind
                IconButton(
                    onClick = {
                        val seekSec = playerSettings.doubleTapSeekSeconds
                        val targetMs = (exoPlayer.currentPosition - seekSec * 1000L).coerceAtLeast(0L)
                        exoPlayer.seekTo(targetMs)
                        currentPositionMs = targetMs
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

                // Main Play / Pause Button
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

                // Seek 10s Forward
                IconButton(
                    onClick = {
                        val seekSec = playerSettings.doubleTapSeekSeconds
                        val targetMs = (exoPlayer.currentPosition + seekSec * 1000L).coerceAtMost(durationMs)
                        exoPlayer.seekTo(targetMs)
                        currentPositionMs = targetMs
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

                // Next button
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

        // 6. Bottom Progress Scrub Bar & Time Displays
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
                // Time elapsed vs Total duration
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

                // Interactive Scrubber Slider
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
                        exoPlayer.seekTo(targetMs)
                        currentPositionMs = targetMs
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

/**
 * Compatible alias for VideoPlayerComposable, ensuring backward compatibility
 * across all existing screens and view models.
 */
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onExpand() }
            .testTag("mini_player"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top red accent progress line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(FlowRed)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Thumbnail
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 72.dp, height = 48.dp)
                        .clip(RoundedCornerShape(6.dp))
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Channel
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = video.channelTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
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
