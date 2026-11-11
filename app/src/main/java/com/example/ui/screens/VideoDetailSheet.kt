package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.Comment
import com.example.data.model.VideoItem

/**
 * VideoDetailSheet delegating directly to PlayerScreen to maintain full compatibility
 * with all existing codebases while providing native ExoPlayer playback without redirects.
 */
@Composable
fun VideoDetailSheet(
    video: VideoItem,
    isPlaying: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    isSubscribed: Boolean,
    comments: List<Comment>,
    relatedVideos: List<VideoItem>,
    onTogglePlay: () -> Unit,
    onCollapse: () -> Unit,
    onToggleLike: (VideoItem) -> Unit,
    onToggleSave: (VideoItem) -> Unit,
    onStartDownload: (VideoItem, String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onToggleSubscribe: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onSelectRelatedVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    PlayerScreen(
        video = video,
        isPlaying = isPlaying,
        isLiked = isLiked,
        isSaved = isSaved,
        isDownloaded = isDownloaded,
        downloadProgress = downloadProgress,
        isSubscribed = isSubscribed,
        comments = comments,
        relatedVideos = relatedVideos,
        onTogglePlay = onTogglePlay,
        onCollapse = onCollapse,
        onToggleLike = onToggleLike,
        onToggleSave = onToggleSave,
        onStartDownload = onStartDownload,
        onRemoveDownload = onRemoveDownload,
        onToggleSubscribe = onToggleSubscribe,
        onAddComment = onAddComment,
        onSelectRelatedVideo = onSelectRelatedVideo,
        modifier = modifier
    )
}
