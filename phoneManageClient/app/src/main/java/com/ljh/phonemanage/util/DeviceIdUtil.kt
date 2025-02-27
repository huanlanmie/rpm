package com.ljh.phonemanage.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * 设备ID工具类
 * 提供多种备份策略确保设备ID的持久性
 */
object DeviceIdUtil {
    private const val TAG = "DeviceIdUtil"
    private const val PREFS_DEVICE_ID = "device_prefs"
    private const val KEY_DEVICE_ID = "persistent_device_id"
    private const val BACKUP_FILENAME = "device_id.backup"
    
    /**
     * 获取或创建设备唯一标识符
     * 使用多重备份确保ID的持久性
     */
    fun getDeviceId(context: Context): String {
        // 1. 尝试从SharedPreferences读取
        val prefs = context.getSharedPreferences(PREFS_DEVICE_ID, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        // 2. 如果SharedPreferences中没有，尝试从备份文件读取
        if (deviceId == null) {
            deviceId = readFromBackupFile(context)
        }
        
        // 3. 如果都没有，生成一个基于设备硬件特征的ID
        if (deviceId == null) {
            deviceId = generateHardwareBasedId()
            
            // 存储到SharedPreferences
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
            
            // 存储到备份文件
            writeToBackupFile(context, deviceId)
            
            Log.d(TAG, "生成了新的设备ID: $deviceId")
        } else {
            Log.d(TAG, "使用已存在的设备ID: $deviceId")
        }
        
        return deviceId
    }
    
    /**
     * 生成基于设备硬件特征的唯一ID
     */
    private fun generateHardwareBasedId(): String {
        // 收集设备特征形成一个相对稳定的签名
        val deviceSignature = StringBuilder()
            .append(Build.BOARD)
            .append(Build.BRAND)
            .append(Build.DEVICE)
            .append(Build.HARDWARE)
            .append(Build.MANUFACTURER)
            .append(Build.MODEL)
            .append(Build.PRODUCT)
            .append(Build.DISPLAY)
            .append(Build.HOST)
            .toString()
        
        // 获取ANDROID_ID作为辅助
        val androidId = Settings.Secure.ANDROID_ID
        
        // 使用这些信息生成UUID
        val combinedInfo = "$deviceSignature:$androidId"
        val uuid = UUID.nameUUIDFromBytes(combinedInfo.toByteArray())
        
        return uuid.toString()
    }
    
    /**
     * 从备份文件读取设备ID
     */
    private fun readFromBackupFile(context: Context): String? {
        val backupFile = File(context.filesDir, BACKUP_FILENAME)
        if (!backupFile.exists()) {
            return null
        }
        
        return try {
            backupFile.readText()
        } catch (e: IOException) {
            Log.e(TAG, "读取备份设备ID失败", e)
            null
        }
    }
    
    /**
     * 将设备ID写入备份文件
     */
    private fun writeToBackupFile(context: Context, deviceId: String) {
        try {
            val backupFile = File(context.filesDir, BACKUP_FILENAME)
            FileOutputStream(backupFile).use { fos ->
                fos.write(deviceId.toByteArray())
            }
            Log.d(TAG, "设备ID已写入备份文件")
        } catch (e: IOException) {
            Log.e(TAG, "备份设备ID失败", e)
        }
    }
} 