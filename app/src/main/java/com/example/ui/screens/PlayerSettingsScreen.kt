package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerSettings
import com.example.ui.theme.FlowRed

@Composable
fun PlayerSettingsScreen(
    playerSettings: PlayerSettings,
    onUpdatePlayerSettings: (PlayerSettings) -> Unit,
    sleepTimerRemainingSeconds: Int = 0,
    onSetSleepTimer: (Int) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
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
                onClick = onBack,
                modifier = Modifier.testTag("player_settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Player & Playback",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Gestures, background audio, PiP & controls",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    onUpdatePlayerSettings(PlayerSettings())
                },
                modifier = Modifier.testTag("reset_player_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset player settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // --- 2. Scrollable Settings Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // ==========================================
            // SECTION: BACKGROUND PLAY (MAX PLAYER ENGINE)
            // ==========================================
            PlayerSettingsSectionHeader(
                title = "Background Play",
                subtitle = "Screen-lock and background audio playback",
                icon = Icons.Default.MusicNote
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Highlight Banner
                    Surface(
                        color = FlowRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = FlowRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Max Player Engine Active • WAKE_LOCK enabled",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlowRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PlayerSettingToggle(
                        title = "Background Play",
                        subtitle = "Keep playing audio seamlessly when app goes to background or the screen is locked.",
                        checked = playerSettings.backgroundPlayEnabled,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(backgroundPlayEnabled = it))
                        },
                        testTag = "background_play_toggle"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    PlayerSettingToggle(
                        title = "Pause when headphones disconnect",
                        subtitle = "Automatically pause playback when wired headphones or Bluetooth devices disconnect.",
                        checked = playerSettings.pauseOnHeadphonesDisconnect,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(pauseOnHeadphonesDisconnect = it))
                        },
                        testTag = "headphones_pause_toggle"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SECTION: GESTURES
            // ==========================================
            PlayerSettingsSectionHeader(
                title = "Gestures & Controls",
                subtitle = "Touch gestures on video screen",
                icon = Icons.Default.TouchApp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PlayerSettingToggle(
                        title = "Brightness swipe (left edge)",
                        subtitle = "Swipe up or down on the left half of the video player to adjust screen brightness.",
                        icon = Icons.Default.WbSunny,
                        checked = playerSettings.brightnessGesture,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(brightnessGesture = it))
                        },
                        testTag = "brightness_gesture_toggle"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    PlayerSettingToggle(
                        title = "Volume swipe (right edge)",
                        subtitle = "Swipe up or down on the right half of the video player to adjust media volume.",
                        icon = Icons.Default.VolumeUp,
                        checked = playerSettings.volumeGesture,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(volumeGesture = it))
                        },
                        testTag = "volume_gesture_toggle"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    PlayerSettingToggle(
                        title = "Seek gesture (horizontal swipe)",
                        subtitle = "Swipe left or right across the screen to fast-forward or rewind.",
                        icon = Icons.Default.FastForward,
                        checked = playerSettings.seekGesture,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(seekGesture = it))
                        },
                        testTag = "seek_gesture_toggle"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    // Double-tap seek duration selector
                    Text(
                        text = "Double-tap seek duration",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Jump duration when double-tapping either side of the video player.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 30).forEach { seconds ->
                            val isSelected = playerSettings.doubleTapSeekSeconds == seconds
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onUpdatePlayerSettings(playerSettings.copy(doubleTapSeekSeconds = seconds))
                                },
                                label = { Text("${seconds}s") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FlowRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("seek_duration_${seconds}s")
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    PlayerSettingToggle(
                        title = "Zoom to fill / fit screen",
                        subtitle = "Crop 16:9 videos to seamlessly fill tall modern smartphone displays.",
                        icon = Icons.Default.CropFree,
                        checked = playerSettings.zoomToFill,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(zoomToFill = it))
                        },
                        testTag = "zoom_to_fill_toggle"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SECTION: PLAYER BUTTONS
            // ==========================================
            PlayerSettingsSectionHeader(
                title = "Player Buttons",
                subtitle = "Quick toggles visible on player control overlay",
                icon = Icons.Default.Widgets
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PlayerSettingToggle(
                        title = "Sleep timer button",
                        subtitle = "Display quick sleep timer button directly on the player overlay.",
                        icon = Icons.Default.Timer,
                        checked = playerSettings.showSleepTimerButton,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(showSleepTimerButton = it))
                        },
                        testTag = "sleep_timer_button_toggle"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    PlayerSettingToggle(
                        title = "Cast button",
                        subtitle = "Display Google Cast / DLNA button to stream to Smart TVs and speakers.",
                        icon = Icons.Default.Cast,
                        checked = playerSettings.showCastButton,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(showCastButton = it))
                        },
                        testTag = "cast_button_toggle"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    PlayerSettingToggle(
                        title = "SponsorBlock button",
                        subtitle = "Skip sponsored segments, intro animations, and outro credits.",
                        icon = Icons.Default.Shield,
                        checked = playerSettings.showSponsorBlockButton,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(showSponsorBlockButton = it))
                        },
                        testTag = "sponsor_block_button_toggle"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    PlayerSettingToggle(
                        title = "Previous / Next buttons",
                        subtitle = "Always show dedicated skip track & next video buttons in player controls.",
                        icon = Icons.Default.SkipNext,
                        checked = playerSettings.showPrevNextButtons,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(showPrevNextButtons = it))
                        },
                        testTag = "prev_next_buttons_toggle"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    PlayerSettingToggle(
                        title = "Comments button",
                        subtitle = "Show one-tap comments icon directly on player control bar.",
                        icon = Icons.Default.Comment,
                        checked = playerSettings.showCommentsButton,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(showCommentsButton = it))
                        },
                        testTag = "comments_button_toggle"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SECTION: PiP & MINI PLAYER
            // ==========================================
            PlayerSettingsSectionHeader(
                title = "PiP & Mini Player",
                subtitle = "Multitasking while browsing feeds and other apps",
                icon = Icons.Default.PictureInPicture
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PlayerSettingToggle(
                        title = "Auto Picture-in-Picture (PiP)",
                        subtitle = "Automatically shrink playing video into floating PiP window when leaving FLOW.",
                        checked = playerSettings.autoPipEnabled,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(autoPipEnabled = it))
                        },
                        testTag = "auto_pip_toggle"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    PlayerSettingToggle(
                        title = "Continue Watching Mini-Player",
                        subtitle = "Dock persistent mini-player bar at bottom of the screen when minimizing active video.",
                        checked = playerSettings.miniPlayerEnabled,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(miniPlayerEnabled = it))
                        },
                        testTag = "mini_player_toggle"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    PlayerSettingToggle(
                        title = "Resume playback position",
                        subtitle = "Remember last playback position and prompt to resume where you left off.",
                        checked = playerSettings.resumePlaybackEnabled,
                        onCheckedChange = {
                            onUpdatePlayerSettings(playerSettings.copy(resumePlaybackEnabled = it))
                        },
                        testTag = "resume_playback_toggle"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SECTION: SLEEP TIMER QUICK SETTING
            // ==========================================
            PlayerSettingsSectionHeader(
                title = "Sleep Timer",
                subtitle = "Automatically pause playback after selected duration",
                icon = Icons.Default.Timer
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Timer Status",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (sleepTimerRemainingSeconds > 0) {
                            val mins = sleepTimerRemainingSeconds / 60
                            val secs = sleepTimerRemainingSeconds % 60
                            Surface(
                                color = FlowRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = String.format("%02d:%02d remaining", mins, secs),
                                    color = FlowRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Inactive",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val timerOptions = listOf(
                            Pair("Off", 0),
                            Pair("15m", 15),
                            Pair("30m", 30),
                            Pair("45m", 45),
                            Pair("60m", 60)
                        )
                        timerOptions.forEach { (label, minutes) ->
                            val isSelected = (minutes == 0 && sleepTimerRemainingSeconds == 0) ||
                                (minutes > 0 && sleepTimerRemainingSeconds > (minutes - 15) * 60 && sleepTimerRemainingSeconds <= minutes * 60)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onSetSleepTimer(minutes)
                                },
                                label = { Text(label, fontSize = 12.sp) },
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

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PlayerSettingsSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(FlowRed.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FlowRed,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlayerSettingToggle(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .padding(end = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FlowRed,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
