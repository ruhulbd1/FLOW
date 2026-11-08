package com.example.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.ui.theme.FlowRed
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerView(
    video: VideoItem,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var progress by remember(video.id) { mutableFloatStateOf(0.12f) }
    var showControls by remember { mutableStateOf(false) }

    // Simulate playback timer progress when playing
    LaunchedEffect(isPlaying, video.id) {
        while (isPlaying) {
            delay(1000)
            progress = (progress + 0.01f).coerceAtMost(1f)
            if (progress >= 1f) progress = 0f
        }
    }

    val boxModifier = if (isFullscreen) {
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .clickable { showControls = !showControls }
    }

    Box(modifier = boxModifier) {
        // If it's a real YouTube ID (usually 11 chars or from API), load WebView Embed
        if (video.id.length == 11 && !video.id.startsWith("demo")) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()

                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                    body, html { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; }
                                    iframe { width:100%; height:100%; border:none; }
                                </style>
                            </head>
                            <body>
                                <iframe 
                                    src="https://www.youtube-nocookie.com/embed/${video.id}?autoplay=1&playsinline=1&controls=1&rel=0"
                                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                    allowfullscreen>
                                </iframe>
                            </body>
                            </html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Fullscreen toggle overlay button for webview
            if (onToggleFullscreen != null) {
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .testTag("fullscreen_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                        tint = Color.White
                    )
                }
            }
        } else {
            // High fidelity interactive player with cover art and controls
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark semi-transparent scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (showControls || !isPlaying) 0.55f else 0.15f))
            )

            // Top right fullscreen button
            if (onToggleFullscreen != null) {
                AnimatedVisibility(
                    visible = showControls || !isPlaying,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .testTag("player_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }

            // Centered playback controls overlay
            AnimatedVisibility(
                visible = showControls || !isPlaying,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { progress = (progress - 0.05f).coerceAtLeast(0f) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(64.dp)
                            .background(FlowRed, shape = RoundedCornerShape(32.dp))
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
                        onClick = { progress = (progress + 0.05f).coerceAtMost(1f) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Bottom scrub / progress bar and time indicator
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentMin = ((progress * 38).toInt())
                    val currentSec = (((progress * 38 * 60) % 60).toInt())
                    Text(
                        text = String.format("%02d:%02d", currentMin, currentSec),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = video.duration,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (onToggleFullscreen != null) {
                            Spacer(modifier = Modifier.width(8.dp))
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
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = FlowRed,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

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
            LinearProgressIndicator(
                progress = { 0.35f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = FlowRed,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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
