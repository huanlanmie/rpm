package com.ljh.phonemanage.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ljh.phonemanage.ui.LockScreenActivity
import android.content.Context

class LockScreenService : Service() {
    private val TAG = "LockScreenService"
    
    companion object {
        const val ACTION_LOCK = "com.ljh.phonemanage.action.LOCK"
        const val ACTION_UNLOCK = "com.ljh.phonemanage.action.UNLOCK"
        const val EXTRA_PASSWORD = "password"
    }
    
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
            
            // 等待一小段时间，确保广播有机会被处理
            try {
                Thread.sleep(100)
            } catch (e: Exception) {
                // 忽略中断异常
            }
            
            // 方法2: 启动活动但带有结束标志
            // 只有在需要时才使用这种方法
            /*
            val finishIntent = Intent(this, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("finish", true)
            }
            startActivity(finishIntent)
            */
            
            Log.d(TAG, "已请求关闭锁屏页面")
        } catch (e: Exception) {
            Log.e(TAG, "关闭锁屏活动时发生错误", e)
        }
    }
} 