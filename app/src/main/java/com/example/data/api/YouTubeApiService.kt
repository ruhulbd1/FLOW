package com.example.data.api

import com.example.data.model.YouTubeApiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface YouTubeApiService {

    @GET("youtube/v3/search")
    suspend fun searchVideos(
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet,id",
        @Query("channelId") channelId: String? = null,
        @Query("q") query: String? = null,
        @Query("type") type: String = "video",
        @Query("order") order: String = "date",
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null
    ): YouTubeApiResponse

    @GET("youtube/v3/channels")
    suspend fun getChannels(
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet,statistics",
        @Query("id") id: String? = null,
        @Query("forHandle") forHandle: String? = null,
        @Query("forUsername") forUsername: String? = null
    ): com.example.data.model.YouTubeChannelResponse

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/"

        fun create(): YouTubeApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(YouTubeApiService::class.java)
        }
    }
}
