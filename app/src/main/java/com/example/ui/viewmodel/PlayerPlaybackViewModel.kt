package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PlayerSettings
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import com.example.service.MediaPlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Dedicated ViewModel for Player & Playback configurations,
 * real-time DataStore persistence, and Mini-Player / Background audio bindings.
 */
class PlayerPlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application.applicationContext)

    // Player settings flow synced directly with persistent DataStore storage
    val playerSettings: StateFlow<PlayerSettings> = MediaPlaybackManager.playerSettings
    val isPlaying: StateFlow<Boolean> = MediaPlaybackManager.isPlaying
    val currentVideo: StateFlow<VideoItem?> = MediaPlaybackManager.currentVideo
    val currentPositionMs: StateFlow<Long> = MediaPlaybackManager.currentPositionMs
    val durationMs: StateFlow<Long> = MediaPlaybackManager.durationMs
    val sleepTimerRemainingSeconds: StateFlow<Int> = MediaPlaybackManager.sleepTimerRemainingSeconds

    private val _isPlayerSettingsOpen = MutableStateFlow(false)
    val isPlayerSettingsOpen: StateFlow<Boolean> = _isPlayerSettingsOpen.asStateFlow()

    init {
        // Hydrate settings from DataStore on startup and sync with MediaPlaybackManager
        viewModelScope.launch {
            repository.playerSettingsFlow.collect { savedSettings ->
                MediaPlaybackManager.updatePlayerSettings(savedSettings)
            }
        }
    }

    /**
     * Updates playback configurations (Background Play, Gestures, PiP, Sleep Timer)
     * and persists them to DataStore for persistent storage across app launches.
     */
    fun updatePlayerSettings(settings: PlayerSettings) {
        MediaPlaybackManager.updatePlayerSettings(settings)
        viewModelScope.launch {
            repository.savePlayerSettings(settings)
        }
    }

    /**
     * Toggles play/pause for the active video or resumes the currently bound track.
     * Binding targetVideo ensures the mini-player never plays the wrong media ID.
     */
    fun togglePlayPause(video: VideoItem? = null) {
        val target = video ?: currentVideo.value
        if (target != null) {
            MediaPlaybackManager.togglePlayPause(getApplication(), target)
        }
    }

    fun startPlayback(video: VideoItem, isPlaying: Boolean = true) {
        MediaPlaybackManager.startPlayback(getApplication(), video, isPlaying)
    }

    fun setSleepTimer(minutes: Int) {
        MediaPlaybackManager.setSleepTimer(minutes, getApplication())
        val updated = playerSettings.value.copy(sleepTimerMinutes = minutes)
        viewModelScope.launch {
            repository.savePlayerSettings(updated)
        }
    }

    fun cancelSleepTimer() {
        MediaPlaybackManager.cancelSleepTimer()
        val updated = playerSettings.value.copy(sleepTimerMinutes = 0)
        viewModelScope.launch {
            repository.savePlayerSettings(updated)
        }
    }

    fun skipNext() {
        MediaPlaybackManager.skipNext(getApplication())
    }

    fun skipPrevious() {
        MediaPlaybackManager.skipPrevious(getApplication())
    }

    fun stop() {
        MediaPlaybackManager.stop(getApplication())
    }

    fun openPlayerSettings() {
        _isPlayerSettingsOpen.value = true
    }

    fun closePlayerSettings() {
        _isPlayerSettingsOpen.value = false
    }
}
