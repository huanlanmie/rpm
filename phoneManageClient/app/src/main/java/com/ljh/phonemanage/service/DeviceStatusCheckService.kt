package com.ljh.phonemanage.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ljh.phonemanage.MainActivity
import com.ljh.phonemanage.R
import com.ljh.phonemanage.data.repository.DeviceRepository
import com.ljh.phonemanage.manager.DeviceManager
import com.ljh.phonemanage.manager.ScreenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.min

/**
 * 设备状态检查服务
 * 定期检查设备状态，根据服务器返回的状态决定是否锁定设备
 */
@AndroidEntryPoint
class DeviceStatusCheckService : Service() {
    private val TAG = "DeviceStatusCheckService"
    
    // 使用Handler进行定时
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = Runnable { checkDeviceStatus() }
    
    // 使用协程执行网络请求
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 默认检查间隔1分钟（60000毫秒）
    private var checkIntervalMs = TimeUnit.SECONDS.toMillis(10)
    
    // 连续失败次数计数器
    private var consecutiveFailures = 0
    private val MAX_FAILURES = 5
    
    // 标记服务是否正在运行
    private var isServiceRunning = false
    
    // 记录请求次数和状态
    private var requestCount = 0
    private var lastRequestTime = 0L
    
    // 添加一个标志来标识是否是手动停止
    private var isManualStopping = false
    
    @Inject
    lateinit var deviceRepository: DeviceRepository
    
    @Inject
    lateinit var deviceManager: DeviceManager
    
    @Inject
    lateinit var screenManager: ScreenManager
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "device_status_check_channel"
        
        // 设置检查间隔时间的Intent动作
        const val ACTION_SET_INTERVAL = "com.ljh.phonemanage.SET_CHECK_INTERVAL"
        const val EXTRA_INTERVAL_MS = "interval_ms"
        
        // 启动服务
        fun startService(context: Context, intervalMs: Long = TimeUnit.MINUTES.toMillis(1)) {
            val intent = Intent(context, DeviceStatusCheckService::class.java).apply {
                putExtra(EXTRA_INTERVAL_MS, intervalMs)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            Log.d("DeviceStatusCheckService", "服务启动请求已发送，间隔: ${intervalMs}ms")
        }
        
        // 停止服务
        fun stopService(context: Context) {
            val intent = Intent(context, DeviceStatusCheckService::class.java)
            context.stopService(intent)
            Log.d("DeviceStatusCheckService", "服务停止请求已发送")
        }
        
        // 检查服务是否正在运行
        fun isServiceRunning(context: Context): Boolean {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val services = activityManager.getRunningServices(Integer.MAX_VALUE)
            
            for (serviceInfo in services) {
                if (DeviceStatusCheckService::class.java.name == serviceInfo.service.className) {
                    Log.d("DeviceStatusCheckService", "服务正在运行")
                    return true
                }
            }
            Log.d("DeviceStatusCheckService", "服务未在运行")
            return false
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "服务创建 - 开始初始化")
        
        // 创建通知渠道
        createNotificationChannel()
        
        // 启动前台服务
        val notification = createNotification("设备状态检查服务正在运行")
        startForeground(NOTIFICATION_ID, notification)
        
        Log.d(TAG, "服务创建完成 - 前台服务已启动")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val currentTime = getCurrentTimeString()
        Log.d(TAG, "[$currentTime] 服务启动命令 - action=${intent?.action}, startId=$startId")
        
        when (intent?.action) {
            ACTION_SET_INTERVAL -> {
                val newInterval = intent.getLongExtra(EXTRA_INTERVAL_MS, checkIntervalMs)
                if (newInterval != checkIntervalMs) {
                    val oldInterval = checkIntervalMs
                    checkIntervalMs = newInterval
                    Log.d(TAG, "[$currentTime] 检查间隔已更改: ${oldInterval}ms -> ${checkIntervalMs}ms")
                    
                    // 重启检查任务
                    startCheckingStatus()
                }
            }
            else -> {
                // 从Intent获取间隔时间，如果有的话
                intent?.getLongExtra(EXTRA_INTERVAL_MS, checkIntervalMs)?.let { interval ->
                    val oldInterval = checkIntervalMs
                    checkIntervalMs = interval
                    Log.d(TAG, "[$currentTime] 使用Intent提供的间隔: ${oldInterval}ms -> ${checkIntervalMs}ms")
                }
                
                // 启动检查任务
                startCheckingStatus()
            }
        }
        
        // 如果服务被杀死，系统会尝试重新创建服务
        Log.d(TAG, "[$currentTime] 返回START_STICKY - 如果服务被杀死将会重新创建")
        return START_STICKY
    }
    
