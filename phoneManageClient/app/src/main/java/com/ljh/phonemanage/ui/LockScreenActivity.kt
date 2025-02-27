package com.ljh.phonemanage.ui

import android.app.Activity
import android.app.ActivityManager
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljh.phonemanage.MainActivity
import com.ljh.phonemanage.data.repository.DeviceRepository
import com.ljh.phonemanage.manager.DeviceManager
import com.ljh.phonemanage.manager.EmergencyUnlockManager
import com.ljh.phonemanage.service.LockScreenService
import com.ljh.phonemanage.ui.theme.PhoneManageTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.isActive

@AndroidEntryPoint
class LockScreenActivity : ComponentActivity() {
    private var activityManager: ActivityManager? = null
    private var isLocked = true
    private val TAG = "LockScreenActivity"
    private var lockTaskEnabled = false

    // 添加依赖注入
    @Inject
    lateinit var deviceRepository: DeviceRepository
    
    @Inject
    lateinit var deviceManager: DeviceManager

    @Inject
    lateinit var emergencyUnlockManager: EmergencyUnlockManager

    // 定时器相关变量
    private val handler = Handler(Looper.getMainLooper())
    private val checkStatusRunnable = Runnable { checkDeviceStatus() }
    private var deviceCheckJob: Job? = null
    private val checkIntervalMs = TimeUnit.SECONDS.toMillis(5) // 每5秒检查一次
    
    // 在Activity级别创建番茄状态
    private val pomodoroState = PomodoroState()
    
