package com.ljh.phonemanage

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ljh.phonemanage.manager.DeviceManager
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
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
} 