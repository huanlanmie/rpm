package com.ruoyi.project.system.lockevents.service;

import java.util.List;
import com.ruoyi.project.system.lockevents.domain.RpmLockevents;

/**
 * 锁定信息Service接口
 * 
 * @author Lan
 * @date 2025-02-26
 */
public interface IRpmLockeventsService 
{
    /**
     * 查询锁定信息
     * 
     * @param id 锁定信息主键
     * @return 锁定信息
     */
    public RpmLockevents selectRpmLockeventsById(Long id);

    /**
     * 查询锁定信息列表
     * 
     * @param rpmLockevents 锁定信息
     * @return 锁定信息集合
     */
    public List<RpmLockevents> selectRpmLockeventsList(RpmLockevents rpmLockevents);

    /**
     * 新增锁定信息
     * 
     * @param rpmLockevents 锁定信息
     * @return 结果
     */
    public int insertRpmLockevents(RpmLockevents rpmLockevents);

    /**
     * 修改锁定信息
     * 
     * @param rpmLockevents 锁定信息
     * @return 结果
     */
    public int updateRpmLockevents(RpmLockevents rpmLockevents);

    /**
     * 批量删除锁定信息
     * 
     * @param ids 需要删除的锁定信息主键集合
     * @return 结果
     */
    public int deleteRpmLockeventsByIds(String ids);

    /**
     * 删除锁定信息信息
     * 
     * @param id 锁定信息主键
     * @return 结果
     */
    public int deleteRpmLockeventsById(Long id);
}
