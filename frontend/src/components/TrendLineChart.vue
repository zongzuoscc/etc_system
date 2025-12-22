<template>
  <div class="trend-line-chart" :class="{ 'fullscreen-mode': mode === 'fullscreen' }">
    <div class="card-header" v-if="mode === 'thumbnail'">
      <el-icon><TrendCharts /></el-icon>
      <span>实时车流趋势</span>
    </div>
    <div class="card-body">
      <div class="chart-title" v-if="mode === 'fullscreen'">
        <h2><el-icon><TrendCharts /></el-icon> 实时车流趋势监控</h2>
      </div>
      <div ref="chartRef" class="chart-container"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import gsap from 'gsap'

const props = defineProps({
  mode: { type: String, default: 'thumbnail' },
  data: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chartInstance = null

const renderChart = () => {
  if (!chartRef.value || !props.data.length) return
  
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  
  const isFullscreen = props.mode === 'fullscreen'
  
  // 趋势数据格式: [{time_str: "23:25", vehicle_count: 52}, ...]
  // 按时间顺序排列（time_str 已经是 HH:mm 格式）
  const sortedData = [...props.data].sort((a, b) => {
    return (a.time_str || '').localeCompare(b.time_str || '')
  })
  
  // 排除最新的1分钟数据（因10秒刷新导致数据不完整，会显示突降）
  const filteredData = sortedData.length > 1 ? sortedData.slice(0, -1) : sortedData
  
  const timeData = filteredData.map(item => item.time_str || '未知')
  const countData = filteredData.map(item => item.vehicle_count || 0)
  
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
      bottom: '10%',
      top: isFullscreen ? '15%' : '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: timeData,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
      axisLabel: { color: '#fff', fontSize: isFullscreen ? 14 : 10 }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
      axisLabel: { color: '#fff', fontSize: isFullscreen ? 14 : 10 },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
    },
    series: [{
      type: 'line',
      smooth: true,
      symbol: isFullscreen ? 'circle' : 'none',
      symbolSize: 8,
      lineStyle: {
        width: isFullscreen ? 3 : 2,
        color: { 
          type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
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
            { offset: 0, color: 'rgba(102, 126, 234, 0.4)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0.05)' }
          ]
        }
      },
      data: countData
    }]
  }
  
  chartInstance.setOption(option, true)
  chartInstance.resize()
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(() => [props.data, props.mode], () => {
  setTimeout(() => {
    if (chartInstance) {
      chartInstance.resize()
    }
    renderChart()
  }, 150)
}, { deep: true })

onMounted(() => {
  setTimeout(renderChart, 50)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.trend-line-chart {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
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
  flex: 1;
  padding: 16px;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chart-title {
  margin-bottom: 20px;
}

.chart-title h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #00d4ff;
  font-size: 24px;
  margin: 0;
}

.chart-container {
  flex: 1;
  width: 100%;
  min-height: 150px;
}

.fullscreen-mode .chart-container {
  min-height: 400px;
}
</style>
