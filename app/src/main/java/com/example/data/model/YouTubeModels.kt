package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class VideoItem(
    val id: String,
    val title: String,
    val description: String = "",
    val channelTitle: String,
    val channelId: String = "",
    val channelAvatarUrl: String = "",
    val thumbnailUrl: String,
    val publishedAt: String = "1 week ago",
    val publishedAtIso: String = "",
    val viewCount: String = "1.2M views",
    val duration: String = "38:59",
    val category: String = "Bangla Natok",
    val isShort: Boolean = false,
    val isNewUpload: Boolean = false,
    val likes: Int = 42500,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val streamId: String = id
)

data class Comment(
    val id: String,
    val author: String,
    val avatarUrl: String = "",
    val timeAgo: String,
    val text: String,
    val likes: Int,
    val isLiked: Boolean = false
)

data class Channel(
    val id: String,
    val name: String,
    val avatarUrl: String = "",
    val subscribers: String = "",
    val verified: Boolean = true,
    val isSubscribed: Boolean = true,
    val isEnabledInFeed: Boolean = true,
    val isCustom: Boolean = false
)

data class Playlist(
    val id: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val videoCount: Int
)

data class AppNotification(
    val id: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val timeAgo: String,
    val isRead: Boolean = false
)

// Retrofit Moshi Data Transfer Objects for YouTube Data API v3
@JsonClass(generateAdapter = true)
data class YouTubeApiResponse(
    @Json(name = "items") val items: List<YouTubeSearchItem>? = null,
    @Json(name = "nextPageToken") val nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchItem(
    @Json(name = "id") val id: YouTubeIdInfo?,
    @Json(name = "snippet") val snippet: YouTubeSnippetInfo?
)

@JsonClass(generateAdapter = true)
data class YouTubeIdInfo(
    @Json(name = "kind") val kind: String? = null,
    @Json(name = "videoId") val videoId: String? = null,
    @Json(name = "channelId") val channelId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippetInfo(
    @Json(name = "publishedAt") val publishedAt: String? = null,
    @Json(name = "channelId") val channelId: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "channelTitle") val channelTitle: String? = null,
    @Json(name = "thumbnails") val thumbnails: YouTubeThumbnailsContainer? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnailsContainer(
    @Json(name = "default") val defaultThumb: YouTubeThumbnailInfo? = null,
    @Json(name = "medium") val medium: YouTubeThumbnailInfo? = null,
    @Json(name = "high") val high: YouTubeThumbnailInfo? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnailInfo(
    @Json(name = "url") val url: String? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelResponse(
    @Json(name = "items") val items: List<YouTubeChannelItem>? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "snippet") val snippet: YouTubeChannelSnippet? = null,
    @Json(name = "statistics") val statistics: YouTubeChannelStatistics? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelSnippet(
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "customUrl") val customUrl: String? = null,
    @Json(name = "thumbnails") val thumbnails: YouTubeThumbnailsContainer? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelStatistics(
    @Json(name = "subscriberCount") val subscriberCount: String? = null,
    @Json(name = "videoCount") val videoCount: String? = null,
    @Json(name = "viewCount") val viewCount: String? = null
)
