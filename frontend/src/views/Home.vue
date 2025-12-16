<template>
  <div class="home-container">
    <!-- 流体动画背景 -->
    <canvas id="fluidCanvas" class="fluid-bg"></canvas>
    
    <!-- 内容层 -->
    <div class="content-wrapper">
      <!-- 顶部导航栏 -->
      <nav class="navbar glass-card">
        <div class="nav-logo">
          <el-icon :size="32" color="#667eea"><Monitor /></el-icon>
          <span class="logo-text gradient-text">ETC大数据管理平台</span>
        </div>
        <div class="nav-time">
          <el-icon :size="20"><Clock /></el-icon>
          <span>{{ currentTime }}</span>
        </div>
      </nav>

      <!-- 主标题 -->
      <div class="hero-section fade-in">
        <h1 class="main-title">
          高速公路ETC智能监控平台
        </h1>
        <p class="subtitle">Highway ETC Intelligent Monitoring Platform</p>
      </div>

      <!-- 功能卡片导航区 -->
      <div class="cards-grid">
        <div 
          v-for="(card, index) in navigationCards" 
          :key="index"
          class="nav-card glass-card hover-lift"
          :style="{ animationDelay: `${index * 0.1}s` }"
          @click="navigateTo(card.path)"
        >
          <div class="card-icon" :style="{ background: card.color }">
            <el-icon :size="40"><component :is="card.icon" /></el-icon>
          </div>
          <h3 class="card-title">{{ card.title }}</h3>
          <p class="card-desc">{{ card.description }}</p>
          <div class="card-footer">
            <span>点击进入</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 页脚 -->
      <footer class="footer">
        <p>&copy; 2024 ETC大数据管理平台 | 中国矿业大学</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Monitor, Clock, ArrowRight,
  DataAnalysis, Search, TrendCharts
} from '@element-plus/icons-vue'

const router = useRouter()
const currentTime = ref('')

// 导航卡片配置
const navigationCards = [
  {
    title: '数据大屏',
    description: '实时数据可视化，动态展示车流统计与趋势分析',
    icon: 'TrendCharts',
    path: '/dashboard',
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    title: '交互式查询',
    description: '多条件查询ETC通行记录，灵活数据检索',
    icon: 'Search',
    path: '/query',
    color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    title: '离线分析',
    description: '历史数据深度分析，报表生成与导出',
    icon: 'DataAnalysis',
    path: '/analysis',
    color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  }
]

// 更新时间
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

// 导航跳转
const navigateTo = (path) => {
  router.push(path)
}

// 流体动画类 - 修复版
class FluidAnimation {
  constructor(canvas) {
    this.canvas = canvas
    this.ctx = canvas.getContext('2d')
    this.particles = []
    this.mouse = { 
      x: window.innerWidth / 2, 
      y: window.innerHeight / 2, 
      radius: 150,
      clicked: false
    }
    this.hue = 0
    this.resizeTimer = null
    this.animationId = null
    
    this.resize()
    this.init()
    
    // 监听窗口resize（带防抖）
    this.handleResize = () => {
      if (this.resizeTimer) clearTimeout(this.resizeTimer)
      this.resizeTimer = setTimeout(() => {
        this.resize()
        this.init() // 重新初始化粒子
      }, 300) // 300ms防抖
    }
    
    // 使用window监听确保能捕获所有事件
    this.handleMouseMove = (e) => {
      this.mouse.x = e.clientX
      this.mouse.y = e.clientY
    }
    
    this.handleMouseDown = () => {
      this.mouse.clicked = true
    }
    
    this.handleMouseUp = () => {
      this.mouse.clicked = false
    }
    
    // 绑定事件
    window.addEventListener('resize', this.handleResize)
    window.addEventListener('mousemove', this.handleMouseMove)
    window.addEventListener('mousedown', this.handleMouseDown)
    window.addEventListener('mouseup', this.handleMouseUp)
  }
  
  resize() {
    this.canvas.width = window.innerWidth
    this.canvas.height = window.innerHeight
  }
  
