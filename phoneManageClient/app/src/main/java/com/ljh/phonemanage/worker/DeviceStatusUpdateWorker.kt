package com.ljh.phonemanage.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ljh.phonemanage.manager.DeviceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 设备状态更新工作器
 * 用于定时更新设备在线状态
 */
@HiltWorker
class DeviceStatusUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val deviceManager: DeviceManager
) : CoroutineWorker(context, params) {
    
    private val TAG = "DeviceStatusUpdateWorker"
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "开始执行设备状态更新任务")
        return try {
            deviceManager.updateDeviceStatus()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "设备状态更新失败", e)
            Result.retry()
        }
    }
} 