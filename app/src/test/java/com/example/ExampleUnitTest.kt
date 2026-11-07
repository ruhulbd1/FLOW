package com.example

import com.example.data.model.Channel
import com.example.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testVideoModelAttributes() {
    val sampleVideo = VideoItem(
      id = "demo1",
      title = "Delivery Girl | ডেলিভারি গার্ল | Niloy Alamgir | New Bangla Natok",
      channelTitle = "Deepto Natok",
      channelId = "c_deepto",
      thumbnailUrl = "https://example.com/thumb.jpg",
      publishedAtIso = "2026-10-25T14:00:00Z",
      duration = "38:59",
      category = "Bangla Natok"
    )

    assertEquals("demo1", sampleVideo.id)
    assertTrue(sampleVideo.title.contains("Delivery Girl"))
    assertEquals("Deepto Natok", sampleVideo.channelTitle)
    assertEquals("c_deepto", sampleVideo.channelId)
    assertEquals("38:59", sampleVideo.duration)
    assertEquals("2026-10-25T14:00:00Z", sampleVideo.publishedAtIso)
  }

  @Test
  fun testMultipleChannelIdsParsing() {
    val rawInput = "UC12345, UC67890\n c_deepto  c_naf "
    val parsed = rawInput
      .split(",", "\n", " ")
      .map { it.trim() }
      .filter { it.isNotBlank() }

    assertEquals(4, parsed.size)
    assertTrue(parsed.contains("UC12345"))
    assertTrue(parsed.contains("UC67890"))
    assertTrue(parsed.contains("c_deepto"))
    assertTrue(parsed.contains("c_naf"))
  }

  @Test
  fun testChannelHandleAndUrlResolution() {
    fun parseChannelInput(input: String): Pair<String, String?> {
      val trimmed = input.trim()
      var channelId = trimmed
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
      return Pair(channelId, handle)
    }

    val res1 = parseChannelInput("https://www.youtube.com/@GoogleDevelopers")
    assertEquals("@GoogleDevelopers", res1.first)
    assertEquals("GoogleDevelopers", res1.second)

    val res2 = parseChannelInput("https://www.youtube.com/channel/UC_x5XG1OV2P6uZZ5FSM9Ttw?feature=shared")
    assertEquals("UC_x5XG1OV2P6uZZ5FSM9Ttw", res2.first)

    val res3 = parseChannelInput("@mkbhd")
    assertEquals("@mkbhd", res3.first)
    assertEquals("mkbhd", res3.second)
  }

  @Test
  fun testMultiChannelCombinedSorting() {
    val videoCh1 = VideoItem(
      id = "v1",
      title = "Video from Deepto",
      channelTitle = "Deepto Natok",
      channelId = "c_deepto",
      thumbnailUrl = "",
      publishedAtIso = "2026-10-20T10:00:00Z"
    )
    val videoCh2 = VideoItem(
      id = "v2",
      title = "Video from NAF",
      channelTitle = "NAF Entertainment",
      channelId = "c_naf",
      thumbnailUrl = "",
      publishedAtIso = "2026-10-25T12:00:00Z"
    )
    val videoCh3 = VideoItem(
      id = "v3",
      title = "Video from Rtv",
      channelTitle = "Rtv Drama",
      channelId = "c_rtv",
      thumbnailUrl = "",
      publishedAtIso = "2026-10-28T09:00:00Z"
    )

    val combined = listOf(videoCh1, videoCh2, videoCh3).sortedByDescending { it.publishedAtIso }
    assertEquals("v3", combined[0].id) // Most recent upload
    assertEquals("v2", combined[1].id)
    assertEquals("v1", combined[2].id)
  }

  @Test
  fun testChannelFeedInclusionFilter() {
    val channels = listOf(
      Channel(id = "c1", name = "Channel 1", isEnabledInFeed = true),
      Channel(id = "c2", name = "Channel 2", isEnabledInFeed = false),
      Channel(id = "c3", name = "Channel 3", isEnabledInFeed = true)
    )

    val enabledIds = channels.filter { it.isEnabledInFeed }.map { it.id }.toSet()
    assertEquals(setOf("c1", "c3"), enabledIds)
    assertFalse(enabledIds.contains("c2"))
  }

  @Test
  fun testRealTimeNewUploadDetectionAndPrepend() {
    val existingVideos = listOf(
      VideoItem(id = "v1", title = "Old Video 1", channelTitle = "Deepto", thumbnailUrl = ""),
      VideoItem(id = "v2", title = "Old Video 2", channelTitle = "NAF", thumbnailUrl = "")
    )
    val knownIds = existingVideos.map { it.id }.toSet()

    val polledFromApi = listOf(
      VideoItem(id = "v_new_1", title = "Brand New Upload", channelTitle = "Deepto", thumbnailUrl = "", isNewUpload = true),
      VideoItem(id = "v1", title = "Old Video 1", channelTitle = "Deepto", thumbnailUrl = "")
    )

    val newDiscovered = polledFromApi.filter { !knownIds.contains(it.id) }
    assertEquals(1, newDiscovered.size)
    assertEquals("v_new_1", newDiscovered[0].id)
    assertTrue(newDiscovered[0].isNewUpload)

    // Verify prepending to feed without duplicates
    val updatedFeed = (newDiscovered + existingVideos).distinctBy { it.id }
    assertEquals(3, updatedFeed.size)
    assertEquals("v_new_1", updatedFeed[0].id) // Appears right at the top!
  }

  @Test
  fun testDurationToMillisecondsParsing() {
    fun parseDuration(duration: String?): Long {
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

    assertEquals(214000L, parseDuration("3:34")) // 3*60 + 34 = 214s -> 214000ms
    assertEquals(2339000L, parseDuration("38:59")) // 38*60 + 59 = 2339s -> 2339000ms
    assertEquals(3725000L, parseDuration("1:02:05")) // 1*3600 + 2*60 + 5 = 3725s -> 3725000ms
    assertEquals(38 * 60 * 1000L, parseDuration("")) // Fallback default
  }

  @Test
  fun testPlaylistNavigationIndices() {
    val playlist = listOf("vid_1", "vid_2", "vid_3", "vid_4")
    val current = "vid_2"
    val idx = playlist.indexOf(current)
    assertEquals(1, idx)

    // Next track index
    val nextIdx = idx + 1
    assertTrue(nextIdx < playlist.size)
    assertEquals("vid_3", playlist[nextIdx])

    // Previous track index
    val prevIdx = idx - 1
    assertTrue(prevIdx >= 0)
    assertEquals("vid_1", playlist[prevIdx])
  }

  @Test
  fun testYouTubeRssAtomXmlParsing() {
    val sampleXml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns:media="http://search.yahoo.com/mrss/" xmlns="http://www.w3.org/2005/Atom">
        <link rel="self" href="http://www.youtube.com/feeds/videos.xml?channel_id=UC_x5XG1OV2P6uZZ5FSM9Ttw"/>
        <id>yt:channel:UC_x5XG1OV2P6uZZ5FSM9Ttw</id>
        <title>Google for Developers</title>
        <entry>
          <id>yt:video:rsOiZE19RVo</id>
          <yt:videoId>rsOiZE19RVo</yt:videoId>
          <title>Accelerate shipping with Antigravity</title>
          <published>2026-10-29T20:00:20+00:00</published>
          <author>
            <name>Google for Developers</name>
          </author>
          <media:group>
            <media:description>Building software to move millions of vehicles.</media:description>
            <media:thumbnail url="https://i3.ytimg.com/vi/rsOiZE19RVo/hqdefault.jpg" width="480" height="360"/>
            <media:community>
              <media:statistics views="45210"/>
            </media:community>
          </media:group>
        </entry>
      </feed>
    """.trimIndent()

    val videoId = Regex("<yt:videoId>(.*?)</yt:videoId>").find(sampleXml)?.groupValues?.get(1)?.trim() ?: ""
    val title = Regex("<entry>[\\s\\S]*?<title>(.*?)</title>").find(sampleXml)?.groupValues?.get(1)?.trim() ?: ""
    val author = Regex("<author>[\\s\\S]*?<name>(.*?)</name>").find(sampleXml)?.groupValues?.get(1)?.trim() ?: ""
    val views = Regex("views=\"(\\d+)\"").find(sampleXml)?.groupValues?.get(1) ?: ""

    assertEquals("rsOiZE19RVo", videoId)
    assertTrue(title.contains("Antigravity"))
    assertEquals("Google for Developers", author)
    assertEquals("45210", views)
  }

  @Test
  fun testRealYouTubeChannelAvatarResolution() {
    val knownAvatars = mapOf(
      "UCVlUfN0sAILHcqgtWSxEPRg" to "https://yt3.googleusercontent.com/r6ReSFjFYh9VCmf7glNN1XJcPmuZHAgIKDIgnTGJyP8wFbQAqt1ksLcJT48qQkH6CsRH8Aga=s900-c-k-c0x00ffffff-no-rj",
      "UCaE9aMdlRomFYL4LkGhC1FQ" to "https://yt3.googleusercontent.com/WcJVfCa-wGUfUauDu-958MnOrkdzyOX440KdsY4QaKUA_HxyOQhxXOqDxzYYnYsHCyIx65dj2w=s900-c-k-c0x00ffffff-no-rj"
    )

    fun resolveAvatar(channelId: String, currentUrl: String): String {
      if (currentUrl.isNotBlank() && !currentUrl.contains("unsplash.com")) return currentUrl
      return knownAvatars[channelId] ?: ""
    }

    // Outdated Unsplash URL should be upgraded to real YouTube avatar
    val resolvedFromUnsplash = resolveAvatar("UCVlUfN0sAILHcqgtWSxEPRg", "https://images.unsplash.com/photo-1534528741775-53994a69daeb")
    assertTrue(resolvedFromUnsplash.contains("yt3.googleusercontent.com"))
    assertFalse(resolvedFromUnsplash.contains("unsplash.com"))

    // Blank avatar should be resolved to real YouTube avatar
    val resolvedFromBlank = resolveAvatar("UCaE9aMdlRomFYL4LkGhC1FQ", "")
    assertTrue(resolvedFromBlank.contains("yt3.googleusercontent.com"))
  }

  @Test
  fun testRealisticViewsAndDurationFormatting() {
    fun formatViews(views: Long?): String {
      if (views == null || views <= 0) return "Recent upload"
      return when {
        views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000.0)
        views >= 1_000 -> String.format("%.1fK views", views / 1_000.0)
        views == 1L -> "1 view"
        else -> "$views views"
      }
    }

    fun deriveDuration(videoId: String, isShort: Boolean): String {
      if (isShort) return "0:45"
      val hash = Math.abs(videoId.hashCode())
      val minutes = (hash % 22) + 4
      val seconds = (hash / 17) % 60
      return String.format("%d:%02d", minutes, seconds)
    }

    assertEquals("1.2K views", formatViews(1212L))
    assertEquals("2.4M views", formatViews(2400000L))
    assertEquals("45 views", formatViews(45L))
    assertEquals("1 view", formatViews(1L))

    val durRegular = deriveDuration("DfanjKOWW_k", false)
    assertTrue(durRegular.contains(":"))
    val durShort = deriveDuration("DfanjKOWW_k", true)
    assertEquals("0:45", durShort)
  }
}
