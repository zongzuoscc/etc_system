package com.cumt.lyz.backend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cumt.lyz.backend.mapper.EtcMapper;
import com.cumt.lyz.backend.pojo.EtcData;
import com.cumt.lyz.backend.service.EtcService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EtcServiceImpl extends ServiceImpl<EtcMapper, EtcData> implements EtcService {

    @Override
    public Page<EtcData> getHistoryList(int page, int size, String plateNumber) {
        Page<EtcData> pageParam = new Page<>(page, size);
        QueryWrapper<EtcData> query = new QueryWrapper<>();
        if (plateNumber != null && !plateNumber.isEmpty()) {
            query.like("plate_number", plateNumber);
        }
        query.orderByDesc("pass_time");
        return this.page(pageParam, query);
    }

    @Override
    public List<Map<String, Object>> getRealTimeTrend() {
        // 【修改】不再查原始记录，而是调用 Mapper 的聚合统计方法
        // 返回格式示例: [{"time_str": "10:01", "vehicle_count": 52}, ...]
        return baseMapper.getRealTimeTrendAggregation();
    }

    @Override
    public List<Map<String, Object>> getDistrictStats() {
        return baseMapper.countByDistrict();
    }

    @Override
    public EtcData getLastRecord(String plateNumber) {
        QueryWrapper<EtcData> query = new QueryWrapper<>();
        query.eq("plate_number", plateNumber)
                .orderByDesc("pass_time")
                .last("LIMIT 1");
        return this.getOne(query);
    }
}