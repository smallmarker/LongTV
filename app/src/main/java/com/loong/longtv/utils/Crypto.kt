package com.loong.longtv.utils

import android.util.Base64
import java.util.regex.Pattern

/**
 * @author zl
 * @Date 2025/10/9
 **/
object Crypto {

    fun decodeContent(content: String): String {
        var result = content
        val pattern = Pattern.compile("[A-Za-z0]{8}\\*\\*")
        val matcher = pattern.matcher(content)
        if (matcher.find()) {
            result = content.substring(content.indexOf(matcher.group()) + 10)
            result = String(Base64.decode(result, Base64.DEFAULT))
        }
        return result
    }
}