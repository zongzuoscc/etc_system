package com.cumt.lyz.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cumt.lyz.backend.common.Result;
import com.cumt.lyz.backend.mapper.EtcMapper;
import com.cumt.lyz.backend.pojo.EtcData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 流量统计API（简化版 - 仅地区统计）
 */
@RestController
@RequestMapping("/api/flow")
public class FlowController {

    @Autowired
    private EtcMapper etcMapper;

    /**
     * 获取地区流量统计
     * 
     * @param timeRange 时间范围: 1h/24h/7d
     * @return 各地区流量统计
     */
    @GetMapping("/district-traffic")
    public Result<Map<String, Object>> getDistrictTraffic(
            @RequestParam(defaultValue = "1h") String timeRange) {
        try {
            int hours = parseTimeRange(timeRange);
            LocalDateTime startTime = LocalDateTime.now().minusHours(hours);

            // 查询指定时间范围内的数据
            QueryWrapper<EtcData> query = new QueryWrapper<>();
            query.ge("pass_time", toDate(startTime))
                    .orderByDesc("pass_time");

            List<EtcData> records = etcMapper.selectList(query);

            // 地区名称归一化映射（数据库名称 → 地图GeoJSON名称）
            Map<String, String> nameNormalizationMap = new HashMap<>();
            nameNormalizationMap.put("铜山县", "铜山区"); // 数据库是铜山县，地图GeoJSON是铜山区
            // 可以添加更多映射，例如：
            // nameNormalizationMap.put("沛县", "沛县");
            // nameNormalizationMap.put("丰县", "丰县");

            // 按地区分组统计流量
            Map<String, Integer> districtVolume = new HashMap<>();
            for (EtcData record : records) {
                String district = record.getDistrictName();
                if (district != null && !district.isEmpty()) {
                    // 归一化地区名称
                    String normalizedName = nameNormalizationMap.getOrDefault(district, district);
                    districtVolume.put(normalizedName, districtVolume.getOrDefault(normalizedName, 0) + 1);
                }
            }

            // 构建返回数据
            List<DistrictStat> districts = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : districtVolume.entrySet()) {
                DistrictStat stat = new DistrictStat();
                stat.name = entry.getKey();
                stat.volume = entry.getValue();
                districts.add(stat);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("districts", districts);
            result.put("totalVolume", records.size());

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.success(new HashMap<>());
        }
    }

    // 工具方法
    private int parseTimeRange(String range) {
        if (range == null)
            return 1;
        if (range.endsWith("h")) {
            return Integer.parseInt(range.substring(0, range.length() - 1));
        }
        if (range.endsWith("d")) {
            return Integer.parseInt(range.substring(0, range.length() - 1)) * 24;
        }
        return 1;
    }

    private Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    // 地区统计DTO
    public static class DistrictStat {
        public String name;
        public int volume;
    }
}
