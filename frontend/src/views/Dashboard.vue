<template>
  <div class="dashboard-container">
    <!-- 顶部标题栏 -->
    <header class="dashboard-header">
      <div class="header-left">
        <el-button class="back-btn" @click="goBack" circle>
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
      </div>
      <h1 class="dashboard-title">大数据存储平台交通监控</h1>
      <div class="header-right">
        <div class="current-time">{{currentTime}}</div>
      </div>
    </header>

    <!--九宫格数据展示区 -->
    <div class="dashboard-grid">
      <!-- 左上：总车流量 -->
      <div class="data-card total-flow">
        <div class="card-header">
          <el-icon><Odometer /></el-icon>
          <span>总车流量</span>
        </div>
        <div class="card-body">
          <div class="big-number">{{ totalFlow }}</div>
          <div class="label">总流量</div>
        </div>
      </div>

      <!-- 左中：行政区车流占比 -->
      <div class="data-card district-chart">
        <div class="card-header">
          <el-icon><PieChart /></el-icon>
          <span>行政区车流占比</span>
        </div>
        <div class="card-body">
          <div ref="districtChartRef" class="chart-box"></div>
        </div>
      </div>

      <!-- 左下：各地市车流统计 (暂时复用行政区数据) -->
      <div class="data-card city-stats">
        <div class="card-header">
          <el-icon><Histogram /></el-icon>
          <span>各地市车流统计</span>
        </div>
        <div class="card-body">
          <div ref="cityChartRef" class="chart-box"></div>
        </div>
      </div>

      <!-- 中心：交通流量地图（嵌入式） -->
      <div class="data-card center-map">
        <TrafficFlowMap />
      </div>

      <!-- 底部：24小时趋势 (暂时显示刷新信息) -->
      <div class="data-card  bottom-trend">
        <div class="card-header">
          <el-icon><TrendCharts /></el-icon>
          <span>24小时车流趋势</span>
        </div>
        <div class="card-body">
          <div class="refresh-info">
            <el-icon><Refresh /></el-icon>
            <span>{{ refreshCountdown }}秒后自动刷新数据</span>
          </div>
        </div>
      </div>

      <!-- 右上：套牌车告警 -->
      <div class="data-card fake-vehicle-alerts">
        <div class="card-header">
          <el-icon><Warning /></el-icon>
          <span>套牌车告警 ({{ fakeVehicleAlerts.length }})</span>
        </div>
        <div class="card-body">
          <div class="alert-list" v-if="fakeVehicleAlerts.length > 0">
            <TransitionGroup name="alert-slide">
              <div v-for="alert in fakeVehicleAlerts.slice(0, 5)" :key="alert.id" class="alert-item">
                <div class="alert-header">
                  <span class="alert-plate">{{ alert.plateNumber }}</span>
                  <span class="alert-level" :class="alert.alertLevel?.toLowerCase()">{{ alert.alertLevel || 'HIGH' }}</span>
                </div>
                <div class="alert-details">
                  <span class="alert-speed">🚗 {{ alert.actualSpeed?.toFixed(1) }} km/h</span>
                  <span class="alert-time">{{ formatAlertTime(alert.createTime) }}</span>
                </div>
              </div>
            </TransitionGroup>
          </div>
          <div class="no-alerts" v-else>
            <el-icon><CircleCheck /></el-icon>
            <span>暂无告警</span>
          </div>
        </div>
      </div>

      <!-- 右中：实时车流趋势 -->
      <div class="data-card trend-chart">
        <div class="card-header">
          <el-icon><TrendCharts /></el-icon>
          <span>实时车流趋势</span>
        </div>
        <div class="card-body">
          <div ref="trendChartRef" class="chart-box"></div>
        </div>
      </div>

      <!-- 右下：最新通行记录 -->
      <div class="data-card realtime-list">
        <div class="card-header">
          <el-icon><List /></el-icon>
          <span>最新通行记录</span>
        </div>
        <div class="card-body">
          <div class="record-list">
            <TransitionGroup name="record-slide">
              <div v-for="item in realtimeData" :key="item.id" class="record-item">
                <div class="record-time">{{ formatTime(item.passTime) }}</div>
                <div class="record-info">
                  <span class="plate-number">{{ item.plateNumber }}</span>
                  <span class="district">{{ item.districtName }}</span>
                </div>
              </div>
            </TransitionGroup>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Refresh, TrendCharts, PieChart, List,
  Odometer, Warning, Histogram, CircleCheck
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getTotalFlow, getDistrictStats, getTrend, getFakeVehicleAlerts } from '@/api/etc'
import TrafficFlowMap from '@/components/TrafficFlowMap.vue'

