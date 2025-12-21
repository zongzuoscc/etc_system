package com.cumt.lyz.backend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cumt.lyz.backend.mapper.PredictionMapper;
import com.cumt.lyz.backend.pojo.PredictionData;
import com.cumt.lyz.backend.service.PredictionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PredictionServiceImpl extends ServiceImpl<PredictionMapper, PredictionData> implements PredictionService {

    @Override
    public List<Map<String, Object>> getCityTotalTrend() {
        // 调用 Mapper 手写的聚合 SQL
        return baseMapper.getCityTotalPrediction();
    }

    @Override
    public List<PredictionData> getDistrictTrend(String districtName) {
        QueryWrapper<PredictionData> query = new QueryWrapper<>();
        if (districtName != null && !districtName.isEmpty()) {
            query.eq("district_name", districtName);
        }
        query.orderByAsc("predict_time"); // 按时间正序
        return this.list(query);
    }
}