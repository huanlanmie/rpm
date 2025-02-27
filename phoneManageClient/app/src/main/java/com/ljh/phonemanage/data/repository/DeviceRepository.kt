package com.ljh.phonemanage.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ljh.phonemanage.data.api.ApiClient
import com.ljh.phonemanage.data.model.RpmDevices
import com.ljh.phonemanage.util.DateAdapter
import com.ljh.phonemanage.util.NetworkDebugUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设备信息存储库
 */
@Singleton
class DeviceRepository @Inject constructor() {
    private val deviceApiService = ApiClient.deviceApiService
    private val TAG = "DeviceRepository"
    
    // 创建一个配置好的Gson实例，与ApiClient中的配置保持一致
    private val gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateAdapter())
        .setLenient()
        .create()
    
    /**
     * 获取设备信息
     */
    suspend fun getDeviceInfo(deviceToken: String): Result<RpmDevices?> = withContext(Dispatchers.IO) {
        try {
            // 尝试通过UUID直接获取设备信息
            val response = deviceApiService.getDeviceByUuid(deviceToken)
            
            Log.d(TAG, "查询设备API响应状态: ${response.code()} ${response.message()}")
            
            // 使用工具类打印响应体内容，帮助调试
            if (!response.isSuccessful && response.errorBody() != null) {
                NetworkDebugUtil.logResponseBody(response.errorBody())
            }
            
            if (response.isSuccessful) {
                val ajaxResult = response.body()
                Log.d(TAG, "API响应体: $ajaxResult")
                
                if (ajaxResult?.isSuccess == true && ajaxResult.data != null) {
                    // 后端返回的是RpmDevices对象，需要转换为我们的RpmDevices
                    try {
                        // 使用配置好的gson实例而不是新建一个
                        val jsonElement = gson.toJsonTree(ajaxResult.data)
                        val device = gson.fromJson(jsonElement, RpmDevices::class.java)
                        
                        Log.d(TAG, "查询到设备: $device")
                        Result.success(device)
                    } catch (e: Exception) {
                        Log.e(TAG, "设备数据解析异常", e)
                        Result.success(null)
                    }
                } else {
                    // 设备不存在
                    Log.d(TAG, "设备不存在: ${ajaxResult?.msg}")
                    Result.success(null)
                }
            } else {
                // 请求失败
                Log.e(TAG, "获取设备信息失败: ${response.code()} ${response.message()}")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取设备信息异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 添加设备信息
     */
    suspend fun addDevice(rpmDevices: RpmDevices): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, rpmDevices.toString())
            val response = deviceApiService.addDevice(rpmDevices)
            
            // 使用工具类打印响应体内容，帮助调试
            if (!response.isSuccessful && response.errorBody() != null) {
                NetworkDebugUtil.logResponseBody(response.errorBody())
            }
            
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Log.d(TAG, "添加设备信息成功")
                Result.success(true)
            } else {
                Log.e(TAG, "添加设备信息失败: ${response.code()} ${response.message()}")
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "添加设备信息异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 更新设备信息
     */
    suspend fun updateDevice(rpmDevices: RpmDevices): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = deviceApiService.updateDevice(rpmDevices)
            
            // 使用工具类打印响应体内容，帮助调试
            if (!response.isSuccessful && response.errorBody() != null) {
                NetworkDebugUtil.logResponseBody(response.errorBody())
            }
            
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Log.d(TAG, "更新设备信息成功")
                Result.success(true)
            } else {
                Log.e(TAG, "更新设备信息失败: ${response.code()} ${response.message()}")
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新设备信息异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 更新设备状态
     */
    suspend fun updateDeviceStatus(rpmDevices: RpmDevices): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 创建当前时间
            val currentTime = Date()
            
            // 打印当前时间，确认格式正确
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Log.d(TAG, "更新设备状态，当前时间: ${dateFormat.format(currentTime)}")
            
            val statusUpdateInfo = rpmDevices.copy(
                lastSeen = currentTime,
                updateTime = currentTime,
                status = 1L
            )
            Log.d(TAG, "更新对象数据，当前时间: ${statusUpdateInfo}")
            
            // 使用我们配置的gson打印完整的更新对象，确认时间格式
            Log.d(TAG, "更新设备状态请求体: ${gson.toJson(statusUpdateInfo)}")
            
            val response = deviceApiService.updateDeviceStatus(statusUpdateInfo)
            
            // 使用工具类打印响应体内容，帮助调试
            if (!response.isSuccessful && response.errorBody() != null) {
                NetworkDebugUtil.logResponseBody(response.errorBody())
            }
            
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Log.d(TAG, "更新设备状态成功")
                Result.success(true)
            } else {
                Log.e(TAG, "更新设备状态失败: ${response.code()} ${response.message()}")
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新设备状态异常", e)
            Result.failure(e)
        }
    }
} 