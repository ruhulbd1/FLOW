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
}
