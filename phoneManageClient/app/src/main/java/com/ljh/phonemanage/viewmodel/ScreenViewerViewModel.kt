package com.ljh.phonemanage.viewmodel

import androidx.lifecycle.ViewModel
import com.ljh.phonemanage.manager.ScreenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScreenViewerViewModel @Inject constructor(
    private val screenManager: ScreenManager
) : ViewModel() {
    
    private val _deviceId = MutableStateFlow<String?>(null)
    
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()
    
    // 设置当前设备ID
    fun setDeviceId(deviceId: String) {
        _deviceId.value = deviceId
    }
    
    // 切换锁屏状态
    fun toggleLockScreen() {
        val deviceId = _deviceId.value ?: return
        
        if (_isLocked.value) {
            screenManager.unlockDevice(deviceId)
            _isLocked.value = false
        } else {
            screenManager.lockDevice(deviceId)
            _isLocked.value = true
        }
    }
    
    override fun onCleared() {
        super.onCleared()
    }
} 