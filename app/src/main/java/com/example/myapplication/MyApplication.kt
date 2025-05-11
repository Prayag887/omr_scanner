package com.example.myapplication

import android.app.Application
import android.util.Log
import com.example.myapplication.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {

    companion object {
        private const val TAG = "MyApplication"

        init {
            try {
                System.loadLibrary("myapplication")
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            androidContext(this@MyApplication)
            modules(appModule)
        }
    }
}