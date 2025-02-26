package com.ruoyi.project.system.devices.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 存储用户的设备信息对象 rpm_devices
 * 
 * @author Lan
 * @date 2025-02-26
 */
public class RpmDevices extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long id;

    /** 关联用户ID，默认为1 */
    @Excel(name = "关联用户ID，默认为1")
    private Long userId;

    /** 设备唯一标识符 */
    @Excel(name = "设备唯一标识符")
    private String deviceToken;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String deviceName;

    /** 设备状态 */
    @Excel(name = "设备状态")
    private Long deviceStatus;

    /** 操作系统版本 */
    @Excel(name = "操作系统版本")
    private String osVersion;

    /** 应用版本 */
    @Excel(name = "应用版本")
    private String appVersion;

    /** 在线状态 */
    @Excel(name = "在线状态")
    private String status;

    /** 最近一次在线时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "最近一次在线时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeen;


    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setDeviceToken(String deviceToken) 
    {
        this.deviceToken = deviceToken;
    }

    public String getDeviceToken() 
    {
        return deviceToken;
    }

    public void setDeviceName(String deviceName) 
    {
        this.deviceName = deviceName;
    }

    public String getDeviceName() 
    {
        return deviceName;
    }

    public void setDeviceStatus(Long deviceStatus) 
    {
        this.deviceStatus = deviceStatus;
    }

    public Long getDeviceStatus() 
    {
        return deviceStatus;
    }

    public void setOsVersion(String osVersion) 
    {
        this.osVersion = osVersion;
    }

    public String getOsVersion() 
    {
        return osVersion;
    }

    public void setAppVersion(String appVersion) 
    {
        this.appVersion = appVersion;
    }

    public String getAppVersion() 
    {
        return appVersion;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setLastSeen(Date lastSeen) 
    {
        this.lastSeen = lastSeen;
    }

    public Date getLastSeen() 
    {
        return lastSeen;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("deviceToken", getDeviceToken())
            .append("deviceName", getDeviceName())
            .append("deviceStatus", getDeviceStatus())
            .append("osVersion", getOsVersion())
            .append("appVersion", getAppVersion())
            .append("status", getStatus())
            .append("lastSeen", getLastSeen())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
