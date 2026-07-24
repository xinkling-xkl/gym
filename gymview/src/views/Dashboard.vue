<template>
  <div class="dashboard-container">
    <header class="header">
      <h1>📊 数据可视化大屏</h1>
      <div class="user-info">
        <span>欢迎, {{ userName }}</span>
        <button class="back-btn" @click="goBack">返回管理</button>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <main class="main-content">
      <!-- 概览卡片 -->
      <div class="overview-cards">
        <div class="overview-card members">
          <div class="card-icon">👥</div>
          <div class="card-info">
            <h3>{{ overview.totalMembers }}</h3>
            <p>会员总数</p>
          </div>
        </div>
        <div class="overview-card orders">
          <div class="card-icon">📅</div>
          <div class="card-info">
            <h3>{{ overview.totalOrders }}</h3>
            <p>预约总数</p>
          </div>
        </div>
        <div class="overview-card checkins">
          <div class="card-icon">✅</div>
          <div class="card-info">
            <h3>{{ today.todayCheckIns }}</h3>
            <p>今日签到</p>
          </div>
        </div>
        <div class="overview-card equipments">
          <div class="card-icon">💪</div>
          <div class="card-info">
            <h3>{{ overview.totalEquipments }}</h3>
            <p>器材总数</p>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="charts-row">
        <div class="chart-box">
          <h3>会员卡种分布</h3>
          <div ref="memberGrowthChart" class="chart"></div>
        </div>
        <div class="chart-box">
          <h3>器材状态分布</h3>
          <div ref="equipmentChart" class="chart"></div>
        </div>
      </div>

      <div class="charts-row">
        <div class="chart-box full-width">
          <h3>热门课程排行</h3>
          <div ref="courseHotChart" class="chart"></div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()
const userName = ref(localStorage.getItem('name') || '')
const overview = ref({ totalMembers: 0, totalEquipments: 0, totalOrders: 0, totalCheckIns: 0 })
const today = ref({ todayCheckIns: 0, todayOrders: 0 })
const memberGrowthChart = ref(null)
const equipmentChart = ref(null)
const courseHotChart = ref(null)

const handleLogout = () => {
  localStorage.clear()
  window.location.href = '/'
}

const goBack = () => {
  router.push('/admin')
}

const loadEcharts = () => {
  return new Promise((resolve) => {
    if (window.echarts) { resolve(); return }
    const script = document.createElement('script')
    script.src = 'https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js'
    script.onload = () => resolve()
    document.head.appendChild(script)
  })
}

const fetchOverview = async () => {
  try {
    const res = await axios.get('/api/statistics/overview')
    if (res.data.code === 200) overview.value = res.data.data
  } catch (e) { console.error(e) }
}

const fetchToday = async () => {
  try {
    const res = await axios.get('/api/statistics/today')
    if (res.data.code === 200) today.value = res.data.data
  } catch (e) { console.error(e) }
}

const fetchMemberGrowth = async () => {
  try {
    const res = await axios.get('/api/statistics/member-growth')
    if (res.data.code === 200) {
      const data = res.data.data
      const chart = window.echarts.init(memberGrowthChart.value)
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: '0%' },
        series: [{
          type: 'pie',
          radius: ['35%', '65%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 8,
            borderColor: '#1a1a2e',
            borderWidth: 3
          },
          label: { show: true, formatter: '{b}: {c}人' },
          emphasis: { label: { fontSize: 18, fontWeight: 'bold' } },
          data: data.map(d => ({ name: d.name, value: d.value }))
        }]
      })
      window.addEventListener('resize', () => chart.resize())
    }
  } catch (e) { console.error(e) }
}

const fetchEquipmentStatus = async () => {
  try {
    const res = await axios.get('/api/statistics/equipment-status')
    if (res.data.code === 200) {
      const data = res.data.data
      const chart = window.echarts.init(equipmentChart.value)
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: '0%' },
        series: [{
          type: 'pie',
          radius: '70%',
          data: Object.entries(data).map(([name, value]) => ({ name, value })),
          emphasis: {
            itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
          },
          itemStyle: {
            borderRadius: 6,
            borderColor: '#1a1a2e',
            borderWidth: 2
          }
        }]
      })
      window.addEventListener('resize', () => chart.resize())
    }
  } catch (e) { console.error(e) }
}

const fetchCourseHot = async () => {
  try {
    const res = await axios.get('/api/statistics/course-hot')
    if (res.data.code === 200) {
      const data = res.data.data
      const chart = window.echarts.init(courseHotChart.value)
      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        xAxis: {
          type: 'category',
          data: data.map(d => '课程' + d.classId),
          axisLabel: { color: '#ccc' }
        },
        yAxis: {
          type: 'value',
          name: '预约次数',
          axisLabel: { color: '#ccc' },
          splitLine: { lineStyle: { color: '#333' } }
        },
        series: [{
          type: 'bar',
          data: data.map(d => d.count),
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: '#0ea5e9' },
                { offset: 1, color: '#f97316' }
              ]
            }
          }
        }]
      })
      window.addEventListener('resize', () => chart.resize())
    }
  } catch (e) { console.error(e) }
}

onMounted(async () => {
  await loadEcharts()
  await Promise.all([fetchOverview(), fetchToday()])
  await nextTick()
  fetchMemberGrowth()
  fetchEquipmentStatus()
  fetchCourseHot()
})
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  background: #1a1a2e;
  color: #eee;
}

.header {
  background: linear-gradient(135deg, #16213e 0%, #0f3460 100%);
  padding: 20px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px solid #0ea5e9;
}

.header h1 {
  margin: 0;
  font-size: 24px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  padding: 8px 18px;
  background: rgba(14, 165, 233, 0.3);
  border: 1px solid #0ea5e9;
  color: #0ea5e9;
  border-radius: 6px;
  cursor: pointer;
}

.logout-btn {
  padding: 8px 18px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid #555;
  color: #ccc;
  border-radius: 6px;
  cursor: pointer;
}

.main-content {
  padding: 30px 40px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.overview-card {
  background: #16213e;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  border-left: 4px solid #0ea5e9;
  transition: transform 0.2s;
}

.overview-card:hover { transform: translateY(-4px); }
.overview-card.members { border-left-color: #0ea5e9; }
.overview-card.orders { border-left-color: #f97316; }
.overview-card.checkins { border-left-color: #22c55e; }
.overview-card.equipments { border-left-color: #a855f7; }

.card-icon { font-size: 42px; }
.card-info h3 { margin: 0; font-size: 32px; color: #fff; }
.card-info p { margin: 4px 0 0 0; color: #888; font-size: 14px; }

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-box.full-width {
  grid-column: 1 / -1;
}

.chart-box {
  background: #16213e;
  border-radius: 12px;
  padding: 20px;
}

.chart-box h3 {
  margin: 0 0 16px 0;
  color: #ccc;
  font-size: 16px;
}

.chart {
  width: 100%;
  height: 320px;
}
</style>
