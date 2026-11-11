package com.example.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbDownOffAlt
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.api.YouTubeRssService
import com.example.data.model.Comment
import com.example.data.model.VideoItem
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoPlayerComposable
import com.example.ui.theme.FlowRed

/**
 * Dedicated In-App Video Player Screen (PlayerScreen.kt).
 * Hosts the native ExoPlayer component (VideoPlayerComposable) and prevents any
 * unwanted redirects to external apps or browsers.
 */
@Composable
fun PlayerScreen(
    video: VideoItem,
    isPlaying: Boolean,
    defaultQuality: String = "1080p",
    isLiked: Boolean,
    isSaved: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    isSubscribed: Boolean,
    comments: List<Comment>,
    relatedVideos: List<VideoItem>,
    onTogglePlay: () -> Unit,
    onCollapse: () -> Unit,
    onToggleLike: (VideoItem) -> Unit,
    onToggleSave: (VideoItem) -> Unit,
    onStartDownload: (VideoItem, String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onToggleSubscribe: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onSelectRelatedVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var selectedQuality by remember(defaultQuality) {
        mutableStateOf(if (defaultQuality.isNotBlank()) defaultQuality else "1080p FHD")
    }

    // Helper to find host Activity for orientation control
    fun findActivity(): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    val activity = remember(context) { findActivity() }

    // Toggle fullscreen and adjust screen orientation & status/navigation bars
    fun toggleFullscreen() {
        val next = !isFullscreen
        isFullscreen = next
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (next) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val window = act.window
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        if (isFullscreen) {
            toggleFullscreen()
        } else {
            onCollapse()
        }
    }

    if (isFullscreen) {
        // Fullscreen mode - native ExoPlayer fills entire landscape display
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayerComposable(
                video = video,
                isPlaying = isPlaying,
                onTogglePlay = onTogglePlay,
                isFullscreen = true,
                onToggleFullscreen = { toggleFullscreen() },
                currentQuality = selectedQuality,
                onQualityChange = { selectedQuality = it },
                modifier = Modifier.fillMaxSize()
            )

            // Exit fullscreen back button on top-left
            IconButton(
                onClick = { toggleFullscreen() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Exit Fullscreen",
                    tint = Color.White
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Player Area with Collapse Button Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                VideoPlayerComposable(
                    video = video,
                    isPlaying = isPlaying,
                    onTogglePlay = onTogglePlay,
                    isFullscreen = false,
                    onToggleFullscreen = { toggleFullscreen() },
                    currentQuality = selectedQuality,
                    onQualityChange = { selectedQuality = it }
                )

                // Collapse to MiniPlayer button in top-left
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .testTag("collapse_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize player",
                        tint = Color.White
                    )
                }
            }

            // Scrollable Details, Actions, Comments, and Related Videos
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Video Title
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Metadata: Views, time ago, category
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${video.viewCount} • ${video.publishedAt} • #${video.category.replace(" ", "")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Background Audio Playback Banner Badge
                    Surface(
                        color = FlowRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("background_audio_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Background Audio",
                                tint = FlowRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Background Audio Active • Keeps playing when screen locks",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FlowRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Channel Info Bar & Subscribe Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val effectiveAvatar = when {
                                video.channelAvatarUrl.isNotBlank() && !video.channelAvatarUrl.contains("unsplash.com") -> video.channelAvatarUrl.trim()
                                else -> YouTubeRssService.KNOWN_REAL_AVATARS[video.channelId] ?: ""
                            }
                            val hasValidAvatar = effectiveAvatar.isNotBlank() && !effectiveAvatar.contains("unsplash.com")
                            if (hasValidAvatar) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(effectiveAvatar)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = video.channelTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape),
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(FlowRed),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = video.channelTitle.firstOrNull()?.toString() ?: "F",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    error = {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = video.channelTitle.firstOrNull()?.toString() ?: "F",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(FlowRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = video.channelTitle.firstOrNull()?.toString() ?: "F",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = video.channelTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "Official Artist Channel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Subscribe / Subscribed Button
                        Button(
                            onClick = { onToggleSubscribe(video.channelId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else FlowRed,
                                contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("subscribe_button")
                        ) {
                            Text(
                                text = if (isSubscribed) "Subscribed" else "Subscribe",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Bar (Like, Dislike, Share, Download, Save)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like & Dislike Pill
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    modifier = Modifier
                                        .clickable { onToggleLike(video) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                        contentDescription = "Like",
                                        tint = if (isLiked) FlowRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val likeCountDisplay = if (isLiked) "42.6K" else "42.5K"
                                    Text(
                                        text = likeCountDisplay,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isLiked) FlowRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                )

                                Row(
                                    modifier = Modifier
                                        .clickable { isDisliked = !isDisliked }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                                        contentDescription = "Dislike",
                                        tint = if (isDisliked) FlowRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Share Pill
                        PlayerActionPill(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Watch '${video.title}' on FLOW: https://youtu.be/${video.id}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                            }
                        )

                        // Download Pill
                        val isCurrentlyDownloading = downloadProgress != null && downloadProgress < 1f
                        PlayerActionPill(
                            icon = when {
                                isDownloaded -> Icons.Default.DownloadDone
                                isCurrentlyDownloading -> Icons.Default.Download
                                else -> Icons.Default.Download
                            },
                            label = when {
                                isDownloaded -> "Downloaded"
                                isCurrentlyDownloading -> "${(downloadProgress * 100).toInt()}%"
                                else -> "Download"
                            },
                            tint = if (isDownloaded) FlowRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                if (isDownloaded) {
                                    onRemoveDownload(video.id)
                                    Toast.makeText(context, "Removed from Downloads", Toast.LENGTH_SHORT).show()
                                } else {
                                    showDownloadDialog = true
                                }
                            }
                        )

                        // Save Pill
                        PlayerActionPill(
                            icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            label = if (isSaved) "Saved" else "Save",
                            tint = if (isSaved) FlowRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { onToggleSave(video) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Expandable Description Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDescriptionExpanded = !isDescriptionExpanded },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isDescriptionExpanded) "Show less" else "Show more",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FlowRed
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val fullDesc = if (video.description.isNotBlank()) {
                                video.description
                            } else {
                                "Enjoy this special presentation from ${video.channelTitle}. Streamed in crisp native quality on the FLOW platform with continuous background playback."
                            }
                            Text(
                                text = fullDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isDescriptionExpanded) 20 else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Comments Section Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comments_section_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Comments",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${comments.size}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Scrollable comments list strictly constrained
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                            ) {
                                if (comments.isEmpty()) {
                                    Text(
                                        text = "No comments yet. Be the first to comment!",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                        items(comments, key = { it.id }) { comment ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(FlowRed),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = comment.author.firstOrNull()?.toString() ?: "U",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = comment.author,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = comment.timeAgo,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = comment.text,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Add Comment Input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newCommentText,
                                    onValueChange = { newCommentText = it },
                                    placeholder = { Text("Add a comment...", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("add_comment_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        if (newCommentText.isNotBlank()) {
                                            onAddComment(video.id, newCommentText.trim())
                                            newCommentText = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(FlowRed, CircleShape)
                                        .testTag("submit_comment_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Comment",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Related Videos List
                val filteredRelated = relatedVideos.filter { it.id != video.id }
                items(filteredRelated, key = { it.id }) { relatedVideo ->
                    VideoCard(
                        video = relatedVideo,
                        isSaved = isSaved,
                        onVideoClick = { onSelectRelatedVideo(relatedVideo) },
                        onToggleSave = onToggleSave,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
        }
    }

    // In-App Download Quality Modal Dialog
    if (showDownloadDialog) {
        var qualityToDownload by remember { mutableStateOf("1080p FHD") }
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = {
                Text(
                    text = "Download Video",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose download resolution for offline playback inside FLOW:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        "1080p FHD" to "High Definition (~140 MB)",
                        "720p HD" to "Standard Definition (~65 MB)",
                        "480p SD" to "Medium Quality (~35 MB)",
                        "360p Data Saver" to "Low Data Usage (~18 MB)"
                    ).forEach { (qual, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { qualityToDownload = qual }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = qualityToDownload == qual,
                                onClick = { qualityToDownload = qual }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(qual, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadDialog = false
                        onStartDownload(video, qualityToDownload)
                        Toast.makeText(context, "Download started ($qualityToDownload)", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRed)
                ) {
                    Text("Download", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlayerActionPill(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .height(36.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
    }
}
