package com.ljh.phonemanage

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ljh.phonemanage.manager.DeviceManager
import com.ljh.phonemanage.service.DeviceStatusCheckService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PhoneManageApplication : Application(), Configuration.Provider {
    private val TAG = "PhoneManageApplication"
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var deviceManager: DeviceManager
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "应用程序启动")
        
        // 初始化设备信息
        deviceManager.initDevice()
        
        // 直接启动设备状态检查服务
        startDeviceStatusCheckService()
    }
    
    private fun startDeviceStatusCheckService() {
        val intent = Intent(this, DeviceStatusCheckService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        Log.d(TAG, "已启动设备状态检查服务")
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
} 