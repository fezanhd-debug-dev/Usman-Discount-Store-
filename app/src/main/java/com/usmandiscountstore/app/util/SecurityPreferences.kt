package com.usmandiscountstore.app.util

import android.content.Context
import android.content.SharedPreferences

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("uds_prefs", Context.MODE_PRIVATE)

    fun saveSession(userName: String, role: String) {
        prefs.edit().putString("user_name", userName).putString("role", role).apply()
    }

    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    fun getRole(): String = prefs.getString("role", "") ?: ""
    fun isLoggedIn(): Boolean = getRole().isNotEmpty()
    fun clearSession() { prefs.edit().clear().apply() }
}
