package com.ljh.phonemanage.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 番茄时钟状态管理类
 * 将状态提取到此类而不是保留在Compose内部，可以避免重组问题
 */
class PomodoroState {
    private val TAG = "PomodoroState"
    
    // 番茄时钟状态
    var currentMode by mutableStateOf("WORK")
        private set
    
    var isTimerRunning by mutableStateOf(false)
        private set
    
    var remainingTimeMs by mutableLongStateOf(25 * 60 * 1000L)
        private set
    
    var completedPomodoros by mutableIntStateOf(0)
        private set
    
    // 长按进度状态 (0.0f - 1.0f)
    var longPressProgress by mutableStateOf(0f)
        private set
        
    // 是否正在执行长按操作
    var isLongPressing by mutableStateOf(false)
        private set
    
    // 上次点击时间，用于防抖
    private var lastClickTime = 0L
    private val debounceTime = 500L
    
    // 获取当前模式对应的总时间
    val totalTimeMs: Long
        get() = when (currentMode) {
            "WORK" -> 25 * 60 * 1000L
            "SHORT_BREAK" -> 5 * 60 * 1000L
            else -> 15 * 60 * 1000L
        }
    
    // 重置时钟
    fun resetTimer() {
        remainingTimeMs = totalTimeMs
        isTimerRunning = false
        Log.d(TAG, "重置番茄时钟，模式: $currentMode")
    }
    
    // 开始/暂停定时器
    fun toggleTimer() {
        isTimerRunning = !isTimerRunning
        Log.d(TAG, "切换番茄时钟状态: ${if (isTimerRunning) "开始" else "暂停"}")
    }
    
    // 减少剩余时间
    fun decreaseTime(amount: Long) {
        if (isTimerRunning) {
            val oldTime = remainingTimeMs
            remainingTimeMs -= amount
            
            // 添加更详细的日志
            if (remainingTimeMs <= 1000) {  // 接近结束时记录更详细的日志
                Log.d(TAG, "倒计时接近结束: $oldTime -> $remainingTimeMs")
            }
            
            // 时间到，自动切换模式
            if (remainingTimeMs <= 0) {
                Log.d(TAG, "倒计时结束，触发handleTimerComplete()")
                handleTimerComplete()
            }
        }
    }
    
    // 处理时间结束
    private fun handleTimerComplete() {
        val now = System.currentTimeMillis()
        // 防止1秒内多次触发
        if (now - lastClickTime < 1000) {
            Log.d(TAG, "忽略多次触发的计时器完成事件: ${now - lastClickTime}ms")
            return
        }
        
        lastClickTime = now
        Log.d(TAG, "番茄时钟时间到，自动切换模式")
        
        when (currentMode) {
            "WORK" -> {
                Log.d(TAG, "工作时间结束，当前已完成番茄数: $completedPomodoros")
                completedPomodoros++
                Log.d(TAG, "番茄+1，现在完成数量: $completedPomodoros")
                
                if (completedPomodoros % 4 == 0) {
                    currentMode = "LONG_BREAK"
                    remainingTimeMs = 15 * 60 * 1000L
                    Log.d(TAG, "切换到长休息模式")
                } else {
                    currentMode = "SHORT_BREAK"
                    remainingTimeMs = 5 * 60 * 1000L
                    Log.d(TAG, "切换到短休息模式")
                }
            }
            "SHORT_BREAK", "LONG_BREAK" -> {
                currentMode = "WORK"
                remainingTimeMs = 25 * 60 * 1000L
                Log.d(TAG, "休息结束，切换到工作模式")
            }
        }
    }
    
    // 开始长按操作
    fun startLongPress() {
        if (!isLongPressing) {
            Log.d(TAG, "开始长按操作")
            isLongPressing = true
            longPressProgress = 0f
        }
    }
    
    // 更新长按进度
    fun updateLongPressProgress(progress: Float) {
        if (isLongPressing) {
            longPressProgress = progress.coerceIn(0f, 1f)
            // 日志太多会影响性能，只在关键点记录
            if (longPressProgress == 1f) {
                Log.d(TAG, "长按进度达到100%，准备跳过")
                executeSkip()
            }
        }
    }
    
    // 取消长按
    fun cancelLongPress() {
        if (isLongPressing) {
            Log.d(TAG, "取消长按操作，进度: $longPressProgress")
            isLongPressing = false
            longPressProgress = 0f
        }
    }
    
    // 执行跳过操作
    private fun executeSkip() {
        val now = System.currentTimeMillis()
        // 实现点击防抖
        if (now - lastClickTime > debounceTime) {
            lastClickTime = now
            Log.d(TAG, "长按完成，执行跳过")
            
            when (currentMode) {
                "WORK" -> {
                    Log.d(TAG, "跳过工作时间段，当前已完成番茄数: $completedPomodoros")
                    completedPomodoros++
                    
                    if (completedPomodoros % 4 == 0) {
                        currentMode = "LONG_BREAK"
                        remainingTimeMs = 15 * 60 * 1000L
                        Log.d(TAG, "切换到长休息，已完成番茄数: $completedPomodoros")
                    } else {
                        currentMode = "SHORT_BREAK"
                        remainingTimeMs = 5 * 60 * 1000L
                        Log.d(TAG, "切换到短休息，已完成番茄数: $completedPomodoros")
                    }
                }
                "SHORT_BREAK", "LONG_BREAK" -> {
                    Log.d(TAG, "跳过休息时间，切换到工作模式")
                    currentMode = "WORK"
                    remainingTimeMs = 25 * 60 * 1000L
                }
            }
            isTimerRunning = false
        } else {
            Log.d(TAG, "忽略跳过请求，点击间隔过短: ${now - lastClickTime}ms")
        }
        
        // 重置长按状态
        isLongPressing = false
        longPressProgress = 0f
    }
    
    // 确保计时器停止
    fun stopTimer() {
        if (isTimerRunning) {
            isTimerRunning = false
            Log.d(TAG, "停止番茄时钟")
        }
    }
} 