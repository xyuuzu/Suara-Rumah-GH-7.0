package com.example.suararumah.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO untuk menerima hasil klasifikasi dari backend.
 * Diterima sebagai response dari POST /api/v1/analyze & /predict-features
 */
data class ClassificationResponse(
    @SerializedName("is_anomaly")
    val isAnomaly: Boolean = false,

    @SerializedName("anomaly")
    val anomaly: Boolean? = null,

    @SerializedName("confidence")
    val confidence: Float = 0f,

    @SerializedName("label")
    val label: String = "normal",

    @SerializedName("escalating")
    val isEscalating: Boolean = false,

    @SerializedName("alert_id")
    val alertId: String? = null,

    @SerializedName("consecutive_anomalies")
    val consecutiveAnomalies: Int = 0,

    @SerializedName("message")
    val message: String? = null
) {
    /**
     * Helper getter untuk memeriksa apakah anomali terdeteksi baik dari atribut `is_anomaly` maupun `anomaly`.
     */
    val hasAnomaly: Boolean
        get() = isAnomaly || (anomaly == true)
}
