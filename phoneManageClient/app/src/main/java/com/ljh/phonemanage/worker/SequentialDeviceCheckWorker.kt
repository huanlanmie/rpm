package com.ljh.phonemanage.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ljh.phonemanage.service.DeviceStatusCheckService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 负责启动设备状态检查服务的Worker
 * 仅用于在应用启动或重启时确保服务能启动
 */
@HiltWorker
class SequentialDeviceCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    private val TAG = "DeviceCheckWorker"
    
    companion object {
        private const val UNIQUE_WORK_NAME = "device_status_check_worker"
        
        /**
         * 安排Worker执行以启动服务
         */
        fun enqueue(context: Context) {
            // 创建一个一次性任务，立即启动服务
            val immediate = OneTimeWorkRequestBuilder<SequentialDeviceCheckWorker>()
                .build()
            
            // 执行立即任务
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    immediate
                )
            
            Log.d("DeviceCheckWorker", "设备状态检查服务启动Worker已安排")
        }
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "开始执行Worker，确保设备状态检查服务运行")
        
        return try {
            withContext(Dispatchers.Main) {
                // 启动设备状态检查服务
                DeviceStatusCheckService.startService(applicationContext)
            }
            
            // 返回成功结果
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "启动服务失败", e)
            Result.retry()
        }
    }
} 