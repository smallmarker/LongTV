package com.loong.longtv.ui.shared.section

enum class ContentSection(
    val id: String,
    val title: String,
    val siteKey: String,
    val loadingText: String,
    val emptyText: String
) {
    CourseEducation(
        id = "course_education",
        title = "课堂教育",
        siteKey = "课堂教育",
        loadingText = "正在加载课堂教育...",
        emptyText = "暂无课堂教育内容"
    ),
    MusicConcert(
        id = "music_concert",
        title = "音乐演唱会",
        siteKey = "音乐演唱会",
        loadingText = "正在加载音乐演唱会...",
        emptyText = "暂无音乐演唱会内容"
    ),
    AudioBook(
        id = "audio_book",
        title = "听书",
        siteKey = "听书",
        loadingText = "正在加载听书内容...",
        emptyText = "暂无听书内容"
    );

    companion object {
        val nonPopularSiteKeys: Set<String> = entries.map { it.siteKey }.toSet() + "少儿教育"

        fun fromId(id: String?): ContentSection? = entries.firstOrNull { it.id == id }
    }
}
