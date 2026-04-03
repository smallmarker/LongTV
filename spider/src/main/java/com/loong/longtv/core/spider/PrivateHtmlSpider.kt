package com.loong.longtv.core.spider

import android.content.Context
import com.github.catvod.crawler.Spider
import com.google.gson.Gson
import com.loong.longtv.data.bean.CategoryContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.Video
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

abstract class PrivateHtmlSpider(
    private val defaultHost: String
) : Spider() {

    protected val gson = Gson()
    protected val client = OkHttpClient()
    protected var host: String = defaultHost

    override fun init(context: Context?, extend: String?) {
        host = resolveHost(extend)
    }

    protected fun getDocument(url: String, referer: String = "$host/"): Document {
        val response = getResponse(url, referer)
        return Jsoup.parse(response.body, host)
    }

    protected fun getResponse(url: String, referer: String = "$host/"): HtmlResponse {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent())
            .header("Referer", referer)
            .header("Origin", host)
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val cookie = response.headers("Set-Cookie")
                .map { it.substringBefore(";") }
                .joinToString("; ")
            return HtmlResponse(body, cookie)
        }
    }

    protected fun postDocument(
        url: String,
        form: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
        cookies: String = "",
        referer: String = "$host/"
    ): Document {
        val body = FormBody.Builder().apply {
            form.forEach { (key, value) -> add(key, value) }
        }.build()
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            .header("User-Agent", userAgent())
            .header("Referer", referer)
            .header("Origin", host)
        if (cookies.isNotBlank()) requestBuilder.header("Cookie", cookies)
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }
        val html = client.newCall(requestBuilder.build()).execute().use { response ->
            response.body?.string().orEmpty()
        }
        return Jsoup.parse(html, host)
    }

    protected fun pageOf(items: List<Video>, page: Int, pageCount: Int): CategoryContent {
        return CategoryContent(
            code = 1,
            pagecount = pageCount,
            total = items.size,
            page = page,
            msg = "",
            limit = items.size,
            list = items
        )
    }

    protected fun normalizeUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        return host + "/" + url.trimStart('/')
    }

    protected open fun userAgent(): String {
        return "Mozilla/5.0 (Linux; Android 13; TV) AppleWebKit/537.36 Chrome/123.0.0.0 Safari/537.36"
    }

    protected open fun resolveHost(extend: String?): String {
        val raw = extend?.trim()?.trim('"').orEmpty()
        return when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw.trimEnd('/')
            else -> defaultHost
        }
    }

    protected fun categoryName(tid: String, tabs: List<HomeTabItem>): String {
        return tabs.firstOrNull { it.type_id == tid }?.type_name ?: "影视"
    }

    protected data class HtmlResponse(
        val body: String,
        val cookies: String
    )
}
