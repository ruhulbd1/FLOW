package com.example.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.api.YouTubeRssService
import com.example.data.model.Comment
import com.example.data.model.VideoItem
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.FlowRed

@Composable
fun VideoDetailSheet(
    video: VideoItem,
    isPlaying: Boolean,
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
    BackHandler {
        onCollapse()
    }

    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf("720p HD") }

    // Helper to find host Activity
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

    androidx.compose.runtime.DisposableEffect(Unit) {
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
        // Fullscreen mode - player fills entire landscape display without sheet distractions
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayerView(
                video = video,
                isPlaying = isPlaying,
                onTogglePlay = onTogglePlay,
                isFullscreen = true,
                onToggleFullscreen = { toggleFullscreen() },
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
                VideoPlayerView(
                    video = video,
                    isPlaying = isPlaying,
                    onTogglePlay = onTogglePlay,
                    isFullscreen = false,
                    onToggleFullscreen = { toggleFullscreen() }
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
                                            .background(FlowRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = video.channelTitle.firstOrNull()?.toString() ?: "F",
                                            color = Color.White,
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
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = video.channelTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "2.4M subscribers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Subscribe Button
                    if (isSubscribed) {
                        OutlinedButton(
                            onClick = { onToggleSubscribe(video.channelId) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("subscribe_button")
                        ) {
                            Text("Subscribed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = { onToggleSubscribe(video.channelId) },
                            colors = ButtonDefaults.buttonColors(containerColor = FlowRed),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("subscribe_button")
                        ) {
                            Text("Subscribe", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar (Like, Dislike, Share, Save, Active Download)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button
                    ActionButton(
                        icon = if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                        label = if (isLiked) "${(video.likes + 1)}" else "${video.likes}",
                        active = isLiked,
                        onClick = { onToggleLike(video) },
                        testTag = "like_button"
                    )

                    // Dislike button
                    ActionButton(
                        icon = if (isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                        label = "Dislike",
                        active = isDisliked,
                        onClick = { isDisliked = !isDisliked },
                        testTag = "dislike_button"
                    )

                    // Share button
                    ActionButton(
                        icon = Icons.Default.Share,
                        label = "Share",
                        active = false,
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Watch ${video.title} on FLOW: https://youtu.be/${video.id}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                        },
                        testTag = "share_button"
                    )

                    // Save / Watch Later
                    ActionButton(
                        icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        label = if (isSaved) "Saved" else "Save",
                        active = isSaved,
                        onClick = { onToggleSave(video) },
                        testTag = "save_button"
                    )

                    // Active Download Button with State & Progress
                    if (downloadProgress != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(22.dp),
                                color = FlowRed,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = FlowRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        ActionButton(
                            icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                            label = if (isDownloaded) "Downloaded" else "Download",
                            active = isDownloaded,
                            onClick = { showDownloadDialog = true },
                            testTag = "download_button"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Expandable Description Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDescriptionExpanded = !isDescriptionExpanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isDescriptionExpanded) "Description & Cast" else "Tap for synopsis & credits...",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isDescriptionExpanded) {
                                "${video.description}\n\nDirector: Mohin Khan\nCast: Niloy Alamgir, Jannatul Sumaiya Heme\nMusic: Soundtek\nProducer: Deepto TV\n\nEnjoy and stay connected with FLOW for the latest updates!"
                            } else {
                                video.description.take(110) + "..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bounded Comments Card Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("comments_container_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Comments Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Comments (${comments.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add comment input box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                placeholder = { Text("Add a comment...", fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("comment_input_field"),
                                shape = RoundedCornerShape(20.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (newCommentText.isNotBlank()) {
                                        onAddComment(video.id, newCommentText)
                                        newCommentText = ""
                                    }
                                },
                                modifier = Modifier
                                    .background(FlowRed, CircleShape)
                                    .size(42.dp)
                                    .testTag("submit_comment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Post comment",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Render bounded comments inside the card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (comments.isEmpty()) {
                                Text(
                                    text = "No comments yet. Be the first to comment!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                comments.forEach { comment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        AsyncImage(
                                            model = comment.avatarUrl,
                                            contentDescription = comment.author,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = comment.author,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = comment.timeAgo,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = comment.text,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Up Next & Related",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Related Videos
            items(relatedVideos.filter { it.id != video.id }, key = { it.id }) { relVideo ->
                VideoCard(
                    video = relVideo,
                    isSaved = false,
                    onVideoClick = { onSelectRelatedVideo(it) },
                    onToggleSave = onToggleSave
                )
            }
        }
    }

    // --- DOWNLOAD OPTIONS DIALOG ---
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                        contentDescription = null,
                        tint = FlowRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isDownloaded) "Downloaded Video" else "Download & Offline")
                }
            },
            text = {
                Column {
                    Text(
                        text = video.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select an option to save for offline watching or launch externally:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isDownloaded) {
                        Surface(
                            color = FlowRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FlowRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Available in your offline cache", fontSize = 12.sp, color = FlowRed, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Quality Selection Radios
                    Text("Select Cache Quality:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))

                    listOf(
                        Pair("1080p FHD", "High Quality • ~85 MB"),
                        Pair("720p HD", "Standard (Recommended) • ~42 MB"),
                        Pair("360p Data Saver", "Low Quality • ~18 MB")
                    ).forEach { (qual, desc) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedQuality = qual }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedQuality == qual,
                                onClick = { selectedQuality = qual }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(qual, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // External YouTube Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDownloadDialog = false
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.id}"))
                                context.startActivity(webIntent)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = FlowRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Open in YouTube App / Web", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Watch & download using official YouTube app", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadDialog = false
                        onStartDownload(video, selectedQuality)
                        Toast.makeText(context, "Download started in local cache ($selectedQuality)", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRed)
                ) {
                    Text(if (isDownloaded) "Re-download" else "Download to Cache")
                }
            },
            dismissButton = {
                if (isDownloaded) {
                    TextButton(
                        onClick = {
                            showDownloadDialog = false
                            onRemoveDownload(video.id)
                            Toast.makeText(context, "Video removed from cache", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Delete Cache", color = Color.Gray)
                    }
                } else {
                    TextButton(onClick = { showDownloadDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) FlowRed else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (active) FlowRed else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}