    override fun onDestroy() {
        val currentTime = getCurrentTimeString()
        Log.d(TAG, "[$currentTime] 服务销毁 - 正在清理资源")
        stopCheckingStatus()
        serviceScope.cancel() // 取消所有协程
        
        // 移除所有待处理的回调
        handler.removeCallbacksAndMessages(null)
        
        super.onDestroy()
        
        // 检查是否真的需要重启服务
        // 如果是用户主动停止的服务（例如解锁后），不应该重启
        if (!isManualStopping) {
            val restartIntent = Intent(applicationContext, DeviceStatusCheckService::class.java)
            applicationContext.startService(restartIntent)
            Log.d(TAG, "[$currentTime] 服务销毁时已尝试重启服务")
        } else {
            Log.d(TAG, "[$currentTime] 服务是手动停止的，不重启")
        }
    }
    
    // 当应用被从最近任务列表移除时调用
    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentTime = getCurrentTimeString()
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "[$currentTime] 应用从任务列表移除 - 尝试重启服务")
        
        // 尝试重启服务
        val restartIntent = Intent(applicationContext, DeviceStatusCheckService::class.java)
        applicationContext.startService(restartIntent)
        Log.d(TAG, "[$currentTime] 任务移除时已尝试重启服务")
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun startCheckingStatus() {
        val currentTime = getCurrentTimeString()
        if (isServiceRunning) {
            Log.d(TAG, "[$currentTime] 检查任务已在运行，先停止当前任务")
            stopCheckingStatus()
        }
        
        Log.d(TAG, "[$currentTime] 开始设备状态检查任务 - 间隔: ${checkIntervalMs}ms")
        isServiceRunning = true
        requestCount = 0
        
        // 立即执行第一次检查
        Log.d(TAG, "[$currentTime] 安排立即执行首次检查")
        handler.post(checkRunnable)
    }
    
    private fun stopCheckingStatus() {
        val currentTime = getCurrentTimeString()
        handler.removeCallbacks(checkRunnable)
        isServiceRunning = false
        Log.d(TAG, "[$currentTime] 已停止设备状态检查任务 - 共执行了${requestCount}次请求")
    }
    
    private fun checkDeviceStatus() {
        requestCount++
        lastRequestTime = System.currentTimeMillis()
        val currentTime = getCurrentTimeString()
        Log.d(TAG, "[$currentTime] 开始第${requestCount}次设备状态检查")
        
        // 使用协程执行网络请求
        serviceScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                Log.d(TAG, "[$currentTime] 正在从DeviceManager获取当前设备信息")
                // 获取当前设备信息
                val currentDeviceInfo = deviceManager.deviceInfo.value
                if (currentDeviceInfo == null) {
                    Log.e(TAG, "[$currentTime] 当前设备信息为空，无法检查设备状态，本次请求终止")
                    handler.post { 
                        Log.d(TAG, "[$currentTime] 设备信息为空，安排下一次检查")
                        scheduleNextCheck() 
                    }
                    return@launch
                }
                
                val deviceToken = currentDeviceInfo.deviceToken
                Log.d(TAG, "[$currentTime] 获取到设备Token: $deviceToken，开始请求服务器")
                
                // 从服务器获取最新设备信息
                val result = deviceRepository.getDeviceInfo(deviceToken)
                val requestTime = System.currentTimeMillis() - startTime
                
                if (result.isSuccess) {
                    val serverDevice = result.getOrNull()
                    if (serverDevice != null) {
                        Log.d(TAG, "[$currentTime] API请求成功(${requestTime}ms) - 设备状态: ${serverDevice.deviceStatus}")
                        
                        // 更新UI必须在主线程
                        // 设备状态不同时才执行操作，避免无谓的操作
                        if (serverDevice.deviceStatus == 0L) {
                            Log.d(TAG, "[$currentTime] 服务器要求锁定设备 - 检查锁屏页面是否已在运行")
                            
                            // 检查锁屏页面是否已经运行
                            if (!isLockScreenActivityRunning()) {
                                Log.d(TAG, "[$currentTime] 锁屏页面未运行，执行锁定操作")
                                screenManager.lockDevice(deviceToken)
                                updateNotification("设备已锁定")
                            } else {
                                Log.d(TAG, "[$currentTime] 锁屏页面已在运行，无需重复锁定")
                                updateNotification("设备已处于锁定状态")
                            }
                        } else if (serverDevice.deviceStatus == 1L ) {
                            Log.d(TAG, "[$currentTime] 服务器要求解锁设备 - 当前设备已锁定，执行解锁操作")
                            screenManager.unlockDevice(deviceToken)
                            updateNotification("设备已解锁")
                        } else {
                            // 状态没变化，只更新通知
                            val statusText = if (serverDevice.deviceStatus == 0L) "锁定" else "正常"
                            val actionText = if (serverDevice.deviceStatus == 0L)
                                "服务器要求锁定但设备已锁定，无需操作"
                            else
                                "服务器要求解锁但设备已解锁，无需操作"
                            Log.d(TAG, "[$currentTime] $actionText")
                            updateNotification("设备状态: $statusText")
                        }
                        
                        // 成功检查后重置失败计数
                        if (consecutiveFailures > 0) {
                            Log.d(TAG, "[$currentTime] 请求成功，重置连续失败计数($consecutiveFailures -> 0)")
                        }
                        consecutiveFailures = 0
                    } else {
                        Log.e(TAG, "[$currentTime] API请求成功但设备信息为空 - 可能是服务器返回了空数据")
                        consecutiveFailures++
                    }
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "[$currentTime] API请求失败(${requestTime}ms) - ${error?.message}", error)
                    consecutiveFailures++
                }
            } catch (e: Exception) {
                val requestTime = System.currentTimeMillis() - startTime
                Log.e(TAG, "[$currentTime] 设备状态检查异常(${requestTime}ms) - ${e.message}", e)
                consecutiveFailures++
            } finally {
                // 安排下一次检查，确保在主线程上调用
                val executionTime = System.currentTimeMillis() - startTime
                handler.post { 
                    Log.d(TAG, "[$currentTime] 第${requestCount}次请求完成，耗时${executionTime}ms，安排下一次检查")
                    scheduleNextCheck() 
                }
            }
        }
    }
    
    private fun scheduleNextCheck() {
        if (!isServiceRunning) {
            Log.d(TAG, "服务已停止，不再安排下一次检查")
            return
        }
        
        var nextInterval = checkIntervalMs
        val currentTime = getCurrentTimeString()
        
        // 如果连续失败多次，临时增加间隔时间
        if (consecutiveFailures >= MAX_FAILURES) {
            val oldInterval = nextInterval
            nextInterval = min(checkIntervalMs * 2, TimeUnit.MINUTES.toMillis(10))
            Log.w(TAG, "[$currentTime] 连续失败${consecutiveFailures}次，临时增加检查间隔: ${oldInterval}ms -> ${nextInterval}ms")
        }
        
        // 计算下次执行时间
        val nextTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(System.currentTimeMillis() + nextInterval))
        
        // 安排下一次检查
        Log.d(TAG, "[$currentTime] 安排下一次检查 - 间隔${nextInterval}ms ")
        handler.postDelayed(checkRunnable, nextInterval)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "设备状态检查"
            val description = "定期检查设备状态的服务"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "已创建通知渠道: $CHANNEL_ID")
        }
    }
    
    private fun createNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("设备监控")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
    
    private fun updateNotification(content: String) {
        val currentTime = getCurrentTimeString()
        val notification = createNotification("$content (更新于:$currentTime)")
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "[$currentTime] 通知已更新: $content")
    }
    
    private fun getCurrentTimeString(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
    
    /**
     * 检查锁屏页面是否正在运行
     */
    private fun isLockScreenActivityRunning(): Boolean {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            
            // 在较新版本的Android上，getRunningTasks可能返回有限的结果
            // 但我们只关心顶部活动，所以应该仍然有效
            val tasks = activityManager.getRunningTasks(1)
            if (tasks.isNotEmpty()) {
                val topActivity = tasks[0].topActivity
                val isLockScreenRunning = topActivity?.className?.contains("LockScreenActivity") == true
                Log.d(TAG, "当前顶部活动: ${topActivity?.className}, 是否为锁屏页面: $isLockScreenRunning")
                return isLockScreenRunning
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查锁屏活动状态时出错", e)
            
            // 备用方法：检查特定服务是否运行
            val serviceRunning = isServiceRunning(LockScreenService::class.java)
            Log.d(TAG, "备用检查 - 锁屏服务是否运行: $serviceRunning")
            return serviceRunning
        }
        return false
    }
    
    /**
     * 检查指定服务是否正在运行
     */
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查服务状态时出错", e)
        }
        return false
    }
    
    // 添加一个公共方法用于手动停止服务
    fun stopService() {
        isManualStopping = true
        stopSelf()
    }
} 