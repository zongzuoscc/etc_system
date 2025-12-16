package com.cumt.lyz.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cumt.lyz.backend.pojo.EtcData;

import java.util.List;
import java.util.Map;

public interface EtcService extends IService<EtcData> {

    /**
     * 业务查询：分页获取 ETC 记录
     * @param page 页码
     * @param size 每页条数
     * @param plateNumber 车牌号 (可选，模糊查询)
     * @return 分页结果
     */
    Page<EtcData> getHistoryList(int page, int size, String plateNumber);

    /**
     * 业务统计：获取大屏实时趋势数据
     * @return 最近的 10 条记录
     */
    List<EtcData> getRealTimeTrend();

    /**
     * 业务统计：获取各行政区车流量占比
     */
    List<Map<String, Object>> getDistrictStats();
}