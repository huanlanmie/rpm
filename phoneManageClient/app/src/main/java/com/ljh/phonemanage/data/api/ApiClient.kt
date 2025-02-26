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
 */
object ApiClient {
    // 如果使用Android模拟器:
    private const val BASE_URL = "http://192.168.1.7:18888/"  // 10.0.2.2是Android模拟器访问主机的特殊IP
    
    // 如果使用真机通过USB连接:
    // private const val BASE_URL = "http://你电脑的局域网IP:18888/"  // 例如 "http://192.168.1.100:18888/"
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateAdapter())
        .setLenient()
        .create()
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val deviceApiService: DeviceApiService = retrofit.create(DeviceApiService::class.java)
} 