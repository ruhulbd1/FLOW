package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.model.Channel
import com.example.data.model.VideoItem
import com.example.ui.components.VideoCard
import com.example.ui.theme.FlowRed

@Composable
fun HomeScreen(
    videos: List<VideoItem>,
    channels: List<Channel> = emptyList(),
    channelIds: List<String>,
    selectedCategory: String,
    selectedChannelFilter: String?,
    savedVideoIds: Set<String>,
    unreadNotificationsCount: Int,
    isLoading: Boolean,
    onSelectCategory: (String) -> Unit,
    onSelectChannelFilter: (String?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenManageChannels: () -> Unit = onOpenSettings,
    onRefresh: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onToggleSave: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Bangla Natok", "Natok", "New to you", "Music", "Drama", "Trending")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Top Header (Top Bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // FLOW Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("app_logo_row")
            ) {
                Surface(
                    color = FlowRed,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "F",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "FLOW",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Top Icons (Refresh, Search, Bell/Notifications, Settings)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("top_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh feed",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.testTag("top_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.testTag("top_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge(
                                    containerColor = FlowRed,
                                    contentColor = Color.White
                                ) {
                                    Text("$unreadNotificationsCount")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("top_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // 2. Filter Tags / Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { tag ->
                val isSelected = tag == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCategory(tag) },
                    label = {
                        Text(
                            text = tag,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                        selectedLabelColor = MaterialTheme.colorScheme.background,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = null,
                    modifier = Modifier.testTag("filter_chip_$tag")
                )
            }
        }

        // 3. Multi-Channel Combined Feed Status & Channel Selector Bar
        val activeChannels = if (channels.isNotEmpty()) {
            channels.filter { it.isEnabledInFeed }
        } else {
            channelIds.map { Channel(id = it, name = it) }
        }

        if (activeChannels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All channels combined chip
                val isAllSelected = selectedChannelFilter == null
                Surface(
                    color = if (isAllSelected) FlowRed else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { onSelectChannelFilter(null) }
                        .testTag("filter_all_channels")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Combined (${activeChannels.size})",
                            fontSize = 11.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Individual Channel filter chips
                activeChannels.forEach { channel ->
                    val isChannelSelected = selectedChannelFilter == channel.id
                    val displayName = channel.name.take(14)

                    Surface(
                        color = if (isChannelSelected) FlowRed else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable {
                                onSelectChannelFilter(if (isChannelSelected) null else channel.id)
                            }
                            .testTag("filter_channel_${channel.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (channel.avatarUrl.isNotBlank() && !channel.avatarUrl.contains("unsplash.com")) {
                                AsyncImage(
                                    model = channel.avatarUrl,
                                    contentDescription = channel.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isChannelSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChannelSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Quick Manage Channel Feed Chip
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { onOpenManageChannels() }
                        .testTag("filter_manage_channels")
                ) {
                    Text(
                        text = "+ Channels",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlowRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 4. Main Content Feed / Combined Video List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FlowRed)
            }
        } else if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No videos found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try switching channels or resetting in Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("video_feed_list")
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        isSaved = savedVideoIds.contains(video.id),
                        onVideoClick = onVideoClick,
                        onToggleSave = onToggleSave
                    )
                }
            }
        }
    }
}
