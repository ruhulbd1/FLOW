package com.example.data.repository

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.api.YouTubeApiService
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

private val Context.dataStore by preferencesDataStore(name = "flow_settings")

class VideoRepository(
    private val context: Context,
    private val apiService: YouTubeApiService = YouTubeApiService.create()
) {
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
        val KEY_BLOCKED_TOPICS = stringPreferencesKey("blocked_topics_csv")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled") // default true

        val DEFAULT_CHANNELS = listOf(
            Channel(
                id = "c_deepto",
                name = "Deepto Natok",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=60",
                subscribers = "4.2M subscribers",
                verified = true,
                isSubscribed = true,
                isEnabledInFeed = true
            ),
            Channel(
                id = "c_naf",
                name = "NAF Entertainment",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&auto=format&fit=crop&q=60",
                subscribers = "2.8M subscribers",
                verified = true,
                isSubscribed = true,
                isEnabledInFeed = true
            ),
            Channel(
                id = "c_rtv",
                name = "Rtv Drama",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120&auto=format&fit=crop&q=60",
                subscribers = "8.9M subscribers",
                verified = true,
                isSubscribed = true,
                isEnabledInFeed = true
            ),
            Channel(
                id = "c_cmv",
                name = "CMV Natok",
                avatarUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=120&auto=format&fit=crop&q=60",
                subscribers = "6.1M subscribers",
                verified = true,
                isSubscribed = true,
                isEnabledInFeed = true
            ),
            Channel(
                id = "c_dhruba",
                name = "Dhruba TV",
                avatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=120&auto=format&fit=crop&q=60",
                subscribers = "7.5M subscribers",
                verified = true,
                isSubscribed = false,
                isEnabledInFeed = false
            ),
            Channel(
                id = "c_csb",
                name = "Coke Studio Bangla",
                avatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=120&auto=format&fit=crop&q=60",
                subscribers = "3.9M subscribers",
                verified = true,
                isSubscribed = false,
                isEnabledInFeed = false
            )
        )

        val DEFAULT_CHANNEL_IDS = DEFAULT_CHANNELS.filter { it.isEnabledInFeed }.map { it.id }
    }

    private val channelsMoshiAdapter by lazy {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, Channel::class.java)
        moshi.adapter<List<Channel>>(type)
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_YOUTUBE_API_KEY] ?: ""
    }

    val channelsFlow: Flow<List<Channel>> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_CHANNELS_JSON]
        if (!json.isNullOrBlank()) {
            try {
                channelsMoshiAdapter.fromJson(json) ?: DEFAULT_CHANNELS
            } catch (e: Exception) {
                DEFAULT_CHANNELS
            }
        } else {
            // Check legacy CSV if present
            val csv = preferences[KEY_CHANNEL_IDS]
            if (!csv.isNullOrBlank()) {
                val ids = csv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                DEFAULT_CHANNELS.map { ch ->
                    if (ids.contains(ch.id)) ch.copy(isEnabledInFeed = true, isSubscribed = true)
                    else ch.copy(isEnabledInFeed = false)
                }
            } else {
                DEFAULT_CHANNELS
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

    // Dynamic Multi-Channel Management
    suspend fun addChannel(input: String): Channel {
        val trimmed = input.trim()
        require(trimmed.isNotBlank()) { "Channel identifier cannot be empty" }

        var channelId: String = trimmed
        var handle: String? = null
        if (trimmed.contains("youtube.com/channel/")) {
            channelId = trimmed.substringAfter("youtube.com/channel/").substringBefore("/").substringBefore("?")
        } else if (trimmed.contains("youtube.com/@")) {
            handle = trimmed.substringAfter("youtube.com/@").substringBefore("/").substringBefore("?")
            channelId = "@$handle"
        } else if (trimmed.startsWith("@")) {
            handle = trimmed.removePrefix("@")
            channelId = trimmed
        }

        val apiKey = apiKeyFlow.first()
        var resolvedName = if (handle != null) "@$handle" else if (channelId.startsWith("c_")) {
            channelId.removePrefix("c_").replaceFirstChar { it.uppercase() }
        } else {
            "Channel ${channelId.takeLast(6)}"
        }
        var resolvedAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120&auto=format&fit=crop&q=60"
        var resolvedSubs = "Active creator"
        var finalChannelId = channelId

        if (apiKey.isNotBlank() && apiKey != "YOUR_YOUTUBE_API_KEY") {
            try {
                val response = if (handle != null) {
                    apiService.getChannels(apiKey = apiKey, forHandle = handle)
                } else if (channelId.startsWith("UC")) {
                    apiService.getChannels(apiKey = apiKey, id = channelId)
                } else {
                    null
                }

                val item = response?.items?.firstOrNull()
                if (item != null) {
                    finalChannelId = item.id ?: channelId
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
                // Keep resolved defaults
            }
        }

        val newChannel = Channel(
            id = finalChannelId,
            name = resolvedName,
            avatarUrl = resolvedAvatar,
            subscribers = resolvedSubs,
            verified = true,
            isSubscribed = true,
            isEnabledInFeed = true,
            isCustom = true
        )

        context.dataStore.edit { preferences ->
            val currentChannels = channelsFlow.first().toMutableList()
            val existingIdx = currentChannels.indexOfFirst { it.id == finalChannelId }
            if (existingIdx >= 0) {
                currentChannels[existingIdx] = currentChannels[existingIdx].copy(
                    isEnabledInFeed = true,
                    isSubscribed = true
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
            allChannels.take(4)
        }

        // 2. Fetch dynamically from multiple channels in parallel via YouTube Data API v3
        if (apiKey.isNotBlank() && apiKey != "YOUR_YOUTUBE_API_KEY") {
            try {
                coroutineScope {
                    val deferredList = channelsToQuery.map { channel ->
                        async(Dispatchers.IO) {
                            try {
                                val queryParam = if (searchQuery.isNotBlank()) {
                                    searchQuery
                                } else if (categoryFilter != "All" && categoryFilter != "New to you") {
                                    categoryFilter
                                } else null

                                val channelParam = if (channel.id.isNotBlank() && !channel.id.startsWith("c_") && !channel.id.startsWith("@") && channel.id != "YOUR_CHANNEL_ID") {
                                    channel.id
                                } else null

                                val response = apiService.searchVideos(
                                    apiKey = apiKey,
                                    channelId = channelParam,
                                    query = queryParam,
                                    order = "date",
                                    maxResults = 10
                                )

                                response.items?.mapNotNull { item ->
                                    val videoId = item.id?.videoId
                                    if (!videoId.isNullOrBlank()) {
                                        val snippet = item.snippet
                                        val publishedDate = formatPublishedTime(snippet?.publishedAt)
                                        val channelTitle = snippet?.channelTitle ?: channel.name
                                        val channelAvatar = channel.avatarUrl.ifBlank {
                                            snippet?.thumbnails?.defaultThumb?.url ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120&auto=format&fit=crop&q=60"
                                        }
                                        VideoItem(
                                            id = videoId,
                                            title = snippet?.title?.replace("&quot;", "\"")?.replace("&#39;", "'")?.replace("&amp;", "&") ?: "YouTube Video",
                                            description = snippet?.description ?: "",
                                            channelTitle = channelTitle,
                                            channelId = snippet?.channelId ?: channel.id,
                                            channelAvatarUrl = channelAvatar,
                                            thumbnailUrl = snippet?.thumbnails?.high?.url
                                                ?: snippet?.thumbnails?.medium?.url
                                                ?: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=60",
                                            publishedAt = publishedDate,
                                            publishedAtIso = snippet?.publishedAt ?: "",
                                            viewCount = "Recent upload",
                                            duration = "28:15",
                                            category = categoryFilter,
                                            streamId = videoId
                                        )
                                    } else null
                                } ?: emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }

                    val allChannelVideos = deferredList.awaitAll().flatten()
                    results.addAll(allChannelVideos)
                }
            } catch (e: Exception) {
                // Graceful fallback to demo data
            }
        }

        // 3. Fallback / demo handling: combine demo videos for all enabled channels
        if (results.isEmpty()) {
            val allDemos = getDemoVideos()
            val enabledIds = channelsToQuery.map { it.id }.toSet()

            val matchingDemos = if (!selectedChannelFilter.isNullOrBlank() && selectedChannelFilter != "All") {
                allDemos.filter { it.channelId == selectedChannelFilter }
            } else {
                allDemos.filter { enabledIds.contains(it.channelId) }
            }

            results.addAll(if (matchingDemos.isNotEmpty()) matchingDemos else allDemos)

            // Synthesize realistic demo entries for any custom channels added by the user
            for (ch in channelsToQuery) {
                if (ch.isCustom && results.none { it.channelId == ch.id }) {
                    results.add(
                        0,
                        VideoItem(
                            id = "demo_custom_${ch.id.hashCode()}",
                            title = "Latest upload from ${ch.name} | Exclusive Premiere",
                            description = "Watch the newest video release from ${ch.name}. Added dynamically via Multi-Channel Flow Engine.",
                            channelTitle = ch.name,
                            channelId = ch.id,
                            channelAvatarUrl = ch.avatarUrl,
                            thumbnailUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=800&auto=format&fit=crop&q=80",
                            publishedAt = "Just now",
                            publishedAtIso = "2026-11-05T00:00:00Z",
                            viewCount = "142K views",
                            duration = "22:15",
                            category = "Bangla Natok",
                            streamId = "demo1"
                        )
                    )
                }
            }
        }

        // 4. Sort combined results chronologically by upload date
        results.sortByDescending { it.publishedAtIso.ifEmpty { it.publishedAt } }

        // 5. Apply filters: categories, search query, and blocked topics
        var filtered = results.toList()

        // Exclude blocked topics
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

        emit(filtered)
    }

    suspend fun checkForNewUploads(knownVideoIds: Set<String>): List<VideoItem> {
        val apiKey = apiKeyFlow.first()
        val allChannels = channelsFlow.first()
        val enabledChannels = allChannels.filter { it.isEnabledInFeed }
        val channelsToQuery = if (enabledChannels.isNotEmpty()) enabledChannels else allChannels.take(4)

        val newVideos = mutableListOf<VideoItem>()

        // 1. If live YouTube API key is active, poll YouTube Data API v3 for latest uploads
        if (apiKey.isNotBlank() && apiKey != "YOUR_YOUTUBE_API_KEY") {
            try {
                coroutineScope {
                    val deferredList = channelsToQuery.map { channel ->
                        async(Dispatchers.IO) {
                            try {
                                val channelParam = if (channel.id.isNotBlank() && !channel.id.startsWith("c_") && !channel.id.startsWith("@")) {
                                    channel.id
                                } else null

                                val response = apiService.searchVideos(
                                    apiKey = apiKey,
                                    channelId = channelParam,
                                    order = "date",
                                    maxResults = 5
                                )

                                response.items?.mapNotNull { item ->
                                    val videoId = item.id?.videoId
                                    if (!videoId.isNullOrBlank() && !knownVideoIds.contains(videoId)) {
                                        val snippet = item.snippet
                                        VideoItem(
                                            id = videoId,
                                            title = snippet?.title?.replace("&quot;", "\"")?.replace("&#39;", "'")?.replace("&amp;", "&") ?: "New Upload",
                                            description = snippet?.description ?: "",
                                            channelTitle = snippet?.channelTitle ?: channel.name,
                                            channelId = snippet?.channelId ?: channel.id,
                                            channelAvatarUrl = channel.avatarUrl.ifBlank {
                                                snippet?.thumbnails?.defaultThumb?.url ?: ""
                                            },
                                            thumbnailUrl = snippet?.thumbnails?.high?.url
                                                ?: snippet?.thumbnails?.medium?.url
                                                ?: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=60",
                                            publishedAt = "Just now",
                                            publishedAtIso = snippet?.publishedAt ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Instant.now().toString() else "",
                                            viewCount = "Just uploaded",
                                            duration = "29:45",
                                            category = "Bangla Natok",
                                            isNewUpload = true,
                                            streamId = videoId
                                        )
                                    } else null
                                } ?: emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }

                    newVideos.addAll(deferredList.awaitAll().flatten())
                }
            } catch (e: Exception) {
                // Return empty if network / quota failed
            }
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

        return VideoItem(
            id = "realtime_upload_$uniqueTimestamp",
            title = "$selectedTitle | ${targetChannel.name}",
            description = "Just dropped! Watch the brand-new real-time upload from ${targetChannel.name} dynamically added to your main feed.",
            channelTitle = targetChannel.name,
            channelId = targetChannel.id,
            channelAvatarUrl = targetChannel.avatarUrl,
            thumbnailUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=800&auto=format&fit=crop&q=80",
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

    fun getShorts(): List<VideoItem> {
        return listOf(
            VideoItem(
                id = "short1",
                title = "Niloy Alamgir hilarious comedy scene from Delivery Girl Natok! 😂 #shorts #banglanatok",
                description = "Best moments from Delivery Girl Natok starring Niloy Alamgir and Heme.",
                channelTitle = "Deepto Natok",
                channelId = "c_deepto",
                channelAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
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
                channelAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=600&auto=format&fit=crop&q=80",
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
                channelAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=600&auto=format&fit=crop&q=80",
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
                channelAvatarUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600&auto=format&fit=crop&q=80",
                publishedAt = "5 days ago",
                viewCount = "2.8M",
                duration = "0:39",
                category = "Shorts",
                isShort = true,
                likes = 142000
            )
        )
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
                channelAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80",
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
                channelAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=800&auto=format&fit=crop&q=80",
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
                channelAvatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&auto=format&fit=crop&q=80",
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
                channelAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&auto=format&fit=crop&q=80",
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
                channelAvatarUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?w=800&auto=format&fit=crop&q=80",
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
                channelAvatarUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&auto=format&fit=crop&q=80",
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
                channelAvatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&auto=format&fit=crop&q=80",
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
                channelAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&auto=format&fit=crop&q=60",
                thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80",
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