    // 关闭广播接收器
    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "收到关闭广播，准备结束活动")
            isLocked = false  // 设置为已解锁，避免onPause中重新激活
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "锁屏活动创建")
        
        // 安全获取intent
        val intentExtra = intent?.getBooleanExtra("finish", false) ?: false
        
        // 检查是否是解锁请求
        if (intentExtra) {
            Log.d(TAG, "收到结束标志，正在关闭")
            isLocked = false  // 设置为已解锁，避免onPause中重新激活
            finish()
            return
        }
        
        // 从Intent获取密码参数
        val password = intent?.getStringExtra(LockScreenService.EXTRA_PASSWORD)
        if (!password.isNullOrEmpty()) {
            // 上报锁屏事件和密码
            reportLockEvent(password)
        } else {
            // 如果没有传递密码，使用随机生成的密码
            val randomPassword = generateRandomPassword()
            reportLockEvent(randomPassword)
        }
        
        // 获取ActivityManager
        activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        
        // 设置窗口标志
        window.apply {
            addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            
            // 设置窗口类型为系统警告窗口
            if (Settings.canDrawOverlays(this@LockScreenActivity)) {
                attributes.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                // 设置窗口优先级为最高，但允许触摸输入
                attributes.flags = attributes.flags or 
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                attributes.gravity = Gravity.TOP
            }
            
            // 设置窗口始终在顶部并处理软键盘
            attributes.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            attributes.softInputMode = 
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        }
        
        // 禁止分屏和旋转
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 设置固定竖屏
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            // 禁止传感器旋转
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR)
            
            // 禁止分屏
            try {
                val method = Activity::class.java.getMethod("setDisablePreviewScreenshots", Boolean::class.java)
                method.invoke(this, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // 锁定任务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    startLockTask()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // 防止截屏和录屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        // 阻止用户通过任务管理器关闭应用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        
        try {
            // 注册关闭广播接收器
            val filter = IntentFilter("com.ljh.phonemanage.CLOSE_LOCK_SCREEN")
            registerReceiver(closeReceiver, filter)
            Log.d(TAG, "广播接收器注册成功")
        } catch (e: Exception) {
            Log.e(TAG, "广播接收器注册失败", e)
        }
        
        Log.d(TAG, "Lock screen started with password: $password")
        
        setContent {
            PhoneManageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LockScreenContent(
                        password = password ?: generateRandomPassword(),
                        pomodoroState = pomodoroState,
                        onUnlock = { handleUnlock() },
                        onEmergencyUnlock = { handleEmergencyUnlock() }
                    )
                }
            }
        }
        
        // 启动后端设备状态检查定时器
        startDeviceStatusChecker()
        
        // 安排午夜重置任务
        emergencyUnlockManager.scheduleMidnightReset()
        
        // 检查各个依赖是否正确注入
        Log.d(TAG, "emergencyUnlockManager 是否为空: ${emergencyUnlockManager == null}")
        Log.d(TAG, "deviceManager 是否为空: ${deviceManager == null}")
        Log.d(TAG, "deviceRepository 是否为空: ${deviceRepository == null}")
    }
    
    // 处理解锁操作
    private fun handleUnlock() {
        // 防止重复解锁导致的问题
        if (!isLocked) {
            Log.d(TAG, "已经处于解锁状态，忽略重复解锁请求")
            return
        }
        
        Log.d(TAG, "开始解锁流程...")
        // 解锁时立即设置isLocked为false，停止监控
        isLocked = false
        
        // 停止设备状态检查
        stopDeviceStatusChecker()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && lockTaskEnabled) {
            try {
                stopLockTask()
                lockTaskEnabled = false
                Log.d(TAG, "已解除锁定任务模式")
            } catch (e: Exception) {
                Log.e(TAG, "解除锁定任务模式失败", e)
            }
        }
        
        // 确保活动结束
        finish()
    }
    
    // 启动设备状态检查定时器
    private fun startDeviceStatusChecker() {
        Log.d(TAG, "启动设备状态检查定时器，间隔: ${checkIntervalMs}ms")
        
        deviceCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (isLocked) {
                try {
                    checkDeviceStatus()
                } catch (e: Exception) {
                    Log.e(TAG, "设备状态检查出错: ${e.message}", e)
                }
                delay(checkIntervalMs)
            }
        }
    }
    
    // 停止设备状态检查定时器
    private fun stopDeviceStatusChecker() {
        Log.d(TAG, "停止设备状态检查定时器")
        deviceCheckJob?.cancel()
        deviceCheckJob = null
    }
    
    // 检查设备状态
    private fun checkDeviceStatus() {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        Log.d(TAG, "[$currentTime] 检查设备状态...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 获取当前设备信息
                val currentDeviceInfo = deviceManager.deviceInfo.value
                if (currentDeviceInfo == null) {
                    Log.e(TAG, "[$currentTime] 当前设备信息为空，无法检查设备状态")
                    return@launch
                }
                
                // 从服务器获取最新设备信息
                val deviceToken = currentDeviceInfo.deviceToken
                val result = deviceRepository.getDeviceInfo(deviceToken)
                
                if (result.isSuccess) {
                    val serverDevice = result.getOrNull()
                    if (serverDevice != null) {
                        Log.d(TAG, "[$currentTime] 获取到服务器设备状态: ${serverDevice.deviceStatus}")
                        
                        // 如果服务器设备状态为1（正常/解锁），则解锁设备
                        if (serverDevice.deviceStatus == 1L) {
                            Log.d(TAG, "[$currentTime] 服务器设备状态为解锁，关闭锁屏页面")
                            withContext(Dispatchers.Main) {
                                handleUnlock()
                            }
                        } else {
                            Log.d(TAG, "[$currentTime] 服务器设备状态为锁定，保持锁屏")
                        }
                    } else {
                        Log.e(TAG, "[$currentTime] 服务器返回的设备信息为空")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "[$currentTime] 获取设备信息失败: ${error?.message}", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "[$currentTime] 检查设备状态异常: ${e.message}", e)
            }
        }
    }
    
    // 生成随机6位数密码
    private fun generateRandomPassword(): String {
        return (0..5)
            .map { Random.nextInt(0, 10) }
            .joinToString("")
    }
    
    override fun onBackPressed() {
        // 不调用super.onBackPressed()，返回键失效
        Log.d(TAG, "返回键被禁用")
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_APP_SWITCH -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        Log.d(TAG, "onPause: isLocked=$isLocked, isFinishing=$isFinishing")
        
        // 关键改动: 只有在仍然锁定且活动没有结束的情况下才尝试重新激活
        if (isLocked && !isFinishing) {
            try {
                // 避免立即重新激活，给予一点延迟让其他流程完成
                handler.postDelayed({
                    // 再次检查锁定状态，确保在延迟期间没有解锁
                    if (isLocked && !isFinishing) {
                        Log.d(TAG, "尝试重新激活锁屏")
                        startActivity(Intent(this, LockScreenActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } else {
                        Log.d(TAG, "锁屏已解锁或正在结束，不再重新激活")
                    }
                }, 200) // 添加200ms延迟
            } catch (e: Exception) {
                Log.e(TAG, "重新激活锁屏失败", e)
            }
        } else {
            // 已解锁或正在结束，尝试解除锁定任务模式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && lockTaskEnabled) {
                try {
                    stopLockTask()
                    lockTaskEnabled = false
                    Log.d(TAG, "已解除锁定任务模式")
                } catch (e: Exception) {
                    Log.e(TAG, "解除锁定任务模式失败", e)
                }
            }
            
            Log.d(TAG, "锁屏已解锁或正在结束，允许页面关闭")
        }
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isLocked) {
            try {
                // 获取当前应用的任务信息
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val taskList = am.getRunningTasks(10) // 获取运行中的前10个任务
                
                // 查找当前应用的任务ID
                for (taskInfo in taskList) {
                    if (taskInfo.topActivity?.packageName == packageName) {
                        // 找到当前应用的任务，将其移至前台
                        am.moveTaskToFront(taskInfo.id, 0)
                        Log.d(TAG, "已将应用任务移至前台")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "尝试将任务移至前台时发生错误", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(closeReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "解除广播接收器注册失败", e)
        }

        // 不再需要解注册lockTaskReceiver
        
        // 确保销毁时停止计时器
        pomodoroState.stopTimer()
        Log.d(TAG, "锁屏页面销毁，已停止计时器")
        Log.d(TAG, "锁屏活动销毁，isLocked=$isLocked")
        
        isLocked = false
    }
    
    // 增强手势拦截效果，不仅拦截上划，也拦截所有可能的导航手势
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // 扩大拦截范围，保护番茄状态不被意外触发
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            // 锁屏模式下，任何边缘手势都被拦截
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val edgeThreshold = 100 // 边缘区域像素

            // 检测是否在屏幕边缘
            val isOnEdge = event.x < edgeThreshold || // 左边缘
                           event.x > screenWidth - edgeThreshold || // 右边缘
                           event.y < edgeThreshold || // 上边缘
                           event.y > screenHeight - edgeThreshold // 下边缘

            if (isOnEdge) {
                Log.d(TAG, "拦截屏幕边缘手势，坐标: (${event.x}, ${event.y})")
                return true
            }
        }
        
        // 原有的上划检测逻辑
        if (isSystemGestureInProgress(event)) {
            Log.d(TAG, "检测到系统手势尝试，拦截并忽略")
            return true
        }
        
        return super.dispatchTouchEvent(event)
    }
    
    // 检测系统手势
    private fun isSystemGestureInProgress(event: MotionEvent): Boolean {
        // 只关注ACTION_DOWN和ACTION_MOVE事件
        if (event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_MOVE) {
            return false
        }
        
        // 检测屏幕底部的上划手势
        val screenHeight = resources.displayMetrics.heightPixels
        val gestureZone = screenHeight * 0.15 // 屏幕底部15%区域
        
        // 如果触摸点在屏幕底部，且是上划动作
        return if (event.action == MotionEvent.ACTION_MOVE 
                && event.y > (screenHeight - gestureZone)
                && event.historySize > 0 
                && event.getHistoricalY(0) - event.y > 100) { // 上划距离超过100像素
            Log.d(TAG, "检测到底部上划手势，忽略此手势")
            true
        } else {
            false
        }
    }

    // 保留简化版的onResume方法，使用安全的启动锁定方式
    override fun onResume() {
        super.onResume()
        
        // 简化锁定逻辑，增加异常处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !isFinishing) {
            try {
                // 首先检查应用是否已经处于锁定任务模式
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                    startLockTask()
                    lockTaskEnabled = true
                    Log.d(TAG, "锁定任务模式已启用")
                }
            } catch (e: Exception) {
                Log.e(TAG, "启用锁定任务模式失败: ${e.message}", e)
                // 继续执行，不阻断应用流程
            }
        }
    }

    // 添加一个函数用于上报锁屏事件
    private fun reportLockEvent(password: String) {
        Log.d(TAG, "准备上报锁屏事件，密码: $password")
        
        // 获取设备ID - 修复：使用正确的方法获取设备信息
        val deviceInfo = deviceManager.deviceInfo.value // 使用 deviceInfo 属性而不是 getCurrentDeviceInfo 方法
        if (deviceInfo == null) {
            Log.e(TAG, "无法上报锁屏事件：设备信息为空")
            return
        }
        
        val deviceId = deviceInfo.deviceToken
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, deviceId+password);
                val result = deviceRepository.reportLockEvent(deviceId, password)
                if (result.isSuccess) {
                    Log.d(TAG, "锁屏事件上报成功")
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "锁屏事件上报失败: ${error?.message}", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "上报锁屏事件时发生异常", e)
            }
        }
    }

    // 修改handleEmergencyUnlock方法，使用Compose版本的AlertDialog
    private fun handleEmergencyUnlock() {
        Log.d(TAG, "准备执行紧急解锁...")
        
        try {
            // 检查emergencyUnlockManager是否已初始化
            if (emergencyUnlockManager == null) {
                Log.e(TAG, "紧急解锁失败：emergencyUnlockManager为空")
                Toast.makeText(this, "系统错误：未能初始化解锁管理器", Toast.LENGTH_LONG).show()
                return
            }
            
            // 检查今天的解锁次数是否已用完
            if (!emergencyUnlockManager.canUnlockToday()) {
                Toast.makeText(
                    this,
                    "您今天的紧急解锁次数已用完，请明天再试",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            
            // 获取剩余解锁次数
            val remainingUnlocks = emergencyUnlockManager.remainingUnlocks.value
            
            // 使用Material3对话框来替代AppCompat对话框
            setContent {
                PhoneManageTheme {
                    var showDialog by remember { mutableStateOf(true) }
                    
                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { 
                                showDialog = false
                                // 恢复锁屏界面
                                restoreLockScreenUI()
                            },
                            title = { Text("紧急解锁确认") },
                            text = { 
                                Text(
                                    "您确定要使用紧急解锁吗？\n\n" +
                                    "今天剩余解锁次数：$remainingUnlocks" +
                                    (if (remainingUnlocks <= 1) "\n\n⚠️ 这是今天最后的解锁机会" else "")
                                ) 
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDialog = false
                                        performEmergencyUnlock()
                                        // 恢复锁屏界面
                                        restoreLockScreenUI()
                                    }
                                ) {
                                    Text("确认")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { 
                                        showDialog = false
                                        // 恢复锁屏界面
                                        restoreLockScreenUI()
                                    }
                                ) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // 捕获并记录任何异常
            Log.e(TAG, "紧急解锁过程中发生异常", e)
            Toast.makeText(this, "解锁失败：${e.message}", Toast.LENGTH_LONG).show()
            // 确保恢复UI
            restoreLockScreenUI()
        }
    }

    // 修改restoreLockScreenUI方法，确保不再触发无限循环
    private fun restoreLockScreenUI() {
        // 手动避免触发新的生命周期事件
        var dialogWasShowing = false
        
        setContent {
            PhoneManageTheme {
                // 添加一个标志表示我们是通过对话框返回主UI的
                dialogWasShowing = true
                
                LockScreenContent(
                    password = generateRandomPassword(),
                    pomodoroState = pomodoroState,
                    onUnlock = { handleUnlock() },
                    onEmergencyUnlock = { handleEmergencyUnlock() }
                )
            }
        }
        
        // 如果是从对话框返回，额外处理以避免重启锁定任务
        if (dialogWasShowing) {
            Log.d(TAG, "从对话框返回UI，保持当前锁定状态")
        }
    }

    // 修改performEmergencyUnlock方法，增加错误处理
    private fun performEmergencyUnlock() {
        Log.d(TAG, "执行紧急解锁...")
        
        try {
            // 获取设备ID
            val deviceInfo = deviceManager?.deviceInfo?.value
            if (deviceInfo == null) {
                Log.e(TAG, "无法执行紧急解锁：设备信息为空")
                Toast.makeText(this, "解锁失败：无法获取设备信息", Toast.LENGTH_LONG).show()
                return
            }
            
            val deviceId = deviceInfo.deviceToken
            
            // 显示进度对话框
            val progressDialog = ProgressDialog(this).apply {
                setMessage("正在发送紧急解锁请求...")
                setCancelable(false)
                show()
            }
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 确保deviceRepository不为空
                    if (deviceRepository == null) {
                        withContext(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Log.e(TAG, "解锁失败：deviceRepository未初始化")
                            Toast.makeText(this@LockScreenActivity, "系统错误：设备服务未初始化", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                    
                    val result = deviceRepository.emergencyUnlockDevice(deviceId)
                    
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        
                        if (result.isSuccess) {
                            try {
                                // 记录解锁次数
                                val remaining = emergencyUnlockManager.recordUnlock()
                                
                                Log.d(TAG, "紧急解锁成功，今日剩余次数：$remaining")
                                Toast.makeText(
                                    this@LockScreenActivity, 
                                    "紧急解锁成功，今日剩余次数：$remaining", 
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                handleUnlock()
                            } catch (e: Exception) {
                                Log.e(TAG, "记录解锁次数失败", e)
                                Toast.makeText(this@LockScreenActivity, "解锁成功，但记录次数失败", Toast.LENGTH_SHORT).show()
                                // 即使记录失败也继续解锁
                                handleUnlock()
                            }
                        } else {
                            val error = result.exceptionOrNull()
                            Log.e(TAG, "紧急解锁失败: ${error?.message}", error)
                            Toast.makeText(this@LockScreenActivity, "紧急解锁失败: ${error?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Log.e(TAG, "紧急解锁异常", e)
                        Toast.makeText(this@LockScreenActivity, "紧急解锁出现异常: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "准备紧急解锁过程中发生异常", e)
            Toast.makeText(this, "解锁失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 修改 LockScreenContent 函数，确保结构正确
    @Composable
    fun LockScreenContent(
        password: String,
        pomodoroState: PomodoroState,
        onUnlock: () -> Unit,
        onEmergencyUnlock: () -> Unit
    ) {
        var isPasswordError by remember { mutableStateOf(false) }
        var inputPassword by remember { mutableStateOf("") }
        var isUnlocking by remember { mutableStateOf(false) }
        
        // 计算进度百分比
        val progress = 1f - (pomodoroState.remainingTimeMs.toFloat() / pomodoroState.totalTimeMs.toFloat())
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            label = "progress animation"
        )
        
        // 颜色设置
        val progressColor = when (pomodoroState.currentMode) {
            "WORK" -> Color(0xFFE57373) // 红色，工作时间
            "SHORT_BREAK" -> Color(0xFF81C784) // 绿色，短休息
            else -> Color(0xFF64B5F6) // 蓝色，长休息
        }
        
        // 时钟更新，只有在未解锁时才运行
        LaunchedEffect(pomodoroState.isTimerRunning, pomodoroState.currentMode, isUnlocking) {
            if (pomodoroState.isTimerRunning && !isUnlocking) {
                while (pomodoroState.isTimerRunning && !isUnlocking) {
                    delay(1000)
                    if (!isUnlocking) {
                        pomodoroState.decreaseTime(1000)
                    }
                }
            }
        }
        
        // 使用AnimatedVisibility包装整个UI，以便在解锁时显示动画
        AnimatedVisibility(
            visible = !isUnlocking,
            exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 显示当前解锁密码
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "解锁密码",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = password,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // 番茄时钟标题
                Text(
                    text = "专注时间",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // 番茄时钟状态
                Text(
                    text = when (pomodoroState.currentMode) {
                        "WORK" -> "工作时间"
                        "SHORT_BREAK" -> "短休息"
                        else -> "长休息"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = progressColor
                )
                
                // 番茄时钟进度和时间显示
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.size(200.dp),
                        color = progressColor,
                        trackColor = progressColor.copy(alpha = 0.2f),
                        strokeWidth = 10.dp
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(pomodoroState.remainingTimeMs)
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(pomodoroState.remainingTimeMs) -
                                TimeUnit.MINUTES.toSeconds(minutes)
                        
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Text(
                            text = "已完成 ${pomodoroState.completedPomodoros} 个番茄",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // 控制按钮
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 重置按钮
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                shape = CircleShape
                            )
                            .clickable { pomodoroState.resetTimer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "重置",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // 开始/暂停按钮
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp)
                            .background(
                                color = if (pomodoroState.isTimerRunning)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                shape = CircleShape
                            )
                            .clickable { pomodoroState.toggleTimer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (pomodoroState.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (pomodoroState.isTimerRunning) "暂停" else "开始",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // 跳过按钮（长按）
                    Box(
                        modifier = Modifier
                            .size(72.dp)  // 增大整个容器尺寸以容纳外部进度条
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 长按进度环 - 放在外围
                        if (pomodoroState.isLongPressing) {
                            CircularProgressIndicator(
                                progress = { pomodoroState.longPressProgress },
                                modifier = Modifier.size(72.dp),  // 比按钮大，防止手指遮挡
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        }

                        // 添加长按进度更新逻辑
                        LaunchedEffect(pomodoroState.isLongPressing) {
                            if (pomodoroState.isLongPressing) {
                                val longPressTime = 1200L
                                val startTime = System.currentTimeMillis()

                                try {
                                    while (pomodoroState.isLongPressing) {
                                        val elapsedTime = System.currentTimeMillis() - startTime
                                        val progress = (elapsedTime.toFloat() / longPressTime).coerceIn(0f, 1f)
                                        pomodoroState.updateLongPressProgress(progress)

                                        if (progress >= 1f) {
                                            Log.d("LockScreen", "长按进度完成")
                                            break
                                        }

                                        delay(16)
                                    }
                                } catch (e: Exception) {
                                    Log.e("LockScreen", "长按进度更新异常", e)
                                }
                            }
                        }
                        
                        // 内部按钮
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    shape = CircleShape
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = { offset ->
                                            try {
                                                pomodoroState.startLongPress()
                                                Log.d("LockScreen", "开始按压，准备长按")
                                                val released = tryAwaitRelease()
                                                if (!released) {
                                                    Log.d("LockScreen", "按压被取消")
                                                } else {
                                                    Log.d("LockScreen", "按压释放")
                                                }
                                            } finally {
                                                pomodoroState.cancelLongPress()
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // 跳过图标
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "长按跳过",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 密码输入框
                OutlinedTextField(
                    value = inputPassword,
                    onValueChange = { 
                        inputPassword = it 
                        isPasswordError = false
                    },
                    label = { Text("输入密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inputPassword == password) {
                                pomodoroState.toggleTimer()
                                isUnlocking = true
                                onUnlock()
                            } else {
                                isPasswordError = true
                            }
                        }
                    ),
                    isError = isPasswordError,
                    supportingText = if (isPasswordError) {
                        { Text("密码错误，请重试") }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 将解锁按钮和紧急解锁按钮放在同一行
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 解锁按钮
                    Button(
                        onClick = {
                            if (inputPassword == password) {
                                // 在解锁前停止所有进行中的操作
                                pomodoroState.toggleTimer()
                                isUnlocking = true
                                onUnlock()
                            } else {
                                isPasswordError = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "解锁",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 紧急解锁按钮 - 修复Compose上下文问题
                    Button(
                        onClick = onEmergencyUnlock,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "紧急解锁", 
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
} 