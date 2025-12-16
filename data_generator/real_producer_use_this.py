import time
import json
import random
import csv
import os
import glob
from datetime import datetime, timedelta
from faker import Faker
from kafka import KafkaProducer

# --- 配置 ---
CSV_FOLDER = "202312（交通流量）"  # 文件夹名称（确保和脚本同目录）
KAFKA_TOPIC = 'etc_traffic'
KAFKA_ALERT_TOPIC = 'fake_plate_alert'  # 套牌车预警专用topic
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

# --- 真实卡口信息（ID 1~19）---
REAL_GATES = [
    None,  # index 0 占位
    {"name": "G3京台高速K731江苏高速五大队江苏徐州-G3-苏鲁界省际卡口", "lon": 117.396433, "lat": 34.487818},
    {"name": "徐州市丰县G237国道237线K148荣庄卡口省际卡口", "lon": 116.493658, "lat": 34.593566},
    {"name": "徐州市丰县G518518国道K358马楼公路站省际卡口", "lon": 116.649384, "lat": 34.923857},
    {"name": "徐州市丰县鹿梁路K19丰县梁寨检查站市际卡口", "lon": 116.789378, "lat": 34.926602},
    {"name": "徐州市新沂市G235国道235K10江苏徐州-G235-交界市际卡口", "lon": 118.338161, "lat": 34.403598},
    {"name": "徐州市新沂市S323连徐线K10阿湖卡口-323省道连云港交界市际卡口", "lon": 118.616398, "lat": 34.403239},
    {"name": "徐州市新沂市S505505省道K10新沂高速西出口-505省道宿迁界市际卡口", "lon": 118.275318, "lat": 34.236139},
    {"name": "徐州市沛县S253郑沛龙线K0江苏徐州-S253-苏鲁界省际卡口", "lon": 116.849843, "lat": 34.896006},
    {"name": "徐州市睢宁县G104北京-福州K873江苏徐州-G104-苏皖界省际卡口", "lon": 117.895947, "lat": 33.912474},
    {"name": "徐州市睢宁县S252塔双线K56江苏徐州-S252-苏皖界省际卡口", "lon": 117.893307, "lat": 33.972047},
    {"name": "徐州市睢宁县S324燕沭睢线K201省道桑庄王马路路口西侧-向东卡口市际卡口", "lon": 117.789449,
     "lat": 34.065145},
    {"name": "徐州市睢宁县S325淮宿线K63(325省道)63K+100M东侧-向西卡口市际卡口", "lon": 118.044163, "lat": 33.904848},
    {"name": "徐州市邳州市S250宿邳线K1江苏徐州-S250-苏鲁界省际卡口", "lon": 118.06454, "lat": 34.641634},
    {"name": "徐州市邳州市S251枣睢线K5江苏徐州-S251-苏鲁界省际卡口", "lon": 117.717927, "lat": 34.535382},
    {"name": "徐州市铜山县G104北京-福州K744江苏徐州-G104-苏鲁界省际卡口", "lon": 117.362832, "lat": 34.580054},
    {"name": "徐州市铜山县G206烟台-汕头K816江苏徐州-G206-苏皖界省际卡口", "lon": 117.165938, "lat": 34.08271},
    {"name": "徐州市铜山县G310连云港-天水K310江苏徐州-G310-苏皖界省际卡口", "lon": 117.013287, "lat": 34.286473},
    {"name": "徐州市铜山县G311徐州-西峡K207江苏徐州-G311-苏皖界省际卡口", "lon": 117.103463, "lat": 34.141885},
    {"name": "江苏省徐州市新沂市S323连徐线K96瓦窑检查站市际卡口", "lon": 118.250688, "lat": 34.376804},
]

# --- 距离和时间矩阵 ---
N = 19
distance_matrix = [[0] * (N + 1) for _ in range(N + 1)]
time_matrix = [[0] * (N + 1) for _ in range(N + 1)]

