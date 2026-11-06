package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import coil.Coil
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.R
import com.example.data.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPlaybackService : Service(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        const val CHANNEL_ID = "flow_media_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.service.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_PLAY_PAUSE = "com.example.service.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.service.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.service.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_VIDEO_ID = "extra_video_id"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
        const val EXTRA_CHANNEL_TITLE = "extra_channel_title"
        const val EXTRA_THUMBNAIL_URL = "extra_thumbnail_url"
        const val EXTRA_DURATION = "extra_duration"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackingJob: Job? = null

    private lateinit var mediaSession: MediaSession
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // Audio Engine
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentPositionMs = 0L
    private var durationMs = 38 * 60 * 1000L // Default duration ~38 mins

    // Current metadata
    private var currentVideoId: String = ""
    private var currentTitle: String = "FLOW Video"
    private var currentChannel: String = "Creator"
    private var currentThumbnailUrl: String = ""
    private var currentThumbnailBitmap: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // Initialize PARTIAL_WAKELOCK to keep CPU awake during screen-off / phone lock
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "flow:media_playback_wakelock").apply {
            setReferenceCounted(false)
        }

        createNotificationChannel()
        initializeMediaSession()
        initializeMediaPlayer()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FLOW Background Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls and track information"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSession(this, "FLOW_MediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    handlePlay()
                }

                override fun onPause() {
                    handlePause()
                }

                override fun onSkipToNext() {
                    MediaPlaybackManager.skipNext(applicationContext)
                }

                override fun onSkipToPrevious() {
                    MediaPlaybackManager.skipPrevious(applicationContext)
                }

                override fun onStop() {
                    handleStop()
                }

                override fun onSeekTo(pos: Long) {
                    currentPositionMs = pos
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            it.seekTo((pos % (it.duration.coerceAtLeast(1))).toInt())
                        }
                    }
                    updatePlaybackState()
                }
            })
            isActive = true
        }
    }

    private fun initializeMediaPlayer() {
        try {
            // Setup MediaPlayer with wake lock and audio attributes
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                setAudioAttributes(audioAttributes)
                isLooping = true
            }
        } catch (e: Exception) {
            // Handle gracefully
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: currentVideoId
        val title = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: currentTitle
        val channel = intent.getStringExtra(EXTRA_CHANNEL_TITLE) ?: currentChannel
        val thumbUrl = intent.getStringExtra(EXTRA_THUMBNAIL_URL) ?: currentThumbnailUrl
        val durationStr = intent.getStringExtra(EXTRA_DURATION)

        val isNewTrack = videoId != currentVideoId && videoId.isNotBlank()
        if (isNewTrack) {
            currentVideoId = videoId
            currentTitle = title
            currentChannel = channel
            currentThumbnailUrl = thumbUrl
            currentPositionMs = 0L
            durationMs = parseDurationToMs(durationStr)
            loadThumbnailBitmap(thumbUrl)
        }

        when (action) {
            ACTION_PLAY -> handlePlay()
            ACTION_PAUSE -> handlePause()
            ACTION_PLAY_PAUSE -> {
                if (isPlaying) handlePause() else handlePlay()
            }
            ACTION_NEXT -> MediaPlaybackManager.skipNext(applicationContext)
            ACTION_PREVIOUS -> MediaPlaybackManager.skipPrevious(applicationContext)
            ACTION_STOP -> handleStop()
        }

        return START_STICKY
    }

    private fun parseDurationToMs(duration: String?): Long {
        if (duration.isNullOrBlank()) return 38 * 60 * 1000L
        return try {
            val parts = duration.split(":").map { it.trim().toInt() }
            when (parts.size) {
                2 -> (parts[0] * 60 + parts[1]) * 1000L
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
                else -> 38 * 60 * 1000L
            }
        } catch (e: Exception) {
            38 * 60 * 1000L
        }
    }

    private fun handlePlay() {
        if (!requestAudioFocus()) {
            return
        }

        isPlaying = true
        acquireWakeLock()

        // Update MediaSession state
        updatePlaybackState()
        updateMetadata()

        // Start Foreground Notification
        val notification = buildMediaNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Notify MediaPlaybackManager
        MediaPlaybackManager.updatePlaybackState(isPlaying = true, positionMs = currentPositionMs, durationMs = durationMs)

        // Start background position ticker
        startProgressTracking()
    }

    private fun handlePause() {
        isPlaying = false
        releaseWakeLock()

        updatePlaybackState()
        stopProgressTracking()

        val notification = buildMediaNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }

        MediaPlaybackManager.updatePlaybackState(isPlaying = false, positionMs = currentPositionMs, durationMs = durationMs)
    }

    private fun handleStop() {
        isPlaying = false
        releaseWakeLock()
        abandonAudioFocus()
        stopProgressTracking()

        updatePlaybackState(isStopped = true)
        mediaSession.isActive = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        MediaPlaybackManager.updatePlaybackState(isPlaying = false, positionMs = 0L)
        stopSelf()
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = serviceScope.launch {
            while (isActive && isPlaying) {
                delay(1000)
                currentPositionMs += 1000
                if (currentPositionMs >= durationMs) {
                    currentPositionMs = 0L
                }
                MediaPlaybackManager.updatePlaybackState(isPlaying = true, positionMs = currentPositionMs, durationMs = durationMs)
            }
        }
    }

    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(3 * 60 * 60 * 1000L) // 3 hours timeout safety
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    // --- AUDIO FOCUS MANAGEMENT ---
    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build()

            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss of audio focus (e.g. another media player took over)
                handlePause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss (e.g. phone call, GPS navigation prompt)
                handlePause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume or pause
                handlePause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained audio focus
                if (!isPlaying) {
                    handlePlay()
                }
            }
        }
    }

    // --- MEDIASESSION STATE & METADATA ---
    private fun updatePlaybackState(isStopped: Boolean = false) {
        val state = when {
            isStopped -> PlaybackState.STATE_STOPPED
            isPlaying -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }

        val playbackState = PlaybackState.Builder()
            .setState(state, currentPositionMs, 1.0f)
            .setActions(
                PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_STOP or
                        PlaybackState.ACTION_SEEK_TO
            )
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun updateMetadata() {
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, currentChannel)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "FLOW")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)

        currentThumbnailBitmap?.let {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, it)
        }

        mediaSession.setMetadata(metadataBuilder.build())
    }

    // --- NOTIFICATION WITH MEDIASTYLE & LOCK SCREEN CONTROLS ---
    private fun buildMediaNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntents for Media Actions
        val prevIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MediaPlaybackService::class.java).setAction(ACTION_PREVIOUS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MediaPlaybackService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MediaPlaybackService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 4,
            Intent(this, MediaPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val artBitmap = currentThumbnailBitmap ?: createFallbackArtBitmap(currentTitle)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText("$currentChannel • FLOW Background Audio")
            .setSubText("Background Audio Active")
            .setSmallIcon(R.drawable.ic_music_note)
            .setLargeIcon(artBitmap)
            .setContentIntent(contentPendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(Notification.Action.Builder(R.drawable.ic_skip_previous, "Previous", prevIntent).build())
            .addAction(Notification.Action.Builder(playPauseIcon, playPauseTitle, playPauseIntent).build())
            .addAction(Notification.Action.Builder(R.drawable.ic_skip_next, "Next", nextIntent).build())
            .addAction(Notification.Action.Builder(R.drawable.ic_stop, "Stop", stopIntent).build())
            .build()
    }

    private fun loadThumbnailBitmap(url: String) {
        if (url.isBlank()) {
            currentThumbnailBitmap = createFallbackArtBitmap(currentTitle)
            updateMetadata()
            return
        }

        serviceScope.launch {
            try {
                val imageLoader = Coil.imageLoader(applicationContext)
                val request = ImageRequest.Builder(applicationContext)
                    .data(url)
                    .allowHardware(false) // Needed for Notification Large Icon
                    .build()

                val result = withContext(Dispatchers.IO) {
                    imageLoader.execute(request)
                }

                val drawable = result.drawable
                if (drawable is BitmapDrawable) {
                    currentThumbnailBitmap = drawable.bitmap
                } else {
                    currentThumbnailBitmap = createFallbackArtBitmap(currentTitle)
                }

                updateMetadata()
                if (isPlaying) {
                    val notification = buildMediaNotification()
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                currentThumbnailBitmap = createFallbackArtBitmap(currentTitle)
                updateMetadata()
            }
        }
    }

    private fun createFallbackArtBitmap(title: String): Bitmap {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#E50914")) // FLOW Brand Red

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 96f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        val initial = title.firstOrNull()?.uppercase() ?: "F"
        val yPos = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(initial, canvas.width / 2f, yPos, paint)

        return bitmap
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopProgressTracking()
        releaseWakeLock()
        abandonAudioFocus()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession.release()
    }
}
