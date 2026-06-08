package com.loong.longtv.engine.spider

import android.content.Context
import android.util.Log
import com.github.catvod.crawler.Spider
import dalvik.system.DexClassLoader
import java.io.File

/**
 * Spider加载器 - 通过DexClassLoader动态加载Jar中的Spider类
 *
 * TVBox的Jar插件本质：
 * - 一个包含多个Spider实现类的dex文件
 * - 所有Spider类都在 com.github.catvod.spider 包下
 * - 通过 csp_XXX 映射到 com.github.catvod.spider.XXX
 * - Jar中可能包含 com.github.catvod.spider.Init 类用于初始化
 *
 * Android 10+ 可写dex限制说明：
 * - 饭太硬等Spider JAR使用DouDouGuard保护，其native lib(.so)在Init.init(Context)
 *   内部调用 DexNative.getLoader(context) 时，会解密guard文件并写入
 *   cache/sharedb/config.db，然后尝试创建DexClassLoader。
 * - Android 10+ 禁止从可写dex文件创建DexClassLoader；若config.db只读，native则
 *   无法写入（每次都重写），直接返回null。两种情况都导致Init.loader()为null。
 * - 修复方案：调用init(Context)触发config.db写入，然后将其复制到codeCacheDir并
 *   设为只读，手动创建DexClassLoader并通过反射注入到Init单例，绕过native限制。
 */
class SpiderLoader(private val context: Context) {

    private var classLoader: ClassLoader? = null
    private var guardLoader: ClassLoader? = null
    private var currentJarPath: String? = null

    /**
     * 加载Jar文件，创建DexClassLoader
     */
    fun loadJar(jarFile: File): Boolean {
        if (!jarFile.exists()) {
            Log.e(TAG, "Jar file not found: ${jarFile.absolutePath}")
            return false
        }

        // 避免重复加载同一个Jar
        if (currentJarPath == jarFile.absolutePath && classLoader != null) {
            return true
        }

        // Android 10+ 禁止从可写文件加载 dex，必须先设为只读
        if (jarFile.canWrite()) jarFile.setReadOnly()

        val optimizedDir = File(context.codeCacheDir, "spider_dex_${jarFile.name.hashCode()}").apply { mkdirs() }
        val libDir = jarFile.parentFile?.absolutePath ?: context.filesDir.absolutePath

        val loader = DexClassLoader(
            jarFile.absolutePath,
            optimizedDir.absolutePath,
            libDir,
            context.classLoader
        )

        // 初始化Jar运行时（含两次调用init(Context)的Android 10+修复逻辑）
        initJarRuntime(loader)

        classLoader = loader
        currentJarPath = jarFile.absolutePath
        // cache the guardLoader resolved during initJarRuntime for createSpider fallback
        guardLoader = resolveGuardLoader(loader)
        Log.i(TAG, "Jar loaded: ${jarFile.absolutePath}")
        return true
    }

    /**
     * 获取已注入的 guard DexClassLoader（来自 Init.loader()）
     */
    private fun resolveGuardLoader(spiderLoader: ClassLoader): ClassLoader? {
        return runCatching {
            val initClass = spiderLoader.loadClass("com.github.catvod.spider.Init")
            initClass.getMethod("loader").invoke(null) as? ClassLoader
        }.getOrNull()
    }

    /**
     * 根据类名创建Spider实例
     * @param className 短类名（如 "DouDouGuard" 或 "XPathGuard"）
     */
    fun createSpider(className: String): Spider? {
        val loader = classLoader ?: run {
            Log.e(TAG, "ClassLoader not initialized, call loadJar first")
            return null
        }

        // 在实例化Spider前检查JAR的Init.loader()是否已就绪。
        // Init.loader()为null时，Spider构造器中的DexNative.getSpider(null,name)
        // 会触发JNI FATAL ABORT（无法被try-catch捕获），直接杀死进程。
        if (!checkJarInitLoaderReady(loader)) {
            Log.e(TAG, "JAR Init.loader() is null - Spider creation aborted to prevent JNI crash")
            return null
        }

        val fullClassName = "$SPIDER_PACKAGE.$className"

        // 优先从 spider.jar 的 classloader 加载；对于只存在于 guard.dex 的类
        // （如 XPathGuard），fallback 到 guardLoader。
        val instance = runCatching {
            val clazz = loader.loadClass(fullClassName)
            clazz.getDeclaredConstructor().newInstance()
        }.getOrElse { e1 ->
            if (e1 !is ClassNotFoundException) {
                Log.e(TAG, "Failed to create spider: $fullClassName", e1)
                return null
            }
            // ClassNotFoundException：尝试从 guard.dex classloader 加载
            val gl = guardLoader ?: run {
                Log.e(TAG, "Failed to create spider: $fullClassName (not in spider.jar, no guard loader)", e1)
                return null
            }
            runCatching {
                val clazz = gl.loadClass(fullClassName)
                clazz.getDeclaredConstructor().newInstance()
            }.onFailure {
                Log.e(TAG, "Failed to create spider in guard loader: $fullClassName", it)
            }.getOrNull()
        }
        return instance as? Spider
    }

