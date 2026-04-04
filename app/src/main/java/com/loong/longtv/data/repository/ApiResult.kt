package com.loong.longtv.data.repository

/**
 * @description: 网络请求结果
 * @author zl
 * @Date 2025/10/9
 **/
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String?, val exception: Exception? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}
