package com.usmandiscountstore.app.util

import android.content.Context
import android.content.SharedPreferences

object PasswordHelper {
    private const val PREFS = "uds_passwords"
    private const val KEY_ADMIN = "admin_pwd"
    private const val KEY_MODERATOR = "moderator_pwd"

    private fun prefs(c: Context): SharedPreferences =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getAdminPassword(c: Context): String =
        prefs(c).getString(KEY_ADMIN, "admin123") ?: "admin123"

    fun getModeratorPassword(c: Context): String =
        prefs(c).getString(KEY_MODERATOR, "mod123") ?: "mod123"

    fun setAdminPassword(c: Context, pwd: String) {
        prefs(c).edit().putString(KEY_ADMIN, pwd).apply()
    }

    fun setModeratorPassword(c: Context, pwd: String) {
        prefs(c).edit().putString(KEY_MODERATOR, pwd).apply()
    }
}
