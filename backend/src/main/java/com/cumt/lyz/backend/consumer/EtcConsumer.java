package com.cumt.lyz.backend.consumer;

import com.cumt.lyz.backend.pojo.EtcData;
import com.cumt.lyz.backend.service.EtcService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ETC 数据消费者
 * 职责：只负责接收 Kafka 的原始数据并存入数据库 (etc_data 表)
 * 变化：套牌车检测逻辑已剥离给 Spark 引擎处理
 */
@Component
public class EtcConsumer {

    @Autowired
    private EtcService etcService;

    // ❌ 已移除：private FakeVehicleDetectionService detectionService;
    // 原因：检测任务已交给 Spark，后端不再重复计算

    private final Gson gson = new Gson();

    private Date parseTime(String timeStr) {
        try {
            if (timeStr == null) return new Date();
            String cleanTime = timeStr.replace("T", " ");
            if (cleanTime.length() > 19) {
                cleanTime = cleanTime.substring(0, 19);
            }
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(cleanTime);
        } catch (Exception e) {
            return new Date();
        }
    }

    @KafkaListener(topics = "etc_traffic", groupId = "etc-backend-group")
    public void consume(String message) {
        try {
            // 1. 解析 JSON
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            EtcData data = new EtcData();

            // 2. 基础字段映射
            if (json.has("XZQHMC")) data.setDistrictName(json.get("XZQHMC").getAsString());
            if (json.has("KKMC")) data.setBayonetName(json.get("KKMC").getAsString());
            if (json.has("FXLX")) data.setDirectionType(json.get("FXLX").getAsString());
            if (json.has("HPZL")) data.setPlateType(json.get("HPZL").getAsString());
            if (json.has("HPHM")) data.setPlateNumber(json.get("HPHM").getAsString());
            if (json.has("CLPPXH")) data.setVehicleModel(json.get("CLPPXH").getAsString());

            // 时间处理
            if (json.has("GCSJ")) {
                data.setPassTime(parseTime(json.get("GCSJ").getAsString()));
            } else {
                data.setPassTime(new Date());
            }

            // 3. 经纬度映射 (重要！确保前端地图能显示)
            if (json.has("JINGDU")) {
                try {
                    data.setLongitude(json.get("JINGDU").getAsDouble());
                } catch (Exception e) { /* 忽略格式错误 */ }
            }
            if (json.has("WEIDU")) {
                try {
                    data.setLatitude(json.get("WEIDU").getAsDouble());
                } catch (Exception e) { /* 忽略格式错误 */ }
            }

            // 4. 【逻辑变更】这里不再进行套牌检测
            // Spark Streaming 会独立监听 "etc_traffic" 主题，
            // 发现异常后直接写入 "fake_vehicle_alert" 数据库表。
            // 这样的架构实现了"计算"与"存储"的分离，性能更高。

            // 5. 存入数据库 (只存原始流水)
            etcService.save(data);

        } catch (Exception e) {
            System.err.println("数据处理异常: " + e.getMessage());
        }
    }
}