package com.example.suararumah.data.remote

import com.example.suararumah.util.Constants
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton HTTP client untuk komunikasi dengan backend.
 *
 * Fitur:
 * - API key statis di header setiap request (autentikasi minimal)
 * - Logging interceptor untuk debugging
 * - Timeout configuration
 * - Retry tidak dilakukan di level OkHttp — dihandle di Repository layer
 *   agar bisa mencatat ke Room DB saat gagal
 */
object ApiClient {

    private var _apiService: ApiService? = null

    /**
     * Mendapatkan instance ApiService (lazy singleton).
     */
    fun getApiService(): ApiService {
        return _apiService ?: synchronized(this) {
            _apiService ?: createApiService().also { _apiService = it }
        }
    }

    private fun createApiService(): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(createApiKeyInterceptor())
            .addInterceptor(createLoggingInterceptor())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }

    /**
     * Interceptor untuk menambahkan API key statis di header setiap request.
     * Autentikasi minimal — cukup menutup celah endpoint publik tanpa proteksi.
     */
    private fun createApiKeyInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val authenticatedRequest = originalRequest.newBuilder()
                .header(Constants.API_KEY_HEADER, Constants.API_KEY)
                .build()
            chain.proceed(authenticatedRequest)
        }
    }

    /**
     * Logging interceptor — body level untuk debug, headers level untuk production.
     */
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * Reset client — untuk testing atau saat base URL berubah.
     */
    fun reset() {
        _apiService = null
    }
}