  init() {
    this.particles = []
    const area = this.canvas.width * this.canvas.height
    const particleCount = Math.min(
      Math.floor(area / 9000),
      200 // 限制最大粒子数
    )
    
    for (let i = 0; i < particleCount; i++) {
      this.particles.push({
        x: Math.random() * this.canvas.width,
        y: Math.random() * this.canvas.height,
        vx: (Math.random() - 0.5) * 2,
        vy: (Math.random() - 0.5) * 2,
        radius: Math.random() * 2 + 1,
        originalRadius: Math.random() * 2 + 1
      })
    }
  }
  
  draw() {
    // 半透明背景实现拖尾效果
    this.ctx.fillStyle = 'rgba(10, 14, 39, 0.15)'
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height)
    
    this.particles.forEach((particle, i) => {
      // 计算与鼠标的距离
      const dx = this.mouse.x - particle.x
      const dy = this.mouse.y - particle.y
      const distance = Math.sqrt(dx * dx + dy * dy)
      
      if (distance < this.mouse.radius) {
        const force = (this.mouse.radius - distance) / this.mouse.radius
        const angle = Math.atan2(dy, dx)
        
        if (this.mouse.clicked) {
          // 按住鼠标：吸引粒子
          particle.vx += Math.cos(angle) * force * 0.8
          particle.vy += Math.sin(angle) * force * 0.8
          particle.radius = particle.originalRadius * (1 + force * 1.5)
        } else {
          // 正常状态：排斥粒子
          particle.vx -= Math.cos(angle) * force * 0.6
          particle.vy -= Math.sin(angle) * force * 0.6
          particle.radius = particle.originalRadius * (1 - force * 0.2)
        }
      } else {
        // 恢复原始大小
        particle.radius += (particle.originalRadius - particle.radius) * 0.1
      }
      
      // 阻尼效果
      particle.vx *= 0.98
      particle.vy *= 0.98
      
      // 更新位置
      particle.x += particle.vx
      particle.y += particle.vy
      
      // 边界反弹
      if (particle.x < 0 || particle.x > this.canvas.width) {
        particle.vx *= -0.8
        particle.x = Math.max(0, Math.min(this.canvas.width, particle.x))
      }
      if (particle.y < 0 || particle.y > this.canvas.height) {
        particle.vy *= -0.8
        particle.y = Math.max(0, Math.min(this.canvas.height, particle.y))
      }
      
      // 绘制粒子光晕
      const gradient = this.ctx.createRadialGradient(
        particle.x, particle.y, 0,
        particle.x, particle.y, particle.radius * 2.5
      )
      gradient.addColorStop(0, `hsla(${this.hue}, 70%, 60%, 0.9)`)
      gradient.addColorStop(1, `hsla(${this.hue}, 70%, 60%, 0)`)
      
      this.ctx.beginPath()
      this.ctx.arc(particle.x, particle.y, particle.radius * 2.5, 0, Math.PI * 2)
      this.ctx.fillStyle = gradient
      this.ctx.fill()
      
      // 核心粒子
      this.ctx.beginPath()
      this.ctx.arc(particle.x, particle.y, particle.radius, 0, Math.PI * 2)
      this.ctx.fillStyle = `hsla(${this.hue}, 80%, 70%, 1)`
      this.ctx.fill()
      
      // 连线效果（只连接距离近的粒子）
      for (let j = i + 1; j < this.particles.length; j++) {
        const otherParticle = this.particles[j]
        const dx = particle.x - otherParticle.x
        const dy = particle.y - otherParticle.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        
        if (dist < 120) {
          this.ctx.beginPath()
          this.ctx.moveTo(particle.x, particle.y)
          this.ctx.lineTo(otherParticle.x, otherParticle.y)
          this.ctx.strokeStyle = `hsla(${this.hue}, 70%, 60%, ${(1 - dist / 120) * 0.6})`
          this.ctx.lineWidth = 1
          this.ctx.stroke()
        }
      }
    })
    