const router = useRouter()

// 响应式数据
const currentTime = ref('')
const refreshCountdown = ref(10)
const totalFlow = ref('0')
const fakeVehicleAlerts = ref([])
const realtimeData = ref([])

// ECharts实例引用
const districtChartRef = ref(null)
const cityChartRef = ref(null)
const trendChartRef = ref(null)
let districtChart = null
let cityChart = null
let trendChart = null
let countdownTimer = null

// 返回首页
const goBack = () => {
  router.push('/')
}

const goToAnalysis = () => {
  router.push('/analysis')
}

// 更新当前时间
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    hour12: false,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 获取总车流量
const fetchTotalFlow = async () => {
  try {
    const res = await getTotalFlow()
    if (res.code === 200) {
      totalFlow.value = res.data.toLocaleString()
    }
  } catch (error) {
    console.error('获取总车流量失败：', error)
  }
}

// 获取行政区统计并渲染图表
const fetchDistrictStats = async () => {
  try {
    const res = await getDistrictStats()
    if (res.code === 200 && res.data) {
      const data = res.data
      
      // 渲染饼图和柱状图
      renderDistrictChart(data)
      renderCityChart(data)
    }
  } catch (error) {
    console.error('获取行政区统计失败：', error)
  }
}

// 获取套牌车告警数据
const fetchFakeVehicleAlerts = async () => {
  try {
    const res = await getFakeVehicleAlerts(24)
    if (res.code === 200 && res.data) {
      fakeVehicleAlerts.value = res.data
    }
  } catch (error) {
    console.error('获取套牌车告警失败：', error)
  }
}

// 格式化告警时间
const formatAlertTime = (timeStr) => {
  if (!timeStr) return ''
  const time = new Date(timeStr)
  return time.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

// 渲染行政区饼图
const renderDistrictChart = (data) => {
  if (!districtChart) {
    districtChart = echarts.init(districtChartRef.value)
  }
  
  const colors = [
    '#667eea', '#764ba2', '#f093fb', '#f5576c',
    '#4facfe', '#00f2fe', '#43e97b', '#38f9d7',
    '#fa709a', '#fee140', '#30cfd0', '#330867', '#a8edea'
  ]
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: 'rgba(10, 14, 39, 0.9)',
      borderColor: '#667eea',
      textStyle: { color: '#fff' }
    },
    legend: {
      show: false  // 隐藏图例，腾出更多空间
    },
    series: [{
      type: 'pie',
      radius: ['40%', '75%'],  // 增大饼图尺寸
      center: ['50%', '50%'],  // 完全居中
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#0a0e27',
        borderWidth: 2
      },
      label: { 
        show: true,
        formatter: '{b}',  // 显示名称
        color: '#fff',
        fontSize: 11
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold',
          color: '#fff'
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      },
      data: data.map((item, index) => ({
        value: item.count || item.value,
        name: item.districtName || item.name,
        itemStyle: { color: colors[index % colors.length] }
      }))
    }]
  }
  
  districtChart.setOption(option)
}

// 渲染地市柱状图
const renderCityChart = (data) => {
  if (!cityChart) {
    cityChart = echarts.init(cityChartRef.value)
  }
  
  // 按流量降序排序
  const sortedData = [...data].sort((a, b) => {
    const aVal = a.count || a.value || 0
    const bVal = b.count || b.value || 0
    return aVal - bVal  // 升序排列，因为柱状图是从下到上显示，最大值会在最上面
  })
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(10, 14, 39, 0.9)',
      borderColor: '#667eea',
      textStyle: { color: '#fff' }
    },
    grid: {
      left: '5%',
      right: '5%',
      bottom: '5%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
      axisLabel: { color: '#fff', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
    },
    yAxis: {
      type: 'category',
      data: sortedData.map(item => (item.districtName || item.name).replace('市', '')),
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
      axisLabel: { color: '#fff', fontSize: 10 }
    },
    series: [{
      type: 'bar',
      data: sortedData.map((item, index) => ({
        value: item.count || item.value,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#667eea' },
              { offset: 1, color: '#00d4ff' }
            ]
          }
        }
      })),
      barWidth: '60%',
      label: {
        show: true,
        position: 'right',
        color: '#fff',
        fontSize: 10
      }
    }]
  }
  
  cityChart.setOption(option)
}

