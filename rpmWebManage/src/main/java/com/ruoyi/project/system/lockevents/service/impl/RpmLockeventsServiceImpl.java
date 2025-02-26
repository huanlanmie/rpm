package com.ruoyi.project.system.lockevents.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.system.lockevents.mapper.RpmLockeventsMapper;
import com.ruoyi.project.system.lockevents.domain.RpmLockevents;
import com.ruoyi.project.system.lockevents.service.IRpmLockeventsService;
import com.ruoyi.common.utils.text.Convert;

/**
 * 锁定信息Service业务层处理
 * 
 * @author Lan
 * @date 2025-02-26
 */
@Service
public class RpmLockeventsServiceImpl implements IRpmLockeventsService 
{
    @Autowired
    private RpmLockeventsMapper rpmLockeventsMapper;

    /**
     * 查询锁定信息
     * 
     * @param id 锁定信息主键
     * @return 锁定信息
     */
    @Override
    public RpmLockevents selectRpmLockeventsById(Long id)
    {
        return rpmLockeventsMapper.selectRpmLockeventsById(id);
    }

    /**
     * 查询锁定信息列表
     * 
     * @param rpmLockevents 锁定信息
     * @return 锁定信息
     */
    @Override
    public List<RpmLockevents> selectRpmLockeventsList(RpmLockevents rpmLockevents)
    {
        return rpmLockeventsMapper.selectRpmLockeventsList(rpmLockevents);
    }

    /**
     * 新增锁定信息
     * 
     * @param rpmLockevents 锁定信息
     * @return 结果
     */
    @Override
    public int insertRpmLockevents(RpmLockevents rpmLockevents)
    {
        rpmLockevents.setCreateTime(DateUtils.getNowDate());
        return rpmLockeventsMapper.insertRpmLockevents(rpmLockevents);
    }

    /**
     * 修改锁定信息
     * 
     * @param rpmLockevents 锁定信息
     * @return 结果
     */
    @Override
    public int updateRpmLockevents(RpmLockevents rpmLockevents)
    {
        rpmLockevents.setUpdateTime(DateUtils.getNowDate());
        return rpmLockeventsMapper.updateRpmLockevents(rpmLockevents);
    }

    /**
     * 批量删除锁定信息
     * 
     * @param ids 需要删除的锁定信息主键
     * @return 结果
     */
    @Override
    public int deleteRpmLockeventsByIds(String ids)
    {
        return rpmLockeventsMapper.deleteRpmLockeventsByIds(Convert.toStrArray(ids));
    }

    /**
     * 删除锁定信息信息
     * 
     * @param id 锁定信息主键
     * @return 结果
     */
    @Override
    public int deleteRpmLockeventsById(Long id)
    {
        return rpmLockeventsMapper.deleteRpmLockeventsById(id);
    }
}
