package com.ljh.phonemanage.data.api

import com.google.gson.annotations.SerializedName

/**
 * 后端表格数据响应模型
 * 对应后端TableDataInfo类
 */
data class TableDataInfo<T>(
    @SerializedName("total")
    val total: Int = 0,
    
    @SerializedName("rows")
    val rows: List<T> = emptyList(),
    
    @SerializedName("code")
    val code: Int = 0,
    
    @SerializedName("msg")
    val msg: String = ""
) {
    val isSuccess: Boolean
        get() = code == 0
} 