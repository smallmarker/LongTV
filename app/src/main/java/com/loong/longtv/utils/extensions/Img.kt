package com.loong.longtv.utils.extensions
import android.content.Context
import coil3.disk.DiskCache
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.loong.longtv.data.cache.CacheStorage
import com.loong.longtv.engine.source.model.PosterImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Path.Companion.toOkioPath
import java.security.MessageDigest
import java.io.File
import android.net.Uri
import java.net.URLDecoder
import org.json.JSONObject

/**
 * @author zl
 * @Date 2025/10/13
 **/
@Volatile
private var sharedSmartImageLoader: ImageLoader? = null

fun provideSmartImageLoader(context: Context): ImageLoader {
    sharedSmartImageLoader?.let { return it }
    synchronized(::provideSmartImageLoader) {
        sharedSmartImageLoader?.let { return it }
        return buildSmartImageLoader(context.applicationContext).also {
            sharedSmartImageLoader = it
        }
    }
}

fun prefetchSmartImages(context: Context, models: List<PosterImage>, limit: Int = 24) {
    if (models.isEmpty()) return
    val appContext = context.applicationContext
    val imageLoader = provideSmartImageLoader(appContext)
    models.asSequence()
        .filter { it.url.isNotBlank() }
        .distinctBy { it.url to it.headers }
        .take(limit)
        .forEach { poster ->
            imageLoader.enqueue(createPosterImageRequest(appContext, poster))
        }
}

fun createPosterImageRequest(context: Context, poster: PosterImage, retryAttempt: Int = 0): ImageRequest {
    val cacheKey = poster.cacheKey()
    val headers = NetworkHeaders.Builder().apply {
        poster.effectiveHeaders().forEach { (name, value) -> this[name] = value }
    }.build()
    return ImageRequest.Builder(context)
        .data(poster.url)
        .httpHeaders(headers)
        .memoryCacheKey("$cacheKey#$retryAttempt")
        .diskCacheKey(cacheKey)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .build()
}

/**
 * 部分图床会校验请求来源：
 * - 豆瓣：无 Referer / 非浏览器 UA 时常返回 418；
 * - 爱奇艺：部分 CDN 节点会对直连图片返回 403。
 * - B 站：部分 CDN 节点需要站内 Referer。
 */
private fun PosterImage.effectiveHeaders(): Map<String, String> {
    val result = headers.toMutableMap()
    val host = Uri.parse(url).host.orEmpty()
    if (host.endsWith("doubanio.com", ignoreCase = true)) {
        result.putIfAbsent("Referer", "https://movie.douban.com/")
        result.putIfAbsent("User-Agent", BROWSER_USER_AGENT)
    }
    if (host.endsWith("iqiyipic.com", ignoreCase = true)) {
        result.putIfAbsent("Referer", "https://www.iqiyi.com/")
        result.putIfAbsent("User-Agent", BROWSER_USER_AGENT)
    }
    if (host.endsWith("hdslb.com", ignoreCase = true)) {
        result.putIfAbsent("Referer", "https://www.bilibili.com/")
        result.putIfAbsent("User-Agent", BROWSER_USER_AGENT)
    }
    return result
}

private fun buildSmartImageLoader(context: Context): ImageLoader {
    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val newRequest = buildSmartRequest(original)
            chain.proceed(newRequest)
        }
        .build()

    return ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.15)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(context.cacheDir, "image_disk_cache").toOkioPath())
                .maxSizeBytes(CacheStorage.MAX_IMAGE_DISK_CACHE_BYTES)
                .build()
        }
        .components {
            add(OkHttpNetworkFetcherFactory(client))
        }
        .build()
}

private fun PosterImage.cacheKey(): String {
    val source = buildString {
        append(url)
        headers.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (name, value) ->
            append('\n').append(name).append('=').append(value)
        }
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(source.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

/** 标准浏览器 UA；不要在此加入 App 名称，部分 CDN 会将其识别为非浏览器请求。 */
private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

/** 保持普通图床此前已验证可用的请求特征，浏览器 UA 只按域名精确启用。 */
private const val DEFAULT_IMAGE_USER_AGENT =
    "Dalvik/2.1.0 (Linux; U; Android 13; M2102J2SC Build/TKQ1.220829.002)"

/**
 * 自动识别类似：
 * https://img.doubanio.com/...jpg@Referer=...@User-Agent=...@Headers=%7B...%7D
 */
private fun buildSmartRequest(original: Request): Request {
    var url = original.url.toString()
    fun extract(tag: String): String? {
        val regex = "@$tag=([^@]+)".toRegex()
        return regex.find(url)?.groups?.get(1)?.value?.let {
            URLDecoder.decode(it, "UTF-8")
        }
    }

    val referer = extract("Referer")
    val ua = extract("User-Agent")
    val cookie = extract("Cookie")
    val customHeaders = extract("Headers").toHttpHeaders()

    url = url.substringBefore("@")

    val builder = original.newBuilder().url(url)

    customHeaders.forEach { (name, value) -> builder.header(name, value) }
    referer?.let { builder.header("Referer", it) }
    ua?.let { builder.header("User-Agent", it) }
    cookie?.let { builder.header("Cookie", it) }

    if (ua.isNullOrEmpty() && original.header("User-Agent").isNullOrEmpty() && !customHeaders.hasHeader("User-Agent")) {
        builder.header("User-Agent", DEFAULT_IMAGE_USER_AGENT)
    }
    return builder.build()
}

/** 兼容 TVBox 常用的 @Headers=URL 编码 JSON 约定。无效内容直接忽略，不影响图片请求。 */
private fun String?.toHttpHeaders(): Map<String, String> {
    if (isNullOrBlank()) return emptyMap()
    return runCatching {
        val json = JSONObject(this)
        buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                val value = json.optString(name)
                if (name.isNotBlank() && value.isNotBlank()) put(name, value)
            }
        }
    }.getOrDefault(emptyMap())
}

private fun Map<String, String>.hasHeader(name: String): Boolean =
    keys.any { it.equals(name, ignoreCase = true) }
