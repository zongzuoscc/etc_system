package com.cumt.lyz.backend.service;

import com.cumt.lyz.backend.mapper.EtcMapper;
import com.cumt.lyz.backend.pojo.EtcData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 跨省流量识别服务
 */
@Service
public class CrossProvinceFlowService {

    @Autowired
    private EtcMapper etcMapper;

    // 省际卡口关键词
    private static final String[] BORDER_KEYWORDS = {
            "界", "省际", "跨省", "通道", "边界", "交界"
    };

    // 省份组合
    private static final String[] PROVINCE_PAIRS = {
            "苏皖", "苏鲁", "鲁苏", "皖苏"
    };

    /**
     * 判断是否为省际卡口
     * 
     * @param bayonetName 卡口名称
     * @return true=省际卡口, false=省内卡口
     */
    public boolean isProvincialBorder(String bayonetName) {
        if (bayonetName == null || bayonetName.isEmpty()) {
            return false;
        }

        // 临时方案：显示所有卡口用于可视化测试
        // 后续可根据实际需求细化省际识别逻辑
        return true;

        /*
         * // 原有严格过滤逻辑（暂时禁用）
         * // 模式1: 明确包含"界"字及相关词
         * for (String keyword : BORDER_KEYWORDS) {
         * if (bayonetName.contains(keyword)) {
         * return true;
         * }
         * }
         * 
         * // 模式2: 包含省份名称组合
         * for (String pair : PROVINCE_PAIRS) {
         * if (bayonetName.contains(pair)) {
         * return true;
         * }
         * }
         * 
         * // 模式3: 特定格式（如"XX界省际卡口"）
         * if (Pattern.matches(".*[界边].*(省际|跨省|通道).*", bayonetName)) {
         * return true;
         * }
         * 
         * return false;
         */
    }

    /**
     * 判断流向类型
     * 
     * @param etcData 通行记录
     * @return 0=省内, 1=入徐(来自省外), 2=出徐(流向省外)
     */
    public int getFlowDirection(EtcData etcData) {
        String bayonet = etcData.getBayonetName();
        String direction = etcData.getDirectionType();

        if (!isProvincialBorder(bayonet)) {
            return 0; // 非省界卡口，省内流动
        }

        // 省界卡口，根据方向类型判断
        if (direction != null) {
            // 上行 = 入徐
            if (direction.contains("上行") || direction.equals("1")) {
                return 1;
            }
            // 下行 = 出徐
            else if (direction.contains("下行") || direction.equals("2")) {
                return 2;
            }
        }

        // 根据卡口名称推断
        if (bayonet.contains("入口") || bayonet.contains("进") ||
                bayonet.contains("来")) {
            return 1; // 入徐
        } else if (bayonet.contains("出口") || bayonet.contains("出") ||
                bayonet.contains("往")) {
            return 2; // 出徐
        }

        // 根据区县名称推断（如果不是徐州辖区，且在省界卡口，则视为出徐）
        String district = etcData.getDistrictName();
        if (district != null && !isXuzhouDistrict(district)) {
            return 2; // 外地车辆，可能是出徐
        }

        return 0; // 无法判断，默认省内
    }

    /**
     * 判断是否为徐州辖区
     */
    private boolean isXuzhouDistrict(String district) {
        if (district == null)
            return false;

        String[] xuzhouDistricts = {
                "徐州", "铜山", "鼓楼", "云龙", "贾汪", "泉山",
                "新沂", "邳州", "沛县", "睢宁", "丰县"
        };

        for (String d : xuzhouDistricts) {
            if (district.contains(d)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取流向描述
     */
    public String getFlowDirectionName(int direction) {
        switch (direction) {
            case 1:
                return "入徐";
            case 2:
                return "出徐";
            default:
                return "省内";
        }
    }
}