pairs = [
    (1, 2, 109981, 5911), (1, 3, 120752, 4984), (1, 4, 106239, 5265), (1, 5, 119295, 4982), (1, 6, 145348, 6397),
    (1, 7, 144877, 6215), (1, 8, 121978, 5376), (1, 9, 105032, 4782), (1, 10, 133932, 5909), (1, 11, 123598, 5356),
    (1, 12, 118987, 5181), (1, 13, 126754, 5546), (1, 14, 54879, 2344), (1, 15, 43758, 1850), (1, 16, 9894, 419),
    (1, 17, 43698, 1851), (1, 18, 41908, 1774), (1, 19, 142398, 6108), (2, 3, 31284, 1358), (2, 4, 17772, 788),
    (2, 5, 28810, 1251), (2, 6, 54863, 2412), (2, 7, 54392, 2393), (2, 8, 31493, 1392), (2, 9, 18584, 832),
    (2, 10, 43447, 1912), (2, 11, 33113, 1456), (2, 12, 28502, 1254), (2, 13, 36269, 1596), (2, 14, 103486, 4554),
    (2, 15, 114607, 5043), (2, 16, 109875, 4832), (2, 17, 85185, 3748), (2, 18, 87467, 3847), (2, 19, 51913, 2285),
    (3, 4, 14511, 644), (3, 5, 42321, 1863), (3, 6, 68374, 3008), (3, 7, 67903, 2988), (3, 8, 18003, 797),
    (3, 9, 29095, 1285), (3, 10, 56958, 2507), (3, 11, 46624, 2051), (3, 12, 42013, 1849), (3, 13, 49780, 2191),
    (3, 14, 116997, 5150), (3, 15, 128118, 5638), (3, 16, 123386, 5427), (3, 17, 98696, 4343), (3, 18, 100978, 4443),
    (3, 19, 65424, 2880), (4, 5, 28311, 1247), (4, 6, 54364, 2392), (4, 7, 53893, 2372), (4, 8, 16992, 751),
    (4, 9, 15084, 667), (4, 10, 42947, 1891), (4, 11, 32613, 1435), (4, 12, 28002, 1233), (4, 13, 35769, 1574),
    (4, 14, 102986, 4530), (4, 15, 114107, 5019), (4, 16, 109375, 4809), (4, 17, 84685, 3724), (4, 18, 86967, 3824),
    (4, 19, 51413, 2261), (5, 6, 26054, 1146), (5, 7, 25583, 1126), (5, 8, 28319, 1248), (5, 9, 26372, 1162),
    (5, 10, 14637, 644), (5, 11, 4303, 189), (5, 12, 8914, 392), (5, 13, 7541, 332), (5, 14, 131606, 5789),
    (5, 15, 142727, 6278), (5, 16, 138064, 6073), (5, 17, 113374, 4988), (5, 18, 115656, 5088), (5, 19, 23124, 1017),
    (6, 7, 1179, 52), (6, 8, 54373, 2393), (6, 9, 52426, 2307), (6, 10, 40691, 1790), (6, 11, 30357, 1335),
    (6, 12, 34968, 1539), (6, 13, 33014, 1453), (6, 14, 157660, 6936), (6, 15, 168781, 7425), (6, 16, 164151, 7224),
    (6, 17, 139461, 6139), (6, 18, 141743, 6238), (6, 19, 3071, 135), (7, 8, 53902, 2372), (7, 9, 51955, 2286),
    (7, 10, 40220, 1770), (7, 11, 29886, 1315), (7, 12, 34497, 1518), (7, 13, 32543, 1432), (7, 14, 157189, 6915),
    (7, 15, 168310, 7404), (7, 16, 163680, 7203), (7, 17, 138990, 6118), (7, 18, 141272, 6218), (7, 19, 2600, 114),
    (8, 9, 14047, 620), (8, 10, 41973, 1847), (8, 11, 31639, 1392), (8, 12, 27028, 1190), (8, 13, 34795, 1531),
    (8, 14, 103522, 4555), (8, 15, 114643, 5044), (8, 16, 109942, 4837), (8, 17, 85252, 3750), (8, 18, 87534, 3850),
    (8, 19, 51449, 2263), (9, 10, 28926, 1273), (9, 11, 18592, 818), (9, 12, 13981, 615), (9, 13, 21748, 957),
    (9, 14, 90475, 3981), (9, 15, 101596, 4470), (9, 16, 96895, 4264), (9, 17, 72205, 3178), (9, 18, 74487, 3278),
    (9, 19, 38402, 1690), (10, 11, 10335, 454), (10, 12, 14946, 657), (10, 13, 7179, 316), (10, 14, 118549, 5215),
    (10, 15, 129670, 5704), (10, 16, 124969, 5499), (10, 17, 100279, 4413), (10, 18, 102561, 4513),
    (10, 19, 38026, 1673),
    (11, 12, 4611, 203), (11, 13, 3156, 139), (11, 14, 128884, 5669), (11, 15, 140005, 6158), (11, 16, 135304, 5953),
    (11, 17, 110614, 4867), (11, 18, 112896, 4967), (11, 19, 27691, 1218), (12, 13, 7767, 342), (12, 14, 124273, 5466),
    (12, 15, 135394, 5955), (12, 16, 130693, 5750), (12, 17, 106003, 4664), (12, 18, 108285, 4764),
    (12, 19, 32302, 1421),
    (13, 14, 131080, 5766), (13, 15, 142201, 6255), (13, 16, 137460, 6046), (13, 17, 112770, 4961),
    (13, 18, 115052, 5061),
    (13, 19, 30535, 1343), (14, 15, 11121, 489), (14, 16, 45985, 2023), (14, 17, 14819, 652), (14, 18, 12987, 571),
    (14, 19, 145519, 6404), (15, 16, 34864, 1534), (15, 17, 4302, 189), (15, 18, 5634, 248), (15, 19, 139898, 6154),
    (16, 17, 35166, 1547), (16, 18, 34834, 1533), (16, 19, 145034, 6381), (17, 18, 2668, 117), (17, 19, 140200, 6167),
    (18, 19, 137532, 6050)
]

