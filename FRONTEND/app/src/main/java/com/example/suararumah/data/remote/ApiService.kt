package com.example.suararumah.data.remote

import com.example.suararumah.data.remote.dto.AlertRequest
import com.example.suararumah.data.remote.dto.ApiResponse
import com.example.suararumah.data.remote.dto.AudioFeatureRequest
import com.example.suararumah.data.remote.dto.ClassificationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface untuk berkomunikasi dengan backend FastAPI.
 * Sesuai dengan koleksi Postman resmi (API.pdf).
 */
interface ApiService {

    /**
     * 1. Setup Device -> Register Device
     */
    @POST("devices/register")
    suspend fun registerDevice(
        @Body request: Map<String, String> = emptyMap()
    ): Response<Map<String, String>>

    /**
     * 1. Setup Device -> Set Emergency Contact
     */
    @POST("devices/{device_id}/contact")
    suspend fun setEmergencyContact(
        @Header("X-Device-Id") deviceId: String,
        @Header("X-Api-Key") apiKey: String,
        @Path("device_id") targetDeviceId: String,
        @Body request: Map<String, String>
    ): Response<ApiResponse>

    /**
     * 2. Kirim Fitur Suara (RMS/ZCR/Peak + GPS) ke predict-features
     */
    @POST("predict-features")
    suspend fun predictFeatures(
        @Header("X-Device-Id") deviceId: String,
        @Header("X-Api-Key") apiKey: String,
        @Body request: AudioFeatureRequest
    ): Response<ClassificationResponse>

    /**
     * 4. Follow-up Alert -> Confirm Safe (tandai alarm palsu)
     */
    @POST("alerts/{alert_id}/confirm-safe")
    suspend fun confirmSafe(
        @Header("X-Device-Id") deviceId: String,
        @Header("X-Api-Key") apiKey: String,
        @Path("alert_id") alertId: String
    ): Response<ApiResponse>

    @POST("api/v1/analyze")
    suspend fun analyzeAudio(
        @Body request: AudioFeatureRequest
    ): Response<ClassificationResponse>

    @POST("predict")
    suspend fun predictAudio(
        @Body request: AudioFeatureRequest
    ): Response<ClassificationResponse>

    @POST("api/v1/trigger-alert")
    suspend fun triggerAlert(
        @Body request: AlertRequest
    ): Response<ApiResponse>

    @POST("api/v1/cancel-alert")
    suspend fun cancelAlert(
        @Body request: AlertRequest
    ): Response<ApiResponse>

    @POST("api/v1/report-safe")
    suspend fun reportSafe(
        @Body request: AlertRequest
    ): Response<ApiResponse>

    @GET("api/v1/health")
    suspend fun healthCheck(): Response<ApiResponse>
}