    /**
     * 检查JAR内部Init.loader()是否已就绪（非null）。
     * 仅对包含 com.github.catvod.spider.Init 的Jar执行检查。
     */
    private fun checkJarInitLoaderReady(loader: ClassLoader): Boolean {
        return runCatching {
            val initClass = loader.loadClass("com.github.catvod.spider.Init")
            val loaderVal = initClass.getMethod("loader").invoke(null)
            if (loaderVal == null) {
                Log.e(TAG, "Init.loader() returned null")
                false
            } else {
                Log.d(TAG, "Init.loader() OK: ${loaderVal.javaClass.simpleName}")
                true
            }
        }.getOrElse { e ->
            // 该Jar没有Init.loader()方法（非DouDouGuard类型），允许直接创建Spider
            Log.d(TAG, "No Init.loader() method, skipping check: ${e.message}")
            true
        }
    }

    /**
     * 初始化Jar中的 com.github.catvod.spider.Init 类
     *
     * 饭太硬 fanty JAR 的 Init.init(Context) 逻辑（反编译）：
     *   1. 将 (Application) context 存入单例字段
     *   2. 调用 DexNative.getLoader(context) → native code（ftyguard_v8.so）：
     *      a. 从 ftyshinidie.guard 解密 → 每次都重写 sharedb/config.db（可写文件）
     *      b. 尝试 new DexClassLoader(config.db, ...) → Android 10+ 因可写dex失败 → 返回null
     *      c. 若 config.db 为只读，写入失败 → native直接返回null（不尝试DexClassLoader）
     *   3. 将返回的 DexClassLoader（通常为null）存入 Init 单例字段
     *
     * Android 10+ 修复方案（三步走）：
     *   1. 删除旧的 config.db（必须可写才能让 native 写入新文件）
     *   2. 调用 init(Context) → native 写入新的 config.db（可写，内容正确）
     *   3. 将 config.db 复制到 codeCacheDir 并设为只读，手动创建 DexClassLoader 并
     *      通过反射注入到 Init 单例，彻底绕过 native 的 DexClassLoader 创建步骤
     */
    private fun initJarRuntime(loader: ClassLoader) {
        val initClass = runCatching {
            loader.loadClass("com.github.catvod.spider.Init")
        }.getOrElse {
            Log.d(TAG, "No Init class in JAR, skipping init: ${it.message}")
            return
        }

        val initMethod = runCatching {
            initClass.getMethod("init", Context::class.java)
        }.getOrElse {
            Log.d(TAG, "No init(Context) method in JAR Init class: ${it.message}")
            return
        }

        val appContext = context.applicationContext

        // 步骤1: 删除旧的 config.db，确保 native 每次都能正常写入新文件
        val sharedbDir = File(context.cacheDir, "sharedb").apply { mkdirs() }
        val configDb = File(sharedbDir, "config.db")
        if (configDb.exists()) {
            if (!configDb.canWrite()) configDb.setWritable(true)
            configDb.delete()
            Log.d(TAG, "Deleted stale config.db to allow fresh native write")
        }

        // 步骤2: 调用 init(Context) → native 解密 guard 文件并写入新的 config.db
        runCatching {
            initMethod.invoke(null, appContext)
            Log.d(TAG, "Init.init(Context) completed, config.db exists: ${configDb.exists()} size: ${configDb.length()}")
        }.onFailure {
            Log.w(TAG, "Init.init(Context) failed: ${it.message}")
        }

        // 步骤3: 复制 config.db 到 codeCacheDir，设为只读，手动创建并注入 DexClassLoader
        if (configDb.exists() && configDb.length() > 0) {
            injectGuardLoader(initClass, loader, configDb)
        } else {
            Log.e(TAG, "config.db not created or empty - guard init failed")
        }
    }

