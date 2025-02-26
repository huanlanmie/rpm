package com.ljh.phonemanage.ui

import android.app.Activity
import android.app.ActivityManager
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
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljh.phonemanage.MainActivity
import com.ljh.phonemanage.data.repository.DeviceRepository
import com.ljh.phonemanage.manager.DeviceManager
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

@AndroidEntryPoint
class LockScreenActivity : ComponentActivity() {
    private var activityManager: ActivityManager? = null
    private var isLocked = true
    private val TAG = "LockScreenActivity"
    
    // 添加依赖注入
    @Inject
    lateinit var deviceRepository: DeviceRepository
    
    @Inject
    lateinit var deviceManager: DeviceManager
    
    // 定时器相关变量
    private val handler = Handler(Looper.getMainLooper())
    private val checkStatusRunnable = Runnable { checkDeviceStatus() }
    private var deviceCheckJob: Job? = null
    private val checkIntervalMs = TimeUnit.SECONDS.toMillis(5) // 每5秒检查一次
    
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
        
        try {
            // 注册关闭广播接收器
            registerReceiver(closeReceiver, IntentFilter("com.ljh.phonemanage.CLOSE_LOCK_SCREEN"))
            Log.d(TAG, "广播接收器注册成功")
        } catch (e: Exception) {
            Log.e(TAG, "广播接收器注册失败", e)
        }
        
        // 获取传入的密码，如果没有则生成随机6位数密码
        val passedPassword = intent?.getStringExtra(LockScreenService.EXTRA_PASSWORD)
        val password = if (passedPassword.isNullOrEmpty()) {
            generateRandomPassword()
        } else {
            passedPassword
        }
        
        Log.d(TAG, "Lock screen started with password: $password")
        