    // 鼠标点击时显示光环
    if (this.mouse.clicked) {
      const ringGradient = this.ctx.createRadialGradient(
        this.mouse.x, this.mouse.y, this.mouse.radius * 0.5,
        this.mouse.x, this.mouse.y, this.mouse.radius
      )
      ringGradient.addColorStop(0, `hsla(${this.hue}, 70%, 60%, 0)`)
      ringGradient.addColorStop(0.7, `hsla(${this.hue}, 70%, 60%, 0.4)`)
      ringGradient.addColorStop(1, `hsla(${this.hue}, 70%, 60%, 0)`)
      
      this.ctx.beginPath()
      this.ctx.arc(this.mouse.x, this.mouse.y, this.mouse.radius, 0, Math.PI * 2)
      this.ctx.fillStyle = ringGradient
      this.ctx.fill()
    }
    
    this.hue += 0.3
    if (this.hue > 360) this.hue = 0
  }
  
  animate() {
    this.draw()
    this.animationId = requestAnimationFrame(() => this.animate())
  }
  
  destroy() {
    // 清理资源
    if (this.animationId) {
      cancelAnimationFrame(this.animationId)
    }
    if (this.resizeTimer) {
      clearTimeout(this.resizeTimer)
    }
    window.removeEventListener('resize', this.handleResize)
    window.removeEventListener('mousemove', this.handleMouseMove)
    window.removeEventListener('mousedown', this.handleMouseDown)
    window.removeEventListener('mouseup', this.handleMouseUp)
  }
}

let fluidAnim = null
let timer = null

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  
  // 初始化流体动画
  const canvas = document.getElementById('fluidCanvas')
  if (canvas) {
    fluidAnim = new FluidAnimation(canvas)
    fluidAnim.animate()
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (fluidAnim) fluidAnim.destroy()
})
</script>

<style scoped>
.home-container {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0e27 0%, #1a1f45 50%, #0a0e27 100%);
  overflow: hidden;
}

.fluid-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.content-wrapper {
  position: relative;
  z-index: 1;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 玻璃态效果 */
.glass-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

/* 导航栏 */
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 30px;
  margin-bottom: 60px;
  color: white;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-text {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #00d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-time {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
}

/* 主标题区 */
.hero-section {
  text-align: center;
  color: white;
  margin-bottom: 80px;
  animation: fadeIn 0.8s ease;
}

.main-title {
  font-size: 64px;
  font-weight: 800;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #ffffff 0%, #667eea 50%, #00d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 40px rgba(102, 126, 234, 0.3);
}

.subtitle {
  font-size: 22px;
  font-weight: 300;
  letter-spacing: 4px;
  opacity: 0.85;
  text-transform: uppercase;
}

/* 功能卡片网格 */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 32px;
  margin-bottom: 80px;
}

.nav-card {
  padding: 40px;
  cursor: pointer;
  color: white;
  animation: fadeInUp 0.6s ease forwards;
  opacity: 0;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  position: relative;
  overflow: hidden;
}

.nav-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  transition: left 0.5s;
}

.nav-card:hover::before {
  left: 100%;
}

.nav-card:hover {
  transform: translateY(-8px) scale(1.02);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 16px 48px rgba(102, 126, 234, 0.4);
}

.card-icon {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  color: white;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
}

.card-title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 16px;
  letter-spacing: 1px;
}

.card-desc {
  font-size: 15px;
  line-height: 1.7;
  opacity: 0.9;
  margin-bottom: 24px;
  min-height: 48px;
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 15px;
  font-weight: 600;
  transition: gap 0.3s ease;
}

.nav-card:hover .card-footer {
  gap: 12px;
}

/* 页脚 */
.footer {
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
  padding: 40px 0;
  font-size: 14px;
  letter-spacing: 1px;
}

/* 动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .main-title {
    font-size: 36px;
  }
  
  .subtitle {
    font-size: 16px;
    letter-spacing: 2px;
  }
  
  .cards-grid {
    grid-template-columns: 1fr;
  }
  
  .navbar {
    flex-direction: column;
    gap: 16px;
  }
}
</style>
