package com.ljh.phonemanage.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * 存储用户的设备信息对象 rpm_devices
 * 与后端实体保持一致
 */
data class RpmDevices(
    /** 设备ID */
    @SerializedName("id")
    val id: Long? = null,

    /** 关联用户ID，默认为1 */
    @SerializedName("userId")
    val userId: Long? = 1,

    /** 设备唯一标识符 */
    @SerializedName("deviceToken")
    val deviceToken: String,

    /** 设备名称 */
    @SerializedName("deviceName")
    val deviceName: String,

    /** 设备状态 */
    @SerializedName("deviceStatus")
    val deviceStatus: Long? = 0,

    /** 操作系统版本 */
    @SerializedName("osVersion")
    val osVersion: String,

    /** 应用版本 */
    @SerializedName("appVersion")
    val appVersion: String,

    /** 在线状态 */
    @SerializedName("status")
    val status: String? = "online",

    /** 最近一次在线时间 */
    @SerializedName("lastSeen")
    val lastSeen: Date? = Date(),

    /** 创建者 */
    @SerializedName("createBy")
    val createBy: String? = "",

    /** 创建时间 */
    @SerializedName("createTime")
    val createTime: Date? = null,

    /** 更新者 */
    @SerializedName("updateBy")
    val updateBy: String? = "",

    /** 更新时间 */
    @SerializedName("updateTime")
    val updateTime: Date? = null,

    /** 备注 */
    @SerializedName("remark")
    val remark: String? = ""
) 