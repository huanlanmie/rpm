package com.ljh.phonemanage

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import android.view.WindowManager
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class SplashActivity : ComponentActivity() {
    private val TAG = "SplashActivity"
    private val SPLASH_DISPLAY_TIME = 2000L // 2秒

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏显示
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // 设置内容视图为启动页布局
        setContentView(R.layout.activity_splash)
        
        // 使用Glide加载启动页图片
        val splashImageView = findViewById<ImageView>(R.id.splash_image)
        
        Glide.with(this)
            .load(R.drawable.splash_image)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop() // 确保图片填充整个ImageView
            .into(splashImageView)
        
        Log.d(TAG, "启动页已显示，将在${SPLASH_DISPLAY_TIME}ms后进入主程序")
        
        // 延迟2秒后跳转到主界面
        Handler(Looper.getMainLooper()).postDelayed({
            val mainIntent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(mainIntent)
            finish() // 结束当前Activity
        }, SPLASH_DISPLAY_TIME)
    }
} 