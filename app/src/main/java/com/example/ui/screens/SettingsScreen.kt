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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import com.example.ui.theme.FlowRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentApiKey: String,
    channelIds: List<String>,
    channels: List<com.example.data.model.Channel> = emptyList(),
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
    BackHandler {
        onCloseSettings()
    }

    val context = LocalContext.current
    var settingsSearchQuery by remember { mutableStateOf("") }

    // Channel manager local state
    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var activeChannels by remember(channelIds) { mutableStateOf(channelIds) }
    var newChannelInput by remember { mutableStateOf("") }
    var newTopicInput by remember { mutableStateOf("") }

    // Dialogs local state
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputJson by remember { mutableStateOf("") }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showStorageMenu by remember { mutableStateOf(false) }

    val presetChannels = listOf(
        Pair("c_deepto", "Deepto Natok"),
        Pair("c_naf", "NAF Ent"),
        Pair("c_rtv", "Rtv Drama"),
        Pair("c_cmv", "CMV Natok"),
        Pair("c_dhruba", "Dhruba TV"),
        Pair("c_csb", "Coke Studio Bangla")
    )

    // Helper for real-time settings filtering
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
        // 1. Top Bar
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

        // 2. Search Settings Bar with Real-Time Filtering
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            OutlinedTextField(
                value = settingsSearchQuery,
                onValueChange = { settingsSearchQuery = it },
                placeholder = { Text("Search settings (theme, history, quality, downloads...)", fontSize = 13.sp) },
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

        // 3. Scrollable Settings Categories
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ==========================================
            // CATEGORY: FLOW ENGINE
            // ==========================================
            if (matchesSearch("flow", "engine", "deep flow", "recommendation", "history", "watch history", "interest", "blocked", "topic")) {
                SettingsSectionHeader(title = "Flow Engine", icon = Icons.Default.AutoAwesome)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Deep Flow Mode Toggle
                        SettingsToggleRow(
                            title = "Deep Flow Mode",
                            subtitle = "Distraction-free chronological mode. Pauses personalized algorithmic recommendations.",
                            checked = deepFlowMode,
                            onCheckedChange = { onSetDeepFlowMode(it) },
                            testTag = "deep_flow_toggle"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Save to Watch History Toggle
                        SettingsToggleRow(
                            title = "Save to Watch History",
                            subtitle = "When enabled, watched videos are logged to your Library history.",
                            checked = saveWatchHistory,
                            onCheckedChange = { onSetSaveWatchHistory(it) },
                            testTag = "save_history_toggle"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Blocked Topics Management
                        Text(
                            text = "Interests & Blocked Topics",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Videos containing blocked keywords will be automatically removed from feeds.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Blocked Topic Chips
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

                        // Add Blocked Topic Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTopicInput,
                                onValueChange = { newTopicInput = it },
                                placeholder = { Text("Block keyword (e.g. spoilers, horror)", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newTopicInput.isNotBlank()) {
                                        onAddBlockedTopic(newTopicInput.trim())
                                        newTopicInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FlowRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Block")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ==========================================
            // CATEGORY: LOOK & FEEL
            // ==========================================
            if (matchesSearch("look", "feel", "appearance", "theme", "dark", "light", "system", "color", "language", "region")) {
                SettingsSectionHeader(title = "Look & Feel", icon = Icons.Default.Palette)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Appearance Theme",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose your preferred viewing contrast. Changes apply in real-time.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Theme Mode Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("System", "Dark", "Light").forEach { mode ->
                                val isSelected = themeMode == mode
                                Surface(
                                    color = if (isSelected) FlowRed else MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSetThemeMode(mode) }
                                        .testTag("theme_mode_$mode")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = mode,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Language & Region Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageMenu = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = FlowRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Language & Region",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = languageRegion,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Box {
                                TextButton(onClick = { showLanguageMenu = true }) {
                                    Text("Change", color = FlowRed, fontSize = 12.sp)
                                }

                                DropdownMenu(
                                    expanded = showLanguageMenu,
                                    onDismissRequest = { showLanguageMenu = false }
                                ) {
                                    listOf("English (US)", "Bengali (বাংলা)", "Global").forEach { lang ->
                                        DropdownMenuItem(
                                            text = { Text(lang) },
                                            onClick = {
                                                onSetLanguageRegion(lang)
                                                showLanguageMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ==========================================
            // CATEGORY: WATCHING & PLAYBACK
            // ==========================================
            if (matchesSearch("watching", "player", "playback", "autoplay", "quality", "1080p", "720p", "360p", "feeds", "channel", "api", "youtube")) {
                SettingsSectionHeader(title = "Watching & Playback", icon = Icons.Default.PlayCircle)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Autoplay Next Video
                        SettingsToggleRow(
                            title = "Autoplay Next Video",
                            subtitle = "Automatically load the next video when playback finishes.",
                            checked = autoplay,
                            onCheckedChange = { onSetAutoplay(it) },
                            testTag = "autoplay_toggle"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Video Quality Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showQualityMenu = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Streaming Quality Preference",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Current: $videoQuality",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box {
                                TextButton(onClick = { showQualityMenu = true }) {
                                    Text(videoQuality, color = FlowRed, fontWeight = FontWeight.Bold)
                                }

                                DropdownMenu(
                                    expanded = showQualityMenu,
                                    onDismissRequest = { showQualityMenu = false }
                                ) {
                                    listOf("1080p FHD", "720p HD", "360p Data Saver", "Auto").forEach { q ->
                                        DropdownMenuItem(
                                            text = { Text(q) },
                                            onClick = {
                                                onSetVideoQuality(q)
                                                showQualityMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Feeds & Content Controls (Multi-Channel API Setup)
                        Text(
                            text = "Feeds & YouTube Data API",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "FLOW queries multiple YouTube channels in parallel and combines their latest uploads into your main chronological feed in real-time.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("YouTube Data API v3 Key") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_api_key_field"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Curated Presets
                        Text(
                            text = "Quick Presets:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetChannels.forEach { preset ->
                                val isPresent = channels.any { it.id == preset.first }
                                Surface(
                                    color = if (isPresent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isPresent) FlowRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable {
                                        onAddChannel(preset.first)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isPresent) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = null,
                                            tint = if (isPresent) FlowRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(text = preset.second, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Active channel management list
                        val channelListToDisplay = if (channels.isNotEmpty()) channels else activeChannels.map { com.example.data.model.Channel(id = it, name = it) }
                        Text(
                            text = "Configured Channels (${channelListToDisplay.size}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            channelListToDisplay.forEach { ch ->
                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ch.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = ch.id,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (ch.isEnabledInFeed) "In Feed" else "Off",
                                                fontSize = 10.sp,
                                                color = if (ch.isEnabledInFeed) FlowRed else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Switch(
                                                checked = ch.isEnabledInFeed,
                                                onCheckedChange = { isChecked ->
                                                    onToggleChannelInFeed(ch.id, isChecked)
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = FlowRed
                                                ),
                                                modifier = Modifier.size(32.dp)
                                            )

                                            Spacer(modifier = Modifier.width(4.dp))

                                            IconButton(
                                                onClick = { onRemoveChannel(ch.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove channel",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Add channel input field
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newChannelInput,
                                onValueChange = { newChannelInput = it },
                                placeholder = { Text("Channel ID (UC...) or @handle", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if (newChannelInput.isNotBlank()) {
                                        onAddChannel(newChannelInput.trim())
                                        newChannelInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FlowRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Add")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val enabledIds = channelListToDisplay.filter { it.isEnabledInFeed }.map { it.id }
                                    onSaveApiSettings(apiKeyInput, enabledIds)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FlowRed),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save Key & Sync Feed", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    apiKeyInput = ""
                                    onResetChannelsToDefault()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset Defaults", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Real-Time Auto-Sync Toggle Row
                        SettingsToggleRow(
                            title = "Real-Time Upload Sync",
                            subtitle = "Automatically fetch and insert new uploads from subscribed channels into the main feed dynamically without manual refresh.",
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { onSetAutoSyncEnabled(it) },
                            testTag = "auto_sync_toggle"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Polling Frequency Selector
                        var showIntervalMenu by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showIntervalMenu = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Auto-Sync Polling Interval",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Current: ${autoSyncIntervalSeconds}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box {
                                TextButton(onClick = { showIntervalMenu = true }) {
                                    Text("${autoSyncIntervalSeconds}s", color = FlowRed, fontWeight = FontWeight.Bold)
                                }

                                DropdownMenu(
                                    expanded = showIntervalMenu,
                                    onDismissRequest = { showIntervalMenu = false }
                                ) {
                                    listOf(15 to "15s (Instant)", 30 to "30s (Recommended)", 60 to "1 minute", 300 to "5 minutes").forEach { (sec, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                onSetAutoSyncInterval(sec)
                                                showIntervalMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Test Actions: Check Now / Simulate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCheckNewUploadsNow,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Check Uploads Now", fontSize = 11.sp)
                            }

                            Button(
                                onClick = onSimulateNewUpload,
                                colors = ButtonDefaults.buttonColors(containerColor = FlowRed),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Simulate Upload", fontSize = 11.sp)
                            }
                        }

                        if (!statusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = statusMessage, color = FlowRed, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ==========================================
            // CATEGORY: YOUR DATA & PRIVACY
            // ==========================================
            if (matchesSearch("your data", "data", "history", "privacy", "clear", "cache", "backup", "restore", "downloads", "storage")) {
                SettingsSectionHeader(title = "Your Data & Storage", icon = Icons.Default.Storage)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Clear History & Cache Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "History & Privacy Cache",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Clear all watched video entries and local memory cache.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { showClearConfirmDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FlowRed)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Downloads Storage Location
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStorageMenu = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Downloads Storage Location",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = storageLocation,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box {
                                TextButton(onClick = { showStorageMenu = true }) {
                                    Text("Change", color = FlowRed, fontSize = 12.sp)
                                }

                                DropdownMenu(
                                    expanded = showStorageMenu,
                                    onDismissRequest = { showStorageMenu = false }
                                ) {
                                    listOf("Internal Storage", "SD Card (External)").forEach { loc ->
                                        DropdownMenuItem(
                                            text = { Text(loc) },
                                            onClick = {
                                                onSetStorageLocation(loc)
                                                showStorageMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Backup & Restore State
                        Text(
                            text = "Backup & Restore",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Export your channels, saved list, and configuration to JSON.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onExportBackup,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Export Backup", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { showRestoreDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Restore JSON", fontSize = 11.sp)
                            }
                        }

                        // Display exported backup preview
                        if (!backupExportJson.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Backup JSON generated:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FlowRed)
                                    Text(
                                        text = backupExportJson.take(120) + "...",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("FlowBackup", backupExportJson)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("Copy Full JSON to Clipboard", fontSize = 11.sp, color = FlowRed)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ==========================================
            // CATEGORY: SYSTEM & ABOUT
            // ==========================================
            if (matchesSearch("system", "about", "notification", "update", "version", "check for updates")) {
                SettingsSectionHeader(title = "System & About", icon = Icons.Default.Info)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Notifications Toggle
                        SettingsToggleRow(
                            title = "Notifications & Live Alerts",
                            subtitle = "Receive notifications when subscribed channels upload or premiere.",
                            checked = notificationsEnabled,
                            onCheckedChange = { onSetNotificationsEnabled(it) },
                            testTag = "notifications_toggle"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Check for updates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "App Updates",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Current: FLOW v1.2.0 (Stable)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = onOpenUpdateModal,
                                colors = ButtonDefaults.buttonColors(containerColor = FlowRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check Updates", fontSize = 11.sp)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Version & Credits
                        Text(
                            text = "FLOW for Android • Multi-Channel Edition",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Powered by Jetpack Compose & YouTube Data API v3. Built with clean architecture, offline caching, and real-time state management.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // --- MODALS & DIALOGS ---

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
                    Text("Version: 1.2.0 (Build 20261105)")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Status: You are running the latest stable release of FLOW. All YouTube Data API and streaming components are up to date.")
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
