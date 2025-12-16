package com.cumt.lyz.backend.controller;

import com.cumt.lyz.backend.common.Result;
import com.cumt.lyz.backend.service.FakeVehicleDetectionService;
import com.cumt.lyz.backend.service.FakeVehicleDetectionService.FakeVehicleAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 套牌车告警API
 */
@RestController
@RequestMapping("/api/fake-vehicle")
public class FakeVehicleController {

    @Autowired
    private FakeVehicleDetectionService fakeDetector;

    /**
     * 获取套牌车告警列表
     * 
     * @param hours 最近N小时，默认24小时
     * @return 告警列表
     */
    @GetMapping("/alerts")
    public Result<List<FakeVehicleAlert>> getAlerts(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            List<FakeVehicleAlert> alerts = fakeDetector.getRecentAlerts(hours);
            return Result.success(alerts);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.success(new ArrayList<>());
        }
    }
}
