package com.cumt.lyz.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cumt.lyz.backend.mapper.EtcMapper;
import com.cumt.lyz.backend.pojo.EtcData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 套牌车检测服务（降低阈值展示容错）
 */
@Service
public class FakeVehicleDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FakeVehicleDetectionService.class);

    @Autowired
    private EtcMapper etcMapper;

    // 降低阈值以展示容错性
    @Value("${fake.detection.max-speed:120}")
    private double maxSpeedKmh = 120.0; // 从150降到120

    @Value("${fake.detection.time-window:10}")
    private int timeWindowMinutes = 10; // 从5增到10

    @Value("${fake.detection.min-distance:30}")
    private double minDistanceKm = 30.0; // 从50降到30

    /**
     * 检测套牌车
     * 
     * @param newRecord 新通行记录
     * @return 套牌告警，null表示正常
     */
    public FakeVehicleAlert detectFakePlate(EtcData newRecord) {
        // 必须有经纬度
        if (newRecord.getLatitude() == null || newRecord.getLongitude() == null) {
            return null;
        }

        // 查询该车牌最近N分钟的记录
        LocalDateTime startTime = toLocalDateTime(newRecord.getPassTime())
                .minusMinutes(timeWindowMinutes);

        QueryWrapper<EtcData> query = new QueryWrapper<>();
        query.eq("plate_number", newRecord.getPlateNumber())
                .ge("pass_time", toDate(startTime))
                .isNotNull("latitude")
                .isNotNull("longitude")
                .orderByDesc("pass_time");

        List<EtcData> recentRecords = etcMapper.selectList(query);

        // 遍历检查
        for (EtcData oldRecord : recentRecords) {
            // 跳过同一卡口
            if (oldRecord.getBayonetName().equals(newRecord.getBayonetName())) {
                continue;
            }

            // 计算距离
            double distance = calculateDistance(
                    oldRecord.getLatitude(), oldRecord.getLongitude(),
                    newRecord.getLatitude(), newRecord.getLongitude());

            // 距离太近，跳过
            if (distance < minDistanceKm) {
                continue;
            }

            // 计算时间差（分钟）
            long timeDiff = Duration.between(
                    toLocalDateTime(oldRecord.getPassTime()),
                    toLocalDateTime(newRecord.getPassTime())).toMinutes();

            if (timeDiff == 0) {
                timeDiff = 1; // 避免除零
            }

            // 计算所需速度
            double requiredSpeed = distance / (timeDiff / 60.0);

            // 判断是否超速
            if (requiredSpeed > maxSpeedKmh) {
                String level = getAlertLevel(requiredSpeed);

                log.warn("⚠️ 套牌嫌疑: {} - {}km in {}min = {}km/h ({})",
                        newRecord.getPlateNumber(),
                        String.format("%.1f", distance),
                        timeDiff,
                        String.format("%.0f", requiredSpeed),
                        level);

                return new FakeVehicleAlert(
                        newRecord.getPlateNumber(),
                        oldRecord,
                        newRecord,
                        distance,
                        timeDiff,
                        requiredSpeed,
                        level);
            }
        }

        return null;
    }

    /**
     * Haversine公式计算地球表面两点距离
     * 
     * @return 距离（公里）
     */
    private double calculateDistance(double lat1, double lon1,
            double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                        Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 获取告警级别
     */
    private String getAlertLevel(double speed) {
        if (speed > 300)
            return "high"; // 严重
        if (speed > 200)
            return "medium"; // 中等
        return "low"; // 轻微
    }

    /**
     * 获取最近N小时的所有告警
     */
    public List<FakeVehicleAlert> getRecentAlerts(int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);

        QueryWrapper<EtcData> query = new QueryWrapper<>();
        query.ge("pass_time", toDate(startTime))
                .isNotNull("latitude")
                .isNotNull("longitude")
                .orderByDesc("pass_time");

        List<EtcData> records = etcMapper.selectList(query);
        List<FakeVehicleAlert> alerts = new ArrayList<>();

        for (EtcData record : records) {
            FakeVehicleAlert alert = detectFakePlate(record);
            if (alert != null) {
                alerts.add(alert);
            }
        }

        return alerts;
    }

    // 时间转换工具
    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 套牌车告警DTO
     */
    public static class FakeVehicleAlert {
        private String plateNumber;
        private EtcData firstRecord;
        private EtcData secondRecord;
        private double distance;
        private long timeDiff;
        private double requiredSpeed;
        private String alertLevel;

        public FakeVehicleAlert(String plateNumber, EtcData first, EtcData second,
                double distance, long timeDiff, double speed, String level) {
            this.plateNumber = plateNumber;
            this.firstRecord = first;
            this.secondRecord = second;
            this.distance = distance;
            this.timeDiff = timeDiff;
            this.requiredSpeed = speed;
            this.alertLevel = level;
        }

        // Getters
        public String getPlateNumber() {
            return plateNumber;
        }

        public EtcData getFirstRecord() {
            return firstRecord;
        }

        public EtcData getSecondRecord() {
            return secondRecord;
        }

        public double getDistance() {
            return distance;
        }

        public long getTimeDiff() {
            return timeDiff;
        }

        public double getRequiredSpeed() {
            return requiredSpeed;
        }

        public String getAlertLevel() {
            return alertLevel;
        }

        public String getAlertMessage() {
            return String.format(
                    "⚠️ %s在%d分钟内从%s到%s，距离%.1fkm，需要速度%.0fkm/h",
                    plateNumber, timeDiff,
                    firstRecord.getBayonetName(),
                    secondRecord.getBayonetName(),
                    distance, requiredSpeed);
        }
    }
}
