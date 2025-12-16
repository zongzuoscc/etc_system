/**
 * ETC数据管理控制器
 * 提供ETC数据的查询、统计和接收功能
 * 包含大屏可视化所需的各类统计接口
 *
 * @author cumt.lyz
 * @since 2024
 */
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

/**
 * ETC控制器类
 * 使用Swagger注解提供API文档说明
 * 基于RESTful风格设计接口
 *
 * @Api: Swagger注解，用于描述控制器的作用和标签
 * @RestController: Spring注解，标识这是一个REST控制器，返回JSON数据
 * @RequestMapping: 基础路径映射，所有接口都以"/etc"开头
 */
@Api(tags = "ETC数据管理与大屏接口")
@RestController
@RequestMapping("/etc")
public class EtcController {

    /**
     * ETC业务服务层
     * 使用Spring的依赖注入自动装配
     * 负责具体的业务逻辑处理
     */
    @Autowired
    private EtcService etcService;

    // --- 数据查询类接口 ---

    /**
     * 分页查询历史数据接口
     * 支持按车牌号模糊查询和分页展示
     * @GetMapping: HTTP GET请求映射，路径为"/etc/list"
     *
     * @param page 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param plateNumber 车牌号查询条件，可选参数
     * @return 封装了分页数据的统一响应结果
     */
    @ApiOperation("分页查询历史数据")
    @GetMapping("/list")
    public Result<Page<EtcData>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String plateNumber) {
        // 逻辑下沉到 Service，Controller 只负责传递参数
        Page<EtcData> result = etcService.getHistoryList(page, size, plateNumber);
        return Result.success(result);
    }

    // --- 大屏可视化统计接口 ---

    /**
     * 获取总车流量统计
     * 用于大屏展示总车流量数据
     *
     * @ApiOperation: 接口功能描述
     * @GetMapping: HTTP GET请求映射，路径为"/etc/stats/total"
     *
     * @return 封装了总车流量的统一响应结果
     */
    @ApiOperation("大屏：获取总车流量")
    @GetMapping("/stats/total")
    public Result<Long> getTotalFlow() {
        long count = etcService.count(); // MyBatis-Plus 原生支持
        return Result.success(count);
    }

    /**
     * 获取各行政区车流占比统计
     * 用于大屏饼图展示各区域车流分布情况
     *
     * @ApiOperation: 接口功能描述
     * @GetMapping: HTTP GET请求映射，路径为"/etc/stats/district"
     *
     * @return 封装了各行政区统计数据的统一响应结果
     *         数据格式为List<Map<String, Object>>，包含区域名称和对应的流量数据
     */
    @ApiOperation("大屏：获取各行政区车流占比(饼图)")
    @GetMapping("/stats/district")
    public Result<List<Map<String, Object>>> getDistrictStats() {
        return Result.success(etcService.getDistrictStats());
    }

    /**
     * 获取实时趋势数据
     * 用于大屏展示最近10条ETC通行记录
     *
     * @ApiOperation: 接口功能描述
     * @GetMapping: HTTP GET请求映射，路径为"/etc/stats/trend"
     *
     * @return 封装了实时趋势数据的统一响应结果
     *         返回最近10条ETC数据记录
     */
    @ApiOperation("大屏：获取实时趋势(近10条)")
    @GetMapping("/stats/trend")
    public Result<List<EtcData>> getTrend() {
        return Result.success(etcService.getRealTimeTrend());
    }

    // --- (可选) 数据接收接口 ---
    // 如果队友的 Flink/Spark 需要通过 HTTP 接口推数据给你，可以用这个

    /**
     * 接收外部数据推送接口
     * 用于接收来自Flink/Spark等外部系统的ETC数据
     * 支持通过HTTP POST方式推送单条数据
     *
     * @ApiOperation: 接口功能描述
     * @PostMapping: HTTP POST请求映射，路径为"/etc/push"
     * @RequestBody: 请求体参数绑定，将JSON数据转换为EtcData对象
     *
     * @param data 推送的ETC数据对象
     * @return 封装了保存结果的统一响应结果
     *         成功返回true，失败返回false
     */
    @ApiOperation("接收外部数据推送")
    @PostMapping("/push")
    public Result<Boolean> pushData(@RequestBody EtcData data) {
        boolean success = etcService.save(data);
        return Result.success(success);
    }
}
