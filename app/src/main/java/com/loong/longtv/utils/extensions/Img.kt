package com.loong.longtv.utils.extensions
import android.content.Context
import android.util.Log
import coil3.ImageLoader
import okhttp3.OkHttpClient
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.Request
import java.net.URLDecoder

/**
 * @author zl
 * @Date 2025/10/13
 **/
fun provideSmartImageLoader(context: Context): ImageLoader {
    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val newRequest = buildSmartRequest(original)
            chain.proceed(newRequest)
        }
        .build()

    return ImageLoader.Builder(context)
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
    Log.d("SmartImageLoader", "buildSmartRequest: $url")
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



