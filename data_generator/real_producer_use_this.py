import time
import json
import random
import csv
import os
import glob
from datetime import datetime, timedelta
from faker import Faker
from kafka import KafkaProducer
from district_coords import get_district_coordinates

# --- 配置 ---
CSV_FOLDER = "202312（交通流量）"  # 文件夹名称（确保和脚本同目录）
KAFKA_TOPIC = 'etc_traffic'
KAFKA_SERVER = 'localhost:9092'

# 初始化 Faker 和 Kafka Producer
fake = Faker('zh_CN')
producer = KafkaProducer(
    bootstrap_servers=KAFKA_SERVER,
    value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode('utf-8')
)

# 徐州经纬度合理范围
MIN_LAT, MAX_LAT = 33.8, 34.6
MIN_LON, MAX_LON = 116.8, 118.4

# 套牌车注入配置
INJECTION_GATE_A = {'KKMC': 'G03_徐州主线_A001', 'LAT': 34.2, 'LON': 117.2}
INJECTION_GATE_B = {'KKMC': 'S01_跨省通道_B999', 'LAT': 32.0, 'LON': 118.5}
FAKE_PLATE = '苏A888PK'
last_injection_time = datetime.now() - timedelta(minutes=6)

# --- 获取所有 CSV 文件 ---
def get_all_csv_files(folder):
    pattern = os.path.join(folder, "*.csv")
    files = glob.glob(pattern)
    files.sort()  # 按文件名排序
    if not files:
        print(f"错误：文件夹 '{folder}' 中未找到任何 .csv 文件！")
        exit()
    print(f"找到 {len(files)} 个 CSV 文件，将依次循环读取：")
    for f in files:
        print(f"   - {os.path.basename(f)}")
    return files

csv_files = get_all_csv_files(CSV_FOLDER)

# --- 流量控制：早晚高峰加速 ---
def get_flow_multiplier():
    hour = datetime.now().hour
    if 7 <= hour < 9:    # 早高峰
        return 1.5
    elif 17 <= hour < 19:  # 晚高峰
        return 1.8
    else:
        return 1.0

# --- 套牌车注入 ---
def inject_fake_plate_records():
    global last_injection_time
    if (datetime.now() - last_injection_time) > timedelta(minutes=5):
        current_time = datetime.now().isoformat()

        # 第一条：远距离卡口 B
        rec1 = {
            'GCXH': str(random.randint(10000, 99999)),
            'XZQHMC': '跨省',
            'KKMC': INJECTION_GATE_B['KKMC'],
            'FXLX': 'S',
            'GCSJ': current_time,
            'HPZL': '02',
            'HPHM': FAKE_PLATE,
            'CLPPXH': '未知',
            'CS': round(random.gauss(110, 5), 2),
            'WEIDU': INJECTION_GATE_B['LAT'],
            'JINGDU': INJECTION_GATE_B['LON']
        }
        producer.send(KAFKA_TOPIC, rec1)
        print(f"🔥 INJECTED FAKE (1/2): {FAKE_PLATE} @ {rec1['KKMC']}")
        time.sleep(0.5)

        # 第二条：近距离卡口 A
        rec2 = {
            'GCXH': str(random.randint(10000, 99999)),
            'XZQHMC': '徐州市',
            'KKMC': INJECTION_GATE_A['KKMC'],
            'FXLX': 'N',
            'GCSJ': datetime.now().isoformat(),
            'HPZL': '02',
            'HPHM': FAKE_PLATE,
            'CLPPXH': '未知',
            'CS': round(random.gauss(100, 10), 2),
            'WEIDU': INJECTION_GATE_A['LAT'],
            'JINGDU': INJECTION_GATE_A['LON']
        }
        producer.send(KAFKA_TOPIC, rec2)
        print(f"🔥 INJECTED FAKE (2/2): {FAKE_PLATE} @ {rec2['KKMC']}")
        last_injection_time = datetime.now()
        time.sleep(0.5)

# --- 主循环：依次处理每个 CSV 文件 ---
print(f"\n✅ 生产者启动！目标 Topic: {KAFKA_TOPIC}\n")

file_index = 0

while True:
    current_csv_file = csv_files[file_index]
    filename = os.path.basename(current_csv_file)
    print(f"\n📂 正在读取文件: {filename}")

    try:
        with open(current_csv_file, encoding='gb18030', errors='ignore') as f:
            reader = csv.DictReader(f)
            rows = list(reader)
        print(f"   加载 {len(rows)} 条记录")
    except Exception as e:
        print(f"   读取失败: {e}")
        rows = []

    if not rows:
        print("   文件为空或读取失败，跳到下一个文件")
        file_index = (file_index + 1) % len(csv_files)
        continue

    # === 提取当前文件中所有唯一的 KKMC（通用逻辑）===
    kkmc_set = {row.get("KKMC", "").strip() for row in rows if row.get("KKMC", "").strip()}
    current_kkmc_list = list(kkmc_set)

    if current_kkmc_list:
        print(f"   从文件中提取到 {len(current_kkmc_list)} 个唯一真实卡口，将优先使用")
    else:
        print(f"   文件中无有效 KKMC 字段，将使用随机生成卡口名")
        current_kkmc_list = None

    # 打乱行顺序，增加随机性
    random.shuffle(rows)

    # 注入一次套牌车
    inject_fake_plate_records()

    # 发送当前文件的所有记录
    for row_index, row in enumerate(rows):
        data = {
            "GCXH": row.get("GCXH", str(random.randint(10000, 99999))).strip(),
            "XZQHMC": row.get("XZQHMC", fake.city()).strip(),
            "FXLX": row.get("FXLX", "1").strip(),  # 使用CSV原始方向值
            "GCSJ": datetime.now().isoformat(),
            "HPZL": row.get("HPZL", "02").strip(),
            "CLPPXH": row.get("CLPPXH", "未知").strip(),
            "HPHM": fake.license_plate(),
            "CS": max(10, round(random.gauss(100, 10), 2))
        }

        # === 通用 KKMC 处理 ===
        if current_kkmc_list:
            data["KKMC"] = random.choice(current_kkmc_list)
        else:
            # 随机生成一个看起来合理的卡口名（备选）
            road = random.choice(['G3', 'G30', 'G104', 'S25', 'S32', 'G2', 'G206'])
            area = random.choice(['徐州', '新沂', '邳州', '睢宁', '沛县', '丰县', '铜山'])
            data["KKMC"] = f"{road}_{area}_卡口{random.randint(1, 999):03d}"

        # 使用区县中心坐标（带随机偏移）
        district_name = data["XZQHMC"]
        coords = get_district_coordinates(district_name)
        data["WEIDU"] = round(coords['latitude'], 6)
        data["JINGDU"] = round(coords['longitude'], 6)

        # 发送到 Kafka
        producer.send(KAFKA_TOPIC, data)

        # 流量控制
        multiplier = get_flow_multiplier()
        base_rate = 50
        sleep_interval = 1.0 / (base_rate * multiplier)

        # 进度打印
        if row_index % 100 == 0:
            kkmc_show = data["KKMC"]
            if len(kkmc_show) > 40:
                kkmc_show = kkmc_show[:37] + "..."
            print(
                f"   [{filename}] 已发送 {row_index:5d}/{len(rows)} 条 │ "
                f"速率: {base_rate * multiplier:.1f} msg/s │ "
                f"卡口: {kkmc_show}"
            )

        time.sleep(sleep_interval)

    # 当前文件发送完毕，切换下一个
    print(f"✅ 文件 {filename} 发送完成，切换到下一个文件。\n")
    file_index = (file_index + 1) % len(csv_files)