// 格式化时间显示
const formatTime = (timeStr) => {
  const time = new Date(timeStr)
  return time.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

// 获取实时趋势并渲染折线图
const fetchTrend = async () => {
  try {
    const res = await getTrend()
    if (res.code === 200 && res.data) {
      const data = res.data
      // 新数据插入到开头，保持最多10条
      realtimeData.value = [...data.slice(0, 10)]
      renderTrendChart(data)
    }
  } catch (error) {
    console.error('获取实时趋势失败：', error)
  }
}

// 渲染趋势折线图
const renderTrendChart = (data) => {
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  
  // 反转数据顺序，使时间从左到右递增（早->晚）
  const sortedData = [...data].reverse()
  
  const timeData = sortedData.map(item => {
    const time = new Date(item.passTime)
    return time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
  })
  
  const countData = sortedData.map((_, index) => index + 1)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10, 14, 39, 0.9)',
      borderColor: '#667eea',
      textStyle: { color: '#fff' }
    },
    grid: {
      left: '5%',
      right: '5%',
      bottom: '5%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: timeData,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
      axisLabel: { color: '#fff', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
      axisLabel: { color: '#fff', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
    },
    series: [{
      type: 'line',
      smooth: true,
      lineStyle: {
        width: 2,
        color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#00d4ff' }
          ]
        }
      },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0.05)' }
          ]
        }
      },
      data: countData
    }]
  }
  
  trendChart.setOption(option)
}

// 刷新所有数据
const refreshAllData = async () => {
  await Promise.all([
    fetchTotalFlow(),
    fetchDistrictStats(),
    fetchTrend(),
    fetchFakeVehicleAlerts()
  ])
  refreshCountdown.value = 10
}

// 倒计时
const startCountdown = () => {
  countdownTimer = setInterval(() => {
    refreshCountdown.value--
    if (refreshCountdown.value <= 0) {
      refreshAllData()
    }
  }, 1000)
}

// 组件挂载
onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)
  
  // 初始化数据
  refreshAllData()
  
  // 启动30秒自动刷新
  startCountdown()
  
  // 窗口大小变化时重绘图表
  const handleResize = () => {
    districtChart?.resize()
    cityChart?.resize()
    trendChart?.resize()
  }
  window.addEventListener('resize', handleResize)
})

// 组件卸载
onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  districtChart?.dispose()
  cityChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  background: #0a0e27;
  min-height: 100vh;
  color: #fff;
  padding: 16px;
  overflow: hidden;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 10px;
}

.back-btn {
  background: rgba(102, 126, 234, 0.2);
  border: 1px solid rgba(102, 126, 234, 0.5);
  color: #fff;
}

.back-btn:hover {
  background: rgba(102, 126, 234, 0.4);
  border-color: #667eea;
}

.dashboard-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(90deg, #667eea 0%, #00d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  letter-spacing: 2px;
}

.current-time {
  font-size: 16px;
  font-weight: 600;
  color: #00d4ff;
  font-family: 'Courier New', monospace;
}

/* 九宫格布局 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 420px 1fr 420px;
  grid-template-rows: repeat(3, minmax(200px, 1fr)) auto;
  gap: 16px;
  height: calc(100vh - 120px);
}

/* 数据卡片通用样式 */
.data-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  backdrop-filter: blur(10px);
  box-shadow: 
    0 0 10px rgba(102, 126, 234, 0.2),
    inset 0 0 20px rgba(102, 126, 234, 0.05);
  overflow: hidden;
  transition: all 0.3s ease;
}

.data-card:hover {
  border-color: rgba(102, 126, 234, 0.6);
  box-shadow: 
    0 0 20px rgba(102, 126, 234, 0.4),
    inset 0 0 30px rgba(102, 126, 234, 0.1);
}

.card-header {
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.2), transparent);
  border-left: 3px solid #667eea;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #00d4ff;
}

