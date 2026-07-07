package com.loong.longtv.utils.extensions
import android.content.Context
import coil3.disk.DiskCache
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import okhttp3.OkHttpClient
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.Cache
import okhttp3.Request
import okio.Path.Companion.toOkioPath
import java.io.File
import java.net.URLDecoder

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

fun prefetchSmartImages(context: Context, models: List<String>, limit: Int = 24) {
    if (models.isEmpty()) return
    val appContext = context.applicationContext
    val imageLoader = provideSmartImageLoader(appContext)
    models.asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .take(limit)
        .forEach { model ->
            val request = ImageRequest.Builder(appContext)
                .data(model)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build()
            imageLoader.enqueue(request)
        }
}

private fun buildSmartImageLoader(context: Context): ImageLoader {
    val client = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "image_http_cache"), 96L * 1024L * 1024L))
        .addInterceptor { chain ->
            val original = chain.request()
            val newRequest = buildSmartRequest(original)
            chain.proceed(newRequest)
        }
        .build()

    return ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(context.cacheDir, "image_disk_cache").toOkioPath())
                .maxSizeBytes(256L * 1024L * 1024L)
                .build()
        }
        .components {
            add(OkHttpNetworkFetcherFactory(client))
        }
        .build()
}

/**
 * 自动识别类似：
 * https://img.doubanio.com/...jpg@Referer=...@User-Agent=...
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

    url = url.substringBefore("@")

    val builder = original.newBuilder().url(url)

    referer?.let { builder.addHeader("Referer", it) }
    ua?.let { builder.addHeader("User-Agent", it) }
    cookie?.let { builder.addHeader("Cookie", it) }

    if (ua.isNullOrEmpty()) {
        builder.addHeader(
            "User-Agent",
            "Dalvik/2.1.0 (Linux; U; Android 13; M2102J2SC Build/TKQ1.220829.002)"
        )
    }

    return builder.build()
}
