package com.ljh.phonemanage.data.api

import com.ljh.phonemanage.data.model.RpmDevices
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 设备API接口
 * 根据后端接口进行调整
 */
interface DeviceApiService {
    
    /**
     * 通过唯一标识符获取设备信息
     */
    @GET("system/devices/getByUuid/{uuid}")
    suspend fun getDeviceByUuid(@Path("uuid") uuid: String): Response<AjaxResult>
    
    /**
     * 查询设备列表
     */
    @POST("system/devices/list")
    suspend fun getDevicesList(@Body rpmDevices: RpmDevices): Response<TableDataInfo<RpmDevices>>
    
    /**
     * 添加新设备
     */
    @POST("system/devices/client-add")
    suspend fun addDevice(@Body rpmDevices: RpmDevices): Response<AjaxResult>
    
    /**
     * 更新设备信息
     */
    @POST("system/devices/client-edit")
    suspend fun updateDevice(@Body rpmDevices: RpmDevices): Response<AjaxResult>
    
    /**
     * 更新设备在线状态（使用client-edit接口）
     */
    @POST("system/devices/client-edit")
    suspend fun updateDeviceStatus(@Body rpmDevices: RpmDevices): Response<AjaxResult>
} 