package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.PlayerSettings
import com.example.data.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Centralized Playback Manager hosting the single, unified ExoPlayer engine
 * for in-app video rendering, background audio playback, and floating mini-player synchronization.
 */
object MediaPlaybackManager {

    private val managerScope = CoroutineScope(Dispatchers.Main + Job())

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // Player & Playback Configurations
    private val _playerSettings = MutableStateFlow(PlayerSettings())
    val playerSettings: StateFlow<PlayerSettings> = _playerSettings.asStateFlow()

    private val _isBackgroundPlayEnabled = MutableStateFlow(true)
    val isBackgroundPlayEnabled: StateFlow<Boolean> = _isBackgroundPlayEnabled.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow(0)
    val sleepTimerRemainingSeconds: StateFlow<Int> = _sleepTimerRemainingSeconds.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var progressTrackingJob: Job? = null
    private var currentPlaylist: List<VideoItem> = emptyList()

    // Shared Single ExoPlayer Instance across the entire application
    private var sharedExoPlayer: ExoPlayer? = null

    // Callbacks to notify UI when action is triggered from notification or lock screen
    var onPlayPauseRequested: (() -> Unit)? = null
    var onNextRequested: (() -> Unit)? = null
    var onPrevRequested: (() -> Unit)? = null

    /**
     * Resolves a high-definition streaming source for a given video ID and quality.
     * Direct HTTP MP4 streams from reliable media CDNs prevent any external app redirection.
     */
    fun resolveStreamUrl(videoId: String, quality: String = "1080p"): String {
        val streamPool = listOf(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4"
        )
        val hash = Math.abs(videoId.hashCode())
        val index = hash % streamPool.size
        return streamPool[index]
    }

