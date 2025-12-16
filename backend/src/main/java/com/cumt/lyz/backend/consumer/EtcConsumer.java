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

@Component
public class EtcConsumer {

    @Autowired
    private EtcService etcService;

    // 只需要一个普通的 Gson 实例
    private final Gson gson = new Gson();

    // 专门处理 Python 发来的 ISO 时间格式 (例如: 2023-12-14T15:30:00)
    // 注意：SimpleDateFormat 线程不安全，这里简单演示，高并发建议用 LocalTime
    private Date parseTime(String timeStr) {
        try {
            if (timeStr == null) return new Date();
            // 替换掉 ISO 格式里的 'T'，变成标准 SQL 时间格式
            String cleanTime = timeStr.replace("T", " ");
            // 截取前19位 (yyyy-MM-dd HH:mm:ss)，去掉可能存在的毫秒
            if (cleanTime.length() > 19) {
                cleanTime = cleanTime.substring(0, 19);
            }
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(cleanTime);
        } catch (Exception e) {
            return new Date(); // 解析失败就用当前时间
        }
    }

    @KafkaListener(topics = "etc_traffic", groupId = "etc-backend-group")
    public void consume(String message) {
        try {
            // 1. 先解析成通用的 JsonObject (字典)
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();

            // 2. 手动构建 Java 对象 (手动映射：拼音 -> 英文)
            EtcData data = new EtcData();

            // 行政区划 XZQHMC -> districtName
            if (json.has("XZQHMC")) data.setDistrictName(json.get("XZQHMC").getAsString());

            // 卡口名称 KKMC -> bayonetName
            if (json.has("KKMC")) data.setBayonetName(json.get("KKMC").getAsString());

            // 方向类型 FXLX -> directionType
            if (json.has("FXLX")) data.setDirectionType(json.get("FXLX").getAsString());

            // 号牌种类 HPZL -> plateType
            if (json.has("HPZL")) data.setPlateType(json.get("HPZL").getAsString());

            // 车牌号码 HPHM -> plateNumber
            if (json.has("HPHM")) data.setPlateNumber(json.get("HPHM").getAsString());

            // 车辆品牌 CLPPXH -> vehicleModel
            if (json.has("CLPPXH")) data.setVehicleModel(json.get("CLPPXH").getAsString());

            // 过车时间 GCSJ -> passTime (需要特殊处理时间格式)
            if (json.has("GCSJ")) {
                data.setPassTime(parseTime(json.get("GCSJ").getAsString()));
            } else {
                data.setPassTime(new Date());
            }

            // 3. 存入数据库 (MyBatis-Plus 会自动生成 ID)
            etcService.save(data);

            // 调试日志 (可选)
            // System.out.println("成功入库: " + data.getPlateNumber());

        } catch (Exception e) {
            System.err.println("数据转换异常: " + e.getMessage());
        }
    }
}