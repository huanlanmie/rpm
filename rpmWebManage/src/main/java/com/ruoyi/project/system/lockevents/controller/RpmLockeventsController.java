package com.ruoyi.project.system.lockevents.controller;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.project.system.lockevents.domain.RpmLockevents;
import com.ruoyi.project.system.lockevents.service.IRpmLockeventsService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 锁定信息Controller
 * 
 * @author Lan
 * @date 2025-02-26
 */
@Controller
@RequestMapping("/system/lockevents")
public class RpmLockeventsController extends BaseController
{
    private String prefix = "system/lockevents";

    @Autowired
    private IRpmLockeventsService rpmLockeventsService;

    @RequiresPermissions("system:lockevents:view")
    @GetMapping()
    public String lockevents()
    {
        return prefix + "/lockevents";
    }

    /**
     * 查询锁定信息列表
     */
    @RequiresPermissions("system:lockevents:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(RpmLockevents rpmLockevents)
    {
        startPage();
        List<RpmLockevents> list = rpmLockeventsService.selectRpmLockeventsList(rpmLockevents);
        // 按照时间倒序
        list.sort((o1, o2) -> o2.getLockedAt().compareTo(o1.getLockedAt()));
        return getDataTable(list);
    }

    /**
     * 导出锁定信息列表
     */
    @RequiresPermissions("system:lockevents:export")
    @Log(title = "锁定信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(RpmLockevents rpmLockevents)
    {
        List<RpmLockevents> list = rpmLockeventsService.selectRpmLockeventsList(rpmLockevents);
        ExcelUtil<RpmLockevents> util = new ExcelUtil<RpmLockevents>(RpmLockevents.class);
        return util.exportExcel(list, "锁定信息数据");
    }

    /**
     * 新增锁定信息
     */
    @RequiresPermissions("system:lockevents:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存锁定信息
     */
    @RequiresPermissions("system:lockevents:add")
    @Log(title = "锁定信息", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(RpmLockevents rpmLockevents)
    {
        return toAjax(rpmLockeventsService.insertRpmLockevents(rpmLockevents));
    }

    /**
     * 新增保存锁定信息
     */
    @Log(title = "客户端上传锁定信息", businessType = BusinessType.INSERT)
    @PostMapping("/client-add")
    @ResponseBody
    public AjaxResult clientAddSave(@RequestBody RpmLockevents rpmLockevents)
    {
        return toAjax(rpmLockeventsService.insertRpmLockevents(rpmLockevents));
    }

    /**
     * 修改锁定信息
     */
    @RequiresPermissions("system:lockevents:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap)
    {
        RpmLockevents rpmLockevents = rpmLockeventsService.selectRpmLockeventsById(id);
        mmap.put("rpmLockevents", rpmLockevents);
        return prefix + "/edit";
    }

    /**
     * 修改保存锁定信息
     */
    @RequiresPermissions("system:lockevents:edit")
    @Log(title = "锁定信息", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(RpmLockevents rpmLockevents)
    {
        return toAjax(rpmLockeventsService.updateRpmLockevents(rpmLockevents));
    }

    /**
     * 删除锁定信息
     */
    @RequiresPermissions("system:lockevents:remove")
    @Log(title = "锁定信息", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(rpmLockeventsService.deleteRpmLockeventsByIds(ids));
    }
}
