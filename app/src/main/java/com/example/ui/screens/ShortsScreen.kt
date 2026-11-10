package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.Comment
import com.example.data.model.VideoItem
import com.example.ui.theme.FlowRed

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    shorts: List<VideoItem>,
    currentIndex: Int,
    isPlaying: Boolean,
    isLiked: Boolean,
    isSubscribed: Boolean,
    comments: List<Comment>,
    onTogglePlay: () -> Unit,
    onToggleLike: (VideoItem) -> Unit,
    onToggleSubscribe: (String) -> Unit,
    onNextShort: () -> Unit,
    onPrevShort: () -> Unit,
    onAddComment: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (shorts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No Shorts available", color = Color.White)
        }
        return
    }

    val currentShort = shorts.getOrElse(currentIndex) { shorts.first() }
    val context = LocalContext.current
    var showCommentsSheet by remember { mutableStateOf(false) }
    var commentInput by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    val resolvedVideoId = remember(currentShort.id, currentShort.thumbnailUrl) {
        if (currentShort.id.length == 11 && !currentShort.id.startsWith("short") && !currentShort.id.startsWith("demo")) {
            currentShort.id
        } else if (currentShort.thumbnailUrl.contains("/vi/")) {
            val candidate = currentShort.thumbnailUrl.substringAfter("/vi/").substringBefore("/")
            if (candidate.length == 11) candidate else currentShort.id
        } else {
            when (currentShort.id) {
                "short1" -> "DfanjKOWW_k"
                "short2" -> "LGsd_mH1c6k"
                "short3" -> "ridzjSgy1bc"
                "short4" -> "rsOiZE19RVo"
                else -> currentShort.id
            }
        }
    }

    // Rotating music disc animation
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -35) {
                        onNextShort()
                    } else if (dragAmount > 35) {
                        onPrevShort()
                    }
                }
            }
    ) {
        // Fullscreen Active Short Video Player (Autoplay & Continuous playback)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onTogglePlay() }
        ) {
            // Immediate backdrop thumbnail preview
            AsyncImage(
                model = currentShort.thumbnailUrl,
                contentDescription = currentShort.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Autoplaying Fullscreen Video View
            if (isPlaying) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    update = { webView ->
                        val embedHtml = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                    body, html { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; }
                                    iframe { width:100%; height:100%; border:none; object-fit: cover; }
                                </style>
                            </head>
                            <body>
                                <iframe 
                                    src="https://www.youtube-nocookie.com/embed/${resolvedVideoId}?autoplay=1&playsinline=1&controls=0&loop=1&playlist=${resolvedVideoId}&rel=0&modestbranding=1"
                                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                    allowfullscreen>
                                </iframe>
                            </body>
                            </html>
                        """.trimIndent()
                        webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top & Bottom shadow gradients for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Top Bar: "Shorts" label and navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shorts",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(
                    onClick = onPrevShort,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous Short",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onNextShort,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next Short",
                        tint = Color.White
                    )
                }
            }
        }

        // Center pause overlay indicator
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(68.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // Bottom Content: Channel Details, Caption, Sound tag
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 24.dp)
        ) {
            // Channel row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = currentShort.channelAvatarUrl,
                    contentDescription = currentShort.channelTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )

                Text(
                    text = "@${currentShort.channelTitle.replace(" ", "").lowercase()}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Subscribe Pill
                Surface(
                    color = if (isSubscribed) Color.White.copy(alpha = 0.25f) else FlowRed,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { onToggleSubscribe(currentShort.channelId) }
                ) {
                    Text(
                        text = if (isSubscribed) "Subscribed" else "Subscribe",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption
            Text(
                text = currentShort.title,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 3,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Audio track indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Original Audio • ${currentShort.channelTitle}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            }
        }

        // Right Action Column (Like, Dislike, Comment, Share, Music Disc)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { onToggleLike(currentShort) },
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .testTag("shorts_like_button")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) FlowRed else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = if (isLiked) "${(currentShort.likes + 1) / 1000}k" else "${currentShort.likes / 1000}k",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Dislike
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Dislike",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("Dislike", color = Color.White, fontSize = 11.sp)
            }

            // Comment
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { showCommentsSheet = true },
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .testTag("shorts_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Comments",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("${comments.size * 340}", color = Color.White, fontSize = 12.sp)
            }

            // Share
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Watch this short on FLOW: https://youtu.be/${currentShort.id}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Short"))
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("Share", color = Color.White, fontSize = 11.sp)
            }

            // Spinning Music Disc
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .rotate(discRotation)
                    .background(Color(0xFF222222), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = currentShort.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                )
            }
        }
    }

    // Comments Bottom Sheet for Shorts
    if (showCommentsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Comments (${comments.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(comments) { comment ->
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
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${comment.author} • ${comment.timeAgo}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = comment.text,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        placeholder = { Text("Add a comment...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (commentInput.isNotBlank()) {
                                onAddComment(currentShort.id, commentInput)
                                commentInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FlowRed)
                    ) {
                        Text("Post")
                    }
                }
            }
        }
    }
}
