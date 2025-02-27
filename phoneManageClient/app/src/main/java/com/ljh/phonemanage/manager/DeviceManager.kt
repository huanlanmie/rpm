package com.ljh.phonemanage.manager

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ljh.phonemanage.data.model.RpmDevices
import com.ljh.phonemanage.data.repository.DeviceRepository
import com.ljh.phonemanage.worker.DeviceStatusUpdateWorker
import com.ljh.phonemanage.util.DeviceIdUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设备管理器
 * 负责设备信息的注册、更新和定时上报
 */
@Singleton
class DeviceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository,
    private val screenManager: ScreenManager
) {
    private val TAG = "DeviceManager"
    private val prefs: SharedPreferences = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // 设备信息状态流
    private val _deviceInfo = MutableStateFlow<RpmDevices?>(null)
    val deviceInfo: StateFlow<RpmDevices?> = _deviceInfo
    
    /**
     * 初始化设备信息
     */
    fun initDevice() {
        Log.d(TAG, "初始化设备信息")
        
        // 从本地获取设备Token
        val deviceToken = getOrCreateDeviceToken()
        
        // 获取设备信息
        coroutineScope.launch {
            try {
                // 从服务器获取设备信息
                val result = deviceRepository.getDeviceInfo(deviceToken)
                
                if (result.isSuccess) {
                    val existingDevice = result.getOrNull()
                    
                    if (existingDevice != null) {
                        // 设备已存在，更新设备状态
                        Log.d(TAG, "设备已存在，更新状态")
                        _deviceInfo.value = existingDevice
                        // 更新设备状态
                        updateDeviceStatus()
                    } else {
                        // 设备不存在，创建新设备
                        Log.d(TAG, "设备不存在，创建新设备")
                        val newDeviceInfo = createDeviceInfo(deviceToken)
                        val addResult = deviceRepository.addDevice(newDeviceInfo)
                        
                        if (addResult.isSuccess && addResult.getOrNull() == true) {
                            // 添加成功，获取新添加的设备
                            val refreshResult = deviceRepository.getDeviceInfo(deviceToken)
                            if (refreshResult.isSuccess) {
                                _deviceInfo.value = refreshResult.getOrNull() ?: newDeviceInfo
                            } else {
                                _deviceInfo.value = newDeviceInfo
                            }
                            Log.d(TAG, "新设备添加成功")
                        } else {
                            // 添加失败，使用本地设备信息
                            _deviceInfo.value = newDeviceInfo
                            Log.e(TAG, "新设备添加失败")
                        }
                    }
                    
                    // 设置定时任务，定期更新设备状态
                    scheduleStatusUpdates()
                } else {
                    // 获取设备信息失败，使用本地设备信息
                    val currentDeviceInfo = createDeviceInfo(deviceToken)
                    _deviceInfo.value = currentDeviceInfo
                    Log.e(TAG, "获取设备信息失败，使用本地设备信息")
                }
            } catch (e: Exception) {
                // 异常处理，使用本地设备信息
                val currentDeviceInfo = createDeviceInfo(deviceToken)
                _deviceInfo.value = currentDeviceInfo
                Log.e(TAG, "初始化设备异常", e)
            }
        }
    }
    
    /**
     * 获取已有设备Token或创建新的Token
     */
    private fun getOrCreateDeviceToken(): String {
        var deviceToken = prefs.getString("device_token", null)
        
        if (deviceToken == null) {
            // 使用更可靠的设备ID生成方式
            deviceToken = DeviceIdUtil.getDeviceId(context)
            prefs.edit().putString("device_token", deviceToken).apply()
            Log.d(TAG, "创建新的设备Token: $deviceToken")
        } else {
            Log.d(TAG, "使用已存在的设备Token: $deviceToken")
        }
        
        return deviceToken
    }
    
    /**
     * 获取设备ID - 已不再使用
     * 保留此方法仅用于参考
     */
    @Deprecated("使用DeviceIdUtil.getDeviceId()替代", ReplaceWith("DeviceIdUtil.getDeviceId(context)"))
    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    
    /**
     * 创建设备信息对象
     */
    private fun createDeviceInfo(deviceToken: String): RpmDevices {
        // 使用PackageManager获取应用版本
        val appVersion = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0" // 默认版本号
        }
        
        return RpmDevices(
            deviceToken = deviceToken,
            deviceName = Build.MODEL,
            deviceStatus = if (screenManager.lockState.value) 1 else 0,
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = appVersion,
            status = 1L,
            lastSeen = Date()
        )
    }
    
    /**
     * 更新设备锁定状态
     */
    fun updateLockStatus(isLocked: Boolean) {
        val currentInfo = _deviceInfo.value ?: return
        
        coroutineScope.launch {
            val updatedInfo = currentInfo.copy(
                deviceStatus = if (isLocked) 1 else 0,
                lastSeen = Date()
            )
            
            deviceRepository.updateDevice(updatedInfo)
            _deviceInfo.value = updatedInfo
        }
    }
    
    /**
     * 安排定时更新设备状态
     */
    private fun scheduleStatusUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val updateRequest = PeriodicWorkRequestBuilder<DeviceStatusUpdateWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "device_status_update",
            ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )
        
        Log.d(TAG, "已安排定时更新设备状态任务")
    }
    
    /**
     * 更新设备在线状态
     */
    fun updateDeviceStatus() {
        val currentInfo = _deviceInfo.value ?: return
        
        coroutineScope.launch {
            try {
                val updatedInfo = currentInfo.copy(
                    status = 1L,
                    lastSeen = Date(),
                    deviceStatus = if (screenManager.lockState.value) 1 else 0
                )
                
                val result = deviceRepository.updateDeviceStatus(updatedInfo)
                if (result.isSuccess && result.getOrNull() == true) {
                    _deviceInfo.value = updatedInfo
                    Log.d(TAG, "设备状态更新成功")
                } else {
                    Log.e(TAG, "设备状态更新失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新设备状态异常", e)
            }
        }
    }
} 