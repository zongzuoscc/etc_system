package com.cumt.lyz.backend.controller;

import com.cumt.lyz.backend.common.Result;
import com.cumt.lyz.backend.pojo.PredictionData;
import com.cumt.lyz.backend.service.PredictionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "离线流量预测接口")
@RestController
@RequestMapping("/api/prediction")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    /**
     * 获取全市流量预测趋势 (用于大屏总览)
     * 返回结构示例: [{"predict_time": "2023-12-20 10:00:00", "total_volume": 5000}, ...]
     */
    @ApiOperation("获取全市预测趋势")
    @GetMapping("/city-trend")
    public Result<List<Map<String, Object>>> getCityTrend() {
        return Result.success(predictionService.getCityTotalTrend());
    }

    /**
     * 获取指定区域的预测数据 (用于下钻分析)
     * 参数 districtName 可选，不传则查所有
     */
    @ApiOperation("获取区域预测数据")
    @GetMapping("/district-trend")
    public Result<List<PredictionData>> getDistrictTrend(
            @RequestParam(required = false) String districtName) {
        return Result.success(predictionService.getDistrictTrend(districtName));
    }
}