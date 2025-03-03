package com.ljh.phonemanage.manager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自启动权限管理器
 * 处理不同厂商手机的自启动权限申请
 */
@Singleton
class AutoStartPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "AutoStartPermissionManager"
    
    // 厂商自启动设置页面的Intent映射
    private val manufacturerIntents = mapOf(
        // 小米
        "xiaomi" to Intent().apply {
            component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        },
        // 华为
        "huawei" to Intent().apply {
            component = ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        },
        // OPPO
        "oppo" to Intent().apply {
            component = ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        },
        // VIVO
        "vivo" to Intent().apply {
            component = ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        },
        // 魅族
        "meizu" to Intent().apply {
            component = ComponentName(
                "com.meizu.safe",
                "com.meizu.safe.security.SHOW_APPSEC"
            )
            putExtra("packageName", context.packageName)
        },
        // 三星
        "samsung" to Intent().apply {
            component = ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
        }
    )
    
    /**
     * 判断是否需要申请自启动权限
     */
    fun needsAutoStartPermission(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturerIntents.keys.any { manufacturer.contains(it) }
    }
    
    /**
     * 根据设备厂商打开对应的自启动权限设置页面
     * @return 是否成功打开设置页面
     */
    fun openAutoStartPermissionSettings(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        Log.d(TAG, "设备厂商: $manufacturer")
        
        var intent: Intent? = null
        
        // 尝试查找匹配的厂商设置页
        for (entry in manufacturerIntents.entries) {
            if (manufacturer.contains(entry.key)) {
                intent = entry.value
                break
            }
        }
        
        // 如果找不到匹配的厂商设置页，使用应用详情页
        if (intent == null) {
            Log.d(TAG, "未找到匹配厂商的自启动设置页，使用应用详情页")
            intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        
        // 检查Intent是否可以解析
        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                // 添加新任务标志，避免在组件内启动活动
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "已打开自启动权限设置页面")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "打开自启动权限设置页面失败: ${e.message}", e)
                
                // 失败后尝试打开应用详情页
                try {
                    val detailsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(detailsIntent)
                    Log.d(TAG, "已打开应用详情页")
                    return true
                } catch (e2: Exception) {
                    Log.e(TAG, "打开应用详情页失败: ${e2.message}", e2)
                }
            }
        } else {
            Log.e(TAG, "无法解析自启动权限设置Intent")
            // 尝试打开应用详情页
            try {
                val detailsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(detailsIntent)
                Log.d(TAG, "已打开应用详情页")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "打开应用详情页失败: ${e.message}", e)
            }
        }
        
        return false
    }
} 