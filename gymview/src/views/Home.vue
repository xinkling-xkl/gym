<template>
  <div class="home-container">
    <header class="header">
      <h1>健身房管理系统</h1>
      <div class="user-info">
        <span>欢迎, {{ userName }}</span>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <main class="main-content">
      <div class="dashboard">
        <div class="card">
          <div class="card-icon">👤</div>
          <div class="card-content">
            <h3>我的信息</h3>
            <p>账号: {{ userAccount }}</p>
          </div>
        </div>

        <div class="card">
          <div class="card-icon">📅</div>
          <div class="card-content">
            <h3>我的课程</h3>
            <p>{{ orders.length }} 门课程</p>
          </div>
        </div>

        <div class="card">
          <div class="card-icon">💪</div>
          <div class="card-content">
            <h3>可用器材</h3>
            <p>{{ availableEquipment }} 件</p>
          </div>
        </div>
      </div>

      <div class="section">
        <h2>我的课程订单</h2>
        <div v-if="orders.length > 0" class="table-container">
          <table>
            <thead>
              <tr>
                <th>订单ID</th>
                <th>课程名称</th>
                <th>教练</th>
                <th>开始时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in orders" :key="order.classOrderId">
                <td>{{ order.classOrderId }}</td>
                <td>{{ getClassName(order.classId) }}</td>
                <td>{{ order.coach }}</td>
                <td>{{ formatDate(order.classBegin) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <p>暂无课程订单</p>
        </div>
      </div>

      <div class="section">
        <h2>全部课程</h2>
        <div class="classes-grid">
          <div v-for="cls in classes" :key="cls.classId" class="class-card">
            <h3>{{ cls.className }}</h3>
            <p>教练: {{ cls.coach }}</p>
            <p>时间: {{ formatDate(cls.classBegin) }}</p>
            <p>时长: {{ cls.classTime }}</p>
            <button class="order-btn" @click="handleOrder(cls)">预约课程</button>
          </div>
        </div>
      </div>

      <div class="section">
        <h2>器材状态</h2>
        <div class="equipment-grid">
          <div 
            v-for="equipment in equipments" 
            :key="equipment.equipmentId"
            :class="['equipment-card', equipment.equipmentStatus === '正常' ? 'normal' : 'broken']"
          >
            <h3>{{ equipment.equipmentName }}</h3>
            <p>位置: {{ equipment.equipmentLocation }}</p>
            <p :class="['status', equipment.equipmentStatus === '正常' ? 'normal' : 'broken']">
              {{ equipment.equipmentStatus }}
            </p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const userName = ref(localStorage.getItem('name') || '')
const userAccount = ref(localStorage.getItem('account') || '')
const orders = ref([])
const classes = ref([])
const equipments = ref([])

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getClassName = (classId) => {
  const cls = classes.value.find(c => c.classId === classId)
  return cls ? cls.className : '未知课程'
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('name')
  localStorage.removeItem('account')
  window.location.href = '/'
}

const fetchOrders = async () => {
  try {
    const response = await axios.get(`/api/order/member/${userAccount.value}`)
    if (response.data.code === 200) {
      orders.value = response.data.data
    }
  } catch (error) {
    console.error('Fetch orders error:', error)
  }
}

const fetchClasses = async () => {
  try {
    const response = await axios.get('/api/class/list')
    if (response.data.code === 200) {
      classes.value = response.data.data
    }
  } catch (error) {
    console.error('Fetch classes error:', error)
  }
}

const fetchEquipments = async () => {
  try {
    const response = await axios.get('/api/equipment/list')
    if (response.data.code === 200) {
      equipments.value = response.data.data
    }
  } catch (error) {
    console.error('Fetch equipments error:', error)
  }
}

const handleOrder = async (cls) => {
  try {
    const response = await axios.post('/api/order/add', {
      classId: cls.classId,
      coach: cls.coach,
      memberName: userName.value,
      memberAccount: userAccount.value,
      classBegin: cls.classBegin
    })
    if (response.data.code === 200) {
      alert('预约成功')
      fetchOrders()
    } else {
      alert('预约失败: ' + response.data.message)
    }
  } catch (error) {
    console.error('Order error:', error)
    alert('预约失败')
  }
}

const availableEquipment = () => {
  return equipments.value.filter(e => e.equipmentStatus === '正常').length
}

onMounted(() => {
  fetchOrders()
  fetchClasses()
  fetchEquipments()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  padding: 20px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h1 {
  margin: 0;
  font-size: 24px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info span {
  font-size: 16px;
}

.logout-btn {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid white;
  color: white;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.3s;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.main-content {
  padding: 40px;
}

.dashboard {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 40px;
}

.card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 20px;
}

.card-icon {
  font-size: 48px;
}

.card-content h3 {
  margin: 0 0 10px 0;
  color: #333;
}

.card-content p {
  margin: 5px 0;
  color: #666;
}

.section {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

.section h2 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 20px;
}

.table-container {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

th {
  background: #f8f9fa;
  color: #666;
  font-weight: 600;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.classes-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.class-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
  transition: box-shadow 0.3s;
}

.class-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.class-card h3 {
  margin: 0 0 10px 0;
  color: #333;
}

.class-card p {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.order-btn {
  margin-top: 15px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.order-btn:hover {
  opacity: 0.9;
}

.equipment-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.equipment-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
}

.equipment-card h3 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 16px;
}

.equipment-card p {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.status.normal {
  color: #22c55e;
  font-weight: 600;
}

.status.broken {
  color: #ef4444;
  font-weight: 600;
}

.equipment-card.normal {
  border-color: #22c55e;
}

.equipment-card.broken {
  border-color: #ef4444;
}
</style>
