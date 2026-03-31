package com.loong.longtv.utils

import com.loong.longtv.App
import java.io.DataInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Random

/**
 * @author zl
 * @Date 2025/10/13
 **/
object UA {

    /**
     * 随机获取一个UA
     */
    fun random(): String {
        return try {
            val fis = App.instance.assets.open("ua.db")
            val dis = DataInputStream(fis)
            val len = dis.readInt()
            val random = Random().nextInt(len)
            dis.skipBytes(random * 4)
            val offset = dis.readInt()
            dis.skipBytes((len - 1 - random) * 4 + offset)
            val s = dis.readUTF()
            s
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36"
        } catch (e: IOException) {
            e.printStackTrace()
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36"
        }
    }
}