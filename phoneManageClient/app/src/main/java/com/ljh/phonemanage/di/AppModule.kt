package com.ljh.phonemanage.di

import android.content.Context
import com.ljh.phonemanage.manager.ScreenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideScreenManager(@ApplicationContext context: Context): ScreenManager {
        return ScreenManager(context)
    }
} 