    /**
     * 将 config.db 复制到 codeCacheDir，设为只读，创建 DexClassLoader，并通过反射
     * 注入到 JAR Init 单例的 DexClassLoader 字段，绕过 native 可写 dex 限制。
     *
     * 同时处理 guard.dex 内部可能包含独立的 com.github.catvod.Init 类问题：
     * guard.dex 的 classloader 优先搜索自身 dex，因此 guard.dex 内的 Spider 类
     * 调用 com.github.catvod.Init.context() 时拿到的是 guard.dex 自己的 Init，
     * 其 application 字段为 null。修复方法：在 guardLoader 创建后主动对其 Init 设置 context。
     */
    private fun injectGuardLoader(initClass: Class<*>, spiderLoader: ClassLoader, configDb: File) {
        runCatching {
            // 复制到 codeCacheDir（专为代码存储设计，ART 允许从此处优化 dex）
            val guardDir = File(context.codeCacheDir, "guard").apply { mkdirs() }
            val guardDex = File(guardDir, "guard.dex")
            configDb.copyTo(guardDex, overwrite = true)
            if (guardDex.canWrite()) guardDex.setReadOnly()
            Log.d(TAG, "Copied config.db to ${guardDex.absolutePath} (${guardDex.length()} bytes, read-only: ${!guardDex.canWrite()})")

            // 创建 DexClassLoader，parent 为 spider.jar 的 ClassLoader
            val guardOptDir = File(context.codeCacheDir, "guard_opt").apply { mkdirs() }
            val gl = DexClassLoader(
                guardDex.absolutePath,
                guardOptDir.absolutePath,
                null,
                spiderLoader
            )
            Log.d(TAG, "DexClassLoader from guard.dex created successfully")

            // 通过反射找到 Init 单例中类型为 DexClassLoader 的字段并注入
            val singleton = initClass.getMethod("get").invoke(null)
            val dexLoaderField = initClass.declaredFields.firstOrNull {
                dalvik.system.DexClassLoader::class.java.isAssignableFrom(it.type)
            }?.apply { isAccessible = true }

            if (dexLoaderField != null) {
                dexLoaderField.set(singleton, gl)
                Log.i(TAG, "Injected guard DexClassLoader into Init singleton - Init.loader() should now be non-null")
            } else {
                Log.e(TAG, "DexClassLoader field not found in Init class - cannot inject guard loader")
            }

            // guard.dex 内部可能有自己的 com.github.catvod.Init，其 application 字段
            // 未被 App.onCreate() 设置，导致 guard.dex 的 Spider 调用 Init.context() 返回 null。
            // 在 guardLoader 创建后立即对其设置 applicationContext。
            initGuardContext(gl)
        }.onFailure {
            Log.e(TAG, "Failed to inject guard DexClassLoader: ${it.message}", it)
        }
    }

    /**
     * 对 guard.dex 内部的 com.github.catvod.Init 和 com.github.catvod.spider.InitOrigin
     * （若存在）设置 applicationContext，防止 guard.dex 的 Spider 类调用
     * Init.context() / InitOrigin.getSpStr() 时 context 为 null。
     */
    private fun initGuardContext(gl: ClassLoader) {
        val app = context.applicationContext
        val candidates = listOf("com.github.catvod.Init", "com.github.catvod.spider.InitOrigin")
        for (className in candidates) {
            runCatching {
                val clazz = gl.loadClass(className)
                val setMethod = clazz.methods.firstOrNull { m ->
                    m.name == "set" && m.parameterTypes.size == 1
                } ?: return@runCatching
                setMethod.invoke(null, app)
                Log.i(TAG, "Set applicationContext on guard.dex's $className via $setMethod")
            }.onFailure {
                Log.d(TAG, "guard.dex $className: ${it.message}")
            }
        }
    }

    fun isLoaded(): Boolean = classLoader != null

    fun release() {
        classLoader = null
        guardLoader = null
        currentJarPath = null
    }

    companion object {
        private const val TAG = "SpiderLoader"
        private const val SPIDER_PACKAGE = "com.github.catvod.spider"
    }
}
