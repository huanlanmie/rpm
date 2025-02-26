package com.ljh.phonemanage.data.api

import com.google.gson.annotations.SerializedName

/**
 * 后端通用响应结果
 * 对应后端AjaxResult类
 */
data class AjaxResult(
    @SerializedName("code")
    val code: Int = 0,
    
    @SerializedName("msg")
    val msg: String = "",
    
    @SerializedName("data")
    val data: Any? = null
) {
    companion object {
        const val SUCCESS_CODE = 0
    }
    
    val isSuccess: Boolean
        get() = code == SUCCESS_CODE
} 