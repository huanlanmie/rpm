package com.ruoyi.project.system.lockevents.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 锁定信息对象 rpm_lockevents
 * 
 * @author Lan
 * @date 2025-02-26
 */
public class RpmLockevents extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 锁定事件ID */
    private Long id;

    /** 关联设备ID */
    @Excel(name = "关联设备ID")
    private Long deviceId;

    /** 锁定代码（6位数密码） */
    @Excel(name = "锁定代码", readConverterExp = "6=位数密码")
    private String lockCode;

    /** 锁定时间 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "锁定时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date lockedAt;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setDeviceId(Long deviceId) 
    {
        this.deviceId = deviceId;
    }

    public Long getDeviceId() 
    {
        return deviceId;
    }

    public void setLockCode(String lockCode) 
    {
        this.lockCode = lockCode;
    }

    public String getLockCode() 
    {
        return lockCode;
    }

    public void setLockedAt(Date lockedAt) 
    {
        this.lockedAt = lockedAt;
    }

    public Date getLockedAt() 
    {
        return lockedAt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("deviceId", getDeviceId())
            .append("lockCode", getLockCode())
            .append("lockedAt", getLockedAt())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
