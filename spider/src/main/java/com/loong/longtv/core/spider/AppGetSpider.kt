package com.loong.longtv.core.spider

import android.content.Context
import android.util.Base64
import com.github.catvod.crawler.Spider
import com.github.catvod.utils.Util
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.loong.longtv.data.bean.CategoryContent
import com.loong.longtv.data.bean.Filter
import com.loong.longtv.data.bean.FilterValue
import com.loong.longtv.data.bean.HomeContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.PlayerContent
import com.loong.longtv.data.bean.Video
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.HashMap
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

open class AppGetSpider : Spider() {

    protected val gson = Gson()
    protected val client = OkHttpClient()
    protected var config = AppGetConfig()

    override fun init(context: Context?, extend: String?) {
        config = defaultConfig().merge(resolveConfig(extend))
    }

    override fun homeContent(filter: Boolean): String {
        val data = getData(initPath())
        val classes = mutableListOf<HomeTabItem>()
        val filters = linkedMapOf<String, List<Filter>>()
        val typeList = data.getAsJsonArray("type_list") ?: data.getAsJsonArray("class") ?: return gson.toJson(
            HomeContent(emptyList(), emptyMap())
        )
        val filterNameMap = mapOf(
            "class" to "类型",
            "area" to "地区",
            "lang" to "语言",
            "year" to "年份",
            "letter" to "字母",
            "by" to "排序",
            "sort" to "排序"
        )
        typeList.forEach { itemElement ->
            val item = itemElement.asJsonObject
            val typeName = readString(item, "type_name", "name").ifBlank { return@forEach }
            if (typeName == "全部") return@forEach
            val typeId = readString(item, "type_id", "id").ifBlank { return@forEach }
            classes += HomeTabItem(type_id = typeId, type_name = typeName)

            val extendRaw = readString(item, "type_extend", "extend")
            val extendObj = runCatching { JsonParser.parseString(extendRaw).asJsonObject }.getOrNull()
            if (extendObj != null) {
                val siteFilters = mutableListOf<Filter>()
                filterNameMap.forEach { (key, label) ->
                    val raw = readString(extendObj, key).trim()
                    if (raw.isBlank()) return@forEach
                    val values = raw.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map { FilterValue(it, it) }
                    if (values.isNotEmpty()) siteFilters += Filter(key, label, values)
                }
                if (siteFilters.isNotEmpty()) filters[typeId] = siteFilters
            }
        }
        return gson.toJson(HomeContent(tabs = classes, filters = filters))
    }

    override fun homeVideoContent(): String {
        val data = getData(initPath())
        val banners = jsonArrayOrEmpty(data.getAsJsonArray("banner_list"))
        val items = mutableListOf<Video>()
        banners.forEach { item -> items += toVideo(item.asJsonObject) }
        if (items.isNotEmpty()) return gson.toJson(pageOf(items))
        val firstType = data.getAsJsonArray("type_list")
            ?.let { firstObject(it) }
            ?.let { readString(it, "type_id", "id") }
            .orEmpty()
        return categoryContent(firstType.ifBlank { "1" }, "1", false, hashMapOf())
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        val body = linkedMapOf(
            "type_id" to tid,
            "page" to pg,
            "area" to extend["area"].orDefault("全部"),
            "year" to extend["year"].orDefault("全部"),
            "sort" to extend["sort"].orDefault("最新"),
            "lang" to extend["lang"].orDefault("全部"),
            "class" to extend["class"].orDefault("全部")
        )
        val data = getData("/api.php/getappapi.index/typeFilterVodList", body)
        val items = mutableListOf<Video>()
        jsonArrayOrEmpty(data.getAsJsonArray("recommend_list") ?: data.getAsJsonArray("list")).forEach { element ->
            items += toVideo(element.asJsonObject)
        }
        val page = pg.toIntOrNull() ?: 1
        return gson.toJson(
            CategoryContent(
                code = 1,
                pagecount = 9999,
                total = 999999,
                page = page,
                msg = "",
                limit = items.size,
                list = items
            )
        )
    }

