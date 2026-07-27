package com.loong.longtv

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.github.catvod.Init
import com.loong.longtv.data.cache.CacheStorage
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    companion object {
        private const val TAG = "App"
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Init.set(this)
        // 旧版本可能已留下数百 MB 的视频缓存，避免首次升级后仍长期占用空间。
        Thread { CacheStorage.enforceBudget(this) }.start()
        installMainThreadCrashProtection()
    }

    /**
     * 防止 spider.jar 内部错误处理器（fM.run()）把整个 App 干掉。
     *
     * spider.jar 在 Spider init 失败时，会向主线程 Handler 投递一个 Runnable，
     * 该 Runnable 内部调用 throwable.getMessage()，当 throwable 为 null 时触发
     * NullPointerException，导致主线程 FATAL CRASH。
     *
     * 原理：在主线程 Looper 启动后，用 try-catch 包住 Looper.loop() 的死循环，
     * 捕获来自 com.github.catvod 包的异常并吞掉，让 Looper 继续运行。
     * 来自 App 自身代码的其它异常仍会向上抛出，不影响正常崩溃报告。
     */
    private fun installMainThreadCrashProtection() {
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                    break // loop() 正常返回（不应该发生）
                } catch (e: Throwable) {
                    val fromSpiderJar = e.stackTrace.any {
                        it.className.startsWith("com.github.catvod")
                    }
                    if (fromSpiderJar) {
                        Log.e(TAG, "Caught non-fatal spider JAR crash on main thread, resuming: ${e.message}", e)
                        // 继续循环，主线程恢复运行
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}
