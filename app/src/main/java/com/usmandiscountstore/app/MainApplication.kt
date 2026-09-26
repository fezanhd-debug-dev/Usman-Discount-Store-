package com.usmandiscountstore.app

import android.app.Application
import com.usmandiscountstore.app.data.local.AppDatabase
import com.usmandiscountstore.app.util.FaceEmbeddingHelper
import com.usmandiscountstore.app.util.Lang

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.get(this)
        FaceEmbeddingHelper.init(this)
        Lang.load(this)
    }
}
