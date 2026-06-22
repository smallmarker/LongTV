package com.github.catvod.spider

import android.content.Context
import android.util.Log
import com.github.catvod.crawler.Spider
import com.google.gson.Gson
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * AppRJ Spider - 热播影视专用爬虫 (v.rbotv.cn 系列站点)
 *
 * ext格式: {"url": "http://example.com"}
 *
 * API协议: 自定义 POST 表单接口 + MD5 时间戳签名
 *   签名: MD5(SIGN_KEY + timestamp)，timestamp = System.currentTimeMillis()/1000
 *
 *   - 首页分类: POST /v3/type/top_type
 *   - 分类列表: POST /v3/home/type_search
 *   - 影片详情: POST /v3/home/vod_details
 *   - 搜索:     POST /v3/home/search
 */
class AppRJ : Spider() {

    private var baseUrl = ""
    private val gson = Gson()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    companion object {
        private const val TAG = "AppRJ"
        private const val SIGN_KEY = "7gp0bnd2sr85ydii2j32pcypscoc4w6c7g5spl"
    }

    override fun init(context: Context, extend: String) {
        runCatching {
            baseUrl = JSONObject(extend).getString("url").trimEnd('/')
        }.onFailure {
            baseUrl = extend.trimEnd('/')
        }
        Log.d(TAG, "AppRJ initialized with baseUrl=$baseUrl")
    }

    private fun signParams(): MutableMap<String, String> {
        val ts = (System.currentTimeMillis() / 1000).toString()
        return mutableMapOf("timestamp" to ts, "sign" to md5(SIGN_KEY + ts))
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return BigInteger(1, bytes).toString(16).padStart(32, '0').lowercase()
    }

