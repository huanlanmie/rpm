package com.ljh.phonemanage.viewmodel

import androidx.lifecycle.ViewModel
import com.ljh.phonemanage.manager.ScreenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val screenManager: ScreenManager
) : ViewModel() {
    
    val isLocked: StateFlow<Boolean> = screenManager.lockState
    
    fun toggleLockScreen() {
        if (isLocked.value) {
            screenManager.unlockDevice("default")
        } else {
            screenManager.lockDevice("default")
        }
    }
} 