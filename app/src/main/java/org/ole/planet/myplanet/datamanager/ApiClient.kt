package org.ole.planet.myplanet.datamanager

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Modifier
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://vi.media.mit.edu/"
    private var retrofit: Retrofit? = null

    @JvmStatic
    val client: Retrofit?
        get() {
            // Create OkHttpClient with GZIP and Retry with Exponential Backoff
            val client = OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    // Add GZIP compression header
                    val request = chain.request().newBuilder()
                        .addHeader("Accept-Encoding", "gzip")
                        .build()
                    chain.proceed(request)
                }
                .retryOnConnectionFailure(true) // Enable retry on failure
                .addInterceptor(RetryInterceptor()) // Custom retry logic
                .build()

            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(
                        GsonConverterFactory.create(
                            GsonBuilder()
                                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                                .serializeNulls()
                                .create()
                        )
                    ).build()
            }
            return retrofit
        }

    // RetryInterceptor to implement exponential backoff
    class RetryInterceptor : Interceptor {
        val maxRetryCount = 3
        val retryDelayMillis = 1000L  // Start with 1 second delay

        override fun intercept(chain: Interceptor.Chain): Response {
            var attempt = 0
            var response: Response
            var lastException: IOException? = null

            while (true) {
                try {
                    response = chain.proceed(chain.request())
                    if (response.isSuccessful) {
                        return response
                    } else {
                        throw IOException("Response unsuccessful: ${response.code()}")
                    }
                } catch (e: IOException) {
                    attempt++
                    lastException = e
                    if (attempt >= maxRetryCount) {
                        throw e // Rethrow the exception if the maximum retries are reached
                    }

                    // Exponential backoff: increase the delay for each attempt
                    val delayMillis = retryDelayMillis * Math.pow(2.0, (attempt - 1).toDouble()).toLong()
                    Thread.sleep(delayMillis) // Wait before retrying
                }
            }
        }
    }
}