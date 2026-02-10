package com.scanner.app

import android.app.Application
import com.scanner.app.data.local.AppDatabase

class ScannerApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ScannerApp
            private set
    }
}