for f, t, d, tm in pairs:
    distance_matrix[f][t] = distance_matrix[t][f] = d
    time_matrix[f][t] = time_matrix[t][f] = tm

# 坐标抖动
ENABLE_COORD_JITTER = True
JITTER_RANGE = 0.0005


# --- 车牌相关配置 ---
def extract_plate_prefix(plate_str):
    """从CSV中的车牌字符串提取前缀（去掉***部分）"""
    if not plate_str or not isinstance(plate_str, str):
        return None
    plate_str = plate_str.strip()
    if '***' in plate_str:
        prefix = plate_str.replace('***', '').strip()
        # 确保前缀长度合理（通常是2-5位）
        if 2 <= len(prefix) <= 5:
            return prefix
    return None


def complete_plate_suffix():
    """
    生成车牌后三位
    每一位可以是：数字(0-9) 或 大写字母(A-Z)
    """
    chars = '0123456789ABCDEFGHJKLMNPQRSTUVWXYZ'  # 数字+大写字母，排除I和O
    return ''.join(random.choice(chars) for _ in range(3))


def generate_plate_from_row(row):
    """从CSV行数据生成完整车牌号"""
    # 尝试多个可能的列名
    plate_raw = (row.get("SUBSTR(HPHM,1,4)||'***'", "") or
                 row.get("HPHM", "") or
                 row.get("车牌", ""))

    prefix = extract_plate_prefix(plate_raw)

    if prefix:
        # 使用真实前缀 + 随机后三位
        suffix = complete_plate_suffix()
        return prefix + suffix
    else:
        # 如果没有提取到前缀，使用faker生成
        return fake.license_plate()


# --- 套牌检测相关变量 ---
plate_records = {}
DETECTION_WINDOW_MINUTES = 30
MIN_SUSPICIOUS_DISTANCE_KM = 30
TIME_TOLERANCE_FACTOR = 0.5

last_injection_time = datetime.now() - timedelta(minutes=10)


# --- 工具函数 ---
def get_gate_id_by_name(name):
    for i, gate in enumerate(REAL_GATES):
        if gate and gate["name"] == name:
            return i
    return None


