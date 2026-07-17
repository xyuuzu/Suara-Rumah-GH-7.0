package com.example.suararumah.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO untuk mengirim hasil ekstraksi fitur audio ke backend.
 * Cocok dengan spesifikasi API.pdf (POST predict-features & /predict).
 */
data class AudioFeatureRequest(
    @SerializedName("rms")
    val rms: Float,

    @SerializedName("zcr")
    val zcr: Float,

    @SerializedName("peak")
    val peakAmplitude: Float,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @SerializedName("device_id")
    val deviceId: String = com.example.suararumah.util.Constants.DEFAULT_DEVICE_ID,

    @SerializedName("user_id")
    val userId: String = com.example.suararumah.util.Constants.DEFAULT_USER_ID,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null
)
