package com.ljh.phonemanage.di

import android.content.Context
import com.ljh.phonemanage.data.repository.DeviceRepository
import com.ljh.phonemanage.manager.DeviceManager
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
    
    @Provides
    @Singleton
    fun provideDeviceRepository(): DeviceRepository {
        return DeviceRepository()
    }
    
    @Provides
    @Singleton
    fun provideDeviceManager(
        @ApplicationContext context: Context,
        deviceRepository: DeviceRepository,
        screenManager: ScreenManager
    ): DeviceManager {
        return DeviceManager(context, deviceRepository, screenManager)
    }
    
    @Provides
    @Singleton
    fun provideDeviceManagerInitializer(
        screenManager: ScreenManager,
        deviceManager: DeviceManager
    ): DeviceManagerInitializer {
        return DeviceManagerInitializer(screenManager, deviceManager)
    }
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext appContext: Context): Context {
        return appContext
    }
} 