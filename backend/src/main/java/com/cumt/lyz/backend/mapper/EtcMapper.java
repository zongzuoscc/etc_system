package com.cumt.lyz.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cumt.lyz.backend.pojo.EtcData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface EtcMapper extends BaseMapper<EtcData> {

    // 1. 各行政区总量统计 (保持不变)
    @Select("SELECT district_name as name, COUNT(*) as value FROM etc_data GROUP BY district_name")
    List<Map<String, Object>> countByDistrict();

    /**
     * 2. 【新增】实时趋势统计 API
     * 逻辑：
     * 1. 筛选最近 1 小时的数据 (pass_time >= NOW() - 1 hour)
     * 2. 把时间格式化为 "HH:mm" (例如 10:05, 10:06)
     * 3. 按分钟分组 (GROUP BY)
     * 4. 统计每分钟的数量 (COUNT)
     */
    @Select("SELECT DATE_FORMAT(pass_time, '%H:%i') as time_str, COUNT(*) as vehicle_count " +
            "FROM etc_data " +
            "WHERE pass_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR) " +
            "GROUP BY time_str " +
            "ORDER BY time_str ASC")
    List<Map<String, Object>> getRealTimeTrendAggregation();
}