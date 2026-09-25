package com.usmandiscountstore.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppHelper {

    fun sendAlert(context: Context, number: String, message: String) {
        if (number.isBlank()) return
        try {
            val encoded = URLEncoder.encode(message, "UTF-8")
            val url = "https://wa.me/$number?text=$encoded"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
