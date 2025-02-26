package com.ljh.phonemanage.manager

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ljh.phonemanage.service.LockScreenService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ScreenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "ScreenManager"
    
    // 屏幕状态
    private val _lockState = MutableStateFlow(false)
    val lockState: StateFlow<Boolean> = _lockState
    
    // 当前密码
    private var currentPassword: String = "123456" // 测试时使用固定密码
    
    /**
     * 锁定设备屏幕
     */
    fun lockDevice(deviceId: String) {
        try {
            // 生成新的随机密码
            currentPassword = generatePassword()
            
            val intent = Intent(context, LockScreenService::class.java).apply {
                action = LockScreenService.ACTION_LOCK
                putExtra(LockScreenService.EXTRA_PASSWORD, currentPassword)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startService(intent)
            
            _lockState.value = true
            Log.d(TAG, "Device locked with password: $currentPassword")
        } catch (e: Exception) {
            Log.e(TAG, "Error locking device: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 解锁设备屏幕
     */
    fun unlockDevice(deviceId: String) {
        try {
            val intent = Intent(context, LockScreenService::class.java).apply {
                action = LockScreenService.ACTION_UNLOCK
            }
            context.startService(intent)
            
            _lockState.value = false
            Log.d(TAG, "Device unlocked")
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking device: ${e.message}")
        }
    }
    
    /**
     * 生成随机6位数密码
     */
    private fun generatePassword(): String {
        return (0..5)
            .map { Random.nextInt(0, 10) }
            .joinToString("")
    }
} 