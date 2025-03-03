package com.ruoyi.project.system.devices.mapper;

import java.util.List;
import com.ruoyi.project.system.devices.domain.RpmDevices;

/**
 * 存储用户的设备信息Mapper接口
 * 
 * @author Lan
 * @date 2025-02-26
 */
public interface RpmDevicesMapper 
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
     * 查询所有存储用户的设备信息列表
     * @return 存储用户的设备信息集合
     */
    public List<RpmDevices> selectAllRpmDevicesList();


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
     * 删除存储用户的设备信息
     * 
     * @param id 存储用户的设备信息主键
     * @return 结果
     */
    public int deleteRpmDevicesById(Long id);

    /**
     * 批量删除存储用户的设备信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteRpmDevicesByIds(String[] ids);

    /**
     * 根据uuid查询设备信息
     * @param uuid  uuid
     * @return
     */
    public RpmDevices selectRpmDevicesByUuid(String uuid);
}
