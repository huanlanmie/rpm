package com.ruoyi.project.system.devices.controller;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.project.system.devices.domain.RpmDevices;
import com.ruoyi.project.system.devices.service.IRpmDevicesService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 存储用户的设备信息Controller
 * 
 * @author Lan
 * @date 2025-02-26
 */
@Controller
@RequestMapping("/system/devices")
public class RpmDevicesController extends BaseController
{
    private String prefix = "system/devices";

    @Autowired
    private IRpmDevicesService rpmDevicesService;

    @RequiresPermissions("system:devices:view")
    @GetMapping()
    public String devices()
    {
        return prefix + "/devices";
    }

    /**
     * 查询存储用户的设备信息列表
     */
    @RequiresPermissions("system:devices:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(RpmDevices rpmDevices)
    {
        startPage();
        List<RpmDevices> list = rpmDevicesService.selectRpmDevicesList(rpmDevices);
        return getDataTable(list);
    }

    /**
     * 导出存储用户的设备信息列表
     */
    @RequiresPermissions("system:devices:export")
    @Log(title = "存储用户的设备信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(RpmDevices rpmDevices)
    {
        List<RpmDevices> list = rpmDevicesService.selectRpmDevicesList(rpmDevices);
        ExcelUtil<RpmDevices> util = new ExcelUtil<RpmDevices>(RpmDevices.class);
        return util.exportExcel(list, "存储用户的设备信息数据");
    }

    /**
     * 新增存储用户的设备信息
     */
    @RequiresPermissions("system:devices:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存存储用户的设备信息
     */
    @RequiresPermissions("system:devices:add")
    @Log(title = "存储用户的设备信息", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(RpmDevices rpmDevices)
    {
        return toAjax(rpmDevicesService.insertRpmDevices(rpmDevices));
    }

    /**
     * 修改存储用户的设备信息
     */
    @RequiresPermissions("system:devices:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap)
    {
        RpmDevices rpmDevices = rpmDevicesService.selectRpmDevicesById(id);
        mmap.put("rpmDevices", rpmDevices);
        return prefix + "/edit";
    }

    /**
     * 修改保存存储用户的设备信息
     */
    @RequiresPermissions("system:devices:edit")
    @Log(title = "存储用户的设备信息", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(RpmDevices rpmDevices)
    {
        return toAjax(rpmDevicesService.updateRpmDevices(rpmDevices));
    }

    /**
     * 删除存储用户的设备信息
     */
    @RequiresPermissions("system:devices:remove")
    @Log(title = "存储用户的设备信息", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(rpmDevicesService.deleteRpmDevicesByIds(ids));
    }
}
