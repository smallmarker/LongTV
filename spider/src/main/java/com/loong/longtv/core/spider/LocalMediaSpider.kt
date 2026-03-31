package com.loong.longtv.core.spider

import android.os.Environment
import android.text.TextUtils
import com.github.catvod.crawler.Spider
import com.google.gson.Gson
import com.loong.longtv.data.bean.CategoryContent
import com.loong.longtv.data.bean.HomeContent
import com.loong.longtv.data.bean.HomeTabItem
import com.loong.longtv.data.bean.PlayerContent
import com.loong.longtv.data.bean.Video
import java.io.File
import java.util.HashMap
import java.util.Locale

class LocalMediaSpider : Spider() {

    private val gson = Gson()

    override fun homeContent(filter: Boolean): String {
        val tabs = rootDirs().map { HomeTabItem(it.absolutePath, it.name.ifBlank { "存储" }) }
        return gson.toJson(HomeContent(tabs = tabs, filters = emptyMap()))
    }

    override fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        val dir = File(tid)
        val items = dir.listFiles()
            ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.getDefault()) })
            ?.filter { !it.name.startsWith(".") }
            ?.filter { it.isDirectory || isMediaFile(it) }
            ?.map { fileToVideo(it) }
            ?: emptyList()
        return gson.toJson(
            CategoryContent(
                code = 1,
                pagecount = 1,
                total = items.size,
                page = 1,
                msg = "",
                limit = items.size,
                list = items
            )
        )
    }

    override fun detailContent(ids: List<String>): String {
        val target = File(ids.first())
        val episodes = when {
            target.isDirectory -> target.listFiles()
                ?.sortedBy { it.name.lowercase(Locale.getDefault()) }
                ?.filter { isMediaFile(it) }
                ?: emptyList()
            isMediaFile(target) -> listOf(target)
            else -> emptyList()
        }
        val vod = if (target.isDirectory) {
            baseVideo(target).copy(
                vod_play_from = "本地播放",
                vod_play_url = episodes.joinToString("#") { "${it.name}\$${it.absolutePath}" },
                vod_remarks = "${episodes.size} 个视频"
            )
        } else {
            baseVideo(target).copy(
                vod_play_from = "本地播放",
                vod_play_url = "${target.name}\$${target.absolutePath}"
            )
        }
        return gson.toJson(
            CategoryContent(
                code = 1,
                pagecount = 1,
                total = 1,
                page = 1,
                msg = "",
                limit = 1,
                list = listOf(vod)
            )
        )
    }

    override fun searchContent(key: String, quick: Boolean): String {
        val result = mutableListOf<Video>()
        rootDirs().forEach { walkAndSearch(it, key, result, 0) }
        return gson.toJson(
            CategoryContent(
                code = 1,
                pagecount = 1,
                total = result.size,
                page = 1,
                msg = "",
                limit = result.size,
                list = result
            )
        )
    }

    override fun playerContent(flag: String, id: String, vipFlags: MutableList<String>?): String {
        return gson.toJson(
            PlayerContent(
                parse = 0,
                url = File(id).toURI().toString(),
                playUrl = id
            )
        )
    }

    private fun rootDirs(): List<File> {
        val roots = mutableListOf<File>()
        roots += Environment.getExternalStorageDirectory()
        File("/storage").listFiles()
            ?.filter { it.isDirectory && it.name !in setOf("self", "emulated", "sdcard") }
            ?.let(roots::addAll)
        return roots.distinctBy { it.absolutePath }.filter { it.exists() && it.canRead() }
    }

    private fun walkAndSearch(dir: File, keyword: String, out: MutableList<Video>, depth: Int) {
        if (depth > 4 || out.size >= 60 || !dir.exists() || !dir.canRead()) return
        dir.listFiles()?.forEach { file ->
            if (out.size >= 60) return
            if (file.name.startsWith(".")) return@forEach
            if (file.isDirectory) {
                if (file.name.contains(keyword, true)) out += fileToVideo(file)
                walkAndSearch(file, keyword, out, depth + 1)
            } else if (isMediaFile(file) && file.name.contains(keyword, true)) {
                out += fileToVideo(file)
            }
        }
    }

    private fun fileToVideo(file: File): Video {
        return baseVideo(file).copy(
            vod_remarks = if (file.isDirectory) "文件夹" else formatSize(file.length()),
            vod_tag = if (file.isDirectory) "folder" else "file"
        )
    }

    private fun baseVideo(file: File): Video {
        return Video(
            vod_id = file.absolutePath,
            vod_name = file.name,
            vod_pic = "",
            vod_remarks = "",
            type_id = if (file.isDirectory) "folder" else "file",
            type_name = if (file.isDirectory) "文件夹" else "本地视频",
            vod_actor = "",
            vod_area = "",
            vod_content = file.absolutePath,
            vod_director = "",
            vod_lang = "",
            vod_play_from = "",
            vod_play_url = "",
            vod_state = "",
            vod_tag = "",
            vod_time = "",
            vod_year = ""
        )
    }

    private fun isMediaFile(file: File): Boolean {
        if (!file.isFile) return false
        val name = file.name.lowercase(Locale.getDefault())
        return MEDIA_EXTENSIONS.any { name.endsWith(it) }
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return ""
        val gb = 1024L * 1024L * 1024L
        val mb = 1024L * 1024L
        return if (size >= gb) String.format(Locale.getDefault(), "%.1f GB", size.toDouble() / gb)
        else String.format(Locale.getDefault(), "%.1f MB", size.toDouble() / mb)
    }

    companion object {
        private val MEDIA_EXTENSIONS = listOf(".mp4", ".mkv", ".avi", ".mov", ".m4v", ".flv", ".ts", ".mpg", ".mpeg", ".wmv")
    }
}
