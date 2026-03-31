package com.loong.longtv

import android.app.Application
import com.github.catvod.Init
import dagger.hilt.android.HiltAndroidApp

/**
 * @author zl
 * @Date 2025/3/10
 **/
@HiltAndroidApp
class App : Application() {

    companion object {
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Init.set(this)
    }

}
