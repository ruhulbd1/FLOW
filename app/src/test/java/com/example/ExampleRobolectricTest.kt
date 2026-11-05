package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.VideoRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FLOW", appName)
  }

  @Test
  fun `test demo videos include Delivery Girl and Premer Injection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = VideoRepository(context)
    val videos = repo.getDemoVideos()

    assertTrue(videos.isNotEmpty())
    val deliveryGirl = videos.find { it.id == "demo1" }
    val premerInjection = videos.find { it.id == "demo2" }

    assertTrue(deliveryGirl?.title?.contains("Delivery Girl") == true)
    assertTrue(premerInjection?.title?.contains("Premer Injection") == true)
  }
}
