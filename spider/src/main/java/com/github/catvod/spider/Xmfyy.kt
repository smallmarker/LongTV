package com.github.catvod.spider

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import com.github.catvod.crawler.Spider
import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

/** Built-in adapter for xmftv; backupUrls must use the same catalogue and page structure. */
class Xmfyy : Spider() {
    @Volatile private var baseUrl = DEFAULT_BASE
    private var candidates = listOf(DEFAULT_BASE)
    private lateinit var preferences: SharedPreferences
    private var configKey = ""
    private var lastRecovery = -RECOVERY_INTERVAL
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override fun init(context: Context, extend: String) {
        val ext = runCatching { JSONObject(extend) }.getOrNull()
        val primary = ext?.optString("url") ?: extend
        val backups = ext?.optJSONArray("backupUrls")
        candidates = (listOf(primary) + (0 until (backups?.length() ?: 0)).map {
            backups?.optString(it).orEmpty()
        } + DEFAULT_BASE).mapNotNull(::origin).distinct().take(5)
        preferences = context.getSharedPreferences("xmfyy_domains", Context.MODE_PRIVATE)
        configKey = candidates.joinToString("|")
        check(resolveSite()) { "小蜜蜂站点不可用或页面结构已变化，请检查域名配置" }
    }

    @Synchronized
    private fun resolveSite(recovery: Boolean = false): Boolean {
        if (recovery) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastRecovery < RECOVERY_INTERVAL) return false
            lastRecovery = now
        }
        val cached = preferences.getString("$configKey.url", null)
        val urls = (listOfNotNull(cached) + candidates).distinct()
        for (url in urls) {
            val page = request("$url/") ?: continue
            val doc = Jsoup.parse(page.html, page.url)
            if (!XmfPageParser.isHome(doc)) {
                Log.w(TAG, "Rejected domain: $url (unsupported page structure)")
                continue
            }
            val resolved = origin(page.url) ?: continue
            val sampleId = preferences.getString("$configKey.sampleId", null)
            val sampleName = preferences.getString("$configKey.sampleName", null)
            // A matching page template alone does not establish that numeric IDs are compatible.
            if (cached != null && cached != resolved && sampleId != null && sampleName != null) {
                val sample = request("$resolved/vod/${sampleId.substringAfter(':')}")
                val matches = sample?.let {
                    val detail = Jsoup.parse(it.html, it.url)
                    detail.selectFirst(".detail h1")?.text() == sampleName &&
                        detail.selectFirst(".episodes a[href^=/play/]") != null
                } == true
                if (!matches) {
                    Log.w(TAG, "Rejected domain: $resolved (catalogue sample does not match)")
                    continue
                }
            }
            baseUrl = resolved
            val sample = XmfPageParser.items(doc).first()
            preferences.edit().putString("$configKey.url", resolved)
                .putString("$configKey.sampleId", sample.getValue("vod_id"))
                .putString("$configKey.sampleName", sample.getValue("vod_name")).apply()
            Log.i(TAG, "Validated domain: $url -> $resolved")
            return true
        }
        Log.e(TAG, "No usable domain; candidates=$urls")
        return false
    }

    override fun homeContent(filter: Boolean): String {
        val doc = getDoc("/") ?: error("小蜜蜂首页加载失败")
        check(XmfPageParser.isHome(doc)) { "小蜜蜂首页结构已变化" }
        val classes = XmfPageParser.categories(doc)
        val filters = if (filter) classes.associate { category ->
            val id = category.getValue("type_id")
            id to (getDoc("/vod/type/$id")?.let(XmfPageParser::filters) ?: emptyList())
        }.filterValues { it.isNotEmpty() } else emptyMap()
        return gson.toJson(mapOf("class" to classes, "filters" to filters))
    }

    override fun homeVideoContent(): String = listResult(getDoc("/"), 1, 24)

    override fun categoryContent(tid: String, pg: String, filter: Boolean, extend: HashMap<String, String>): String {
        require(tid.matches(Regex("\\d+"))) { "无效的小蜜蜂分类" }
        val page = pg.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val url = "$baseUrl/vod/type/$tid".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("page", page.toString())
        extend.filterKeys { it in XmfPageParser.FILTER_KEYS }.forEach { (key, value) ->
            if (value.isNotBlank()) url.addQueryParameter(key, value)
        }
        return listResult(getDoc(url.build().toString()), page)
    }

    override fun searchContent(key: String, quick: Boolean): String = searchContent(key, quick, "1")

    override fun searchContent(key: String, quick: Boolean, pg: String): String {
        val page = pg.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val url = "$baseUrl/search".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("q", key).addQueryParameter("page", page.toString()).build()
        return listResult(getDoc(url.toString()), page)
    }

    override fun detailContent(ids: List<String>): String {
        val id = ids.firstOrNull().orEmpty()
        // Old xmfyy numeric IDs belong to a different catalogue and cannot be migrated.
        if (!id.matches(Regex("xmftv:\\d+"))) return gson.toJson(mapOf("list" to emptyList<Any>()))
        val doc = getDoc("/vod/${id.substringAfter(':')}") ?: error("小蜜蜂详情加载失败")
        return gson.toJson(mapOf("list" to listOf(XmfPageParser.detail(doc, id))))
    }

    override fun playerContent(flag: String, id: String, vipFlags: List<String>): String {
        val path = id.takeIf { it.matches(Regex("/play/\\d+/\\d+")) }
            ?: id.toHttpUrlOrNull()?.takeIf { it.host == baseUrl.toHttpUrlOrNull()?.host }
                ?.encodedPath?.takeIf { it.matches(Regex("/play/\\d+/\\d+")) }
            ?: error("无效的小蜜蜂播放地址")
        val doc = getDoc(path) ?: error("小蜜蜂播放页加载失败")
        val url = XmfPageParser.playerUrl(doc)
        check(url.isNotBlank()) { "小蜜蜂播放页未找到视频地址" }
        return gson.toJson(mapOf("parse" to 0, "url" to url,
            "header" to mapOf("User-Agent" to UA, "Referer" to "$baseUrl$path")))
    }

    private fun listResult(doc: Document?, page: Int, limit: Int = 24): String {
        checkNotNull(doc) { "小蜜蜂列表加载失败" }
        check(doc.selectFirst("main .grid") != null) { "小蜜蜂列表结构已变化" }
        val items = XmfPageParser.items(doc).take(limit)
        val pages = XmfPageParser.pageCount(doc).coerceAtLeast(page)
        return gson.toJson(mapOf("page" to page, "pagecount" to pages, "limit" to limit,
            "total" to pages * limit, "list" to items))
    }

    private fun getDoc(pathOrUrl: String): Document? {
        val current = baseUrl.toHttpUrlOrNull() ?: return null
        val parsed = pathOrUrl.toHttpUrlOrNull()
        val path = parsed?.let { it.encodedPath + (it.encodedQuery?.let { query -> "?$query" } ?: "") }
            ?: pathOrUrl
        val url = if (parsed != null && parsed.host != current.host) return null else "$baseUrl$path"
        var page = request(url)
        val invalidPage = page?.let {
            !XmfPageParser.supportsPage(Jsoup.parse(it.html, it.url), path)
        } ?: true
        val redirected = page?.let { origin(it.url) != baseUrl } ?: false
        if ((invalidPage || redirected) && resolveSite(recovery = true)) page = request("$baseUrl$path")
        return page?.let { Jsoup.parse(it.html, it.url) }?.takeIf {
            XmfPageParser.supportsPage(it, path)
        }
    }

    private fun request(url: String): Page? = runCatching {
        val request = Request.Builder().url(url).header("User-Agent", UA)
            .header("Referer", "$baseUrl/").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP ${response.code}: $url")
                null
            } else response.body?.string()?.let { Page(it, response.request.url.toString()) }
        }
    }.onFailure { Log.w(TAG, "Request failed: $url", it) }.getOrNull()

    private fun origin(value: String): String? = value.trim().toHttpUrlOrNull()?.let {
        it.newBuilder().encodedPath("/").query(null).fragment(null).build().toString().trimEnd('/')
    }

    private data class Page(val html: String, val url: String)

    companion object {
        private const val TAG = "Xmfyy"
        private const val DEFAULT_BASE = "https://www.xmftv.com"
        private const val RECOVERY_INTERVAL = 60_000L
        private const val UA = "Mozilla/5.0 (Linux; Android 12; LongTV) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
    }
}
