package com.ljh.phonemanage.di

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltWorkerModule {
    
    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(
        workerFactory: HiltWorkerFactory
    ): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}

/**
 * WorkManager初始化器
 */
class WorkManagerInitializer : Initializer<WorkManager> {
    override fun create(context: Context): WorkManager {
        val workManager = WorkManager.getInstance(context)
        return workManager
    }
    
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
} 