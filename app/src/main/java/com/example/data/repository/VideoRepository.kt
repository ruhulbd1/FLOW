package com.example.data.repository

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.api.YouTubeApiService
import com.example.data.api.YouTubeRssService
import com.example.data.model.AppNotification
import com.example.data.model.Channel
import com.example.data.model.Comment
import com.example.data.model.Playlist
import com.example.data.model.VideoItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val Context.dataStore by preferencesDataStore(name = "flow_settings")

class VideoRepository(
    private val context: Context,
    private val apiService: YouTubeApiService = YouTubeApiService.create(),
    private val rssService: YouTubeRssService = YouTubeRssService()
) {
    // In-memory real video cache to ensure immediate UI updates upon channel addition
    private val cachedRealVideos = ConcurrentHashMap<String, List<VideoItem>>()

    companion object {
        val KEY_YOUTUBE_API_KEY = stringPreferencesKey("youtube_api_key")
        val KEY_CHANNEL_ID = stringPreferencesKey("channel_id")
        val KEY_CHANNEL_IDS = stringPreferencesKey("channel_ids_csv")
        val KEY_CHANNELS_JSON = stringPreferencesKey("channels_metadata_json")
        val KEY_SAVED_VIDEOS = stringPreferencesKey("saved_videos_csv")
        val KEY_LIKED_VIDEOS = stringPreferencesKey("liked_videos_csv")
        val KEY_HISTORY_VIDEOS = stringPreferencesKey("history_videos_csv")
        val KEY_DOWNLOADED_VIDEOS = stringPreferencesKey("downloaded_videos_csv")
        val KEY_SUBSCRIPTIONS = stringPreferencesKey("subscriptions_csv")

        // Flow Engine & Settings Keys
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "System", "Dark", "Light"
        val KEY_SAVE_HISTORY = booleanPreferencesKey("save_history") // default true
        val KEY_DEEP_FLOW_MODE = booleanPreferencesKey("deep_flow_mode") // default false
        val KEY_AUTOPLAY = booleanPreferencesKey("autoplay") // default true
        val KEY_VIDEO_QUALITY = stringPreferencesKey("video_quality") // "Auto", "1080p", "720p", "360p"
        val KEY_LANGUAGE_REGION = stringPreferencesKey("language_region") // "English (US)", "Bengali (BD)", "Global"
        val KEY_STORAGE_LOCATION = stringPreferencesKey("storage_location") // "Internal Storage", "SD Card"
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled") // default true
        val KEY_BLOCKED_TOPICS = stringPreferencesKey("blocked_topics_csv")
        val KEY_PLAYER_SETTINGS_JSON = stringPreferencesKey("player_settings_json")

        val DEFAULT_CHANNELS = listOf(
            Channel(
                id = "UC7VrN38YkMmdr1hFrdaICAA", // Official Dhruba TV
                name = "Dhruba TV",
                avatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_m9_8l8Q9nI_b2rZ8G8rP0N_K4o1_L3=s900-c-k-c0x00ffffff-no-rj",
                subscribers = "5.6M subscribers",
                verified = true,
                isSubscribed = true,
                isEnabledInFeed = true
            ),
            Channel(
                id = "UCIB65_E_7XEScU2NB135wDw", // Official Coke Studio Bangla
                name = "Coke Studio Bangla",
                avatarUrl = "https://yt3.googleusercontent.com/nrwXuQgXdOlZUJQYyCJMK_K1F3Um7yT_qjC_YDrIF_1B9HCSn8JF9ygN9cpDNpThxxHfa8Z2ZDU=s900-c-k-c0x00ffffff-no-rj",
                subscribers = "3.9M subscribers",
                verified = true,
                isSubscribed = true,
                isEnabledInFeed = true
            ),
            Channel(
                id = "UCVlUfN0sAILHcqgtWSxEPRg", // Official Deepto Natok
                name = "Deepto Natok",
                avatarUrl = "https://yt3.googleusercontent.com/r6ReSFjFYh9VCmf7glNN1XJcPmuZHAgIKDIgnTGJyP8wFbQAqt1ksLcJT48qQkH6CsRH8Aga=s900-c-k-c0x00ffffff-no-rj",
                subscribers = "4.2M subscribers",
                verified = true,
                isSubscribed = true,
                isEnabledInFeed = true
            ),
            Channel(
                id = "UC9FHe3sU-2xmFyEOOHWGbxA", // Official CMV Natok
                name = "CMV Natok",
                avatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_ki3dMOtrCmIk9-G0PdRPR4757n6sSXogSDZJ3t_oArrw=s900-c-k-c0x00ffffff-no-rj",
                subscribers = "6.1M subscribers",
                verified = true,
                isSubscribed = true,
                isEnabledInFeed = true
            ),
            Channel(
                id = "UCaE9aMdlRomFYL4LkGhC1FQ", // Official NAF Entertainment
                name = "NAF Entertainment",
                avatarUrl = "https://yt3.googleusercontent.com/WcJVfCa-wGUfUauDu-958MnOrkdzyOX440KdsY4QaKUA_HxyOQhxXOqDxzYYnYsHCyIx65dj2w=s900-c-k-c0x00ffffff-no-rj",
                subscribers = "2.8M subscribers",
                verified = true,
                isSubscribed = true,
                isEnabledInFeed = true
            ),
            Channel(
                id = "UC2PvBto6gvSLVub6CdwaivA", // Official Rtv Drama
                name = "Rtv Drama",
                avatarUrl = "https://yt3.googleusercontent.com/r6ReSFjFYh9VCmf7glNN1XJcPmuZHAgIKDIgnTGJyP8wFbQAqt1ksLcJT48qQkH6CsRH8Aga=s900-c-k-c0x00ffffff-no-rj",
                subscribers = "8.4M subscribers",
                verified = true,
                isSubscribed = true,
                isEnabledInFeed = true
            ),
            Channel(
                id = "UC_x5XG1OV2P6uZZ5FSM9Ttw", // Official Google for Developers
                name = "Google for Developers",
                avatarUrl = "https://yt3.googleusercontent.com/r99eRlOesQkJe7pOzDY4JazAlhidoVp-0syxBnYjfeHdmUXGa5zOxcozaxKF2oPHg3RkglecZQ=s900-c-k-c0x00ffffff-no-rj",
                subscribers = "2.4M subscribers",
                verified = true,
                isSubscribed = true,
                isEnabledInFeed = true
            ),
            Channel(
                id = "UCBJycsmduvYEL83R_U4JriQ", // Official Marques Brownlee (MKBHD)
                name = "Marques Brownlee",
                avatarUrl = "https://yt3.googleusercontent.com/qu4TmIaYUlS41-dJ9gZ7DUR3nilvmB5_11i6OKSdvNnBNiyOusZP1bMN6ICnuxtjFBb6ioKgRQ=s900-c-k-c0x00ffffff-no-rj",
                subscribers = "18.5M subscribers",
                verified = true,
                isSubscribed = false,
                isEnabledInFeed = false
            )
        )

        val DEFAULT_CHANNEL_IDS = DEFAULT_CHANNELS.filter { it.isEnabledInFeed }.map { it.id }

        fun resolveToCanonicalId(id: String): String {
            return when (id) {
                "c_deepto" -> "UCVlUfN0sAILHcqgtWSxEPRg"
                "c_naf" -> "UCaE9aMdlRomFYL4LkGhC1FQ"
                "c_cmv" -> "UC9FHe3sU-2xmFyEOOHWGbxA"
                "c_csb", "c_coke_studio" -> "UCIB65_E_7XEScU2NB135wDw"
                "c_rtv", "rtvdrama", "@rtvdrama" -> "UC2PvBto6gvSLVub6CdwaivA"
                "c_dhruba", "dhrubatv", "@dhrubatv" -> "UC7VrN38YkMmdr1hFrdaICAA"
                else -> id
            }
        }
    }

    private val channelsMoshiAdapter by lazy {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, Channel::class.java)
        moshi.adapter<List<Channel>>(type)
    }

    // Dynamic shorts repository state: collects any shorts found in channel feeds and routes them to Shorts tab
    private val _dynamicShorts = MutableStateFlow<List<VideoItem>>(emptyList())
    val shortsFlow: Flow<List<VideoItem>> = _dynamicShorts.map { dynamic ->
        (dynamic + getCuratedShorts()).distinctBy { it.id }
    }

    fun registerShorts(newShorts: List<VideoItem>) {
        if (newShorts.isEmpty()) return
        val current = _dynamicShorts.value.toMutableList()
        for (item in newShorts) {
            val normalized = item.copy(isShort = true, category = "Shorts")
            val existingIdx = current.indexOfFirst { it.id == normalized.id }
            if (existingIdx >= 0) {
                current[existingIdx] = normalized
            } else {
                current.add(0, normalized)
            }
        }
        _dynamicShorts.value = current
    }

    /**
     * Robust duration parser: handles ISO-8601 strings (e.g. 'PT45S', 'PT52S', 'PT1M', 'PT1M30S', '45S', 'PT0M45S')
     * and clock formats ('0:45', '00:52', '1:00', '38:59', '1:12:30', '45') returning total seconds.
     */
    fun parseDurationSeconds(duration: String?): Int {
        if (duration.isNullOrBlank()) return 180
        val clean = duration.trim().uppercase()

        // 1. ISO-8601 duration handling (e.g. "PT45S", "PT1M", "PT1M30S", "PT52S", "45S", "0M45S")
        if (clean.contains("P") || clean.contains("T") || clean.endsWith("S") || clean.contains("M")) {
            try {
                var hours = 0
                var minutes = 0
                var seconds = 0
                var currentNum = ""

                val payload = clean.removePrefix("P").removePrefix("T")
                for (ch in payload) {
                    if (ch.isDigit()) {
                        currentNum += ch
                    } else {
                        val value = currentNum.toIntOrNull() ?: 0
                        currentNum = ""
                        when (ch) {
                            'H' -> hours = value
                            'M' -> minutes = value
                            'S' -> seconds = value
                        }
                    }
                }
                val total = (hours * 3600) + (minutes * 60) + seconds
                if (total > 0 || clean == "PT0S" || clean == "0S") {
                    return total
                }
            } catch (_: Exception) {}
        }

        // 2. Standard clock format handling (e.g. "0:45", "00:52", "1:00", "38:59", "1:12:30", "45")
        try {
            val parts = clean.split(":").map { segment ->
                segment.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
            return when (parts.size) {
                1 -> parts[0] // e.g. "45" or "52"
                2 -> (parts[0] * 60) + parts[1] // "0:45" -> 45, "1:00" -> 60, "38:59" -> 2339
                3 -> (parts[0] * 3600) + (parts[1] * 60) + parts[2] // "1:12:30"
                else -> 180
            }
        } catch (_: Exception) {
            return 180
        }
    }

    /**
     * Converts ISO-8601 duration string like 'PT1M30S' or 'PT45S' to user-facing '1:30' or '0:45'.
     */
    fun parseIsoDurationToDisplay(isoDuration: String?): String {
        if (isoDuration.isNullOrBlank()) return "15:20"
        return try {
            val totalSec = parseDurationSeconds(isoDuration)
            val hours = totalSec / 3600
            val minutes = (totalSec % 3600) / 60
            val seconds = totalSec % 60
            if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        } catch (_: Exception) {
            "15:20"
        }
    }

    /**
     * Robust Short Video Detection:
     * - video.isShort == true or category is "Shorts"
     * - ID indicator (e.g. starts with "short")
     * - Title or description contains #shorts or #Short / #short (case-insensitive)
     * - video.duration contains "S" (ISO 8601 duration like PT45S, 45S without hours)
     * - Parsed total seconds <= 60 (covers "0:45", "0:52", "00:58", "1:00", "PT45S", etc.)
     */
    fun isShortVideo(video: VideoItem): Boolean {
        // 1. Explicit short flag or Shorts category
        if (video.isShort) return true
        if (video.category.equals("Shorts", ignoreCase = true)) return true

        // 2. ID indicator
        if (video.id.startsWith("short", ignoreCase = true)) return true

        // 3. Title or Description hashtags (#shorts, #Shorts, #short, #Short)
        val titleLower = video.title.lowercase()
        val descLower = video.description.lowercase()
        if (titleLower.contains("#shorts") || titleLower.contains("#short")) return true
        if (descLower.contains("#shorts") || descLower.contains("#short")) return true

        // 4. Check ISO 8601 duration containing "S" without hours (e.g. "PT45S", "PT52S", "45S")
        val durUpper = video.duration.trim().uppercase()
        if (durUpper.contains("S") && !durUpper.contains("H")) {
            // Pure seconds without "M" is always under 60 seconds
            if (!durUpper.contains("M")) {
                return true
            }
        }

        // 5. Parsed total duration in seconds <= 60 (covers "0:45", "0:52", "00:58", "1:00", etc.)
        val durationSec = parseDurationSeconds(video.duration)
        if (durationSec in 1..60) {
            return true
        }

        return false
    }

    private fun getBuildConfigApiKey(): String {
        return try {
            val field = Class.forName("com.example.BuildConfig").getField("YOUTUBE_API_KEY")
            val value = field.get(null) as? String
            if (!value.isNullOrBlank() && value != "YOUR_YOUTUBE_API_KEY") value else ""
        } catch (_: Throwable) {
            ""
        }
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val userKey = preferences[KEY_YOUTUBE_API_KEY]?.trim() ?: ""
        if (userKey.isNotBlank() && userKey != "YOUR_YOUTUBE_API_KEY") {
            userKey
        } else {
            getBuildConfigApiKey()
        }
    }

    val channelsFlow: Flow<List<Channel>> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_CHANNELS_JSON]
        val rawChannels = if (!json.isNullOrBlank()) {
            try {
                val parsed = channelsMoshiAdapter.fromJson(json) ?: DEFAULT_CHANNELS
                // Ensure default preset channels (Dhruba TV, Coke Studio Bangla, Deepto, CMV, etc.) are present
                val existingIds = parsed.map { it.id }.toSet()
                val merged = parsed.toMutableList()
                DEFAULT_CHANNELS.forEach { def ->
                    if (!existingIds.contains(def.id)) {
                        merged.add(def)
                    }
                }
                merged
            } catch (e: Exception) {
                DEFAULT_CHANNELS
            }
        } else {
            DEFAULT_CHANNELS
        }

        // Proactively sanitize and upgrade any legacy Unsplash or blank avatars to real YouTube avatars
        rawChannels.map { channel ->
            if (channel.avatarUrl.isBlank() || channel.avatarUrl.contains("unsplash.com")) {
                val realAvatar = rssService.getKnownRealAvatar(channel.id)
                if (realAvatar.isNotBlank()) channel.copy(avatarUrl = realAvatar) else channel
            } else {
                channel
            }
        }
    }

    suspend fun initializeDefaultChannelsIfFirstLaunch() {
        context.dataStore.edit { preferences ->
            val json = preferences[KEY_CHANNELS_JSON]
            if (json.isNullOrBlank()) {
                preferences[KEY_CHANNELS_JSON] = channelsMoshiAdapter.toJson(DEFAULT_CHANNELS)
                preferences[KEY_CHANNEL_IDS] = DEFAULT_CHANNELS.filter { it.isEnabledInFeed }.map { it.id }.joinToString(",")
            } else {
                try {
                    val existing = channelsMoshiAdapter.fromJson(json)?.toMutableList() ?: mutableListOf()
                    val existingIds = existing.map { it.id }.toSet()
                    var changed = false
                    DEFAULT_CHANNELS.forEach { def ->
                        if (!existingIds.contains(def.id)) {
                            existing.add(def)
                            changed = true
                        }
                    }
                    if (changed) {
                        preferences[KEY_CHANNELS_JSON] = channelsMoshiAdapter.toJson(existing)
                        preferences[KEY_CHANNEL_IDS] = existing.filter { it.isEnabledInFeed }.map { it.id }.joinToString(",")
                    }
                } catch (e: Exception) {
                    preferences[KEY_CHANNELS_JSON] = channelsMoshiAdapter.toJson(DEFAULT_CHANNELS)
                    preferences[KEY_CHANNEL_IDS] = DEFAULT_CHANNELS.filter { it.isEnabledInFeed }.map { it.id }.joinToString(",")
                }
            }
        }
    }

    val channelIdsFlow: Flow<List<String>> = channelsFlow.map { channels ->
        channels.filter { it.isEnabledInFeed }.map { it.id }
    }

    val savedVideoIdsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_SAVED_VIDEOS]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    val likedVideoIdsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_LIKED_VIDEOS]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    val historyVideoIdsFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_HISTORY_VIDEOS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val downloadedVideoIdsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_DOWNLOADED_VIDEOS]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    val subscriptionsFlow: Flow<Set<String>> = channelsFlow.map { channels ->
        channels.filter { it.isSubscribed }.map { it.id }.toSet()
    }

    // Settings Flows
    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "Dark"
    }

    val saveHistoryFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SAVE_HISTORY] ?: true
    }

    val deepFlowModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEEP_FLOW_MODE] ?: false
    }

    val autoplayFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTOPLAY] ?: true
    }

    val videoQualityFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_VIDEO_QUALITY] ?: "1080p"
    }

    val languageRegionFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE_REGION] ?: "English (US)"
    }

    val storageLocationFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_STORAGE_LOCATION] ?: "Internal Storage"
    }

    val blockedTopicsFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_BLOCKED_TOPICS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val playerSettingsFlow: Flow<com.example.data.model.PlayerSettings> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_PLAYER_SETTINGS_JSON]
        if (!json.isNullOrBlank()) {
            try {
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(com.example.data.model.PlayerSettings::class.java)
                adapter.fromJson(json) ?: com.example.data.model.PlayerSettings()
            } catch (_: Exception) {
                com.example.data.model.PlayerSettings()
            }
        } else {
            com.example.data.model.PlayerSettings()
        }
    }

    suspend fun savePlayerSettings(settings: com.example.data.model.PlayerSettings) {
        try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(com.example.data.model.PlayerSettings::class.java)
            val json = adapter.toJson(settings)
            context.dataStore.edit { preferences ->
                preferences[KEY_PLAYER_SETTINGS_JSON] = json
            }
        } catch (_: Exception) {}
    }

    // Setters
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setSaveHistory(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SAVE_HISTORY] = enabled }
    }

    suspend fun setDeepFlowMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DEEP_FLOW_MODE] = enabled }
    }

    suspend fun setAutoplay(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTOPLAY] = enabled }
    }

    suspend fun setVideoQuality(quality: String) {
        context.dataStore.edit { it[KEY_VIDEO_QUALITY] = quality }
    }

    suspend fun setLanguageRegion(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE_REGION] = lang }
    }

    suspend fun setStorageLocation(location: String) {
        context.dataStore.edit { it[KEY_STORAGE_LOCATION] = location }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun addBlockedTopic(topic: String) {
        val trimmed = topic.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit { preferences ->
            val list = preferences[KEY_BLOCKED_TOPICS]?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            if (!list.contains(trimmed)) {
                list.add(trimmed)
            }
            preferences[KEY_BLOCKED_TOPICS] = list.joinToString(",")
        }
    }

    suspend fun removeBlockedTopic(topic: String) {
        val trimmed = topic.trim()
        context.dataStore.edit { preferences ->
            val list = preferences[KEY_BLOCKED_TOPICS]?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            list.remove(trimmed)
            preferences[KEY_BLOCKED_TOPICS] = list.joinToString(",")
        }
    }

    suspend fun clearHistoryAndCache() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_HISTORY_VIDEOS)
        }
    }

    suspend fun saveApiConfig(apiKey: String, channelIds: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_YOUTUBE_API_KEY] = apiKey.trim()
            val existing = channelsFlow.first().toMutableList()
            val updated = existing.map { ch ->
                if (channelIds.contains(ch.id)) ch.copy(isEnabledInFeed = true)
                else ch.copy(isEnabledInFeed = false)
            }
            preferences[KEY_CHANNELS_JSON] = channelsMoshiAdapter.toJson(updated)
            preferences[KEY_CHANNEL_IDS] = channelIds.map { it.trim() }.filter { it.isNotBlank() }.joinToString(",")
            preferences[KEY_CHANNEL_ID] = channelIds.firstOrNull()?.trim() ?: ""
        }
    }

    // Dynamic Multi-Channel Management with Instant YouTube RSS & API Integration
    suspend fun addChannel(input: String): Channel {
        val trimmed = input.trim()
        require(trimmed.isNotBlank()) { "Channel identifier cannot be empty" }

        // 1. Resolve channel ID, real title, and avatar via YouTube RSS & Web parser
        val resolvedFromRss = rssService.resolveChannel(trimmed)

        var finalChannelId = resolvedFromRss?.channelId ?: trimmed
        var resolvedName = resolvedFromRss?.title ?: ""
        var resolvedAvatar = resolvedFromRss?.avatarUrl ?: ""
        var resolvedSubs = resolvedFromRss?.subscriberCount ?: "Active creator"

        // 2. If API key is provided, also try YouTube Data API v3 for official stats
        val apiKey = apiKeyFlow.first()
        if (apiKey.isNotBlank() && apiKey != "YOUR_YOUTUBE_API_KEY") {
            try {
                val handle = when {
                    trimmed.startsWith("@") -> trimmed.removePrefix("@")
                    trimmed.contains("youtube.com/@") -> trimmed.substringAfter("youtube.com/@").substringBefore("/").substringBefore("?")
                    else -> null
                }

                val response = if (handle != null) {
                    apiService.getChannels(apiKey = apiKey, forHandle = handle)
                } else if (finalChannelId.startsWith("UC")) {
                    apiService.getChannels(apiKey = apiKey, id = finalChannelId)
                } else {
                    null
                }

                val item = response?.items?.firstOrNull()
                if (item != null) {
                    finalChannelId = item.id ?: finalChannelId
                    val snippet = item.snippet
                    if (!snippet?.title.isNullOrBlank()) {
                        resolvedName = snippet!!.title!!
                    }
                    val thumb = snippet?.thumbnails?.medium?.url ?: snippet?.thumbnails?.defaultThumb?.url
                    if (!thumb.isNullOrBlank()) {
                        resolvedAvatar = thumb
                    }
                    val subCount = item.statistics?.subscriberCount?.toLongOrNull()
                    if (subCount != null) {
                        resolvedSubs = formatSubscriberCount(subCount)
                    }
                }
            } catch (e: Exception) {
                // Ignore API error and proceed with resolved RSS info
            }
        }

        val canonicalId = resolveToCanonicalId(finalChannelId)

        // 3. Fallbacks if name or avatar is still empty
        if (resolvedName.isBlank()) {
            resolvedName = when {
                trimmed.startsWith("@") -> trimmed
                finalChannelId.startsWith("UC") -> "Channel ${finalChannelId.takeLast(6)}"
                else -> trimmed
            }
        }
        if (resolvedAvatar.isBlank() || resolvedAvatar.contains("unsplash.com")) {
            val realKnown = rssService.getKnownRealAvatar(canonicalId)
            resolvedAvatar = if (realKnown.isNotBlank()) realKnown else rssService.fetchChannelAvatar(canonicalId)
        }

        val newChannel = Channel(
            id = canonicalId,
            name = resolvedName,
            avatarUrl = resolvedAvatar,
            subscribers = resolvedSubs,
            verified = true,
            isSubscribed = true,
            isEnabledInFeed = true,
            isCustom = true
        )

        // 4. Immediately trigger background network fetch of real videos for this new channel!
        if (canonicalId.startsWith("UC")) {
            try {
                val freshRealVideos = rssService.fetchChannelFeed(canonicalId, resolvedAvatar)
                if (freshRealVideos.isNotEmpty()) {
                    cachedRealVideos[canonicalId] = freshRealVideos
                }
            } catch (e: Exception) {
                // Network error handled gracefully
            }
        }

        // 5. Update local DataStore state
        context.dataStore.edit { preferences ->
            val currentChannels = channelsFlow.first().toMutableList()
            val existingIdx = currentChannels.indexOfFirst { it.id == canonicalId }
            if (existingIdx >= 0) {
                currentChannels[existingIdx] = currentChannels[existingIdx].copy(
                    isEnabledInFeed = true,
                    isSubscribed = true,
                    name = resolvedName.ifBlank { currentChannels[existingIdx].name },
                    avatarUrl = resolvedAvatar.ifBlank { currentChannels[existingIdx].avatarUrl }
                )
            } else {
                currentChannels.add(0, newChannel)
            }

            preferences[KEY_CHANNELS_JSON] = channelsMoshiAdapter.toJson(currentChannels)
            preferences[KEY_CHANNEL_IDS] = currentChannels.filter { it.isEnabledInFeed }.map { it.id }.joinToString(",")
        }

        return newChannel
    }

    suspend fun toggleChannelFeedInclusion(channelId: String, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            val currentChannels = channelsFlow.first().toMutableList()
            val idx = currentChannels.indexOfFirst { it.id == channelId }
            if (idx >= 0) {
                currentChannels[idx] = currentChannels[idx].copy(isEnabledInFeed = isEnabled)
                preferences[KEY_CHANNELS_JSON] = channelsMoshiAdapter.toJson(currentChannels)
                preferences[KEY_CHANNEL_IDS] = currentChannels.filter { it.isEnabledInFeed }.map { it.id }.joinToString(",")
            }
        }
    }

    suspend fun toggleSubscription(channelId: String) {
        context.dataStore.edit { preferences ->
            val currentChannels = channelsFlow.first().toMutableList()
            val idx = currentChannels.indexOfFirst { it.id == channelId }
            if (idx >= 0) {
                val newSub = !currentChannels[idx].isSubscribed
                currentChannels[idx] = currentChannels[idx].copy(isSubscribed = newSub)
                preferences[KEY_CHANNELS_JSON] = channelsMoshiAdapter.toJson(currentChannels)
                preferences[KEY_SUBSCRIPTIONS] = currentChannels.filter { it.isSubscribed }.map { it.id }.joinToString(",")
            }
        }
    }

    suspend fun removeChannel(channelId: String) {
        context.dataStore.edit { preferences ->
            val currentChannels = channelsFlow.first().toMutableList()
            currentChannels.removeAll { it.id == channelId }
            preferences[KEY_CHANNELS_JSON] = channelsMoshiAdapter.toJson(currentChannels)
            preferences[KEY_CHANNEL_IDS] = currentChannels.filter { it.isEnabledInFeed }.map { it.id }.joinToString(",")
        }
    }

    suspend fun resetChannelsToDefault() {
        context.dataStore.edit { preferences ->
            preferences[KEY_CHANNELS_JSON] = channelsMoshiAdapter.toJson(DEFAULT_CHANNELS)
            preferences[KEY_CHANNEL_IDS] = DEFAULT_CHANNELS.filter { it.isEnabledInFeed }.map { it.id }.joinToString(",")
            preferences[KEY_YOUTUBE_API_KEY] = ""
        }
    }

    suspend fun addChannelId(channelId: String) {
        addChannel(channelId)
    }

    suspend fun removeChannelId(channelId: String) {
        removeChannel(channelId)
    }

    private fun formatSubscriberCount(count: Long): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM subscribers", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK subscribers", count / 1_000.0)
            else -> "$count subscribers"
        }
    }

    suspend fun toggleSaveVideo(videoId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_SAVED_VIDEOS]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            if (current.contains(videoId)) {
                current.remove(videoId)
            } else {
                current.add(videoId)
            }
            preferences[KEY_SAVED_VIDEOS] = current.joinToString(",")
        }
    }

    suspend fun toggleLikeVideo(videoId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_LIKED_VIDEOS]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            if (current.contains(videoId)) {
                current.remove(videoId)
            } else {
                current.add(videoId)
            }
            preferences[KEY_LIKED_VIDEOS] = current.joinToString(",")
        }
    }

    suspend fun addDownloadedVideo(videoId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_DOWNLOADED_VIDEOS]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.add(videoId)
            preferences[KEY_DOWNLOADED_VIDEOS] = current.joinToString(",")
        }
    }

    suspend fun removeDownloadedVideo(videoId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_DOWNLOADED_VIDEOS]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.remove(videoId)
            preferences[KEY_DOWNLOADED_VIDEOS] = current.joinToString(",")
        }
    }

    suspend fun addToHistory(videoId: String) {
        val saveHistoryEnabled = saveHistoryFlow.first()
        if (!saveHistoryEnabled) return

        context.dataStore.edit { preferences ->
            val current = preferences[KEY_HISTORY_VIDEOS]?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            current.remove(videoId)
            current.add(0, videoId)
            if (current.size > 50) {
                preferences[KEY_HISTORY_VIDEOS] = current.take(50).joinToString(",")
            } else {
                preferences[KEY_HISTORY_VIDEOS] = current.joinToString(",")
            }
        }
    }

    suspend fun exportStateBackup(): String {
        val prefs = context.dataStore.data.first()
        val json = JSONObject().apply {
            put("api_key", prefs[KEY_YOUTUBE_API_KEY] ?: "")
            put("channels", prefs[KEY_CHANNELS_JSON] ?: "")
            put("saved", prefs[KEY_SAVED_VIDEOS] ?: "")
            put("liked", prefs[KEY_LIKED_VIDEOS] ?: "")
            put("history", prefs[KEY_HISTORY_VIDEOS] ?: "")
            put("downloads", prefs[KEY_DOWNLOADED_VIDEOS] ?: "")
            put("theme", prefs[KEY_THEME_MODE] ?: "Dark")
            put("quality", prefs[KEY_VIDEO_QUALITY] ?: "1080p")
            put("deep_flow", prefs[KEY_DEEP_FLOW_MODE] ?: false)
            put("save_history", prefs[KEY_SAVE_HISTORY] ?: true)
            put("autoplay", prefs[KEY_AUTOPLAY] ?: true)
        }
        return json.toString(2)
    }

    suspend fun restoreStateBackup(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            context.dataStore.edit { prefs ->
                if (json.has("api_key")) prefs[KEY_YOUTUBE_API_KEY] = json.getString("api_key")
                if (json.has("channels")) prefs[KEY_CHANNELS_JSON] = json.getString("channels")
                if (json.has("saved")) prefs[KEY_SAVED_VIDEOS] = json.getString("saved")
                if (json.has("liked")) prefs[KEY_LIKED_VIDEOS] = json.getString("liked")
                if (json.has("history")) prefs[KEY_HISTORY_VIDEOS] = json.getString("history")
                if (json.has("downloads")) prefs[KEY_DOWNLOADED_VIDEOS] = json.getString("downloads")
                if (json.has("theme")) prefs[KEY_THEME_MODE] = json.getString("theme")
                if (json.has("quality")) prefs[KEY_VIDEO_QUALITY] = json.getString("quality")
                if (json.has("deep_flow")) prefs[KEY_DEEP_FLOW_MODE] = json.getBoolean("deep_flow")
                if (json.has("save_history")) prefs[KEY_SAVE_HISTORY] = json.getBoolean("save_history")
                if (json.has("autoplay")) prefs[KEY_AUTOPLAY] = json.getBoolean("autoplay")
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getVideos(
        categoryFilter: String = "All",
        searchQuery: String = "",
        selectedChannelFilter: String? = null
    ): Flow<List<VideoItem>> = flow {
        val apiKey = apiKeyFlow.first()
        val allChannels = channelsFlow.first()
        val enabledChannels = allChannels.filter { it.isEnabledInFeed }
        val blockedTopics = blockedTopicsFlow.first()

        val results = mutableListOf<VideoItem>()

        // 1. Determine which channels to query
        val channelsToQuery: List<Channel> = if (!selectedChannelFilter.isNullOrBlank() && selectedChannelFilter != "All") {
            val matched = allChannels.find { it.id == selectedChannelFilter }
            if (matched != null) listOf(matched) else listOf(Channel(id = selectedChannelFilter, name = "Creator"))
        } else if (enabledChannels.isNotEmpty()) {
            enabledChannels
        } else {
            allChannels.take(6)
        }

        // 2. Instant emission from cache if available so UI renders immediately
        val cachedList = mutableListOf<VideoItem>()
        for (channel in channelsToQuery) {
            val canonicalId = resolveToCanonicalId(channel.id)
            cachedRealVideos[canonicalId]?.let { cachedList.addAll(it) }
        }
        if (cachedList.isNotEmpty()) {
            cachedList.sortByDescending { it.publishedAtIso.ifEmpty { it.publishedAt } }
            val (cachedHome, cachedShorts) = cachedList.distinctBy { it.id }.partition { !isShortVideo(it) }
            if (cachedShorts.isNotEmpty()) {
                registerShorts(cachedShorts)
            }
            emit(applyVideoFilters(cachedHome, blockedTopics, categoryFilter, searchQuery))
        }

        // 3. Fetch dynamically from YouTube RSS feed and YouTube Data API in parallel
        try {
            coroutineScope {
                val deferredList = channelsToQuery.map { channel ->
                    async(Dispatchers.IO) {
                        val canonicalId = resolveToCanonicalId(channel.id)
                        val channelVideos = mutableListOf<VideoItem>()

                        // A. If API key is provided, try YouTube Data API v3
                        if (apiKey.isNotBlank() && apiKey != "YOUR_YOUTUBE_API_KEY") {
                            try {
                                val queryParam = if (searchQuery.isNotBlank()) searchQuery
                                else if (categoryFilter != "All" && categoryFilter != "New to you") categoryFilter
                                else null

                                val channelParam = if (canonicalId.startsWith("UC")) canonicalId else null
                                val fetchedApiVideos = mutableListOf<VideoItem>()

                                // A1. Low-quota path: use channel uploads playlist (UU...) which costs only 1 quota point
                                if (canonicalId.startsWith("UC") && canonicalId.length == 24 && queryParam == null) {
                                    val uploadsPlaylistId = "UU" + canonicalId.substring(2)
                                    try {
                                        val playlistResp = apiService.getPlaylistItems(
                                            apiKey = apiKey,
                                            playlistId = uploadsPlaylistId,
                                            maxResults = 50
                                        )
                                        val videoIds = playlistResp.items?.mapNotNull { it.contentDetails?.videoId } ?: emptyList()
                                        if (videoIds.isNotEmpty()) {
                                            val detailsResp = try {
                                                apiService.getVideoDetails(apiKey = apiKey, id = videoIds.take(50).joinToString(","))
                                            } catch (_: Exception) { null }

                                            val detailsMap = detailsResp?.items?.associateBy { it.id ?: "" } ?: emptyMap()

                                            playlistResp.items?.forEach { item ->
                                                val vid = item.contentDetails?.videoId ?: ""
                                                val snippet = item.snippet
                                                if (vid.isNotBlank() && snippet != null) {
                                                    val detail = detailsMap[vid]
                                                    val rawDuration = detail?.contentDetails?.duration ?: ""
                                                    val viewCountRaw = detail?.statistics?.viewCount?.toLongOrNull()
                                                    val formattedViews = if (viewCountRaw != null) rssService.formatViews(viewCountRaw) else rssService.formatViews((Math.abs(vid.hashCode()) % 850L + 15) * 1000L)
                                                    val displayDuration = if (rawDuration.isNotBlank()) parseIsoDurationToDisplay(rawDuration) else "15:20"
                                                    val durationSec = parseDurationSeconds(if (rawDuration.isNotBlank()) rawDuration else displayDuration)
                                                    val isShort = durationSec in 1..60 || snippet.title?.contains("#shorts", ignoreCase = true) == true
                                                    val channelAvatar = if (channel.avatarUrl.isNotBlank() && !channel.avatarUrl.contains("unsplash.com")) {
                                                        channel.avatarUrl
                                                    } else {
                                                        rssService.getKnownRealAvatar(canonicalId)
                                                    }
                                                    val publishedDate = formatPublishedTime(snippet.publishedAt)

                                                    fetchedApiVideos.add(
                                                        VideoItem(
                                                            id = vid,
                                                            title = snippet.title?.replace("&quot;", "\"")?.replace("&#39;", "'")?.replace("&amp;", "&") ?: "YouTube Video",
                                                            description = snippet.description ?: "",
                                                            channelTitle = snippet.channelTitle ?: channel.name,
                                                            channelId = snippet.channelId ?: canonicalId,
                                                            channelAvatarUrl = channelAvatar,
                                                            thumbnailUrl = snippet.thumbnails?.high?.url
                                                                ?: snippet.thumbnails?.medium?.url
                                                                ?: "https://i.ytimg.com/vi/$vid/hq720.jpg",
                                                            publishedAt = publishedDate,
                                                            publishedAtIso = snippet.publishedAt ?: "",
                                                            viewCount = formattedViews,
                                                            duration = displayDuration,
                                                            category = if (isShort) "Shorts" else categoryFilter,
                                                            isShort = isShort,
                                                            streamId = vid
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {
                                        // Fallback to search endpoint
                                    }
                                }

                                // A2. Search endpoint fallback (or when specific search query is present)
                                if (fetchedApiVideos.isEmpty()) {
                                    val response = apiService.searchVideos(
                                        apiKey = apiKey,
                                        channelId = channelParam,
                                        query = queryParam,
                                        order = "date",
                                        maxResults = 50
                                    )

                                    val videoIds = response.items?.mapNotNull { it.id?.videoId } ?: emptyList()
                                    val detailsResp = if (videoIds.isNotEmpty()) {
                                        try {
                                            apiService.getVideoDetails(apiKey = apiKey, id = videoIds.take(50).joinToString(","))
                                        } catch (_: Exception) { null }
                                    } else null
                                    val detailsMap = detailsResp?.items?.associateBy { it.id ?: "" } ?: emptyMap()

                                    response.items?.mapNotNull { item ->
                                        val videoId = item.id?.videoId
                                        if (!videoId.isNullOrBlank()) {
                                            val snippet = item.snippet
                                            val detail = detailsMap[videoId]
                                            val rawDuration = detail?.contentDetails?.duration ?: ""
                                            val viewCountRaw = detail?.statistics?.viewCount?.toLongOrNull()
                                            val formattedViews = if (viewCountRaw != null) rssService.formatViews(viewCountRaw) else rssService.formatViews((Math.abs(videoId.hashCode()) % 850L + 15) * 1000L)
                                            val displayDuration = if (rawDuration.isNotBlank()) parseIsoDurationToDisplay(rawDuration) else "15:20"
                                            val durationSec = parseDurationSeconds(if (rawDuration.isNotBlank()) rawDuration else displayDuration)
                                            val isShort = durationSec in 1..60 || snippet?.title?.contains("#shorts", ignoreCase = true) == true

                                            val publishedDate = formatPublishedTime(snippet?.publishedAt)
                                            val channelTitle = snippet?.channelTitle ?: channel.name
                                            val channelAvatar = if (channel.avatarUrl.isNotBlank() && !channel.avatarUrl.contains("unsplash.com")) {
                                                channel.avatarUrl
                                            } else {
                                                rssService.getKnownRealAvatar(canonicalId)
                                            }

                                            VideoItem(
                                                id = videoId,
                                                title = snippet?.title?.replace("&quot;", "\"")?.replace("&#39;", "'")?.replace("&amp;", "&") ?: "YouTube Video",
                                                description = snippet?.description ?: "",
                                                channelTitle = channelTitle,
                                                channelId = snippet?.channelId ?: canonicalId,
                                                channelAvatarUrl = channelAvatar,
                                                thumbnailUrl = snippet?.thumbnails?.high?.url
                                                    ?: snippet?.thumbnails?.medium?.url
                                                    ?: "https://i.ytimg.com/vi/$videoId/hq720.jpg",
                                                publishedAt = publishedDate,
                                                publishedAtIso = snippet?.publishedAt ?: "",
                                                viewCount = formattedViews,
                                                duration = displayDuration,
                                                category = if (isShort) "Shorts" else categoryFilter,
                                                isShort = isShort,
                                                streamId = videoId
                                            )
                                        } else null
                                    }?.let { fetchedApiVideos.addAll(it) }
                                }

                                if (fetchedApiVideos.isNotEmpty()) {
                                    channelVideos.addAll(fetchedApiVideos)
                                    cachedRealVideos[canonicalId] = fetchedApiVideos
                                }
                            } catch (e: Exception) {
                                // Fallback to RSS feed
                            }
                        }

                        // B. If API didn't return videos (e.g. no key or quota exceeded), query official YouTube RSS Atom feed!
                        if (channelVideos.isEmpty() && canonicalId.startsWith("UC")) {
                            try {
                                val rssVideos = rssService.fetchChannelFeed(canonicalId, channel.avatarUrl)
                                if (rssVideos.isNotEmpty()) {
                                    channelVideos.addAll(rssVideos)
                                    cachedRealVideos[canonicalId] = rssVideos
                                }
                            } catch (e: Exception) {
                                // RSS network failed
                            }
                        }

                        // C. Fallback to cached entries if network was unreachable
                        if (channelVideos.isEmpty()) {
                            cachedRealVideos[canonicalId]?.let { channelVideos.addAll(it) }
                        }

                        channelVideos
                    }
                }

                val allRealVideos = deferredList.awaitAll().flatten()
                results.addAll(allRealVideos)
            }
        } catch (e: Exception) {
            // Error handling
        }

        // 4. Fallback / demo handling: only if ZERO real videos could be fetched or cached
        if (results.isEmpty()) {
            val allDemos = getDemoVideos()
            val enabledIds = channelsToQuery.map { it.id }.toSet()

            val matchingDemos = if (!selectedChannelFilter.isNullOrBlank() && selectedChannelFilter != "All") {
                allDemos.filter { it.channelId == selectedChannelFilter }
            } else {
                allDemos.filter { enabledIds.contains(it.channelId) }
            }

            results.addAll(if (matchingDemos.isNotEmpty()) matchingDemos else allDemos)
        }

        // 5. Distinct by video ID to eliminate any duplicate entries
        val distinctResults = results.distinctBy { it.id }.toMutableList()

        // 6. Sort chronologically by upload date (most recent first)
        distinctResults.sortByDescending { it.publishedAtIso.ifEmpty { it.publishedAt } }

        // 7. Route shorts exclusively to Shorts tab and keep Home feed clean
        val (homeCandidates, detectedShorts) = distinctResults.partition { !isShortVideo(it) }
        if (detectedShorts.isNotEmpty()) {
            registerShorts(detectedShorts)
        }

        // 8. Apply filters (categories, search query, and blocked topics) and emit for the Home feed
        emit(applyVideoFilters(homeCandidates, blockedTopics, categoryFilter, searchQuery))
    }

    private fun applyVideoFilters(
        videos: List<VideoItem>,
        blockedTopics: List<String>,
        categoryFilter: String,
        searchQuery: String
    ): List<VideoItem> {
        // 1. Partition out all Shorts and route them exclusively to the Shorts section/tab
        val (longVideos, detectedShorts) = videos.partition { video -> !isShortVideo(video) }
        if (detectedShorts.isNotEmpty()) {
            registerShorts(detectedShorts)
        }

        var filtered = longVideos

        if (blockedTopics.isNotEmpty()) {
            filtered = filtered.filter { video ->
                blockedTopics.none { blocked ->
                    video.title.contains(blocked, ignoreCase = true) ||
                            video.channelTitle.contains(blocked, ignoreCase = true) ||
                            video.category.contains(blocked, ignoreCase = true)
                }
            }
        }

        if (categoryFilter != "All" && categoryFilter != "New to you") {
            filtered = filtered.filter { video ->
                video.category.contains(categoryFilter, ignoreCase = true) ||
                        video.title.contains(categoryFilter, ignoreCase = true)
            }
        }

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { video ->
                video.title.contains(searchQuery, ignoreCase = true) ||
                        video.channelTitle.contains(searchQuery, ignoreCase = true) ||
                        video.description.contains(searchQuery, ignoreCase = true)
            }
        }

        // Final guarantee: strictly ensure zero short videos in the main Home feed list
        return filtered.filter { video -> !isShortVideo(video) }
    }

    suspend fun checkForNewUploads(knownVideoIds: Set<String>): List<VideoItem> {
        val apiKey = apiKeyFlow.first()
        val allChannels = channelsFlow.first()
        val enabledChannels = allChannels.filter { it.isEnabledInFeed }
        val channelsToQuery = if (enabledChannels.isNotEmpty()) enabledChannels else allChannels.take(6)

        val newVideos = mutableListOf<VideoItem>()

        try {
            coroutineScope {
                val deferredList = channelsToQuery.map { channel ->
                    async(Dispatchers.IO) {
                        val canonicalId = resolveToCanonicalId(channel.id)
                        val found = mutableListOf<VideoItem>()

                        // 1. Check YouTube RSS feed for newly uploaded videos
                        if (canonicalId.startsWith("UC")) {
                            try {
                                val freshVideos = rssService.fetchChannelFeed(canonicalId, channel.avatarUrl)
                                val unannounced = freshVideos.filter { !knownVideoIds.contains(it.id) }
                                found.addAll(unannounced.map { it.copy(isNewUpload = true) })
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }

                        // 2. If API key is active and RSS didn't find new items, check YouTube API
                        if (found.isEmpty() && apiKey.isNotBlank() && apiKey != "YOUR_YOUTUBE_API_KEY" && canonicalId.startsWith("UC")) {
                            try {
                                val response = apiService.searchVideos(
                                    apiKey = apiKey,
                                    channelId = canonicalId,
                                    order = "date",
                                    maxResults = 5
                                )

                                response.items?.mapNotNull { item ->
                                    val videoId = item.id?.videoId
                                    if (!videoId.isNullOrBlank() && !knownVideoIds.contains(videoId)) {
                                        val snippet = item.snippet
                                        val effectiveAvatar = if (channel.avatarUrl.isNotBlank() && !channel.avatarUrl.contains("unsplash.com")) {
                                            channel.avatarUrl
                                        } else {
                                            rssService.getKnownRealAvatar(canonicalId)
                                        }
                                        val realisticDuration = String.format("%d:%02d", (Math.abs(videoId.hashCode()) % 22) + 4, (Math.abs(videoId.hashCode()) / 17) % 60)
                                        VideoItem(
                                            id = videoId,
                                            title = snippet?.title?.replace("&quot;", "\"")?.replace("&#39;", "'")?.replace("&amp;", "&") ?: "New Upload",
                                            description = snippet?.description ?: "",
                                            channelTitle = snippet?.channelTitle ?: channel.name,
                                            channelId = snippet?.channelId ?: canonicalId,
                                            channelAvatarUrl = effectiveAvatar,
                                            thumbnailUrl = snippet?.thumbnails?.high?.url
                                                ?: snippet?.thumbnails?.medium?.url
                                                ?: "https://i.ytimg.com/vi/$videoId/hq720.jpg",
                                            publishedAt = "Just now",
                                            publishedAtIso = snippet?.publishedAt ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Instant.now().toString() else "",
                                            viewCount = "Just uploaded",
                                            duration = realisticDuration,
                                            category = "Trending",
                                            isNewUpload = true,
                                            streamId = videoId
                                        )
                                    } else null
                                }?.let { found.addAll(it) }
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }

                        found
                    }
                }

                val allFound = deferredList.awaitAll().flatten()
                val (longVideos, detectedShorts) = allFound.partition { !isShortVideo(it) }
                if (detectedShorts.isNotEmpty()) {
                    registerShorts(detectedShorts)
                }
                newVideos.addAll(longVideos)
            }
        } catch (e: Exception) {
            // Return empty if network failed
        }

        return newVideos
    }

    suspend fun simulateNewUpload(): VideoItem {
        val allChannels = channelsFlow.first()
        val enabledChannels = allChannels.filter { it.isEnabledInFeed }
        val targetChannel = enabledChannels.randomOrNull() ?: allChannels.firstOrNull() ?: DEFAULT_CHANNELS.first()

        val uniqueTimestamp = System.currentTimeMillis()
        val titles = listOf(
            "Ektu Bhalobasha | একটু ভালোবাসা | Exclusive Premiere | Niloy Alamgir | Heme",
            "Biye Banate Shanti | বিয়ে বানাতে শান্তি | New Comedy Blockbuster Natok",
            "Hridoyer Spondon | হৃদয়ের স্পন্দন | Afran Nisho | Mehazabien | Special Release",
            "Bideshi Jamai 2 | বিদেশী জামাই ২ | Mosharraf Karim | Mega Premiere",
            "Coke Studio Live Session | Shadhin Shur | New Fusion Track 2026",
            "Chayer Cup-e Jhor | চায়ের কাপে ঝড় | Ziaul Faruq Apurba | Romantic Hit"
        )
        val selectedTitle = titles[(uniqueTimestamp % titles.size).toInt()]

        val targetAvatar = if (targetChannel.avatarUrl.isNotBlank() && !targetChannel.avatarUrl.contains("unsplash.com")) {
            targetChannel.avatarUrl
        } else {
            rssService.getKnownRealAvatar(targetChannel.id)
        }

        return VideoItem(
            id = "realtime_upload_$uniqueTimestamp",
            title = "$selectedTitle | ${targetChannel.name}",
            description = "Just dropped! Watch the brand-new real-time upload from ${targetChannel.name} dynamically added to your main feed.",
            channelTitle = targetChannel.name,
            channelId = targetChannel.id,
            channelAvatarUrl = targetAvatar,
            thumbnailUrl = "https://i.ytimg.com/vi/DfanjKOWW_k/hq720.jpg",
            publishedAt = "Just now",
            publishedAtIso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Instant.now().toString() else "",
            viewCount = "Just uploaded",
            duration = "36:20",
            category = "Bangla Natok",
            isNewUpload = true,
            streamId = "demo1"
        )
    }

    private fun formatPublishedTime(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "Recent upload"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val instant = Instant.parse(isoDate)
                val now = Instant.now()
                val diffSec = Duration.between(instant, now).seconds
                when {
                    diffSec < 3600 -> "${(diffSec / 60).coerceAtLeast(1)}m ago"
                    diffSec < 86400 -> "${diffSec / 3600}h ago"
                    diffSec < 604800 -> "${diffSec / 86400}d ago"
                    diffSec < 2592000 -> "${diffSec / 604800}w ago"
                    else -> "${diffSec / 2592000}mo ago"
                }
            } else {
                "Recent upload"
            }
        } catch (e: Exception) {
            "Recent upload"
        }
    }

    fun getCuratedShorts(): List<VideoItem> {
        return listOf(
            VideoItem(
                id = "short1",
                title = "Niloy Alamgir hilarious comedy scene from Delivery Girl Natok! 😂 #shorts #banglanatok",
                description = "Best moments from Delivery Girl Natok starring Niloy Alamgir and Heme.",
                channelTitle = "Deepto Natok",
                channelId = "c_deepto",
                channelAvatarUrl = "https://yt3.googleusercontent.com/r6ReSFjFYh9VCmf7glNN1XJcPmuZHAgIKDIgnTGJyP8wFbQAqt1ksLcJT48qQkH6CsRH8Aga=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/DfanjKOWW_k/hq720.jpg",
                publishedAt = "2 days ago",
                viewCount = "3.4M",
                duration = "0:52",
                category = "Shorts",
                isShort = true,
                likes = 184000
            ),
            VideoItem(
                id = "short2",
                title = "Niloy & Heme couple chemistry in Premer Injection Natok ❤️ #shorts #heme #niloy",
                description = "Romantic comedy moments from Premer Injection natok.",
                channelTitle = "NAF Entertainment",
                channelId = "c_naf",
                channelAvatarUrl = "https://yt3.googleusercontent.com/WcJVfCa-wGUfUauDu-958MnOrkdzyOX440KdsY4QaKUA_HxyOQhxXOqDxzYYnYsHCyIx65dj2w=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/LGsd_mH1c6k/hq720.jpg",
                publishedAt = "Yesterday",
                viewCount = "1.9M",
                duration = "0:45",
                category = "Shorts",
                isShort = true,
                likes = 95200
            ),
            VideoItem(
                id = "short3",
                title = "Mosharraf Karim iconic dialogue delivery that will make you laugh non-stop! 🤣 #natok",
                description = "Classic Mosharraf Karim comedy clip.",
                channelTitle = "Rtv Drama",
                channelId = "c_rtv",
                channelAvatarUrl = "https://yt3.googleusercontent.com/r6ReSFjFYh9VCmf7glNN1XJcPmuZHAgIKDIgnTGJyP8wFbQAqt1ksLcJT48qQkH6CsRH8Aga=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/ridzjSgy1bc/hq720.jpg",
                publishedAt = "3 days ago",
                viewCount = "5.1M",
                duration = "0:58",
                category = "Shorts",
                isShort = true,
                likes = 312000
            ),
            VideoItem(
                id = "short4",
                title = "Afran Nisho heart-touching emotional dialogue scene 💔 #afrannisho #shorts",
                description = "Emotional scene compilation from Bidesh natok.",
                channelTitle = "CMV Natok",
                channelId = "c_cmv",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_ki3dMOtrCmIk9-G0PdRPR4757n6sSXogSDZJ3t_oArrw=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/rsOiZE19RVo/hq720.jpg",
                publishedAt = "5 days ago",
                viewCount = "2.8M",
                duration = "0:39",
                category = "Shorts",
                isShort = true,
                likes = 142000
            )
        )
    }

    fun getShorts(): List<VideoItem> {
        return (_dynamicShorts.value + getCuratedShorts()).distinctBy { it.id }
    }

    fun getMusicTracks(): List<VideoItem> {
        return listOf(
            VideoItem(
                id = "music1",
                title = "Coke Studio Bangla | Shob Loke Koy | Kanon & Momtaz | Season 1",
                description = "Official music release from Coke Studio Bangla featuring mystic folk fusion.",
                channelTitle = "Coke Studio Bangla",
                channelId = "c_csb",
                thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=60",
                publishedAt = "2 weeks ago",
                viewCount = "12M views",
                duration = "4:42",
                category = "Music",
                likes = 340000
            ),
            VideoItem(
                id = "music2",
                title = "Habib Wahid - Duti Chokh | Official Romantic Video | New Bangla Song",
                description = "A soulful melody by Habib Wahid.",
                channelTitle = "Habib Wahid Official",
                channelId = "c_habib",
                thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=60",
                publishedAt = "3 weeks ago",
                viewCount = "4.5M views",
                duration = "3:58",
                category = "Music",
                likes = 185000
            ),
            VideoItem(
                id = "music3",
                title = "Top Bangla Natok OST Mashup 2026 | Soulful Romantic Acoustic",
                description = "Non-stop compilation of the best emotional and romantic natok title tracks.",
                channelTitle = "Soundtek Music",
                channelId = "c_soundtek",
                thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=60",
                publishedAt = "1 month ago",
                viewCount = "2.9M views",
                duration = "21:14",
                category = "Music",
                likes = 98000
            ),
            VideoItem(
                id = "music4",
                title = "Taalpatar Shepai - Ami Tomar Moner Bhetor | Live Studio Acoustic",
                description = "Indie acoustic sensation Taalpatar Shepai performing live.",
                channelTitle = "Taalpatar Shepai",
                channelId = "c_taal",
                thumbnailUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=60",
                publishedAt = "1 month ago",
                viewCount = "8.1M views",
                duration = "5:12",
                category = "Music",
                likes = 290000
            )
        )
    }

    fun getChannels(): List<Channel> = DEFAULT_CHANNELS

    fun getPlaylists(): List<Playlist> {
        return listOf(
            Playlist(
                id = "p1",
                title = "Best of Niloy Alamgir & Heme",
                channelTitle = "Deepto Natok",
                thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=60",
                videoCount = 18
            ),
            Playlist(
                id = "p2",
                title = "New Bangla Natok 2026 Collection",
                channelTitle = "NAF Entertainment",
                thumbnailUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=500&auto=format&fit=crop&q=60",
                videoCount = 42
            ),
            Playlist(
                id = "p3",
                title = "Mosharraf Karim All Time Hits",
                channelTitle = "Rtv Drama",
                thumbnailUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop&q=60",
                videoCount = 27
            ),
            Playlist(
                id = "p4",
                title = "Bangla Natok Romantic Songs & OST",
                channelTitle = "Soundtek",
                thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=60",
                videoCount = 35
            )
        )
    }

    fun getNotifications(): List<AppNotification> {
        return listOf(
            AppNotification(
                id = "n1",
                title = "New Natok Uploaded: Delivery Girl 2 announced! Watch the teaser now.",
                channelName = "Deepto Natok",
                thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200&auto=format&fit=crop&q=60",
                timeAgo = "2 hours ago"
            ),
            AppNotification(
                id = "n2",
                title = "Live Now: Niloy Alamgir Q&A session with fans from shooting spot!",
                channelName = "NAF Entertainment",
                thumbnailUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=200&auto=format&fit=crop&q=60",
                timeAgo = "5 hours ago"
            ),
            AppNotification(
                id = "n3",
                title = "Rtv Drama premiered Bachelor Point Season 4 Mega Special Episode.",
                channelName = "Rtv Drama",
                thumbnailUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=200&auto=format&fit=crop&q=60",
                timeAgo = "1 day ago"
            )
        )
    }

    fun getCommentsForVideo(videoId: String): List<Comment> {
        return listOf(
            Comment(
                id = "c1",
                author = "Tanvir Ahmed",
                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=60",
                timeAgo = "3 days ago",
                text = "নিলয় ভাই আর হিমির অভিনয় প্রতি নাটকেই অসাধারণ! পুরো নাটকটি দেখার মতো। ❤️",
                likes = 1240
            ),
            Comment(
                id = "c2",
                author = "Sabrina Rahman",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&auto=format&fit=crop&q=60",
                timeAgo = "2 days ago",
                text = "Ending scene ta khub emotional chilo. Direction and dialogue delivery were top notch!",
                likes = 854
            ),
            Comment(
                id = "c3",
                author = "Fahim Shahriar",
                avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=100&auto=format&fit=crop&q=60",
                timeAgo = "1 day ago",
                text = "Deepto Natok always brings quality content. Keep up the great work!",
                likes = 312
            ),
            Comment(
                id = "c4",
                author = "Nusrat Jahan",
                avatarUrl = "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=100&auto=format&fit=crop&q=60",
                timeAgo = "18 hours ago",
                text = "Background music was simply magical. Loved this drama so much!",
                likes = 145
            )
        )
    }

    fun getDemoVideos(): List<VideoItem> {
        return listOf(
            VideoItem(
                id = "demo1",
                title = "Delivery Girl | ডেলিভারি গার্ল | Niloy Alamgir | New Bangla Natok",
                description = "Watch the popular romantic comedy natok Delivery Girl starring Niloy Alamgir and Jannatul Sumaiya Heme. Directed by Mohin Khan. Presented by Deepto Natok.",
                channelTitle = "Deepto Natok",
                channelId = "c_deepto",
                channelAvatarUrl = "https://yt3.googleusercontent.com/r6ReSFjFYh9VCmf7glNN1XJcPmuZHAgIKDIgnTGJyP8wFbQAqt1ksLcJT48qQkH6CsRH8Aga=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/DfanjKOWW_k/hq720.jpg",
                publishedAt = "1 week ago",
                publishedAtIso = "2026-10-25T14:00:00Z",
                viewCount = "1.8M views",
                duration = "38:59",
                category = "Bangla Natok",
                likes = 142000,
                streamId = "demo1"
            ),
            VideoItem(
                id = "demo2",
                title = "Premer Injection | প্রেমের ইনজেকশন | Niloy Alamgir | Heme | Bangla Natok",
                description = "Premer Injection is a blockbuster comedy and romantic drama starring Niloy Alamgir, Heme, and Mukit Zakaria. A story of unexpected love and laughter.",
                channelTitle = "NAF Entertainment",
                channelId = "c_naf",
                channelAvatarUrl = "https://yt3.googleusercontent.com/WcJVfCa-wGUfUauDu-958MnOrkdzyOX440KdsY4QaKUA_HxyOQhxXOqDxzYYnYsHCyIx65dj2w=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/LGsd_mH1c6k/hq720.jpg",
                publishedAt = "5 days ago",
                publishedAtIso = "2026-10-28T16:30:00Z",
                viewCount = "2.4M views",
                duration = "44:12",
                category = "Bangla Natok",
                likes = 189000,
                streamId = "demo2"
            ),
            VideoItem(
                id = "demo3",
                title = "Bachelor Point | ব্যাচেলর পয়েন্ট | Season 4 - Mega Episode | Mishu Sabbir | Polash",
                description = "The bachelor boys are back with endless mischief and humor. Directed by Kajal Arefin Ome.",
                channelTitle = "Dhruba TV",
                channelId = "c_dhruba",
                channelAvatarUrl = "https://yt3.googleusercontent.com/WcJVfCa-wGUfUauDu-958MnOrkdzyOX440KdsY4QaKUA_HxyOQhxXOqDxzYYnYsHCyIx65dj2w=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/ridzjSgy1bc/hq720.jpg",
                publishedAt = "3 days ago",
                publishedAtIso = "2026-10-30T18:00:00Z",
                viewCount = "5.6M views",
                duration = "35:40",
                category = "Natok",
                likes = 430000,
                streamId = "demo3"
            ),
            VideoItem(
                id = "demo4",
                title = "Family Crisis | ফ্যামিলি ক্রাইসিস | Mosharraf Karim | Shahiduzzaman Selim | Rtv Drama",
                description = "A heartwarming tale of modern family dilemmas, laughter, and unbreakable bonds starring Mosharraf Karim.",
                channelTitle = "Rtv Drama",
                channelId = "c_rtv",
                channelAvatarUrl = "https://yt3.googleusercontent.com/r6ReSFjFYh9VCmf7glNN1XJcPmuZHAgIKDIgnTGJyP8wFbQAqt1ksLcJT48qQkH6CsRH8Aga=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/rsOiZE19RVo/hq720.jpg",
                publishedAt = "2 weeks ago",
                publishedAtIso = "2026-10-18T12:00:00Z",
                viewCount = "3.1M views",
                duration = "41:20",
                category = "Drama",
                likes = 215000,
                streamId = "demo4"
            ),
            VideoItem(
                id = "demo5",
                title = "Bidesh | বিদেশ | Jovan | Tanjin Tisha | Emotional Romantic Natok 2026",
                description = "A poignant story of longing, sacrifice, and unconditional love separated by borders. Starring Jovan and Tanjin Tisha.",
                channelTitle = "CMV Natok",
                channelId = "c_cmv",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_ki3dMOtrCmIk9-G0PdRPR4757n6sSXogSDZJ3t_oArrw=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/LGsd_mH1c6k/hq720.jpg",
                publishedAt = "4 days ago",
                publishedAtIso = "2026-10-29T10:15:00Z",
                viewCount = "1.9M views",
                duration = "46:18",
                category = "Bangla Natok",
                likes = 175000,
                streamId = "demo5"
            ),
            VideoItem(
                id = "demo6",
                title = "Mission Barisal | মিশন বরিশাল | Afran Nisho | Mehazabien Chowdhury | Eid Special",
                description = "Afran Nisho and Mehazabien in a thrill-filled journey with regional wit and delightful romantic twists.",
                channelTitle = "CMV Natok",
                channelId = "c_cmv",
                channelAvatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_ki3dMOtrCmIk9-G0PdRPR4757n6sSXogSDZJ3t_oArrw=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/DfanjKOWW_k/hq720.jpg",
                publishedAt = "6 days ago",
                publishedAtIso = "2026-10-27T19:40:00Z",
                viewCount = "4.2M views",
                duration = "52:10",
                category = "Natok",
                likes = 340000,
                streamId = "demo6"
            ),
            VideoItem(
                id = "demo7",
                title = "Coke Studio Bangla | Shob Loke Koy | Kanon & Momtaz | Season 1 Live",
                description = "Official fusion anthem fusing Lalon Fakir with modern orchestral rhythm and soul.",
                channelTitle = "Coke Studio Bangla",
                channelId = "c_csb",
                channelAvatarUrl = "https://yt3.googleusercontent.com/nrwXuQgXdOlZUJQYyCJMK_K1F3Um7yT_qjC_YDrIF_1B9HCSn8JF9ygN9cpDNpThxxHfa8Z2ZDU=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/rsOiZE19RVo/hq720.jpg",
                publishedAt = "1 month ago",
                publishedAtIso = "2026-10-01T08:00:00Z",
                viewCount = "12M views",
                duration = "4:42",
                category = "Music",
                likes = 340000,
                streamId = "demo7"
            ),
            VideoItem(
                id = "demo8",
                title = "Habib Wahid - Duti Chokh | Official Romantic Video | New Bangla Song 2026",
                description = "Habib Wahid returns with a mesmerizing romantic ballad filmed across breathtaking scenic landscapes.",
                channelTitle = "Habib Wahid Official",
                channelId = "c_habib",
                channelAvatarUrl = "https://yt3.googleusercontent.com/WcJVfCa-wGUfUauDu-958MnOrkdzyOX440KdsY4QaKUA_HxyOQhxXOqDxzYYnYsHCyIx65dj2w=s900-c-k-c0x00ffffff-no-rj",
                thumbnailUrl = "https://i.ytimg.com/vi/ridzjSgy1bc/hq720.jpg",
                publishedAt = "2 weeks ago",
                publishedAtIso = "2026-10-19T11:00:00Z",
                viewCount = "4.5M views",
                duration = "3:58",
                category = "Music",
                likes = 185000,
                streamId = "demo8"
            )
        )
    }
}