.card-body {
  padding: 16px;
  height: calc(100% - 44px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

/* 大数字卡片 */
.big-number {
  font-size: 48px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #00d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

/* 图表盒子 */
.chart-box {
  width: 100%;
  height: 100%;
  min-height: 150px;
}

/* 中心地图预留 */
.center-map {
  grid-area: 1 / 2 / 4 / 3;
  display: flex;
  align-items: center;
  justify-content: center;
}

.map-placeholder {
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
}

.map-placeholder p {
  margin-top: 16px;
  font-size: 16px;
}

/* 底部趋势条 */
.bottom-trend {
  grid-area: 4 / 1 / 5 / 4;
}

.refresh-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  color: #667eea;
}

/* 最新记录列表 */
.record-list {
  width: 100%;
  overflow-y: auto;
  max-height: 100%;
  padding-right: 4px;
}

.record-list::-webkit-scrollbar {
  width: 4px;
}

.record-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 2px;
}

.record-list::-webkit-scrollbar-thumb {
  background: rgba(102, 126, 234, 0.5);
  border-radius: 2px;
}

.record-list::-webkit-scrollbar-thumb:hover {
  background: rgba(102, 126, 234, 0.8);
}

.record-item {
  padding: 10px 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
}

.record-item:hover {
  background: rgba(102, 126, 234, 0.1);
  border-left: 2px solid #667eea;
  padding-left: 6px;
}

.record-item:last-child {
  border-bottom: none;
}

.record-time {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 4px;
  font-family: 'Courier New', monospace;
}

.record-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plate-number {
  font-size: 14px;
  font-weight: 600;
  color: #00d4ff;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.3);
}

.district {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
}

/* 记录滑动动画 */
.record-slide-enter-active {
  transition: all 0.4s ease;
}

.record-slide-leave-active {
  transition: all 0.3s ease;
}

.record-slide-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.record-slide-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* 套牌车告警样式 */
.fake-vehicle-alerts .card-header {
  border-left-color: #f5576c;
  color: #f5576c;
}

.alert-list {
  width: 100%;
  max-height: 100%;
  overflow-y: auto;
}

.alert-list::-webkit-scrollbar {
  width: 4px;
}

.alert-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.alert-list::-webkit-scrollbar-thumb {
  background: rgba(245, 87, 108, 0.5);
  border-radius: 2px;
}

.alert-item {
  padding: 10px 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(245, 87, 108, 0.05);
  border-radius: 4px;
  margin-bottom: 6px;
  transition: all 0.3s ease;
}

.alert-item:hover {
  background: rgba(245, 87, 108, 0.15);
  border-left: 2px solid #f5576c;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.alert-plate {
  font-size: 14px;
  font-weight: 700;
  color: #f5576c;
  text-shadow: 0 0 10px rgba(245, 87, 108, 0.4);
}

.alert-level {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 600;
}

.alert-level.high {
  background: rgba(245, 87, 108, 0.3);
  color: #f5576c;
}

.alert-level.medium {
  background: rgba(250, 112, 154, 0.3);
  color: #fa709a;
}

.alert-level.low {
  background: rgba(254, 225, 64, 0.3);
  color: #fee140;
}

.alert-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
}

.alert-speed {
  color: #00d4ff;
}

.alert-time {
  font-family: 'Courier New', monospace;
}

.no-alerts {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: rgba(255, 255, 255, 0.4);
  gap: 10px;
}

.no-alerts .el-icon {
  font-size: 36px;
  color: #43e97b;
}

/* 告警滑动动画 */
.alert-slide-enter-active {
  transition: all 0.4s ease;
}

.alert-slide-leave-active {
  transition: all 0.3s ease;
}

.alert-slide-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.alert-slide-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* 响应式 */
@media (max-width: 1600px) {
  .dashboard-grid {
    grid-template-columns: 380px 1fr 380px;
  }
}

@media (max-width: 1400px) {
  .dashboard-grid {
    grid-template-columns: 280px 1fr 280px;
  }
  
  .dashboard-title {
    font-size: 24px;
  }
}

@media (max-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
    grid-template-rows: auto;
    height: auto;
  }
  
  .center-map {
    grid-area: auto;
  }
  
  .bottom-trend {
    grid-area: auto;
  }
}
</style>
