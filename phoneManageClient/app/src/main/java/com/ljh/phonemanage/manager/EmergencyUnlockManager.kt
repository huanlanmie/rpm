package com.ljh.phonemanage.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 紧急解锁管理器
 * 管理紧急解锁次数限制和日期重置
 */
@Singleton
class EmergencyUnlockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "EmergencyUnlockManager"
    
    companion object {
        private const val PREFS_NAME = "emergency_unlock_prefs"
        private const val KEY_COUNT = "unlock_count"
        private const val KEY_DATE = "last_unlock_date"
        private const val MAX_UNLOCKS_PER_DAY = 3
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val _remainingUnlocks = MutableStateFlow(MAX_UNLOCKS_PER_DAY)
    val remainingUnlocks: StateFlow<Int> = _remainingUnlocks
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    init {
        // 初始化时检查并重置计数器
        checkAndResetCounter()
    }
    
    /**
     * 检查今天是否还有解锁次数
     */
    fun canUnlockToday(): Boolean {
        checkAndResetCounter()
        return _remainingUnlocks.value > 0
    }
    
    /**
     * 记录一次解锁使用
     * @return 今天剩余的解锁次数
     */
    fun recordUnlock(): Int {
        checkAndResetCounter()
        
        if (_remainingUnlocks.value <= 0) {
            return 0
        }
        
        val newCount = _remainingUnlocks.value - 1
        _remainingUnlocks.value = newCount
        
        // 保存到SharedPreferences
        prefs.edit().apply {
            putInt(KEY_COUNT, MAX_UNLOCKS_PER_DAY - newCount)
            putString(KEY_DATE, getCurrentDateString())
            apply()
        }
        
        Log.d(TAG, "记录一次紧急解锁，今天剩余次数: $newCount")
        return newCount
    }
    
    /**
     * 检查日期并在需要时重置计数器
     */
    private fun checkAndResetCounter() {
        val lastDateStr = prefs.getString(KEY_DATE, "") ?: ""
        val currentDateStr = getCurrentDateString()
        
        if (lastDateStr != currentDateStr) {
            // 日期已变更，重置计数器
            Log.d(TAG, "日期已更新，重置紧急解锁计数器")
            _remainingUnlocks.value = MAX_UNLOCKS_PER_DAY
            
            prefs.edit().apply {
                putInt(KEY_COUNT, 0)
                putString(KEY_DATE, currentDateStr)
                apply()
            }
        } else {
            // 同一天，加载已使用的次数
            val usedCount = prefs.getInt(KEY_COUNT, 0)
            _remainingUnlocks.value = MAX_UNLOCKS_PER_DAY - usedCount
        }
        
        Log.d(TAG, "当前日期: $currentDateStr, 今天剩余紧急解锁次数: ${_remainingUnlocks.value}")
    }
    
    /**
     * 获取当前日期字符串 (yyyy-MM-dd)
     */
    private fun getCurrentDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
    
    /**
     * 安排午夜重置任务
     */
    fun scheduleMidnightReset() {
        coroutineScope.launch {
            val calendar = Calendar.getInstance()
            // 设置为明天的00:00:01
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 1)
            
            val now = System.currentTimeMillis()
            val delay = calendar.timeInMillis - now
            
            Log.d(TAG, "安排午夜重置任务，延迟: ${delay/1000/60} 分钟")
            
            kotlinx.coroutines.delay(delay)
            checkAndResetCounter()
            // 重新安排下一次重置
            scheduleMidnightReset()
        }
    }
} 