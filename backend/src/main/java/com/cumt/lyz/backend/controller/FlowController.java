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

import java.util.*;

/**
 * 流量统计API（累计总量版）
 */
@RestController
@RequestMapping("/api/flow")
public class FlowController {

    @Autowired
    private EtcMapper etcMapper;

    /**
     * 获取地区流量统计 (查询数据库里的所有数据总量)
     * * @param timeRange 此参数现在仅作为预留，实际逻辑已改为查全量
     * @return 各地区流量统计
     */
    @GetMapping("/district-traffic")
    public Result<Map<String, Object>> getDistrictTraffic(
            @RequestParam(defaultValue = "all") String timeRange) {
        try {
            // 1. 构建查询条件
            QueryWrapper<EtcData> query = new QueryWrapper<>();

            // ❌ 删除时间限制：query.ge("pass_time", toDate(startTime));
            // ✅ 修改为：无条件查询 (即查询全量历史数据)
            // 只是为了不把内存撑爆，可以加一个合理的上限，比如最近 10万 条
            // 如果你想真的查所有，可以把 limit 去掉
            query.orderByDesc("pass_time")
                    .last("LIMIT 100000");

            List<EtcData> records = etcMapper.selectList(query);

            // 2. 地区名称归一化映射
            Map<String, String> nameNormalizationMap = new HashMap<>();
            nameNormalizationMap.put("铜山县", "铜山区");
            // 兜底映射
            nameNormalizationMap.put("市辖区", "铜山区");

            // 3. 统计逻辑 (包含名字清洗)
            Map<String, Integer> districtVolume = new HashMap<>();

            // 为了防止地图某些区因为没数据而不显示，可以先初始化为 0 (可选)
            // String[] allDistricts = {"鼓楼区", "云龙区", "贾汪区", "泉山区", "铜山区", "丰县", "沛县", "睢宁县", "邳州市", "新沂市"};
            // for(String d : allDistricts) districtVolume.put(d, 0);

            for (EtcData record : records) {
                String rawName = record.getDistrictName();
                if (rawName != null && !rawName.isEmpty()) {
                    // 【关键修复】去掉 "徐州市" 前缀
                    // 数据库里是 "徐州市鼓楼区"，地图要 "鼓楼区"
                    String cleanName = rawName.replace("徐州市", "").trim();

                    // 再次进行特殊名映射 (比如 铜山县 -> 铜山区)
                    String finalName = nameNormalizationMap.getOrDefault(cleanName, cleanName);

                    // 统计
                    districtVolume.put(finalName, districtVolume.getOrDefault(finalName, 0) + 1);
                }
            }

            // 4. 构建返回结果
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

    // 内部类 DTO
    public static class DistrictStat {
        public String name;
        public int volume;
    }
}