package com.ljh.phonemanage.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ljh.phonemanage.MainActivity
import com.ljh.phonemanage.service.LockScreenService

/**
 * 开机启动广播接收器
 * 监听系统开机完成事件，启动应用服务
 */
class BootCompletedReceiver : BroadcastReceiver() {
    private val TAG = "BootCompletedReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "接收到开机广播: ${intent.action}")
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "系统启动完成，准备启动应用")
            
            // 启动主活动
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // 启动锁屏服务
            val serviceIntent = Intent(context, LockScreenService::class.java)
            
            try {
                // 优先启动主活动
                context.startActivity(mainIntent)
                Log.d(TAG, "已启动主活动")
                
                // 然后启动服务
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "已启动锁屏服务")
            } catch (e: Exception) {
                Log.e(TAG, "启动应用失败: ${e.message}", e)
            }
        }
    }
} 