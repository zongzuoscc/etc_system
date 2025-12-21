package com.cumt.lyz.backend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cumt.lyz.backend.mapper.PredictionMapper;
import com.cumt.lyz.backend.pojo.PredictionData;
import com.cumt.lyz.backend.service.PredictionService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

    // ========== 【核心】调用 Python 脚本并等待结果 ==========

    @Override
    public boolean runAnalysisTask() {
        try {
            System.out.println("🐍 [Java] 正在唤醒 Python 进行预测计算...");

            // ⚠️⚠️⚠️ 请根据你的实际环境修改路径 ⚠️⚠️⚠️
            // 1. Python 解释器路径 (根据你之前的报错日志填写的)
            String pythonExe = "D:\\Python\\Python311\\python.exe";

            // 2. Python 脚本路径 (Analyze.py)
            String scriptPath = "C:\\Users\\26515\\Desktop\\trafficSystem\\etc_system\\data_generator\\Analyze.py";

            // 构建命令: python.exe Analyze.py
            ProcessBuilder pb = new ProcessBuilder(pythonExe, scriptPath);
            pb.redirectErrorStream(true); // 合并错误输出，方便调试

            // 启动进程
            Process process = pb.start();

            // 读取 Python 的控制台输出 (实时打印，防止假死)
            // 注意：Windows下 Python 输出通常是 GBK 编码
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("🐍 [Python]: " + line);
            }

            // 【关键一步】阻塞等待脚本执行结束
            // 这行代码会让当前请求"卡住"，直到 Python 跑完退出
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ [Java] Python 预测任务执行成功！数据库已更新。");
                return true;
            } else {
                System.err.println("❌ [Java] Python 脚本异常退出，退出码: " + exitCode);
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ [Java] 调用 Python 脚本失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}