package com.usmandiscountstore.app.util

import android.content.Context

/**
 * Super Admin (Mr.DHooM 4K) ke liye secret access.
 * Default password: MasterAdmin@2026
 * License system isi panel me aayega.
 */
object SuperAdminHelper {

    private const val PREFS = "uds_super_admin"
    private const val KEY_PASSWORD = "super_admin_pwd"
    private const val DEFAULT_PASSWORD = "MasterAdmin@2026"

    fun getPassword(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD

    fun setPassword(context: Context, pwd: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PASSWORD, pwd).apply()
    }

    fun isDefaultPassword(context: Context): Boolean =
        getPassword(context) == DEFAULT_PASSWORD
}