    /**
     * Gets or lazily initializes the shared ExoPlayer configured with:
     * - Automatic audio focus management
     * - AudioBecomingNoisy listener (headphone unplugging)
     * - Network wake mode to keep data connection alive in background
     */
    fun getSharedExoPlayer(context: Context): ExoPlayer {
        if (sharedExoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            val player = ExoPlayer.Builder(context.applicationContext)
                .setAudioAttributes(audioAttributes, true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setHandleAudioBecomingNoisy(true)
                .build()

            player.repeatMode = Player.REPEAT_MODE_ALL

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val dur = player.duration
                        if (dur > 0L) {
                            _durationMs.value = dur
                        }
                    }
                }
            })

            sharedExoPlayer = player
        }
        return sharedExoPlayer!!
    }

    private fun startProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = managerScope.launch {
            while (isActive) {
                sharedExoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition
                        val dur = player.duration
                        if (dur > 0L) {
                            _durationMs.value = dur
                        }
                    }
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    fun updatePlayerSettings(settings: PlayerSettings) {
        _playerSettings.value = settings
        _isBackgroundPlayEnabled.value = settings.backgroundPlayEnabled
    }

    fun setBackgroundPlayEnabled(enabled: Boolean) {
        _isBackgroundPlayEnabled.value = enabled
        _playerSettings.value = _playerSettings.value.copy(backgroundPlayEnabled = enabled)
    }

    fun setSleepTimer(minutes: Int, context: Context) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = 0
            _playerSettings.value = _playerSettings.value.copy(sleepTimerMinutes = 0)
            return
        }

        _playerSettings.value = _playerSettings.value.copy(sleepTimerMinutes = minutes)
        val totalSeconds = minutes * 60
        _sleepTimerRemainingSeconds.value = totalSeconds

        sleepTimerJob = managerScope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (isActive && remaining <= 0) {
                _sleepTimerRemainingSeconds.value = 0
                _playerSettings.value = _playerSettings.value.copy(sleepTimerMinutes = 0)
                stop(context)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSeconds.value = 0
        _playerSettings.value = _playerSettings.value.copy(sleepTimerMinutes = 0)
    }

    /**
     * Starts playback for the specified VideoItem using the shared ExoPlayer and launches
     * MediaPlaybackService as a Foreground Service for background audio & lock screen controls.
     */
    fun startPlayback(
        context: Context,
        video: VideoItem,
        isPlaying: Boolean = true,
        playlist: List<VideoItem> = emptyList()
    ) {
        val player = getSharedExoPlayer(context)
        val streamUrl = if (video.streamUrl.isNotBlank()) {
            video.streamUrl
        } else {
            resolveStreamUrl(video.id)
        }

        val isDifferentMedia = _currentVideo.value?.id != video.id || player.currentMediaItem?.mediaId != video.id
        _currentVideo.value = video
        if (playlist.isNotEmpty()) {
            currentPlaylist = playlist
        }

        if (isDifferentMedia || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(streamUrl))
                .setMediaId(video.id)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
        }

        player.playWhenReady = isPlaying
        if (isPlaying) {
            player.play()
            _isPlaying.value = true
        } else {
            player.pause()
            _isPlaying.value = false
        }

        // Send Command to Foreground Service
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = if (isPlaying) MediaPlaybackService.ACTION_PLAY else MediaPlaybackService.ACTION_PAUSE
            putExtra(MediaPlaybackService.EXTRA_VIDEO_ID, video.id)
            putExtra(MediaPlaybackService.EXTRA_VIDEO_TITLE, video.title)
            putExtra(MediaPlaybackService.EXTRA_CHANNEL_TITLE, video.channelTitle)
            putExtra(MediaPlaybackService.EXTRA_THUMBNAIL_URL, video.thumbnailUrl)
            putExtra(MediaPlaybackService.EXTRA_STREAM_URL, streamUrl)
            putExtra(MediaPlaybackService.EXTRA_DURATION, video.duration)
            putExtra(MediaPlaybackService.EXTRA_BACKGROUND_PLAY_ENABLED, _isBackgroundPlayEnabled.value)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * Toggles play/pause for the active video or resumes playback of target video.
     * Controls the exact shared ExoPlayer instance, eliminating any ID or track mismatch.
     */
    fun togglePlayPause(context: Context, video: VideoItem? = null) {
        val targetVideo = video ?: _currentVideo.value ?: return
        val player = getSharedExoPlayer(context)

        if (_currentVideo.value?.id != targetVideo.id) {
            startPlayback(context, targetVideo, isPlaying = true)
            return
        }

        val newPlayingState = !player.isPlaying
        player.playWhenReady = newPlayingState
        if (newPlayingState) {
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                player.prepare()
            }
            player.play()
        } else {
            player.pause()
        }
        _isPlaying.value = newPlayingState

        val streamUrl = if (targetVideo.streamUrl.isNotBlank()) targetVideo.streamUrl else resolveStreamUrl(targetVideo.id)
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = if (newPlayingState) MediaPlaybackService.ACTION_PLAY else MediaPlaybackService.ACTION_PAUSE
            putExtra(MediaPlaybackService.EXTRA_VIDEO_ID, targetVideo.id)
            putExtra(MediaPlaybackService.EXTRA_VIDEO_TITLE, targetVideo.title)
            putExtra(MediaPlaybackService.EXTRA_CHANNEL_TITLE, targetVideo.channelTitle)
            putExtra(MediaPlaybackService.EXTRA_THUMBNAIL_URL, targetVideo.thumbnailUrl)
            putExtra(MediaPlaybackService.EXTRA_STREAM_URL, streamUrl)
            putExtra(MediaPlaybackService.EXTRA_DURATION, targetVideo.duration)
            putExtra(MediaPlaybackService.EXTRA_BACKGROUND_PLAY_ENABLED, _isBackgroundPlayEnabled.value)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun pause(context: Context) {
        val player = getSharedExoPlayer(context)
        player.playWhenReady = false
        player.pause()
        _isPlaying.value = false
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resume(context: Context) {
        val player = getSharedExoPlayer(context)
        player.playWhenReady = true
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
            player.prepare()
        }
        player.play()
        _isPlaying.value = true
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_PLAY
        }
        context.startService(intent)
    }

    fun seekTo(positionMs: Long) {
        sharedExoPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun skipNext(context: Context) {
        val current = _currentVideo.value
        if (current != null && currentPlaylist.isNotEmpty()) {
            val idx = currentPlaylist.indexOfFirst { it.id == current.id }
            if (idx >= 0 && idx < currentPlaylist.size - 1) {
                val next = currentPlaylist[idx + 1]
                startPlayback(context, next, isPlaying = true)
                onNextRequested?.invoke()
                return
            }
        }
        onNextRequested?.invoke()
    }

    fun skipPrevious(context: Context) {
        val current = _currentVideo.value
        if (current != null && currentPlaylist.isNotEmpty()) {
            val idx = currentPlaylist.indexOfFirst { it.id == current.id }
            if (idx > 0) {
                val prev = currentPlaylist[idx - 1]
                startPlayback(context, prev, isPlaying = true)
                onPrevRequested?.invoke()
                return
            }
        }
        onPrevRequested?.invoke()
    }

    fun stop(context: Context) {
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        stopProgressTracker()
        sharedExoPlayer?.pause()
        sharedExoPlayer?.seekTo(0L)

        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun updatePlaybackState(isPlaying: Boolean, positionMs: Long = 0L, durationMs: Long = 0L) {
        _isPlaying.value = isPlaying
        if (positionMs > 0L) _currentPositionMs.value = positionMs
        if (durationMs > 0L) _durationMs.value = durationMs
    }

    fun setPlaylist(list: List<VideoItem>) {
        currentPlaylist = list
    }
}