    override fun detailContent(ids: MutableList<String>?): String {
        val body = linkedMapOf("vod_id" to ids?.firstOrNull().orEmpty())
        val data = getData("/api.php/getappapi.index/vodDetail", body)
        val vod = (data.getAsJsonObject("vod") ?: data).deepCopy()
        val playNames = mutableListOf<String>()
        val playUrls = mutableListOf<String>()
        jsonArrayOrEmpty(data.getAsJsonArray("vod_play_list")).forEach { item ->
            val play = item.asJsonObject
            val playerInfo = play.getAsJsonObject("player_info") ?: JsonObject()
            val sourceName = readString(playerInfo, "show", "from", "name")
                .ifBlank { readString(play, "flag", "from") }
                .ifBlank { "线路${playNames.size + 1}" }
            val episodes = mutableListOf<String>()
            jsonArrayOrEmpty(play.getAsJsonArray("urls")).forEach { episode ->
                val urlObj = episode.asJsonObject.deepCopy()
                val url = readString(urlObj, "url", "play_url", "link")
                if (url.isBlank()) return@forEach
                playerInfo.entrySet().forEach { (key, value) ->
                    if (!urlObj.has(key)) urlObj.add(key, value)
                }
                val episodeName = readString(urlObj, "name", "title").ifBlank { "播放" }
                val encoded = encodePayload(urlObj.toString())
                episodes += "$episodeName$$encoded"
            }
            if (episodes.isNotEmpty()) {
                playNames += sourceName
                playUrls += episodes.joinToString("#")
            }
        }
        val video = toVideo(vod).copy(
            vod_play_from = playNames.joinToString("$$$"),
            vod_play_url = playUrls.joinToString("$$$")
        )
        return gson.toJson(pageOf(listOf(video)))
    }

    override fun searchContent(key: String?, quick: Boolean): String {
        return searchContent(key, quick, "1")
    }

    override fun searchContent(key: String?, quick: Boolean, pg: String?): String {
        val page = pg ?: "1"
        val body = linkedMapOf(
            "keywords" to key.orEmpty(),
            "type_id" to "0",
            "page" to page
        )
        val data = getData("/api.php/getappapi.index/searchList", body)
        val items = mutableListOf<Video>()
        jsonArrayOrEmpty(data.getAsJsonArray("search_list") ?: data.getAsJsonArray("list")).forEach { element ->
            items += toVideo(element.asJsonObject)
        }
        return gson.toJson(
            CategoryContent(
                code = 1,
                pagecount = 9999,
                total = 999999,
                page = page.toIntOrNull() ?: 1,
                msg = "",
                limit = items.size,
                list = items
            )
        )
    }

    override fun playerContent(flag: String?, id: String?, vipFlags: MutableList<String>?): String {
        val payload = decodePayload(id.orEmpty()).ifBlank { "{}" }
        val info = runCatching { JsonParser.parseString(payload).asJsonObject }.getOrNull() ?: JsonObject()
        val headers = playbackHeaders(readString(info, "user_agent"))
        var url = readString(info, "url", "play_url", "link")
        var parse = 1
        val parseApiUrl = readString(info, "parse_api_url", "parse_api", "parseApi")
        val parseFlag = readString(info, "parse")
        val token = readString(info, "token")

        MediaUrlHelper.extractDirectMediaUrl(url)?.let {
            return gson.toJson(PlayerContent(parse = 0, url = it, playUrl = it, header = headers))
        }

        runCatching {
            if (parseApiUrl.contains("?url=")) {
                val resolved = getJson(parseApiUrl, headers)
                findMediaUrl(resolved)?.let {
                    url = it
                    parse = 0
                }
            } else if (url.isNotBlank() && parseApiUrl.isNotBlank()) {
                val body = linkedMapOf(
                    "parse_api" to if (parseFlag.isNotBlank()) parseFlag else parseApiUrl.replace(url, ""),
                    "url" to URLEncoder.encode(aesEncrypt(url), "UTF-8"),
                    "token" to token
                )
                val parsed = getData("/api.php/getappapi.index/vodParse", body)
                val jsonText = readString(parsed, "json", "data")
                findMediaUrl(runCatching { JsonParser.parseString(jsonText) }.getOrNull())
                    ?.let {
                        url = it
                        parse = 0
                    }
            }
        }

        val direct = MediaUrlHelper.extractDirectMediaUrl(url)
        if (direct != null) {
            return gson.toJson(PlayerContent(parse = 0, url = direct, playUrl = direct, header = headers))
        }

        return gson.toJson(
            PlayerContent(
                parse = parse,
                url = url,
                playUrl = url,
                header = headers
            )
        )
    }

    protected open fun initPath(): String {
        val version = config.version.ifBlank { "119" }
        return "/api.php/getappapi.index/initV$version"
    }

    protected open fun defaultConfig(): AppGetConfig = AppGetConfig()

    protected open fun playbackHeaders(userAgent: String): Map<String, String> {
        val finalUserAgent = userAgent.ifBlank {
            config.ua.ifBlank { "okhttp/3.14.9" }
        }
        return buildMap {
            put("User-Agent", finalUserAgent)
            if (config.host.isNotBlank()) {
                put("Referer", config.host)
                put("Origin", config.host)
            }
        }
    }

