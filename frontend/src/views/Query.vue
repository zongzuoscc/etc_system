<template>
  <div class="query-container">
    <!-- 顶部导航栏 -->
    <header class="query-header glass-card">
      <el-button type="primary" @click="goBack" circle>
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title gradient-text">交互式查询</h1>
      <div class="header-placeholder"></div>
    </header>

    <!-- 查询条件卡片 -->
    <div class="search-section glass-card">
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="车牌号">
          <el-input 
            v-model="queryForm.plateNumber" 
            placeholder="请输入车牌号（支持模糊查询）"
            clearable
            prefix-icon="Search"
            style="width: 300px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格卡片 -->
    <div class="table-section glass-card">
      <div class="table-header">
        <h3 class="table-title">
          <el-icon><List /></el-icon>
          查询结果
          <span class="result-count">（共 {{ total }} 条记录）</span>
        </h3>
        <div class="table-actions">
          <el-button type="success" size="small" @click="exportData">
            <el-icon><Download /></el-icon>
            导出数据
          </el-button>
        </div>
      </div>

      <el-table 
        :data="tableData" 
        v-loading="loading"
        stripe
        border
        height="500"
        :header-cell-style="{ 
          background: 'rgba(102, 126, 234, 0.1)', 
          color: '#333',
          fontWeight: '600'
        }"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="plateNumber" label="车牌号" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.plateNumber }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bayonetName" label="卡口名称" min-width="180" />
        <el-table-column prop="districtName" label="行政区" width="120" align="center" />
        <el-table-column prop="directionType" label="方向" width="100" align="center" />
        <el-table-column prop="plateType" label="号牌种类" width="120" align="center" />
        <el-table-column prop="vehicleModel" label="车辆型号" min-width="150" />
        <el-table-column prop="passTime" label="通行时间" width="180" align="center" />
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          background
        />
      </div>
    </div>

    <!-- 快捷筛选卡片 -->
    <div class="quick-filter glass-card">
      <h3 class="filter-title">
        <el-icon><Filter /></el-icon>
        快捷筛选
      </h3>
      <div class="filter-tags">
        <el-tag 
          v-for="tag in quickFilters" 
          :key="tag.value"
          :type="tag.type"
          effect="plain"
          size="large"
          class="filter-tag"
          @click="applyQuickFilter(tag.value)"
        >
          {{ tag.label }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Search, RefreshLeft, List, Download, Filter
} from '@element-plus/icons-vue'
import { getEtcList } from '@/api/etc'

const router = useRouter()

// 查询表单
const queryForm = reactive({
  plateNumber: '',
  page: 1,
  size: 10
})

// 数据状态
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// 快捷筛选选项
const quickFilters = [
  { label: '最近10条', value: '10', type: '' },
  { label: '最近50条', value: '50', type: 'success' },
  { label: '最近100条', value: '100', type: 'warning' },
  { label: '全部数据', value: 'all', type: 'danger' }
]

// 返回首页
const goBack = () => {
  router.push('/')
}

// 查询数据
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: queryForm.page,
      size: queryForm.size
    }
    
    if (queryForm.plateNumber) {
      params.plateNumber = queryForm.plateNumber
    }
    
    const res = await getEtcList(params)
    
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('查询失败：', error)
    ElMessage.error('查询失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 搜索按钮
const handleSearch = () => {
  queryForm.page = 1
  fetchData()
}

// 重置按钮
const handleReset = () => {
  queryForm.plateNumber = ''
  queryForm.page = 1
  queryForm.size = 10
  fetchData()
}

// 分页大小变化
const handleSizeChange = (size) => {
  queryForm.size = size
  queryForm.page = 1
  fetchData()
}

// 页码变化
const handlePageChange = (page) => {
  queryForm.page = page
  fetchData()
}

// 快捷筛选
const applyQuickFilter = (value) => {
  if (value === 'all') {
    queryForm.size = 100
    queryForm.page = 1
  } else {
    queryForm.size = parseInt(value)
    queryForm.page = 1
  }
  handleReset()
}

// 导出数据
const exportData = () => {
  if (tableData.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  
  // 简单的CSV导出实现
  const headers = ['车牌号', '卡口名称', '行政区', '方向', '号牌种类', '车辆型号', '通行时间']
  const csvContent = [
    headers.join(','),
    ...tableData.value.map(row => 
      [
        row.plateNumber,
        row.bayonetName,
        row.districtName,
        row.directionType,
        row.plateType,
        row.vehicleModel,
        row.passTime
      ].join(',')
    )
  ].join('\n')
  
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `ETC查询结果_${new Date().getTime()}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  ElMessage.success('数据导出成功')
}

// 初始化
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.query-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.query-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 30px;
  margin-bottom: 20px;
  color: white;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
}

.header-placeholder {
  width: 40px;
}

/* 搜索区域 */
.search-section {
  padding: 24px;
  margin-bottom: 20px;
  color: white;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.search-form :deep(.el-form-item__label) {
  color: white;
  font-weight: 600;
}

/* 表格区域 */
.table-section {
  padding: 24px;
  margin-bottom: 20px;
  background: white;
  color: #333;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.table-title {
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.result-count {
  font-size: 14px;
  color: #666;
  font-weight: 400;
}

.table-actions {
  display: flex;
  gap: 12px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 快捷筛选 */
.quick-filter {
  padding: 24px;
  color: white;
}

.filter-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-tag {
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 响应式 */
@media (max-width: 768px) {
  .query-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .page-title {
    font-size: 20px;
  }
  
  .table-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .search-form {
    flex-direction: column;
  }
  
  .search-form :deep(.el-input) {
    width: 100% !important;
  }
}
</style>
