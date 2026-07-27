package com.loong.longtv.engine.config

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.loong.longtv.engine.source.model.DoHConfig
import com.loong.longtv.engine.source.model.LiveConfig
import com.loong.longtv.engine.source.model.ParseConfig
import com.loong.longtv.engine.source.model.SiteConfig
import com.loong.longtv.engine.source.model.TvBoxConfig
import java.io.InputStream

/**
 * 配置解析器 - 解析TVBox格式的JSON配置
 *
 * 负责：
 * 1. 从内置asset读取JSON
 * 2. 解析为TvBoxConfig对象
 * 3. 处理相对路径（如 ./spider.jar → 绝对路径）
 */
class ConfigParser(private val gson: Gson = Gson()) {

    /**
     * 从assets读取并解析配置
     */
    fun parseFromAsset(context: Context, assetPath: String): TvBoxConfig {
        val json = context.assets.open("$assetPath/api.json").bufferedReader().use { it.readText() }
        return parseJson(json, resolveBase = "asset://$assetPath")
    }

    /**
     * 从输入流解析
     */
    fun parseFromStream(stream: InputStream, resolveBase: String = ""): TvBoxConfig {
        val json = stream.bufferedReader().use { it.readText() }
        return parseJson(json, resolveBase = resolveBase)
    }

    private fun parseJson(json: String, resolveBase: String): TvBoxConfig {
        val root = runCatching { JsonParser.parseString(json).asJsonObject }
            .getOrNull() ?: return TvBoxConfig()

        val spider = root.stringOrEmpty("spider").resolveRelativePath(resolveBase)
        val wallpaper = root.stringOrEmpty("wallpaper")

        val sites = root.getAsJsonArray("sites")?.mapNotNull { element ->
            runCatching { parseSite(element.asJsonObject, resolveBase) }.getOrNull()
        } ?: emptyList()

        val doh = root.getAsJsonArray("doh")?.mapNotNull { element ->
            runCatching {
                val obj = element.asJsonObject
                DoHConfig(
                    name = obj.stringOrEmpty("name"),
                    url = obj.stringOrEmpty("url"),
                    ips = obj.getAsJsonArray("ips")?.map { it.asString } ?: emptyList()
                )
            }.getOrNull()
        } ?: emptyList()

        val lives = root.getAsJsonArray("lives")?.mapNotNull { element ->
            runCatching {
                val obj = element.asJsonObject
                LiveConfig(
                    name = obj.stringOrEmpty("name"),
                    type = obj.intOrDefault("type", 0),
                    url = obj.stringOrEmpty("url").resolveRelativePath(resolveBase),
                    playerType = obj.intOrDefault("playerType", 0),
                    epg = obj.stringOrNull("epg"),
                    logo = obj.stringOrNull("logo"),
                    ua = obj.stringOrNull("ua"),
                    timeout = obj.intOrDefault("timeout", 10)
                )
            }.getOrNull()
        } ?: emptyList()

        val parses = root.getAsJsonArray("parses")?.mapNotNull { element ->
            runCatching {
                val obj = element.asJsonObject
                ParseConfig(
                    name = obj.stringOrEmpty("name"),
                    type = obj.intOrDefault("type", 0),
                    url = obj.stringOrEmpty("url"),
                    ext = obj.get("ext")
                )
            }.getOrNull()
        } ?: emptyList()

        val flags = root.getAsJsonArray("flags")?.map { it.asString } ?: emptyList()

        return TvBoxConfig(
            spider = spider,
            wallpaper = wallpaper,
            sites = sites,
            doh = doh,
            lives = lives,
            parses = parses,
            flags = flags
        )
    }

    private fun parseSite(obj: JsonObject, resolveBase: String): SiteConfig {
        val ext = obj.get("ext")?.let { resolveExtPaths(it, resolveBase) }
        return SiteConfig(
            key = obj.stringOrEmpty("key"),
            name = obj.stringOrEmpty("name"),
            type = obj.intOrDefault("type", 3),
            api = obj.stringOrEmpty("api"),
            searchable = obj.intOrDefault("searchable", 0),
            quickSearch = obj.intOrDefault("quickSearch", 0),
            changeable = obj.intOrDefault("changeable", 0),
            ext = ext,
            jar = obj.stringOrNull("jar")?.resolveRelativePath(resolveBase),
            playerType = obj.intOrDefault("playerType", 0),
            timeout = obj.intOrDefault("timeout", 15)
        )
    }

    private fun resolveExtPaths(element: com.google.gson.JsonElement, resolveBase: String): com.google.gson.JsonElement {
        if (element.isJsonPrimitive) {
            val prim = element.asJsonPrimitive
            if (prim.isString) {
                return JsonPrimitive(prim.asString.resolveRelativePath(resolveBase))
            }
        }
        return element
    }

    private fun String.resolveRelativePath(base: String): String {
        if (!startsWith("./")) return this
        val relativePart = removePrefix("./")
        return when {
            base.startsWith("asset://") -> "${base}/$relativePart"
            base.startsWith("http://") || base.startsWith("https://") -> {
                val baseDir = base.substringBeforeLast("/")
                "$baseDir/$relativePart"
            }
            base.isNotBlank() -> "$base/$relativePart"
            else -> this
        }
    }

    private fun JsonObject.stringOrEmpty(key: String): String =
        get(key)?.takeIf { !it.isJsonNull }?.asString.orEmpty()

    private fun JsonObject.stringOrNull(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.asString

    private fun JsonObject.intOrDefault(key: String, default: Int): Int {
        val value = get(key)?.takeIf { !it.isJsonNull } ?: return default
        return runCatching { value.asInt }
            .recoverCatching { value.asString.toInt() }
            .getOrDefault(default)
    }
}
