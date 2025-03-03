package com.ruoyi.framework.task;

import com.ruoyi.project.system.devices.domain.RpmDevices;
import com.ruoyi.project.system.devices.service.IRpmDevicesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author lan
 * @version 1.0
 * @data 2025/3/3 20:15
 */

@Component("deviceTask")
public class DeviceTask {

    @Autowired
    private IRpmDevicesService rpmDevicesService;

    // 获取设备列表 根据设备最后在线时间 最后上报时间 和现在时间间隔超过20分钟 则调整设备状态为离线
    public void resetDeviceStatus() {
        List<RpmDevices> rpmDevicesList = rpmDevicesService.selectRpmDevicesList();
        for (RpmDevices rpmDevices : rpmDevicesList) {
            if (rpmDevices.getLastSeen() != null) {
                long time = System.currentTimeMillis() - rpmDevices.getLastSeen().getTime();
                if (time > 20 * 60 * 1000) {
                    rpmDevices.setStatus(0L);
                    rpmDevicesService.updateRpmDevices(rpmDevices);
                }
            }
        }
    }
}
