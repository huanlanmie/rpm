package com.ruoyi.project.system.devices.service;

import java.util.List;
import com.ruoyi.project.system.devices.domain.RpmDevices;

/**
 * 存储用户的设备信息Service接口
 * 
 * @author Lan
 * @date 2025-02-26
 */
public interface IRpmDevicesService 
{
    /**
     * 查询存储用户的设备信息
     * 
     * @param id 存储用户的设备信息主键
     * @return 存储用户的设备信息
     */
    public RpmDevices selectRpmDevicesById(Long id);

    /**
     * 查询存储用户的设备信息列表
     * 
     * @param rpmDevices 存储用户的设备信息
     * @return 存储用户的设备信息集合
     */
    public List<RpmDevices> selectRpmDevicesList(RpmDevices rpmDevices);

    /**
     * 查询存储用户的设备信息列表
     * @return 存储用户的设备信息集合
     */
    public  List<RpmDevices> selectRpmDevicesList();

    /**
     * 新增存储用户的设备信息
     * 
     * @param rpmDevices 存储用户的设备信息
     * @return 结果
     */
    public int insertRpmDevices(RpmDevices rpmDevices);

    /**
     * 修改存储用户的设备信息
     * 
     * @param rpmDevices 存储用户的设备信息
     * @return 结果
     */
    public int updateRpmDevices(RpmDevices rpmDevices);


    /**
     * 紧急解锁设备
     *
     * @param rpmDevices 存储用户的设备信息
     * @return 结果
     */
    public int exigencyUnlockDevices(RpmDevices rpmDevices);

    /**
     * 批量删除存储用户的设备信息
     * 
     * @param ids 需要删除的存储用户的设备信息主键集合
     * @return 结果
     */
    public int deleteRpmDevicesByIds(String ids);

    /**
     * 删除存储用户的设备信息信息
     * 
     * @param id 存储用户的设备信息主键
     * @return 结果
     */
    public int deleteRpmDevicesById(Long id);

    /**
     * 根据uuid查询设备信息
     */
    public RpmDevices selectRpmDevicesByUuid(String uuid);
}
