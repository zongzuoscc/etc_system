package com.cumt.lyz.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cumt.lyz.backend.pojo.EtcData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface EtcMapper extends BaseMapper<EtcData> {

    // 如果有些复杂的聚合统计 MyCat 支持不好，可以在这里写原生 SQL
    // 例如：统计各行政区的车流量
    @Select("SELECT district_name as name, COUNT(*) as value FROM etc_data GROUP BY district_name")
    List<Map<String, Object>> countByDistrict();
}