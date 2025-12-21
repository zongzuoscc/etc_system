package com.cumt.lyz.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cumt.lyz.backend.common.Result;
import com.cumt.lyz.backend.pojo.EtcData;
import com.cumt.lyz.backend.service.EtcService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "ETC数据管理与大屏接口")
@RestController
@RequestMapping("/etc")
public class EtcController {

    @Autowired
    private EtcService etcService;

    // --- 数据查询类接口 ---

    @ApiOperation("分页查询历史数据")
    @GetMapping("/list")
    public Result<Page<EtcData>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String plateNumber) {
        Page<EtcData> result = etcService.getHistoryList(page, size, plateNumber);
        return Result.success(result);
    }

    // --- 大屏可视化统计接口 ---

    @ApiOperation("大屏：获取总车流量")
    @GetMapping("/stats/total")
    public Result<Long> getTotalFlow() {
        long count = etcService.count();
        return Result.success(count);
    }

    @ApiOperation("大屏：获取各行政区车流占比(饼图)")
    @GetMapping("/stats/district")
    public Result<List<Map<String, Object>>> getDistrictStats() {
        return Result.success(etcService.getDistrictStats());
    }

    /**
     * 【修改】获取实时趋势数据 (聚合版)
     * 返回结构：[{"time_str": "21:30", "vehicle_count": 55}, {"time_str": "21:31", "vehicle_count": 80}]
     */
    @ApiOperation("大屏：获取实时趋势(每分钟聚合)")
    @GetMapping("/stats/trend")
    public Result<List<Map<String, Object>>> getTrend() {
        // 这里返回类型变了，从 List<EtcData> 变成了 List<Map<...>>
        return Result.success(etcService.getRealTimeTrend());
    }

    // --- (可选) 数据接收接口 ---

    @ApiOperation("接收外部数据推送")
    @PostMapping("/push")
    public Result<Boolean> pushData(@RequestBody EtcData data) {
        boolean success = etcService.save(data);
        return Result.success(success);
    }
}