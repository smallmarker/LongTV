package com.loong.longtv.engine.config

import android.content.Context
import android.util.Log
import com.loong.longtv.engine.source.model.TvBoxConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 配置管理器 - TVBox架构的入口点
 *
 * 职责：
 * 1. 管理多个配置源（订阅）
 * 2. 加载/切换当前活跃配置
 * 3. 缓存远程配置到本地
 * 4. 提供当前配置给SpiderManager和UI层
 */
class ConfigManager(
    private val context: Context,
    private val parser: ConfigParser = ConfigParser()
) {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val cacheDir: File by lazy {
        File(context.filesDir, "config_cache").apply { mkdirs() }
    }

    @Volatile
    private var _currentConfig: TvBoxConfig? = null
    val currentConfig: TvBoxConfig? get() = _currentConfig

    @Volatile
    private var _currentSource: ConfigSource? = null
    val currentSource: ConfigSource? get() = _currentSource

    /**
     * 加载配置 - 核心方法
     * 根据ConfigSource类型选择加载策略
     */
    suspend fun loadConfig(source: ConfigSource): Result<TvBoxConfig> = withContext(Dispatchers.IO) {
        runCatching {
            val config = when (source) {
                is ConfigSource.Asset -> loadFromAsset(source)
                is ConfigSource.Remote -> loadFromRemote(source)
                is ConfigSource.Local -> loadFromLocal(source)
            }
            _currentConfig = config
            _currentSource = source
            Log.i(TAG, "Config loaded: ${source.displayName()}, sites=${config.sites.size}")
            config
        }.onFailure {
            Log.e(TAG, "Failed to load config: ${source.displayName()}", it)
        }
    }

    /**
     * 获取当前配置的Spider Jar地址
     * 返回本地文件路径（如果是远程会先下载）
     */
    suspend fun resolveSpiderJar(): File? = withContext(Dispatchers.IO) {
        val config = _currentConfig ?: return@withContext null
        val spiderPath = config.spider
        if (spiderPath.isBlank()) return@withContext null

        when {
            // asset://tvbox/fanty/spider.jar
            spiderPath.startsWith("asset://") -> {
                extractAssetJar(spiderPath.removePrefix("asset://"))
            }
            // 本地绝对路径
            spiderPath.startsWith("/") -> {
                File(spiderPath).takeIf { it.exists() }
            }
            // 远程URL
            spiderPath.startsWith("http://") || spiderPath.startsWith("https://") -> {
                downloadJar(spiderPath)
            }
            // 相对于配置目录的路径
            else -> {
                val source = _currentSource
                when (source) {
                    is ConfigSource.Asset -> extractAssetJar("${source.assetPath}/$spiderPath")
                    is ConfigSource.Local -> File(source.filePath).parentFile?.let { File(it, spiderPath) }
                    else -> null
                }
            }
        }
    }

    private fun loadFromAsset(source: ConfigSource.Asset): TvBoxConfig {
        return parser.parseFromAsset(context, source.assetPath)
    }

    private suspend fun loadFromRemote(source: ConfigSource.Remote): TvBoxConfig {
        val json = fetchRemoteJson(source.url)
        // 缓存到本地
        val cacheFile = File(cacheDir, "${source.name.hashCode()}.json")
        cacheFile.writeText(json)
        return parser.parseFromRemote(json, source.url)
    }

    private fun loadFromLocal(source: ConfigSource.Local): TvBoxConfig {
        val file = File(source.filePath)
        require(file.exists()) { "Config file not found: ${source.filePath}" }
        return parser.parseFromFile(file)
    }

    private suspend fun fetchRemoteJson(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        require(response.isSuccessful) { "HTTP ${response.code}: ${response.message}" }
        return response.body?.string() ?: throw IllegalStateException("Empty response body")
    }

    private fun extractAssetJar(assetPath: String): File {
        val targetFile = File(cacheDir, "spider_${assetPath.hashCode()}.jar")
        if (targetFile.exists()) return targetFile
        context.assets.open(assetPath).use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return targetFile
    }

    private suspend fun downloadJar(url: String): File? {
        val targetFile = File(cacheDir, "spider_${url.hashCode()}.jar")
        if (targetFile.exists()) return targetFile
        return runCatching {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            response.body?.byteStream()?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        }.getOrNull()
    }

    fun clear() {
        _currentConfig = null
        _currentSource = null
    }

    companion object {
        private const val TAG = "ConfigManager"
    }
}