def check_fake_plate(data, current_time):
    hphm = data["HPHM"]
    kkmc = data["KKMC"]
    lat = data["WEIDU"]
    lon = data["JINGDU"]

    gate_id = get_gate_id_by_name(kkmc)

    if hphm not in plate_records:
        return

    prev = plate_records[hphm]
    prev_time = prev["time"]
    prev_gate_id = prev["gate_id"]

    time_diff_sec = (current_time - prev_time).total_seconds()
    if time_diff_sec > DETECTION_WINDOW_MINUTES * 60 or time_diff_sec < 60:
        return

    if gate_id is None or prev_gate_id is None:
        return

    dist_m = distance_matrix[prev_gate_id][gate_id]
    if dist_m < MIN_SUSPICIOUS_DISTANCE_KM * 1000:
        return

    expected_time_sec = time_matrix[prev_gate_id][gate_id]
    if expected_time_sec == 0:
        return

    lower_bound = expected_time_sec * (1 - TIME_TOLERANCE_FACTOR)
    if time_diff_sec < lower_bound:
        speed_kmh = round((dist_m / 1000) / (time_diff_sec / 3600), 1)

        # 构建预警消息
        alert_message = {
            "alert_type": "fake_plate",
            "alert_time": current_time.isoformat(),
            "plate_number": hphm,
            "previous_record": {
                "time": prev_time.isoformat(),
                "location": prev['kkmc'],
                "latitude": prev['lat'],
                "longitude": prev['lon']
            },
            "current_record": {
                "time": current_time.isoformat(),
                "location": kkmc,
                "latitude": lat,
                "longitude": lon
            },
            "analysis": {
                "time_diff_seconds": int(time_diff_sec),
                "time_diff_minutes": round(time_diff_sec / 60, 2),
                "distance_km": round(dist_m / 1000, 2),
                "expected_min_time_seconds": expected_time_sec,
                "expected_min_time_minutes": round(expected_time_sec / 60, 2),
                "average_speed_kmh": speed_kmh,
                "alert_level": "HIGH"  # 可以根据速度设置等级
            },
            "reason": "物理不可能的行驶速度，高度疑似套牌"
        }

        # 发送到Kafka预警topic
        try:
            producer.send(KAFKA_ALERT_TOPIC, alert_message)
            producer.flush()  # 确保立即发送
        except Exception as e:
            print(f"⚠️  预警消息发送Kafka失败: {e}")

        # 控制台打印预警
        print(f"\n{'=' * 80}")
        print(f"🚨【套牌车预警】检测到疑似套牌车！")
        print(f"   车牌: {hphm}")
        print(f"   前次: {prev_time.strftime('%H:%M:%S')} @ {prev['kkmc'][:30]}...")
        print(f"   本次: {current_time.strftime('%H:%M:%S')} @ {kkmc[:30]}...")
        print(f"   时间差: {int(time_diff_sec // 60)}分{int(time_diff_sec % 60)}秒")
        print(f"   距离: {dist_m // 1000} km， 理论最短时间: {expected_time_sec // 60}分")
        print(f"   平均速度: {speed_kmh} km/h → 物理不可能，高度疑似套牌！")
        print(f"   ✅ 预警已发送至Kafka Topic: {KAFKA_ALERT_TOPIC}")
        print(f"{'=' * 80}\n")


def inject_realistic_fake_plate():
    global last_injection_time
    if (datetime.now() - last_injection_time).total_seconds() < random.uniform(28, 32):
        return
    if len(plate_records) < 20:
        return

    fake_plate = random.choice(list(plate_records.keys()))
    from_id, to_id = random.sample(range(1, 20), 2)

    dist = distance_matrix[from_id][to_id]
    base_time_sec = time_matrix[from_id][to_id]
    if dist == 0 or base_time_sec == 0:
        return

    time_sec = int(base_time_sec * random.uniform(0.7, 1.3))
    speed_kmh = round((dist / 1000) / (time_sec / 3600), 1)

    gate_from = REAL_GATES[from_id]
    gate_to = REAL_GATES[to_id]
    base_time = datetime.now()

    rec1 = {
        "GCXH": str(random.randint(10000, 99999)),
        "XZQHMC": "徐州市",
        "KKMC": gate_from["name"],
        "FXLX": random.choice(['N', 'S', 'E', 'W']),
        "GCSJ": base_time.isoformat(),
        "HPZL": "02",
        "HPHM": fake_plate,
        "CLPPXH": "未知",
        "CS": speed_kmh,
        "WEIDU": round(
            gate_from["lat"] + random.uniform(-JITTER_RANGE, JITTER_RANGE) if ENABLE_COORD_JITTER else gate_from["lat"],
            6),
        "JINGDU": round(
            gate_from["lon"] + random.uniform(-JITTER_RANGE, JITTER_RANGE) if ENABLE_COORD_JITTER else gate_from["lon"],
            6)
    }
    producer.send(KAFKA_TOPIC, rec1)
    current_dt = datetime.fromisoformat(rec1["GCSJ"])
    check_fake_plate(rec1, current_dt)
    plate_records[fake_plate] = {"time": current_dt, "gate_id": from_id, "kkmc": gate_from["name"],
                                 "lat": rec1["WEIDU"], "lon": rec1["JINGDU"]}
    print(f"🔥 套牌注入 (1/2): {fake_plate} @ {gate_from['name'][:25]}...")

    rec2_time = base_time + timedelta(seconds=time_sec)
    rec2 = {
        "GCXH": str(random.randint(10000, 99999)),
        "XZQHMC": "徐州市",
        "KKMC": gate_to["name"],
        "FXLX": random.choice(['N', 'S', 'E', 'W']),
        "GCSJ": rec2_time.isoformat(),
        "HPZL": "02",
        "HPHM": fake_plate,
        "CLPPXH": "未知",
        "CS": speed_kmh,
        "WEIDU": round(
            gate_to["lat"] + random.uniform(-JITTER_RANGE, JITTER_RANGE) if ENABLE_COORD_JITTER else gate_to["lat"], 6),
        "JINGDU": round(
            gate_to["lon"] + random.uniform(-JITTER_RANGE, JITTER_RANGE) if ENABLE_COORD_JITTER else gate_to["lon"], 6)
    }
    producer.send(KAFKA_TOPIC, rec2)
    check_fake_plate(rec2, rec2_time)
    plate_records[fake_plate] = {"time": rec2_time, "gate_id": to_id, "kkmc": gate_to["name"], "lat": rec2["WEIDU"],
                                 "lon": rec2["JINGDU"]}
    print(f"🔥 套牌注入 (2/2): {fake_plate} @ {gate_to['name'][:25]}... 距离 {dist // 1000}km，耗时 {time_sec // 60}分\n")

    last_injection_time = datetime.now()