    private fun post(path: String, params: Map<String, String>): String? {
        return runCatching {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .apply { params.forEach { (k, v) -> addFormDataPart(k, v) } }
                .build()
            val request = Request.Builder()
                .url(baseUrl + path)
                .post(body)
                .header("User-Agent", "okhttp-okgo/jeasonlzy")
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string()
                else { Log.w(TAG, "HTTP ${resp.code} for $path"); null }
            }
        }.onFailure { Log.e(TAG, "post failed: $path", it) }.getOrNull()
    }

    override fun homeContent(filter: Boolean): String {
        if (baseUrl.isEmpty()) return emptyHomeContent()
        val json = post("/v3/type/top_type", signParams()) ?: return emptyHomeContent()
        return runCatching {
            val list = JSONObject(json).optJSONObject("data")?.optJSONArray("list")
                ?: return emptyHomeContent()
            val classes = mutableListOf<Map<String, String>>()
            val filters = mutableMapOf<String, List<Map<String, Any>>>()
            for (i in 0 until list.length()) {
                val item = list.optJSONObject(i) ?: continue
                val typeId = item.optString("type_id")
                classes.add(mapOf(
                    "type_id" to typeId,
                    "type_name" to item.optString("type_name")
                ))
                val tabFilters = buildList {
                    addFilter("class", "类型", item.optJSONArray("extend"))
                    addFilter("area", "地区", item.optJSONArray("area"))
                    addFilter("lang", "语言", item.optJSONArray("lang"))
                    addFilter("year", "年份", item.optJSONArray("year"))
                }
                if (typeId.isNotBlank() && tabFilters.isNotEmpty()) {
                    filters[typeId] = tabFilters
                }
            }
            gson.toJson(mapOf("class" to classes, "filters" to filters))
        }.onFailure { Log.e(TAG, "homeContent parse error", it) }.getOrDefault(emptyHomeContent())
    }

    private fun MutableList<Map<String, Any>>.addFilter(key: String, name: String, values: org.json.JSONArray?) {
        if (values == null || values.length() == 0) return
        val items = mutableListOf<Map<String, String>>()
        for (i in 0 until values.length()) {
            val label = values.optString(i).trim()
            if (label.isBlank()) continue
            items.add(mapOf("n" to label, "v" to if (label == "全部") "" else label))
        }
        if (items.size > 1) {
            add(mapOf("key" to key, "name" to name, "value" to items.distinctBy { it["v"] }))
        }
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        if (baseUrl.isEmpty()) return emptyListResult()
        val params = signParams().apply {
            put("type_id", tid); put("limit", "20"); put("page", pg)
            extend["area"]?.let { put("area", it) }
            extend["class"]?.let { put("class", it) }
            extend["lang"]?.let { put("lang", it) }
            extend["year"]?.let { put("year", it) }
        }
        val json = post("/v3/home/type_search", params) ?: return emptyListResult()
        return runCatching {
            val data = JSONObject(json).optJSONObject("data") ?: return emptyListResult()
            val list = data.optJSONArray("list") ?: return emptyListResult()
            val items = (0 until list.length()).mapNotNull { i ->
                val item = list.optJSONObject(i) ?: return@mapNotNull null
                mapOf(
                    "vod_id" to item.optString("vod_id"),
                    "vod_name" to item.optString("vod_name"),
                    "vod_pic" to (item.optString("vod_pic").ifEmpty { item.optString("vod_pic_thumb") }),
                    "vod_remarks" to item.optString("vod_remarks")
                )
            }
            gson.toJson(mapOf<String, Any>("page" to (pg.toIntOrNull() ?: 1), "pagecount" to 99,
                "limit" to 20, "total" to items.size, "list" to items))
        }.onFailure { Log.e(TAG, "categoryContent parse error", it) }.getOrDefault(emptyListResult())
    }

    override fun detailContent(ids: List<String>): String {
        if (baseUrl.isEmpty()) return emptyListResult()
        val params = signParams().apply { put("vod_id", ids.first()) }
        val json = post("/v3/home/vod_details", params) ?: return emptyListResult()
        return runCatching {
            val d = JSONObject(json).optJSONObject("data") ?: return emptyListResult()
            val pic = d.optString("vod_pic").ifEmpty { d.optString("vod_pic_thumb") }
            val playFromParts = mutableListOf<String>()
            val playUrlParts = mutableListOf<String>()
            val playList = d.optJSONArray("vod_play_list")
            if (playList != null) {
                for (i in 0 until playList.length()) {
                    val src = playList.optJSONObject(i) ?: continue
                    playFromParts.add(src.optString("name", "播放"))
                    val urls = src.optJSONArray("urls")
                    val epParts = mutableListOf<String>()
                    if (urls != null) {
                        for (j in 0 until urls.length()) {
                            val ep = urls.optJSONObject(j) ?: continue
                            epParts.add("${ep.optString("name")}$${ep.optString("url")}")
                        }
                    }
                    playUrlParts.add(epParts.joinToString("#"))
                }
            }
            val item = mapOf(
                "vod_id" to ids.first(),
                "vod_name" to d.optString("vod_name"),
                "vod_pic" to pic,
                "vod_remarks" to d.optString("vod_remarks"),
                "vod_content" to d.optString("vod_content"),
                "vod_year" to d.optString("vod_year"),
                "vod_actor" to d.optString("vod_actor"),
                "vod_director" to d.optString("vod_director"),
                "vod_play_from" to playFromParts.joinToString("\$\$\$"),
                "vod_play_url" to playUrlParts.joinToString("\$\$\$")
            )
            gson.toJson(mapOf("list" to listOf(item)))
        }.onFailure { Log.e(TAG, "detailContent parse error", it) }.getOrDefault(emptyListResult())
    }

    override fun searchContent(key: String, quick: Boolean): String = searchContent(key, quick, "1")

    override fun searchContent(key: String, quick: Boolean, pg: String): String {
        if (baseUrl.isEmpty()) return emptyListResult()
        val params = signParams().apply { put("keyword", key); put("limit", "20"); put("page", pg) }
        val json = post("/v3/home/search", params) ?: return emptyListResult()
        return runCatching {
            val list = JSONObject(json).optJSONObject("data")?.optJSONArray("list")
                ?: return emptyListResult()
            val items = (0 until list.length()).mapNotNull { i ->
                val item = list.optJSONObject(i) ?: return@mapNotNull null
                mapOf(
                    "vod_id" to item.optString("vod_id"),
                    "vod_name" to item.optString("vod_name"),
                    "vod_pic" to (item.optString("vod_pic").ifEmpty { item.optString("vod_pic_thumb") }),
                    "vod_remarks" to item.optString("vod_remarks")
                )
            }
            gson.toJson(mapOf("list" to items))
        }.onFailure { Log.e(TAG, "searchContent parse error", it) }.getOrDefault(emptyListResult())
    }

    override fun playerContent(flag: String, id: String, vipFlags: List<String>): String {
        return gson.toJson(mapOf("parse" to 0, "url" to id))
    }

    private fun emptyHomeContent() = gson.toJson(mapOf("class" to emptyList<Any>(), "filters" to emptyMap<String, Any>()))
    private fun emptyListResult() =
        gson.toJson(mapOf("page" to 1, "pagecount" to 0, "limit" to 20, "total" to 0, "list" to emptyList<Any>()))
}
