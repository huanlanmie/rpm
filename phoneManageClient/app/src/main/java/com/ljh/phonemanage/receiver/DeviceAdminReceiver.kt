package com.ljh.phonemanage.receiver

import android.app.admin.DeviceAdminReceiver as AndroidDeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DeviceAdminReceiver : AndroidDeviceAdminReceiver() {
    private val TAG = "DeviceAdminReceiver"
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "设备管理员权限已启用")
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "设备管理员权限已禁用")
    }
} 