package com.ljh.phonemanage.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ljh.phonemanage.ui.LockScreenActivity
import android.content.Context
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import com.ljh.phonemanage.receiver.DeviceAdminReceiver

class LockScreenService : Service() {
    private val TAG = "LockScreenService"
    
    companion object {
        const val ACTION_LOCK = "com.ljh.phonemanage.action.LOCK"
        const val ACTION_UNLOCK = "com.ljh.phonemanage.action.UNLOCK"
        const val EXTRA_PASSWORD = "password"
    }
    
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "接收到命令: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_LOCK -> {
                // 锁定动作
                val password = intent.getStringExtra(EXTRA_PASSWORD) ?: ""
                Log.d(TAG, "锁定设备，密码: $password")
                
                // 启动锁屏活动
                val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(EXTRA_PASSWORD, password)
                }
                startActivity(lockIntent)
            }
            ACTION_UNLOCK -> {
                // 解锁动作
                Log.d(TAG, "解锁设备")
                
                // 关闭锁屏活动
                closeLockScreenActivity()
            }
        }
        
        return START_NOT_STICKY
    }
    
    /**
     * 关闭锁屏活动
     */
    private fun closeLockScreenActivity() {
        try {
            // 方法1: 发送广播通知活动关闭
            val closeIntent = Intent("com.ljh.phonemanage.CLOSE_LOCK_SCREEN")
            sendBroadcast(closeIntent)
            Log.d(TAG, "已发送关闭广播")
            
            // 防止广播未被接收的情况，设置超时机制
            handler.postDelayed({
                Log.d(TAG, "检查锁屏活动是否已关闭")
                if (isLockScreenActivityRunning()) {
                    Log.d(TAG, "锁屏活动仍在运行，尝试替代方法关闭")
                    
                    // 替代方法：使用新的Intent关闭活动
                    val finishIntent = Intent(this, LockScreenActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("finish", true)
                    }
                    startActivity(finishIntent)
                }
            }, 500) // 给广播0.5秒的处理时间
            
            Log.d(TAG, "已请求关闭锁屏页面")
        } catch (e: Exception) {
            Log.e(TAG, "关闭锁屏活动时发生错误", e)
        }
    }

    // 添加一个Handler用于延迟执行
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // 添加检查锁屏活动是否运行的方法
    private fun isLockScreenActivityRunning(): Boolean {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val tasks = activityManager.getRunningTasks(1)
            if (tasks.isNotEmpty()) {
                val topActivity = tasks[0].topActivity
                return topActivity?.className?.contains("LockScreenActivity") == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查锁屏活动状态时出错", e)
        }
        return false
    }

    override fun onCreate() {
        super.onCreate()
        
        // 初始化设备管理员
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, DeviceAdminReceiver::class.java)
    }

    // 在锁定方法中使用设备管理员
    private fun lockDeviceWithAdmin() {
        if (devicePolicyManager.isAdminActive(componentName)) {
            try {
                devicePolicyManager.lockNow()
                Log.d(TAG, "已通过设备管理员锁定设备")
            } catch (e: Exception) {
                Log.e(TAG, "锁定设备失败", e)
            }
        } else {
            Log.w(TAG, "设备管理员未激活，无法锁定设备")
        }
    }
} 