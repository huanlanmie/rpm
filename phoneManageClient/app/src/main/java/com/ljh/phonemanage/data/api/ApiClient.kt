package com.ljh.phonemanage.data.api

import com.google.gson.GsonBuilder
import com.ljh.phonemanage.util.DateAdapter
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.Date

/**
 * API客户端
 * 负责创建和维护API服务实例
 */
object ApiClient {
    // 如果使用Android模拟器:
    private const val BASE_URL = "http://192.168.1.7:18888/"  // 10.0.2.2是Android模拟器访问主机的特殊IP
    
    // 如果使用真机通过USB连接:
    // private const val BASE_URL = "http://你电脑的局域网IP:18888/"  // 例如 "http://192.168.1.100:18888/"
    
    // 创建OkHttpClient实例
    private val okHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // 创建Gson实例，处理日期格式
    private val gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateAdapter())
        .setLenient()
        .create()
    
    // 创建Retrofit实例
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    // 创建API服务实例
    val deviceApiService: DeviceApiService by lazy {
        retrofit.create(DeviceApiService::class.java)
    }
} 