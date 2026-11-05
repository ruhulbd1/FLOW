package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.NotificationsSheet
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShortsScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.screens.VideoDetailSheet
import com.example.ui.theme.FlowRed
import com.example.ui.theme.FlowTheme
import com.example.ui.viewmodel.FlowViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            // Real-time toast feedback on settings and download actions
            LaunchedEffect(uiState.userFeedbackMessage) {
                uiState.userFeedbackMessage?.let { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearFeedbackMessage()
                }
            }

            // Real-time theme determination based on user choice
            val isSystemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (uiState.themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDark
            }

            FlowTheme(darkTheme = isDarkTheme) {
                // Back button handling for secondary states
                BackHandler(
                    enabled = uiState.isPlayerExpanded ||
                            uiState.isSearchOpen ||
                            uiState.isSettingsOpen ||
                            uiState.activeTab != "Home"
                ) {
                    when {
                        uiState.isPlayerExpanded -> viewModel.collapsePlayer()
                        uiState.isSearchOpen -> viewModel.closeSearch()
                        uiState.isSettingsOpen -> viewModel.closeSettings()
                        uiState.activeTab != "Home" -> viewModel.selectTab("Home")
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        // Hide bottom navigation only if the full video player is expanded
                        if (!uiState.isPlayerExpanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Floating Mini Player above bottom bar if video is active
                                AnimatedVisibility(
                                    visible = uiState.activeVideo != null && uiState.activeTab != "Shorts",
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                ) {
                                    uiState.activeVideo?.let { activeVideo ->
                                        MiniPlayer(
                                            video = activeVideo,
                                            isPlaying = uiState.isPlaying,
                                            onTogglePlay = { viewModel.togglePlayPause() },
                                            onExpand = { viewModel.expandPlayer() },
                                            onClose = { viewModel.closePlayer() }
                                        )
                                    }
                                }

                                // YouTube-Style 5-Tab Navigation Bar
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    windowInsets = WindowInsets.navigationBars,
                                    tonalElevation = 4.dp,
                                    modifier = Modifier.testTag("bottom_nav_bar")
                                ) {
                                    val tabs = listOf(
                                        Triple("Home", Icons.Filled.Home, Icons.Outlined.Home),
                                        Triple("Shorts", Icons.Filled.PlayCircleOutline, Icons.Outlined.PlayCircleOutline),
                                        Triple("Music", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
                                        Triple("Subs", Icons.Filled.Subscriptions, Icons.Outlined.Subscriptions),
                                        Triple("Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary)
                                    )

                                    tabs.forEach { (title, selectedIcon, unselectedIcon) ->
                                        val isSelected = uiState.activeTab == title
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = { viewModel.selectTab(title) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                                    contentDescription = title
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = title,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = FlowRed,
                                                selectedTextColor = FlowRed,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                indicatorColor = Color.Transparent
                                            ),
                                            modifier = Modifier.testTag("nav_tab_$title")
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Main Tabs Routing
                        when (uiState.activeTab) {
                            "Home" -> {
                                HomeScreen(
                                    videos = uiState.videos,
                                    channels = uiState.channels,
                                    channelIds = uiState.channelIds,
                                    selectedCategory = uiState.selectedCategory,
                                    selectedChannelFilter = uiState.selectedChannelFilter,
                                    savedVideoIds = uiState.savedVideoIds,
                                    unreadNotificationsCount = if (uiState.notificationsEnabled) uiState.notifications.count { !it.isRead } else 0,
                                    isLoading = uiState.isLoading,
                                    isAutoSyncEnabled = uiState.isAutoSyncEnabled,
                                    isAutoSyncing = uiState.isAutoSyncing,
                                    lastSyncTimestamp = uiState.lastSyncTimestamp,
                                    newUploadBannerMessage = uiState.newUploadBannerMessage,
                                    onDismissNewUploadBanner = { viewModel.dismissNewUploadBanner() },
                                    onSimulateNewUpload = { viewModel.simulateNewUploadNow() },
                                    onSelectCategory = { viewModel.selectCategory(it) },
                                    onSelectChannelFilter = { viewModel.selectChannelFilter(it) },
                                    onOpenSearch = { viewModel.openSearch() },
                                    onOpenNotifications = { viewModel.openNotifications() },
                                    onOpenSettings = { viewModel.openSettings() },
                                    onOpenManageChannels = { viewModel.selectTab("Subs") },
                                    onRefresh = { viewModel.fetchVideos() },
                                    onVideoClick = { viewModel.openVideo(it) },
                                    onToggleSave = { viewModel.toggleSave(it) }
                                )
                            }
                            "Shorts" -> {
                                ShortsScreen(
                                    shorts = uiState.shorts,
                                    currentIndex = uiState.currentShortIndex,
                                    isPlaying = uiState.isPlaying,
                                    isLiked = uiState.activeVideo?.let { uiState.likedVideoIds.contains(it.id) } ?: false,
                                    isSubscribed = uiState.activeVideo?.let { uiState.subscribedChannelIds.contains(it.channelId) } ?: false,
                                    comments = uiState.activeVideo?.let { uiState.videoComments[it.id] } ?: emptyList(),
                                    onTogglePlay = { viewModel.togglePlayPause() },
                                    onToggleLike = { viewModel.toggleLike(it) },
                                    onToggleSubscribe = { viewModel.toggleSubscribe(it) },
                                    onNextShort = { viewModel.nextShort() },
                                    onPrevShort = { viewModel.prevShort() },
                                    onAddComment = { videoId, text -> viewModel.addComment(videoId, text) }
                                )
                            }
                            "Music" -> {
                                MusicScreen(
                                    musicTracks = uiState.musicTracks,
                                    playlists = uiState.playlists,
                                    onTrackClick = { viewModel.openVideo(it) }
                                )
                            }
                            "Subs" -> {
                                SubscriptionsScreen(
                                    channels = uiState.channels,
                                    subscribedChannelIds = uiState.subscribedChannelIds,
                                    videos = uiState.videos,
                                    savedVideoIds = uiState.savedVideoIds,
                                    onToggleSubscribe = { viewModel.toggleSubscribe(it) },
                                    onAddChannel = { viewModel.addChannel(it) },
                                    onToggleChannelInFeed = { id, enabled -> viewModel.toggleChannelInFeed(id, enabled) },
                                    onRemoveChannel = { viewModel.removeChannel(it) },
                                    onResetChannelsToDefault = { viewModel.resetChannelsToDefault() },
                                    onVideoClick = { viewModel.openVideo(it) },
                                    onToggleSave = { viewModel.toggleSave(it) }
                                )
                            }
                            "Library" -> {
                                LibraryScreen(
                                    videos = uiState.videos,
                                    historyVideoIds = uiState.historyVideoIds,
                                    savedVideoIds = uiState.savedVideoIds,
                                    likedVideoIds = uiState.likedVideoIds,
                                    downloadedVideoIds = uiState.downloadedVideoIds,
                                    saveWatchHistoryEnabled = uiState.saveWatchHistory,
                                    playlists = uiState.playlists,
                                    onVideoClick = { viewModel.openVideo(it) },
                                    onOpenSettings = { viewModel.openSettings() },
                                    onToggleSave = { viewModel.toggleSave(it) }
                                )
                            }
                        }

                        // Search Overlay View
                        if (uiState.isSearchOpen) {
                            SearchScreen(
                                searchQuery = uiState.searchQuery,
                                videos = uiState.videos,
                                savedVideoIds = uiState.savedVideoIds,
                                onQueryChange = { viewModel.setSearchQuery(it) },
                                onCloseSearch = { viewModel.closeSearch() },
                                onVideoClick = { viewModel.openVideo(it) },
                                onToggleSave = { viewModel.toggleSave(it) }
                            )
                        }

                        // Settings Overlay View
                        if (uiState.isSettingsOpen) {
                            SettingsScreen(
                                currentApiKey = uiState.apiKey,
                                channelIds = uiState.channelIds,
                                channels = uiState.channels,
                                themeMode = uiState.themeMode,
                                saveWatchHistory = uiState.saveWatchHistory,
                                deepFlowMode = uiState.deepFlowMode,
                                autoplay = uiState.autoplay,
                                videoQuality = uiState.videoQuality,
                                languageRegion = uiState.languageRegion,
                                storageLocation = uiState.storageLocation,
                                blockedTopics = uiState.blockedTopics,
                                notificationsEnabled = uiState.notificationsEnabled,
                                isAutoSyncEnabled = uiState.isAutoSyncEnabled,
                                autoSyncIntervalSeconds = uiState.autoSyncIntervalSeconds,
                                statusMessage = uiState.apiStatusMessage,
                                isUpdateModalOpen = uiState.isUpdateModalOpen,
                                backupExportJson = uiState.backupExportJson,
                                onSaveApiSettings = { key, channels -> viewModel.saveApiSettings(key, channels) },
                                onAddChannel = { viewModel.addChannel(it) },
                                onToggleChannelInFeed = { id, enabled -> viewModel.toggleChannelInFeed(id, enabled) },
                                onRemoveChannel = { viewModel.removeChannel(it) },
                                onResetChannelsToDefault = { viewModel.resetChannelsToDefault() },
                                onSetAutoSyncEnabled = { viewModel.setAutoSyncEnabled(it) },
                                onSetAutoSyncInterval = { viewModel.setAutoSyncInterval(it) },
                                onCheckNewUploadsNow = { viewModel.checkRealTimeUploads() },
                                onSimulateNewUpload = { viewModel.simulateNewUploadNow() },
                                onResetToDemo = { viewModel.resetToDemo() },
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onSetSaveWatchHistory = { viewModel.setSaveWatchHistory(it) },
                                onSetDeepFlowMode = { viewModel.setDeepFlowMode(it) },
                                onSetAutoplay = { viewModel.setAutoplay(it) },
                                onSetVideoQuality = { viewModel.setVideoQuality(it) },
                                onSetLanguageRegion = { viewModel.setLanguageRegion(it) },
                                onSetStorageLocation = { viewModel.setStorageLocation(it) },
                                onSetNotificationsEnabled = { viewModel.setNotificationsEnabled(it) },
                                onAddBlockedTopic = { viewModel.addBlockedTopic(it) },
                                onRemoveBlockedTopic = { viewModel.removeBlockedTopic(it) },
                                onClearCacheAndHistory = { viewModel.clearCacheAndHistory() },
                                onExportBackup = { viewModel.exportBackup() },
                                onRestoreBackup = { viewModel.restoreBackup(it) },
                                onOpenUpdateModal = { viewModel.openUpdateModal() },
                                onCloseUpdateModal = { viewModel.closeUpdateModal() },
                                onCloseSettings = { viewModel.closeSettings() }
                            )
                        }

                        // Notifications Sheet
                        if (uiState.isNotificationsOpen) {
                            NotificationsSheet(
                                notifications = uiState.notifications,
                                onMarkAllRead = { viewModel.markNotificationsRead() },
                                onDismiss = { viewModel.closeNotifications() }
                            )
                        }

                        // Full Video Player Sheet
                        if (uiState.isPlayerExpanded && uiState.activeVideo != null) {
                            val activeVideo = uiState.activeVideo!!
                            VideoDetailSheet(
                                video = activeVideo,
                                isPlaying = uiState.isPlaying,
                                isLiked = uiState.likedVideoIds.contains(activeVideo.id),
                                isSaved = uiState.savedVideoIds.contains(activeVideo.id),
                                isDownloaded = uiState.downloadedVideoIds.contains(activeVideo.id),
                                downloadProgress = uiState.downloadProgress[activeVideo.id],
                                isSubscribed = uiState.subscribedChannelIds.contains(activeVideo.channelId),
                                comments = uiState.videoComments[activeVideo.id] ?: emptyList(),
                                relatedVideos = uiState.videos,
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onCollapse = { viewModel.collapsePlayer() },
                                onToggleLike = { viewModel.toggleLike(it) },
                                onToggleSave = { viewModel.toggleSave(it) },
                                onStartDownload = { vid, quality -> viewModel.startDownload(vid, quality) },
                                onRemoveDownload = { viewModel.removeDownload(it) },
                                onToggleSubscribe = { viewModel.toggleSubscribe(it) },
                                onAddComment = { videoId, text -> viewModel.addComment(videoId, text) },
                                onSelectRelatedVideo = { viewModel.openVideo(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
