package com.ljh.phonemanage.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * 锁定信息对象
 */
data class RpmLockevents(
    /** 锁定事件ID */
    val id: Long? = null,
    
    /** 关联设备ID */
    @SerializedName("deviceId")
    val deviceId: String? = null,
    
    /** 锁定代码（6位数密码） */
    @SerializedName("lockCode")
    val lockCode: String? = null,
    
    /** 锁定时间 */
    @SerializedName("lockedAt")
    val lockedAt: Date? = null,
    
    // 基础字段
    val createBy: String? = null,
    val createTime: Date? = null,
    val updateBy: String? = null,
    val updateTime: Date? = null,
    val remark: String? = null
) 