package com.usmandiscountstore.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AdminApiHelper {

    private const val BASE_URL = "https://staffmanagestore.duckdns.org:10066"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 1. Saare Devices Ki List Hasil Karein
    suspend fun getAllDevices(): JSONArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/admin/devices")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    JSONArray(body)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 2. Device Ko Activate Karein (3, 6, 12 Months)
    suspend fun activateDevice(hardwareId: String, months: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("hardware_id", hardwareId)
                put("duration_months", months)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/admin/activate")
                .post(body)
                .build()

            client.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 3. Device Ko Block Karein
    suspend fun blockDevice(hardwareId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply { put("hardware_id", hardwareId) }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/admin/block")
                .post(body)
                .build()

            client.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
