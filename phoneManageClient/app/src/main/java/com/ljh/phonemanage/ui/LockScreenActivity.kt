package com.ljh.phonemanage.ui

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljh.phonemanage.ui.theme.PhoneManageTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class LockScreenActivity : ComponentActivity() {
    private var activityManager: ActivityManager? = null
    private var isLocked = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
        
        val password = intent.getStringExtra("password") ?: "000000"
        
        setContent {
            PhoneManageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.9f)
                ) {
                    LockScreenContent(
                        password = password,
                        onUnlock = {
                            // 解锁时停止锁定任务
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                try {
                                    stopLockTask()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            isLocked = false
                            finish()
                        }
                    )
                }
            }
        }
        
        // 启动监控任务
        startMonitoring()
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
        moveTaskToFront()
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
        super.onDestroy()
        isLocked = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreenContent(
    password: String,
    onUnlock: () -> Unit
) {
    var inputPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(), // 添加这个来处理软键盘
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "设备已锁定",
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "你的付出定不负你,山无陵,天地合,乃敢与君绝 I will be with you.",
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = "测试密码: $password",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = inputPassword,
            onValueChange = { 
                inputPassword = it
                showError = false
            },
            label = { Text("请输入密码", color = Color.White) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = if (showError) Color.Red else Color.White,
                unfocusedBorderColor = if (showError) Color.Red.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
            ),
            isError = showError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (inputPassword == password) {
                        onUnlock()
                    } else {
                        showError = true
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )
        
        if (showError) {
            Text(
                text = "密码错误",
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (inputPassword == password) {
                    onUnlock()
                } else {
                    showError = true
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("解锁")
        }
    }
} 