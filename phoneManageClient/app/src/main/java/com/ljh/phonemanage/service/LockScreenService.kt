package com.ljh.phonemanage.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ljh.phonemanage.ui.LockScreenActivity

class LockScreenService : Service() {
    private val TAG = "LockScreenService"
    
    companion object {
        const val ACTION_LOCK = "action_lock"
        const val ACTION_UNLOCK = "action_unlock"
        const val EXTRA_PASSWORD = "extra_password"
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_LOCK -> {
                val password = intent.getStringExtra(EXTRA_PASSWORD)
                showLockScreen(password)
            }
            ACTION_UNLOCK -> {
                // 不需要做任何事情，因为Activity会自己关闭
            }
        }
        return START_NOT_STICKY
    }
    
    private fun showLockScreen(password: String?) {
        val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("password", password)
        }
        startActivity(lockIntent)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
} 