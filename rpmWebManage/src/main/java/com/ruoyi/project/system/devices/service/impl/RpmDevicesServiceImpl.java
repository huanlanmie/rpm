package com.ruoyi.project.system.devices.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.system.devices.mapper.RpmDevicesMapper;
import com.ruoyi.project.system.devices.domain.RpmDevices;
import com.ruoyi.project.system.devices.service.IRpmDevicesService;
import com.ruoyi.common.utils.text.Convert;

/**
 * 存储用户的设备信息Service业务层处理
 * 
 * @author Lan
 * @date 2025-02-26
 */
@Service
public class RpmDevicesServiceImpl implements IRpmDevicesService 
{
    @Autowired
    private RpmDevicesMapper rpmDevicesMapper;

    /**
     * 查询存储用户的设备信息
     * 
     * @param id 存储用户的设备信息主键
     * @return 存储用户的设备信息
     */
    @Override
    public RpmDevices selectRpmDevicesById(Long id)
    {
        return rpmDevicesMapper.selectRpmDevicesById(id);
    }

    /**
     * 查询存储用户的设备信息列表
     * 
     * @param rpmDevices 存储用户的设备信息
     * @return 存储用户的设备信息
     */
    @Override
    public List<RpmDevices> selectRpmDevicesList(RpmDevices rpmDevices)
    {
        return rpmDevicesMapper.selectRpmDevicesList(rpmDevices);
    }

    /**
     * 新增存储用户的设备信息
     * 
     * @param rpmDevices 存储用户的设备信息
     * @return 结果
     */
    @Override
    public int insertRpmDevices(RpmDevices rpmDevices)
    {
        rpmDevices.setCreateTime(DateUtils.getNowDate());
        return rpmDevicesMapper.insertRpmDevices(rpmDevices);
    }

    /**
     * 修改存储用户的设备信息
     * 
     * @param rpmDevices 存储用户的设备信息
     * @return 结果
     */
    @Override
    public int updateRpmDevices(RpmDevices rpmDevices)
    {
        rpmDevices.setUpdateTime(DateUtils.getNowDate());
        return rpmDevicesMapper.updateRpmDevices(rpmDevices);
    }

    /**
     * 批量删除存储用户的设备信息
     * 
     * @param ids 需要删除的存储用户的设备信息主键
     * @return 结果
     */
    @Override
    public int deleteRpmDevicesByIds(String ids)
    {
        return rpmDevicesMapper.deleteRpmDevicesByIds(Convert.toStrArray(ids));
    }

    /**
     * 删除存储用户的设备信息信息
     * 
     * @param id 存储用户的设备信息主键
     * @return 结果
     */
    @Override
    public int deleteRpmDevicesById(Long id)
    {
        return rpmDevicesMapper.deleteRpmDevicesById(id);
    }
}
