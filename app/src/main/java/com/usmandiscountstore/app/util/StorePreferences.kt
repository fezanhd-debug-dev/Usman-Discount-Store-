package com.usmandiscountstore.app.util

import android.content.Context

object StorePreferences {

    private const val PREFS_NAME = "store_prefs"
    private const val KEY_STORE_NAME = "store_name"

    // Default store name agar user ne kuch set nahi kiya
    private const val DEFAULT_STORE_NAME = "Usman Discount Store"

    fun getStoreName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_STORE_NAME, DEFAULT_STORE_NAME) ?: DEFAULT_STORE_NAME
    }

    fun setStoreName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STORE_NAME, name.trim()).apply()
    }
}
