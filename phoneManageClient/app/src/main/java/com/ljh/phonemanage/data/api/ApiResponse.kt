package com.ljh.phonemanage.data.api

import com.google.gson.annotations.SerializedName

/**
 * API通用响应模型
 */
data class ApiResponse<T>(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("msg")
    val message: String,
    
    @SerializedName("data")
    val data: T?
) {
    companion object {
        const val SUCCESS_CODE = 0
    }
    
    val isSuccess: Boolean
        get() = code == SUCCESS_CODE
} 