    protected fun getData(path: String, body: Map<String, String> = emptyMap()): JsonObject {
        val encryptedPayload = post(path, body)
        val dataElement = encryptedPayload.get("data")
        val decrypted = when {
            dataElement == null || dataElement.isJsonNull -> "{}"
            dataElement.isJsonObject -> return dataElement.asJsonObject
            else -> aesDecrypt(dataElement.asString)
        }
        return runCatching { JsonParser.parseString(decrypted).asJsonObject }.getOrElse { JsonObject() }
    }

    protected fun getJson(url: String, headers: Map<String, String> = playbackHeaders("")): JsonElement {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }
        client.newCall(requestBuilder.build()).execute().use { response ->
            return JsonParser.parseString(response.body?.string().orEmpty())
        }
    }

    protected fun post(path: String, body: Map<String, String>): JsonObject {
        val timestamp = (System.currentTimeMillis() / 1000L).toString()
        val formBuilder = FormBody.Builder()
        body.forEach { (key, value) -> formBuilder.add(key, value) }
        val requestBuilder = Request.Builder()
            .url(config.host + path)
            .post(formBuilder.build())
        requestHeaders(timestamp).forEach { (key, value) -> requestBuilder.header(key, value) }
        client.newCall(requestBuilder.build()).execute().use { response ->
            return JsonParser.parseString(response.body?.string().orEmpty()).asJsonObject
        }
    }

    protected open fun requestHeaders(timestamp: String): Map<String, String> {
        val deviceSeed = config.deviceId.ifBlank { timestamp }
        return buildMap {
            put("Referer", config.host)
            put("User-Agent", config.ua.ifBlank { "okhttp/3.14.9" })
            put("app-version-code", config.version.ifBlank { "300" })
            put("app-ui-mode", "light")
            put("app-api-verify-time", timestamp)
            put("app-user-device-id", Util.md5(deviceSeed))
            put("app-api-verify-sign", aesEncrypt(timestamp))
            put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }
    }

    protected fun aesEncrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(), ivSpec())
        return Base64.encodeToString(cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
    }

    protected fun aesDecrypt(value: String): String {
        if (value.isBlank()) return value
        return runCatching {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), ivSpec())
            String(cipher.doFinal(Base64.decode(value, Base64.DEFAULT)), StandardCharsets.UTF_8)
        }.getOrElse { value }
    }

    protected fun encodePayload(value: String): String {
        return Base64.encodeToString(value.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }

    protected fun decodePayload(value: String): String {
        if (value.isBlank()) return value
        return runCatching {
            String(Base64.decode(value, Base64.DEFAULT), StandardCharsets.UTF_8)
        }.getOrElse { value }
    }

    protected fun toVideo(obj: JsonObject): Video {
        return Video(
            vod_id = readString(obj, "vod_id", "id"),
            vod_name = readString(obj, "vod_name", "name", "title"),
            vod_pic = normalizeUrl(readString(obj, "vod_pic", "pic", "cover")),
            vod_remarks = readString(obj, "vod_remarks", "remarks"),
            type_id = readString(obj, "type_id", "tid"),
            type_name = readString(obj, "type_name", "vod_class", "type"),
            vod_actor = readString(obj, "vod_actor", "actor"),
            vod_area = readString(obj, "vod_area", "area"),
            vod_content = readString(obj, "vod_content", "content"),
            vod_director = readString(obj, "vod_director", "director"),
            vod_lang = readString(obj, "vod_lang", "lang"),
            vod_play_from = readString(obj, "vod_play_from"),
            vod_play_url = readString(obj, "vod_play_url"),
            vod_state = readString(obj, "vod_state", "state"),
            vod_tag = readString(obj, "vod_tag", "tag"),
            vod_time = readString(obj, "vod_time", "time"),
            vod_year = readString(obj, "vod_year", "year")
        )
    }

    protected fun pageOf(items: List<Video>): CategoryContent {
        return CategoryContent(
            code = 1,
            pagecount = 1,
            total = items.size,
            page = 1,
            msg = "",
            limit = items.size,
            list = items
        )
    }

    protected fun normalizeUrl(url: String): String {
        if (url.isBlank()) return url
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) return url
        return config.host.trimEnd('/') + "/" + url.trimStart('/')
    }

    protected fun readString(obj: JsonObject, vararg names: String): String {
        names.forEach { name ->
            val value = obj.get(name) ?: return@forEach
            if (!value.isJsonNull) return value.asString.orEmpty()
        }
        return ""
    }

    private fun secretKey(): SecretKeySpec {
        val key = normalizedSecret(config.dataKey)
        return SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "AES")
    }

    private fun ivSpec(): IvParameterSpec {
        val iv = normalizedSecret(config.dataIv.ifBlank { config.dataKey })
        return IvParameterSpec(iv.toByteArray(StandardCharsets.UTF_8))
    }

    private fun normalizedSecret(raw: String): String {
        return when {
            raw.length == 16 -> raw
            raw.length > 16 -> raw.substring(0, 16)
            else -> raw.padEnd(16, '0')
        }
    }

    private fun resolveConfig(extend: String?): AppGetConfig {
        if (extend.isNullOrBlank()) return AppGetConfig()
        val cleaned = extend.trim()
        val element = runCatching { JsonParser.parseString(cleaned) }.getOrNull()
        if (element == null) {
            return AppGetConfig(host = cleaned.trim('"'))
        }
        if (element.isJsonPrimitive) {
            return AppGetConfig(host = element.asString)
        }
        val obj = element.asJsonObject
        val host = readString(obj, "url", "host").ifBlank {
            val site = readString(obj, "site")
            resolveSiteHost(site)
        }
        return AppGetConfig(
            host = host.trimEnd('/'),
            dataKey = readString(obj, "dataKey", "key"),
            dataIv = readString(obj, "dataIv", "iv"),
            version = readString(obj, "version"),
            deviceId = readString(obj, "deviceId"),
            siteApi = readString(obj, "siteApi", "api"),
            ua = readString(obj, "ua", "userAgent", "user_agent")
        )
    }

    protected open fun resolveSiteHost(site: String): String {
        if (site.isBlank()) return ""
        if (site.endsWith(".txt") || site.endsWith(".json")) {
            val text = runCatching {
                val request = Request.Builder().url(site).get().build()
                client.newCall(request).execute().use { response -> response.body?.string().orEmpty() }
            }.getOrDefault("")
            if (site.endsWith(".json")) {
                val json = runCatching { JsonParser.parseString(text) }.getOrNull()
                if (json != null) {
                    findSiteHost(json)?.let { return it.trimEnd('/') }
                }
            }
            return text.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
                .orEmpty()
                .trimEnd('/')
        }
        return site.trimEnd('/')
    }

    protected open fun findSiteHost(element: JsonElement?): String? {
        if (element == null || element.isJsonNull) return null
        if (element.isJsonPrimitive) {
            val text = element.asString.trim()
            return text.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        }
        if (element.isJsonArray) {
            element.asJsonArray.forEach { item ->
                findSiteHost(item)?.let { return it }
            }
            return null
        }
        val obj = element.asJsonObject
        listOf("url", "host", "site", "api", "siteUrl", "SiteUrl").forEach { key ->
            findSiteHost(obj.get(key))?.let { return it }
        }
        return null
    }

    private fun findMediaUrl(element: JsonElement?): String? {
        if (element == null || element.isJsonNull) return null
        if (element.isJsonPrimitive) {
            return MediaUrlHelper.extractDirectMediaUrl(URLDecoder.decode(element.asString, "UTF-8"))
        }
        if (element.isJsonArray) {
            element.asJsonArray.forEach { item ->
                findMediaUrl(item)?.let { return it }
            }
            return null
        }
        val obj = element.asJsonObject
        listOf("url", "playUrl", "link").forEach { key ->
            findMediaUrl(obj.get(key))?.let { return it }
        }
        return findMediaUrl(obj.get("data"))
    }
}

data class AppGetConfig(
    val host: String = "",
    val dataKey: String = "",
    val dataIv: String = "",
    val version: String = "",
    val deviceId: String = "",
    val siteApi: String = "",
    val ua: String = ""
)

private fun AppGetConfig.merge(override: AppGetConfig): AppGetConfig {
    return AppGetConfig(
        host = override.host.ifBlank { host },
        dataKey = override.dataKey.ifBlank { dataKey },
        dataIv = override.dataIv.ifBlank { dataIv },
        version = override.version.ifBlank { version },
        deviceId = override.deviceId.ifBlank { deviceId },
        siteApi = override.siteApi.ifBlank { siteApi },
        ua = override.ua.ifBlank { ua }
    )
}

private fun String?.orDefault(default: String): String = this?.takeIf { it.isNotBlank() } ?: default

private fun jsonArrayOrEmpty(array: JsonArray?): JsonArray = array ?: JsonArray()

private fun firstObject(array: JsonArray): JsonObject? {
    if (array.size() == 0) return null
    val first = array[0]
    return if (first.isJsonObject) first.asJsonObject else null
}