        setContent {
            PhoneManageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LockScreenContent(password) {
                        handleUnlock()
                    }
                }
            }
        }
        
        // 启动监控任务
        startMonitoring()
        
        // 启动后端设备状态检查定时器
        startDeviceStatusChecker()
    }
    
    // 处理解锁操作
    private fun handleUnlock() {
        // 解锁时立即设置isLocked为false，停止监控
        isLocked = false
        
        // 停止设备状态检查
        stopDeviceStatusChecker()
        
        // 停止锁定任务模式（如果之前启动了）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                stopLockTask()
            } catch (e: Exception) {
                Log.e(TAG, "停止锁定任务失败", e)
            }
        }
        
        // 启动主活动并结束锁屏活动
        CoroutineScope(Dispatchers.Main).launch {
            delay(300) // 短暂延迟以展示退出动画
            
            // 创建启动主活动的Intent
            val mainIntent = Intent(this@LockScreenActivity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(mainIntent)
            
            // 结束当前活动
            finish()
        }
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
    
    private fun startMonitoring() {
        CoroutineScope(Dispatchers.Default + Job()).launch {
            while (isLocked) {
                try {
                    // 获取当前最顶部的Activity
                    val topActivity = activityManager?.getRunningTasks(1)?.firstOrNull()?.topActivity
                    
                    // 如果当前Activity不是锁屏Activity，则立即将锁屏Activity移到前台
                    if (topActivity?.className != this@LockScreenActivity.javaClass.name) {
                        withContext(Dispatchers.Main) {
                            moveTaskToFront()
                        }
                    }
                    
                    delay(10) // 每10ms检查一次
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun moveTaskToFront() {
        try {
            // 使用FLAG_ACTIVITY_MULTIPLE_TASK确保立即显示
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            startActivity(intent)
            
            // 强制将当前任务移到前台
            activityManager?.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onBackPressed() {
        // 禁用返回键
        // 不调用super.onBackPressed()
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
        if (isLocked) {
            moveTaskToFront()
        }
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isLocked) {
            moveTaskToFront()
        }
    }
    
    override fun onDestroy() {
        Log.d(TAG, "锁屏活动销毁，isLocked=$isLocked")
        
        // 停止设备状态检查
        stopDeviceStatusChecker()
        
        // 取消注册广播接收器
        try {
            unregisterReceiver(closeReceiver)
            Log.d(TAG, "广播接收器注销成功")
        } catch (e: Exception) {
            Log.e(TAG, "取消注册接收器失败", e)
        }
        
        isLocked = false
        super.onDestroy()
    }
}

@Composable
fun LockScreenContent(password: String, onUnlock: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // 新增一个状态，表示界面是否正在解锁过程中
    var isUnlocking by remember { mutableStateOf(false) }
    
    // 番茄时钟状态
    var currentMode by remember { mutableStateOf("WORK") }
    var isTimerRunning by remember { mutableStateOf(false) }
    var remainingTimeMs by remember { mutableLongStateOf(25 * 60 * 1000L) }
    var inputPassword by remember { mutableStateOf("") }
    var isPasswordError by remember { mutableStateOf(false) }
    var completedPomodoros by remember { mutableIntStateOf(0) }
    
    // 计算进度百分比
    val totalTimeMs = when (currentMode) {
        "WORK" -> 25 * 60 * 1000L
        "SHORT_BREAK" -> 5 * 60 * 1000L
        else -> 15 * 60 * 1000L
    }
    val progress = 1f - (remainingTimeMs.toFloat() / totalTimeMs.toFloat())
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "progress animation"
    )
    
    // 颜色设置
    val progressColor = when (currentMode) {
        "WORK" -> Color(0xFFE57373) // 红色，工作时间
        "SHORT_BREAK" -> Color(0xFF81C784) // 绿色，短休息
        else -> Color(0xFF64B5F6) // 蓝色，长休息
    }
    
    // 时钟更新，只有在未解锁时才运行
    LaunchedEffect(isTimerRunning, currentMode, isUnlocking) {
        if (isTimerRunning && !isUnlocking) {
            while (remainingTimeMs > 0 && !isUnlocking) {
                delay(1000)
                if (!isUnlocking) {
                    remainingTimeMs -= 1000
                }
            }
            
            // 时间到，切换模式
            if (remainingTimeMs <= 0 && !isUnlocking) {
                when (currentMode) {
                    "WORK" -> {
                        completedPomodoros++
                        if (completedPomodoros % 4 == 0) {
                            currentMode = "LONG_BREAK"
                            remainingTimeMs = 15 * 60 * 1000L
                        } else {
                            currentMode = "SHORT_BREAK"
                            remainingTimeMs = 5 * 60 * 1000L
                        }
                    }
                    "SHORT_BREAK", "LONG_BREAK" -> {
                        currentMode = "WORK"
                        remainingTimeMs = 25 * 60 * 1000L
                    }
                }
            }
        }
    }
    
    // 当活动被销毁时停止计时器
    DisposableEffect(Unit) {
        onDispose {
            isTimerRunning = false
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
                .verticalScroll(scrollState),
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
                text = when (currentMode) {
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
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTimeMs)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTimeMs) -
                            TimeUnit.MINUTES.toSeconds(minutes)
                    
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Text(
                        text = "已完成 $completedPomodoros 个番茄",
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
                IconButton(
                    onClick = {
                        remainingTimeMs = when (currentMode) {
                            "WORK" -> 25 * 60 * 1000L
                            "SHORT_BREAK" -> 5 * 60 * 1000L
                            else -> 15 * 60 * 1000L
                        }
                        isTimerRunning = false
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "重置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 开始/暂停按钮
                IconButton(
                    onClick = { isTimerRunning = !isTimerRunning },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isTimerRunning) progressColor.copy(alpha = 0.8f) else progressColor)
                ) {
                    Icon(
                        if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isTimerRunning) "暂停" else "开始",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // 跳过按钮
                IconButton(
                    onClick = {
                        when (currentMode) {
                            "WORK" -> {
                                completedPomodoros++
                                if (completedPomodoros % 4 == 0) {
                                    currentMode = "LONG_BREAK"
                                    remainingTimeMs = 15 * 60 * 1000L
                                } else {
                                    currentMode = "SHORT_BREAK"
                                    remainingTimeMs = 5 * 60 * 1000L
                                }
                            }
                            "SHORT_BREAK", "LONG_BREAK" -> {
                                currentMode = "WORK"
                                remainingTimeMs = 25 * 60 * 1000L
                            }
                        }
                        isTimerRunning = false
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "跳过",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 密码输入框
            Text(
                text = "输入密码解锁设备",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            TextField(
                value = inputPassword,
                onValueChange = {
                    inputPassword = it
                    isPasswordError = false
                },
                visualTransformation = PasswordVisualTransformation(),
                isError = isPasswordError,
                label = { Text("密码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (inputPassword == password) {
                            isTimerRunning = false
                            isUnlocking = true
                            onUnlock()
                        } else {
                            isPasswordError = true
                        }
                    }
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    if (inputPassword == password) {
                        // 在解锁前停止所有进行中的操作
                        isTimerRunning = false
                        isUnlocking = true
                        onUnlock()
                    } else {
                        isPasswordError = true
                    }
                }
            ) {
                Text(
                    text = "解锁",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
            
            if (isPasswordError) {
                Text(
                    text = "密码错误，请重试",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            // 添加底部空白，确保在小屏幕设备上所有内容都可见
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
} 