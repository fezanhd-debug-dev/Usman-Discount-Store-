package com.usmandiscountstore.app

import android.app.Application
import com.usmandiscountstore.app.data.local.AppDatabase

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.get(this)
    }
}
