package com.loong.longtv.data.cache

import android.content.Context
import java.io.File

/**
 * App 可清理磁盘缓存的统一预算。
 *
 * Room 数据库位于 filesDir，不属于这里的缓存预算；播放记录、收藏和搜索记录也
 * 因此不会被清理。为预留 Spider 临时文件的空间，已知缓存的固定上限合计为 68MB。
 */
object CacheStorage {
    const val MAX_TOTAL_CACHE_BYTES = 80L * 1024L * 1024L
    const val MAX_VIDEO_CACHE_BYTES = 48L * 1024L * 1024L
    const val MAX_IMAGE_DISK_CACHE_BYTES = 16L * 1024L * 1024L
    const val MAX_DOH_CACHE_BYTES = 4L * 1024L * 1024L

    fun cacheSize(context: Context): Long =
        directorySize(context.cacheDir) + (context.externalCacheDir?.let(::directorySize) ?: 0L)

    /**
     * 处理旧版本遗留缓存和 Spider 的临时文件。按最久未使用文件优先回收，直到
     * 内部与外部缓存总量不大于预算；不会触碰 filesDir 中的用户数据和 Room 数据库。
     */
    fun enforceBudget(context: Context) {
        // Coil 已负责图片磁盘缓存；移除旧版本遗留的 OkHttp 图片响应缓存，避免同一
        // 张海报被保存两份。
        File(context.cacheDir, "image_http_cache").deleteRecursively()
        val roots = listOfNotNull(context.cacheDir, context.externalCacheDir)
        var total = roots.sumOf(::directorySize)
        if (total <= MAX_TOTAL_CACHE_BYTES) return

        roots.asSequence()
            .filter(File::exists)
            .flatMap { it.walkTopDown().asSequence() }
            .filter { it.isFile }
            .sortedBy { it.lastModified() }
            .forEach { file ->
                if (total <= MAX_TOTAL_CACHE_BYTES) return
                val size = file.length()
                if (file.delete()) total -= size
            }
    }

    private fun directorySize(directory: File): Long =
        if (!directory.exists()) 0L else directory.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
