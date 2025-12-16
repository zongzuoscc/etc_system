<template>
  <div class="analysis-placeholder">
    <el-result icon="info" title="功能规划中" sub-title="离线分析功能正在开发，敬请期待">
      <template #extra>
        <el-button type="primary" @click="goToDashboard">返回Dashboard</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goToDashboard = () => {
  router.push('/dashboard')
}
</script>

<style scoped>
.analysis-placeholder {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a0e27 0%, #1a1e3e 100%);
}

:deep(.el-result__title) {
  color: #00d4ff !important;
}

:deep(.el-result__subtitle) {
  color: #8b9dc3 !important;
}
</style>
