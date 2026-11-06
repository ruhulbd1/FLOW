package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MediaPlaybackManager {

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private var currentPlaylist: List<VideoItem> = emptyList()

    // Callbacks to notify UI when action is triggered from notification or lock screen
    var onPlayPauseRequested: (() -> Unit)? = null
    var onNextRequested: (() -> Unit)? = null
    var onPrevRequested: (() -> Unit)? = null

    fun startPlayback(context: Context, video: VideoItem, isPlaying: Boolean = true, playlist: List<VideoItem> = emptyList()) {
        _currentVideo.value = video
        _isPlaying.value = isPlaying
        if (playlist.isNotEmpty()) {
            currentPlaylist = playlist
        }

        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = if (isPlaying) MediaPlaybackService.ACTION_PLAY else MediaPlaybackService.ACTION_PAUSE
            putExtra(MediaPlaybackService.EXTRA_VIDEO_ID, video.id)
            putExtra(MediaPlaybackService.EXTRA_VIDEO_TITLE, video.title)
            putExtra(MediaPlaybackService.EXTRA_CHANNEL_TITLE, video.channelTitle)
            putExtra(MediaPlaybackService.EXTRA_THUMBNAIL_URL, video.thumbnailUrl)
            putExtra(MediaPlaybackService.EXTRA_DURATION, video.duration)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun togglePlayPause(context: Context) {
        val newPlayingState = !_isPlaying.value
        _isPlaying.value = newPlayingState
        val video = _currentVideo.value ?: return

        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = if (newPlayingState) MediaPlaybackService.ACTION_PLAY else MediaPlaybackService.ACTION_PAUSE
            putExtra(MediaPlaybackService.EXTRA_VIDEO_ID, video.id)
            putExtra(MediaPlaybackService.EXTRA_VIDEO_TITLE, video.title)
            putExtra(MediaPlaybackService.EXTRA_CHANNEL_TITLE, video.channelTitle)
            putExtra(MediaPlaybackService.EXTRA_THUMBNAIL_URL, video.thumbnailUrl)
            putExtra(MediaPlaybackService.EXTRA_DURATION, video.duration)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
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
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_STOP
        }
        context.startService(intent)
    }

    // Called by MediaPlaybackService to update local state
    fun updatePlaybackState(isPlaying: Boolean, positionMs: Long = 0L, durationMs: Long = 0L) {
        _isPlaying.value = isPlaying
        if (positionMs > 0L) _currentPositionMs.value = positionMs
        if (durationMs > 0L) _durationMs.value = durationMs
    }

    fun setPlaylist(list: List<VideoItem>) {
        currentPlaylist = list
    }
}
