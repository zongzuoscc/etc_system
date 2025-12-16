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
        // 1. 构建分页对象
        Page<EtcData> pageParam = new Page<>(page, size);

        // 2. 构建查询条件 (业务逻辑在这里)
        QueryWrapper<EtcData> query = new QueryWrapper<>();
        if (plateNumber != null && !plateNumber.isEmpty()) {
            query.like("plate_number", plateNumber);
        }
        query.orderByDesc("pass_time"); // 默认按时间倒序

        // 3. 执行查询
        return this.page(pageParam, query);
    }

    @Override
    public List<EtcData> getRealTimeTrend() {
        // 获取最新的 10 条数据用于展示趋势
        Page<EtcData> page = new Page<>(1, 10);
        QueryWrapper<EtcData> query = new QueryWrapper<>();
        query.orderByDesc("pass_time");

        // 只取 records 部分返回
        return this.page(page, query).getRecords();
    }

    @Override
    public List<Map<String, Object>> getDistrictStats() {
        // 直接调用 Mapper 的自定义 SQL
        return baseMapper.countByDistrict();
    }
}