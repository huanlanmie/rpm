package com.ljh.phonemanage.di

import com.ljh.phonemanage.manager.DeviceManager
import com.ljh.phonemanage.manager.ScreenManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设备管理器初始化器
 * 用于解决ScreenManager和DeviceManager之间的循环依赖
 */
@Singleton
class DeviceManagerInitializer @Inject constructor(
    private val screenManager: ScreenManager,
    private val deviceManager: DeviceManager
) {
    init {
        // 设置循环依赖
        screenManager.setDeviceManager(deviceManager)
    }
} 