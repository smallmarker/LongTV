package com.loong.longtv.utils.extensions

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * @author zl
 * @Date 2025/3/12
 **/
fun File.md5(): String {
    val digest = MessageDigest.getInstance("MD5")
    FileInputStream(this).use { fis ->
        val buffer = ByteArray(4096)
        var read: Int
        while (fis.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
