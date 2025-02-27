package com.ljh.phonemanage.data.api

import com.google.gson.annotations.SerializedName

/**
 * API响应通用封装类
 */
data class ApiResponse<T>(
    // 状态码，0表示成功
    val code: Int = 0,
    // 消息
    val msg: String? = null,
    // 数据
    val data: T? = null
) {
    companion object {
        const val SUCCESS_CODE = 0
    }
    
    val isSuccess: Boolean
        get() = code == SUCCESS_CODE
} 