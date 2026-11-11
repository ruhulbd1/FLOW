package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.data.model.PlayerSettings
import com.example.ui.theme.FlowRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentApiKey: String,
    channelIds: List<String>,
    channels: List<Channel> = emptyList(),
    themeMode: String,
    saveWatchHistory: Boolean,
    deepFlowMode: Boolean,
    autoplay: Boolean,
    videoQuality: String,
    languageRegion: String,
    storageLocation: String,
    blockedTopics: List<String>,
    notificationsEnabled: Boolean,
    isAutoSyncEnabled: Boolean = true,
    autoSyncIntervalSeconds: Int = 30,
    statusMessage: String?,
    isUpdateModalOpen: Boolean,
    backupExportJson: String?,
    playerSettings: PlayerSettings = PlayerSettings(),
    onUpdatePlayerSettings: (PlayerSettings) -> Unit = {},
    isPlayerSettingsOpen: Boolean = false,
    onOpenPlayerSettings: () -> Unit = {},
    onClosePlayerSettings: () -> Unit = {},
    sleepTimerRemainingSeconds: Int = 0,
    onSetSleepTimer: (Int) -> Unit = {},
    onSaveApiSettings: (String, List<String>) -> Unit,
    onAddChannel: (String) -> Unit = {},
    onToggleChannelInFeed: (String, Boolean) -> Unit = { _, _ -> },
    onRemoveChannel: (String) -> Unit = {},
    onResetChannelsToDefault: () -> Unit = {},
    onSetAutoSyncEnabled: (Boolean) -> Unit = {},
    onSetAutoSyncInterval: (Int) -> Unit = {},
    onCheckNewUploadsNow: () -> Unit = {},
    onSimulateNewUpload: () -> Unit = {},
    onResetToDemo: () -> Unit,
    onSetThemeMode: (String) -> Unit,
    onSetSaveWatchHistory: (Boolean) -> Unit,
    onSetDeepFlowMode: (Boolean) -> Unit,
    onSetAutoplay: (Boolean) -> Unit,
    onSetVideoQuality: (String) -> Unit,
    onSetLanguageRegion: (String) -> Unit,
    onSetStorageLocation: (String) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onAddBlockedTopic: (String) -> Unit,
    onRemoveBlockedTopic: (String) -> Unit,
    onClearCacheAndHistory: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: (String) -> Unit,
    onOpenUpdateModal: () -> Unit,
    onCloseUpdateModal: () -> Unit,
    onCloseSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Sub-screen navigation state ("player", null) with rememberSaveable across recompositions
    var currentSubScreen by rememberSaveable { mutableStateOf<String?>(null) }

    // If player sub-screen is active, render PlayerSettingsScreen with BackHandler
    if (isPlayerSettingsOpen || currentSubScreen == "player") {
        PlayerSettingsScreen(
            playerSettings = playerSettings,
            onUpdatePlayerSettings = onUpdatePlayerSettings,
            sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
            onSetSleepTimer = onSetSleepTimer,
            onBack = {
                currentSubScreen = null
                onClosePlayerSettings()
            }
        )
        return
    }

    BackHandler {
        onCloseSettings()
    }

    var settingsSearchQuery by remember { mutableStateOf("") }

    // Channel manager local state
    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var activeChannels by remember(channelIds) { mutableStateOf(channelIds) }
    var newChannelInput by remember { mutableStateOf("") }
    var newTopicInput by remember { mutableStateOf("") }

    // Dialogs local state
    var showQualityDialog by remember { mutableStateOf(false) }
    var showFeedsDialog by remember { mutableStateOf(false) }
    var showIntegrationsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showNetworkDialog by remember { mutableStateOf(false) }
    var showWellbeingDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputJson by remember { mutableStateOf("") }

    // Wellbeing state
    var bedtimeReminder by remember { mutableStateOf(false) }
    var breakReminder by remember { mutableStateOf(true) }

    // Network state
    var streamWifiOnly by remember { mutableStateOf(false) }
    var lowDataMode by remember { mutableStateOf(false) }

    val presetChannels = listOf(
        Pair("UCVlUfN0sAILHcqgtWSxEPRg", "Deepto Natok"),
        Pair("UCaE9aMdlRomFYL4LkGhC1FQ", "NAF Ent"),
        Pair("UC_x5XG1OV2P6uZZ5FSM9Ttw", "Google Developers"),
        Pair("UC9FHe3sU-2xmFyEOOHWGbxA", "CMV Natok"),
        Pair("UCIB65_E_7XEScU2NB135wDw", "Coke Studio Bangla"),
        Pair("UCBJycsmduvYEL83R_U4JriQ", "MKBHD"),
        Pair("@AndroidDevelopers", "Android Devs")
    )

    fun matchesSearch(vararg terms: String): Boolean {
        if (settingsSearchQuery.isBlank()) return true
        val query = settingsSearchQuery.trim().lowercase()
        return terms.any { it.lowercase().contains(query) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCloseSettings,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // --- 2. Search Settings Bar ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            OutlinedTextField(
                value = settingsSearchQuery,
                onValueChange = { settingsSearchQuery = it },
                placeholder = { Text("Search settings (playback, quality, channels...)", fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (settingsSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { settingsSearchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_search_bar"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlowRed
                )
            )
        }

        // --- 3. Scrollable Settings Categories ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            // ==========================================
            // 1. FLOW ENGINE
            // ==========================================
            if (matchesSearch("flow engine", "deep flow", "recommendation", "sync", "interest", "blocked", "topics")) {
                SettingsSectionHeader(title = "Flow Engine", icon = Icons.Default.AutoAwesome)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Deep Flow Mode
                        SettingsToggleRow(
                            title = "Deep Flow Mode",
                            subtitle = "Distraction-free chronological mode. Pauses personalized algorithmic feeds.",
                            checked = deepFlowMode,
                            onCheckedChange = { onSetDeepFlowMode(it) },
                            testTag = "deep_flow_toggle"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Real-Time Sync
                        SettingsToggleRow(
                            title = "Real-Time YouTube Auto-Sync",
                            subtitle = "Background poll for new creator uploads every ${autoSyncIntervalSeconds}s.",
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { onSetAutoSyncEnabled(it) },
                            testTag = "auto_sync_toggle"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Blocked Topics
                        Text(
                            text = "Interests & Blocked Topics",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Videos containing blocked keywords are excluded from your feed.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (blockedTopics.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                blockedTopics.forEach { topic ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.background,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, FlowRed.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                        ) {
                                            Text(text = topic, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                                            IconButton(
                                                onClick = { onRemoveBlockedTopic(topic) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Unblock", tint = FlowRed, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTopicInput,
                                onValueChange = { newTopicInput = it },
                                placeholder = { Text("Add blocked topic (e.g. spoilers)", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newTopicInput.isNotBlank()) {
                                        onAddBlockedTopic(newTopicInput)
                                        newTopicInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FlowRed)
                            ) {
                                Text("Block")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ==========================================
            // 2. LOOK & FEEL
            // ==========================================
            if (matchesSearch("look & feel", "theme", "dark mode", "light mode", "appearance", "language", "region")) {
                SettingsSectionHeader(title = "Look & feel", icon = Icons.Default.Palette)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Theme Mode",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Dark", "Light", "System").forEach { mode ->
                                val isSelected = themeMode.equals(mode, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(mode) },
                                    leadingIcon = {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FlowRed,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        Text(
                            text = "Language & Regional Content",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("English (US)", "Bengali (BD)", "Global").forEach { lang ->
                                val isSelected = languageRegion.equals(lang, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetLanguageRegion(lang) },
                                    label = { Text(lang, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FlowRed,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ==========================================
            // 3. WATCHING
            // ==========================================
            if (matchesSearch("watching", "player & playback", "quality", "feeds", "content", "integrations", "api", "gestures", "background")) {
                SettingsSectionHeader(title = "Watching", icon = Icons.Default.PlayCircle)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {

                        // Player & playback -> Opens PlayerSettingsScreen
                        SettingsNavRow(
                            title = "Player & playback",
                            subtitle = "Background play, gestures, PiP, player buttons",
                            icon = Icons.Default.PlayCircle,
                            onClick = {
                                currentSubScreen = "player"
                                onOpenPlayerSettings()
                            },
                            testTag = "settings_player_playback_row",
                            badgeText = "Max Player Engine"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Quality
                        SettingsNavRow(
                            title = "Quality",
                            subtitle = "Default resolution: $videoQuality (Wi-Fi & Cellular)",
                            icon = Icons.Default.HighQuality,
                            onClick = { showQualityDialog = true },
                            testTag = "settings_quality_row"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Feeds & content
                        SettingsNavRow(
                            title = "Feeds & content",
                            subtitle = "Autoplay (${if (autoplay) "On" else "Off"}), filter shorts to dedicated tab",
                            icon = Icons.Default.DynamicFeed,
                            onClick = { showFeedsDialog = true },
                            testTag = "settings_feeds_row"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Integrations
                        SettingsNavRow(
                            title = "Integrations & Channels",
                            subtitle = "YouTube Data API v3 key, channels (${channelIds.size} active)",
                            icon = Icons.Default.Key,
                            onClick = { showIntegrationsDialog = true },
                            testTag = "settings_integrations_row"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ==========================================
            // 4. YOUR DATA
            // ==========================================
            if (matchesSearch("your data", "backup", "restore", "history", "privacy", "downloads", "storage")) {
                SettingsSectionHeader(title = "Your data", icon = Icons.Default.Storage)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {

                        // Backup & restore
                        SettingsNavRow(
                            title = "Backup & restore",
                            subtitle = "Export state JSON or restore playlists/subscriptions",
                            icon = Icons.Default.CloudDownload,
                            onClick = { showBackupDialog = true },
                            testTag = "settings_backup_row"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // History & privacy
                        SettingsNavRow(
                            title = "History & privacy",
                            subtitle = "Watch history (${if (saveWatchHistory) "Active" else "Paused"}), clear cache",
                            icon = Icons.Default.History,
                            onClick = { showHistoryDialog = true },
                            testTag = "settings_history_row"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Downloads & storage
                        SettingsNavRow(
                            title = "Downloads & storage",
                            subtitle = "Location: $storageLocation • Gallery sync",
                            icon = Icons.Default.Folder,
                            onClick = { showStorageDialog = true },
                            testTag = "settings_storage_row"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ==========================================
            // 5. SYSTEM
            // ==========================================
            if (matchesSearch("system", "notifications", "network", "wellbeing", "about")) {
                SettingsSectionHeader(title = "System", icon = Icons.Default.Info)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {

                        // Notifications
                        SettingsNavRow(
                            title = "Notifications",
                            subtitle = "Upload alerts (${if (notificationsEnabled) "Enabled" else "Off"})",
                            icon = Icons.Default.Notifications,
                            onClick = { showNotificationsDialog = true },
                            testTag = "settings_notifications_row"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Network
                        SettingsNavRow(
                            title = "Network",
                            subtitle = "Wi-Fi streaming, cellular data saver options",
                            icon = Icons.Default.Wifi,
                            onClick = { showNetworkDialog = true },
                            testTag = "settings_network_row"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Wellbeing
                        SettingsNavRow(
                            title = "Wellbeing & Reminders",
                            subtitle = "Bedtime reminders, take a break intervals",
                            icon = Icons.Default.SelfImprovement,
                            onClick = { showWellbeingDialog = true },
                            testTag = "settings_wellbeing_row"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // About
                        SettingsNavRow(
                            title = "About FLOW",
                            subtitle = "Version 2.4.0 • Max Player Engine • Up to date",
                            icon = Icons.Default.Info,
                            onClick = { showAboutDialog = true },
                            testTag = "settings_about_row"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // ==========================================
    // DETAILED MODALS & CONFIGURATION DIALOGS
    // ==========================================

    // Quality Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Default Video Quality") },
            text = {
                Column {
                    listOf("1080p FHD", "720p HD", "480p", "360p", "Auto").forEach { q ->
                        val isSelected = videoQuality.contains(q.take(4))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetVideoQuality(q)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = q, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Close") }
            }
        )
    }

    // Feeds & Content Dialog
    if (showFeedsDialog) {
        AlertDialog(
            onDismissRequest = { showFeedsDialog = false },
            title = { Text("Feeds & Content Preferences") },
            text = {
                Column {
                    SettingsToggleRow(
                        title = "Autoplay videos",
                        subtitle = "Play next related video automatically.",
                        checked = autoplay,
                        onCheckedChange = { onSetAutoplay(it) },
                        testTag = "modal_autoplay_toggle"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Shorts Routing: Enabled",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "All short portrait videos (\u2264 60s) are automatically filtered from Home and routed to the dedicated Shorts tab.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFeedsDialog = false }) { Text("Done") }
            }
        )
    }

    // Integrations & Channels Dialog
    if (showIntegrationsDialog) {
        AlertDialog(
            onDismissRequest = { showIntegrationsDialog = false },
            title = { Text("Integrations & Channels") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "YouTube Data API v3 Key",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("Enter your YouTube Data API Key", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Add YouTube Channel / Handle",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newChannelInput,
                            onValueChange = { newChannelInput = it },
                            placeholder = { Text("e.g. @CokeStudioBangla or ID", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (newChannelInput.isNotBlank()) {
                                    onAddChannel(newChannelInput)
                                    newChannelInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FlowRed)
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            onResetChannelsToDefault()
                            Toast.makeText(context, "Reset to default popular channels", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Default Channels")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveApiSettings(apiKeyInput, activeChannels)
                        showIntegrationsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRed)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIntegrationsDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Backup & Restore Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup & Restore") },
            text = {
                Column {
                    Text(
                        text = "Export your app settings, playlists, and subscriptions to JSON or restore them anytime.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onExportBackup()
                            Toast.makeText(context, "Backup JSON generated!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FlowRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Backup JSON")
                    }

                    if (backupExportJson != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("FLOW_Backup", backupExportJson)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Copy Exported JSON")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            showBackupDialog = false
                            showRestoreDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore from JSON")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Close") }
            }
        )
    }

    // History & Privacy Dialog
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("History & Privacy") },
            text = {
                Column {
                    SettingsToggleRow(
                        title = "Save Watch History",
                        subtitle = "When enabled, watched videos appear in Library history.",
                        checked = saveWatchHistory,
                        onCheckedChange = { onSetSaveWatchHistory(it) },
                        testTag = "modal_history_toggle"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showHistoryDialog = false
                            showClearConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Watch History & Cache")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) { Text("Done") }
            }
        )
    }

    // Storage Dialog
    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("Downloads & Storage") },
            text = {
                Column {
                    Text(
                        text = "Preferred Storage Location",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf("Internal Storage", "SD Card").forEach { loc ->
                        val isSelected = storageLocation.equals(loc, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetStorageLocation(loc)
                                    showStorageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = loc, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Gallery Sync: Offline videos are saved directly to device Gallery/Movies using MediaStore.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorageDialog = false }) { Text("Close") }
            }
        )
    }

    // Notifications Dialog
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Notifications") },
            text = {
                Column {
                    SettingsToggleRow(
                        title = "Creator Upload Alerts",
                        subtitle = "Receive notifications when subscribed channels release new videos.",
                        checked = notificationsEnabled,
                        onCheckedChange = { onSetNotificationsEnabled(it) },
                        testTag = "modal_notifications_toggle"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) { Text("Done") }
            }
        )
    }

    // Network Dialog
    if (showNetworkDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkDialog = false },
            title = { Text("Network & Data Usage") },
            text = {
                Column {
                    SettingsToggleRow(
                        title = "Stream on Wi-Fi only",
                        subtitle = "Pause high-res streaming when using cellular mobile data.",
                        checked = streamWifiOnly,
                        onCheckedChange = { streamWifiOnly = it },
                        testTag = "wifi_only_toggle"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsToggleRow(
                        title = "Mobile Data Saver",
                        subtitle = "Automatically stream at 480p on metered networks.",
                        checked = lowDataMode,
                        onCheckedChange = { lowDataMode = it },
                        testTag = "data_saver_toggle"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showNetworkDialog = false }) { Text("Done") }
            }
        )
    }

    // Wellbeing Dialog
    if (showWellbeingDialog) {
        AlertDialog(
            onDismissRequest = { showWellbeingDialog = false },
            title = { Text("Wellbeing & Reminders") },
            text = {
                Column {
                    SettingsToggleRow(
                        title = "Bedtime Reminder",
                        subtitle = "Show prompt to pause watching at 11:00 PM.",
                        checked = bedtimeReminder,
                        onCheckedChange = { bedtimeReminder = it },
                        testTag = "bedtime_reminder_toggle"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsToggleRow(
                        title = "Remind me to take a break",
                        subtitle = "Prompt a break after 60 minutes of continuous playback.",
                        checked = breakReminder,
                        onCheckedChange = { breakReminder = it },
                        testTag = "break_reminder_toggle"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showWellbeingDialog = false }) { Text("Done") }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = FlowRed, modifier = Modifier.size(40.dp))
            },
            title = { Text("FLOW 2.4.0") },
            text = {
                Column {
                    Text(
                        text = "High-Performance YouTube Streaming & Media Engine",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Max Player Background Audio Service\n• WAKE_LOCK Lockscreen Playback\n• Real-Time YouTube RSS & API v3 Sync\n• High-Fidelity Jetpack Compose Architecture\n• MediaStore Offline Download Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAboutDialog = false
                        onOpenUpdateModal()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRed)
                ) {
                    Text("Check Updates")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }

    // Clear Cache Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear History & Cache?") },
            text = { Text("This will remove all watch history entries and temporary playback cache. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearCacheAndHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore State JSON Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore State from JSON") },
            text = {
                Column {
                    Text("Paste your exported backup JSON below to restore saved videos, channels, and preferences:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreInputJson,
                        onValueChange = { restoreInputJson = it },
                        placeholder = { Text("{\"api_key\": ...}", fontSize = 11.sp) },
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackup(restoreInputJson)
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRed)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Check for Updates Modal
    if (isUpdateModalOpen) {
        AlertDialog(
            onDismissRequest = onCloseUpdateModal,
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FlowRed, modifier = Modifier.size(36.dp))
            },
            title = { Text("FLOW is Up to Date") },
            text = {
                Column {
                    Text("Version: 2.4.0 (Build 20261111)")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Status: Running latest stable release. Max Player Background Service, gestures, and PiP controls active.")
                }
            },
            confirmButton = {
                Button(
                    onClick = onCloseUpdateModal,
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRed)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FlowRed,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    badgeText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(FlowRed.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FlowRed,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = FlowRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlowRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FlowRed
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