def get_all_csv_files(folder):
    pattern = os.path.join(folder, "*.csv")
    files = glob.glob(pattern)
    files.sort()
    if not files:
        print(f"错误：文件夹 '{folder}' 中未找到任何 .csv 文件！")
        exit()
    print(f"找到 {len(files)} 个 CSV 文件，将依次循环读取：")
    for f in files:
        print(f"   - {os.path.basename(f)}")
    return files


def get_flow_multiplier():
    hour = datetime.now().hour
    if 7 <= hour < 9:
        return 1.5
    elif 17 <= hour < 19:
        return 1.8
    return 1.0


# --- 主程序启动 ---
csv_files = get_all_csv_files(CSV_FOLDER)

print(f"\n✅ 交通流量生产者启动（含套牌车检测预警+真实车牌）！")
print(f"   📤 交通数据Topic: {KAFKA_TOPIC}")
print(f"   🚨 预警数据Topic: {KAFKA_ALERT_TOPIC}\n")

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
        file_index = (file_index + 1) % len(csv_files)
        continue

    kkmc_set = {row.get("KKMC", "").strip() for row in rows if row.get("KKMC", "").strip()}
    current_kkmc_list = list(kkmc_set) or None
    random.shuffle(rows)

    for row_index, row in enumerate(rows):
        # 从当前行提取车牌前缀并补全后三位
        plate = generate_plate_from_row(row)

        data = {
            "GCXH": row.get("GCXH", str(random.randint(10000, 99999))).strip(),
            "XZQHMC": row.get("XZQHMC", "徐州市").strip(),
            "FXLX": row.get("FXLX", random.choice(['N', 'S', 'E', 'W'])).strip(),
            "GCSJ": datetime.now().isoformat(),
            "HPZL": row.get("HPZL", "02").strip(),
            "CLPPXH": row.get("CLPPXH", "未知").strip(),
            "HPHM": plate,
            "CS": max(20, round(random.gauss(90, 20), 1))
        }

        if current_kkmc_list:
            data["KKMC"] = random.choice(current_kkmc_list)
        else:
            road = random.choice(['G3', 'G30', 'G104', 'S25', 'S32', 'G206'])
            area = random.choice(['徐州', '新沂', '邳州', '睢宁', '沛县', '丰县', '铜山'])
            data["KKMC"] = f"{road}_{area}_卡口{random.randint(1, 999):03d}"

        gate_id = get_gate_id_by_name(data["KKMC"])
        if gate_id:
            gate = REAL_GATES[gate_id]
            base_lat, base_lon = gate["lat"], gate["lon"]
        else:
            base_lat = random.uniform(MIN_LAT, MAX_LAT)
            base_lon = random.uniform(MIN_LON, MAX_LON)

        data["WEIDU"] = round(
            base_lat + random.uniform(-JITTER_RANGE, JITTER_RANGE) if ENABLE_COORD_JITTER else base_lat, 6)
        data["JINGDU"] = round(
            base_lon + random.uniform(-JITTER_RANGE, JITTER_RANGE) if ENABLE_COORD_JITTER else base_lon, 6)

        current_dt = datetime.now()

        producer.send(KAFKA_TOPIC, data)
        check_fake_plate(data, current_dt)

        if gate_id:
            plate_records[plate] = {
                "time": current_dt,
                "gate_id": gate_id,
                "kkmc": data["KKMC"],
                "lat": data["WEIDU"],
                "lon": data["JINGDU"]
            }

        inject_realistic_fake_plate()

        sleep_interval = 1.0 / (50 * get_flow_multiplier())
        time.sleep(sleep_interval)

        if row_index % 100 == 0:
            kkmc_show = data["KKMC"][:40] + ("..." if len(data["KKMC"]) > 40 else "")
            print(
                f"   [{filename}] {row_index:5d}/{len(rows)} │ 速率 {50 * get_flow_multiplier():.1f} msg/s │ {plate} @ {kkmc_show}")

    print(f"✅ {filename} 发送完成，切换下一文件\n")
    file_index = (file_index + 1) % len(csv_files)