package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.api.YouTubeRssService
import com.example.data.model.VideoItem
import com.example.ui.theme.BadgeBlackTransparent
import com.example.ui.theme.FlowRed

@Composable
fun VideoCard(
    video: VideoItem,
    isSaved: Boolean,
    onVideoClick: (VideoItem) -> Unit,
    onToggleSave: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    onDownloadClick: ((VideoItem) -> Unit)? = null
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_card_${video.id}")
            .clickable { onVideoClick(video) }
            .padding(bottom = 16.dp)
    ) {
        // 1. YouTube-style 16:9 Thumbnail with Duration & New Upload Tag
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.thumbnailUrl.trim().ifBlank { "https://i.ytimg.com/vi/${video.id}/hq720.jpg" })
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = FlowRed,
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            )

            // Duration badge in bottom-right corner
            Surface(
                color = BadgeBlackTransparent,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = video.duration,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                )
            }

            // Real-time newly uploaded indicator badge in top-left corner
            if (video.isNewUpload) {
                Surface(
                    color = FlowRed,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NEW UPLOAD",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. YouTube-style Details Row: Real Channel Avatar + Title & Dynamic Metadata + 3-Dot Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Real YouTube Channel Avatar (dynamic with Coil and graceful letter fallback)
            val effectiveAvatar = when {
                video.channelAvatarUrl.isNotBlank() && !video.channelAvatarUrl.contains("unsplash.com") -> video.channelAvatarUrl.trim()
                else -> YouTubeRssService.KNOWN_REAL_AVATARS[video.channelId] ?: ""
            }
            val hasValidAvatar = effectiveAvatar.isNotBlank() && !effectiveAvatar.contains("unsplash.com")
            if (hasValidAvatar) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(effectiveAvatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.channelTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    loading = {
                        ChannelInitialAvatar(video.channelTitle)
                    },
                    error = {
                        ChannelInitialAvatar(video.channelTitle)
                    }
                )
            } else {
                ChannelInitialAvatar(video.channelTitle)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title and Dynamic Channel Sub-text (Channel Name • Views • Timestamp)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        lineHeight = 19.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${video.channelTitle} • ${video.viewCount} • ${video.publishedAt}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3-dot More Options Menu with 48dp Touch Target
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("more_options_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (isSaved) "Remove from Watch Later" else "Save to Watch Later")
                        },
                        leadingIcon = {
                            Icon(
                                if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isSaved) FlowRed else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onToggleSave(video)
                            showMenu = false
                            Toast.makeText(
                                context,
                                if (isSaved) "Removed from Watch Later" else "Saved to Watch Later",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Watch '${video.title}' on FLOW: https://youtu.be/${video.id}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Link") },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Video URL", "https://youtu.be/${video.id}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                    if (onDownloadClick != null) {
                        DropdownMenuItem(
                            text = { Text("Download") },
                            leadingIcon = {
                                Icon(Icons.Default.Download, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onDownloadClick(video)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelInitialAvatar(
    channelTitle: String,
    modifier: Modifier = Modifier
) {
    val initial = channelTitle.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "Y"
    val avatarColors = listOf(
        Color(0xFFE53935), Color(0xFFD81B60), Color(0xFF8E24AA),
        Color(0xFF5E35B1), Color(0xFF3949AB), Color(0xFF1E88E5),
        Color(0xFF00897B), Color(0xFF43A047), Color(0xFFF4511E)
    )
    val colorIndex = Math.abs(channelTitle.hashCode()) % avatarColors.size
    val bgColor = avatarColors[colorIndex]

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
