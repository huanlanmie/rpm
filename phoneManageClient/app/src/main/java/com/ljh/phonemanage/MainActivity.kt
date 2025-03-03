package com.ljh.phonemanage

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.ljh.phonemanage.ui.theme.PhoneManageTheme
import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.ljh.phonemanage.ui.AppNavigation
import com.ljh.phonemanage.service.DeviceStatusCheckService
import com.ljh.phonemanage.manager.AutoStartPermissionManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var autoStartPermissionManager: AutoStartPermissionManager
    
    // 处理权限请求的启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，可以启动服务
            startDeviceStatusCheckService()
        } else {
            // 显示提示，告知用户需要通知权限
            showNotificationPermissionDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检查悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
        
        // 设置窗口为硬件加速
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        
        // 检查和请求必要的权限
        checkAndRequestPermissions()
        
        // 检查并申请自启动权限
        checkAutoStartPermission()
        
        setContent {
            PhoneManageTheme {
                val navController = rememberNavController()
                
                // 通知权限对话框状态
                var showPermissionDialog by remember { mutableStateOf(false) }
                
                // 通知权限对话框
                if (showPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showPermissionDialog = false },
                        title = { Text("需要通知权限") },
                        text = { Text("此应用需要通知权限才能在后台运行设备状态检查服务。请在设置中授予权限。") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showPermissionDialog = false
                                    openAppSettings()
                                }
                            ) {
                                Text("前往设置")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showPermissionDialog = false }) {
                                Text("取消")
                            }
                        }
                    )
                }
                
                // 应用主导航
                AppNavigation()
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        // 检查通知权限（Android 13+需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    // 已有权限，检查并启动服务
                    if (!DeviceStatusCheckService.isServiceRunning(this)) {
                        startDeviceStatusCheckService()
                    }
                }
                else -> {
                    // 请求权限
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 13以下不需要运行时请求通知权限
            if (!DeviceStatusCheckService.isServiceRunning(this)) {
                startDeviceStatusCheckService()
            }
        }
    }
    
    private fun startDeviceStatusCheckService() {
        // 启动设备状态检查服务
        val serviceIntent = Intent(this, DeviceStatusCheckService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    
    private fun showNotificationPermissionDialog() {
        // 使用Compose State触发对话框显示
        setContent {
            PhoneManageTheme {
                val navController = rememberNavController()
                
                var showDialog by remember { mutableStateOf(true) }
                
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("需要通知权限") },
                        text = { Text("此应用需要通知权限才能在后台运行设备状态检查服务。请在设置中授予权限。") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDialog = false
                                    openAppSettings()
                                }
                            ) {
                                Text("前往设置")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDialog = false }) {
                                Text("取消")
                            }
                        }
                    )
                }
                
                // 应用主导航
                AppNavigation()
            }
        }
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
    
    private fun checkAutoStartPermission() {
        if (autoStartPermissionManager.needsAutoStartPermission()) {
            // 使用传统AlertDialog而不是Compose版本
            android.app.AlertDialog.Builder(this)
                .setTitle("自启动权限")
                .setMessage("为了确保应用能在设备重启后正常工作，请允许应用自启动。")
                .setPositiveButton("前往设置") { _, _ ->
                    autoStartPermissionManager.openAutoStartPermissionSettings()
                }
                .setNegativeButton("稍后再说", null)
                .show()
        }
    }
}