package com.ljh.phonemanage.util

import android.util.Log
import okhttp3.ResponseBody
import java.nio.charset.StandardCharsets

/**
 * 网络调试工具类
 */
object NetworkDebugUtil {
    private const val TAG = "NetworkDebug"
    
    /**
     * 打印响应体内容
     */
    fun logResponseBody(responseBody: ResponseBody?) {
        if (responseBody == null) {
            Log.d(TAG, "响应体为空")
            return
        }
        
        try {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer
            val charset = StandardCharsets.UTF_8
            val bodyString = buffer.clone().readString(charset)
            Log.d(TAG, "响应体内容:\n$bodyString")
        } catch (e: Exception) {
            Log.e(TAG, "解析响应体异常", e)
        }
    }
} 