package com.loong.longtv

import android.app.Application
import com.github.catvod.Init
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Init.set(this)
    }
}
