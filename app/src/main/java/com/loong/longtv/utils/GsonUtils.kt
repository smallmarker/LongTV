package com.loong.longtv.utils

import com.google.gson.Gson
import org.json.JSONObject
import java.lang.reflect.Type

/**
 * @author zl
 * @Date 2025/3/11
 **/
object GsonUtils {

    val gson: Gson = Gson()

    // 将 JSON 字符串转换为指定类型对象的扩展函数
    inline fun <reified T> String.fromJson(): T {
        return gson.fromJson(this, T::class.java)
    }

    // 将对象转换为 JSON 字符串的扩展函数
    fun <T> T.toJson(): String {
        return gson.toJson(this)
    }

    // 将 JSON 字符串转换为指定类型对象的扩展函数，支持泛型类型
    fun <T> String.fromJson(type: Type): T {
        return gson.fromJson(this, type)
    }

    /**
     * 判断字符串是否为 JSON 格式
     */
    fun String.isJson(): Boolean {
        return try {
            JSONObject(this)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
