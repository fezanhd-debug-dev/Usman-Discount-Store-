package com.usmandiscountstore.app.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object LicenseManager {

    private const val BASE_URL = "https://staffmanagestore.duckdns.org:10066"
    private const val PREFS_NAME = "license_prefs"
    private const val KEY_IS_VALID = "is_valid"
    private const val KEY_EXPIRY = "expiry_date"

    // 1. Hardware ID generate karna (Android ID + Serial)
    fun getHardwareId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val serial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { Build.getSerial() } catch (e: SecurityException) { "unknown" }
        } else {
            @Suppress("DEPRECATION")
            Build.SERIAL
        }
        return "$androidId-$serial"
    }

    // Server se baat karne ke liye client
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 2. Device Register (7-Day Free Trial)
    suspend fun registerDevice(context: Context, storeName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val hardwareId = getHardwareId(context)
            val json = JSONObject().apply {
                put("hardware_id", hardwareId)
                put("store_name", storeName)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/device/register")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    saveLicenseLocally(context, true, "7_days_trial")
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 3. License Verify (App start hone par check)
    suspend fun verifyLicense(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val hardwareId = getHardwareId(context)
            val json = JSONObject().apply { put("hardware_id", hardwareId) }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/license/verify")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "{}")
                    val device = jsonResponse.optJSONObject("device")
                    val expiry = device?.optString("license_end") ?: device?.optString("trial_end") ?: ""
                    saveLicenseLocally(context, true, expiry)
                    true
                } else {
                    // Agar server se 403 (expired/blocked) aata hai
                    saveLicenseLocally(context, false, "")
                    false
                }
            }
        } catch (e: Exception) {
            // Agar internet nahi hai, to local cache check karein (offline grace)
            e.printStackTrace()
            isLicenseLocallyValid(context)
        }
    }

    // Local storage mein save karna
    private fun saveLicenseLocally(context: Context, isValid: Boolean, expiry: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_IS_VALID, isValid)
            putString(KEY_EXPIRY, expiry)
            apply()
        }
    }

    // Local cache check karna
    fun isLicenseLocallyValid(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_VALID, false)
    }
}
