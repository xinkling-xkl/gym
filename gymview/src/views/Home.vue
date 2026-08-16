<template>
  <div class="home-container">
    <header class="header">
      <h1>健身房管理系统</h1>
      <div class="user-info">
        <NotificationBell />
        <img v-if="memberInfo && memberInfo.avatar" :src="'/pictures' + memberInfo.avatar" class="header-avatar" alt="头像" />
        <span>欢迎, {{ userName }} (会员)</span>
        <button v-if="role === 'MEMBER'" class="logout-btn" @click="goProfile">个人资料</button>
        <button v-if="role === 'MEMBER'" class="logout-btn plan-btn" @click="goPlan">健身计划</button>
        <button v-if="role === 'MEMBER'" class="logout-btn" @click="showPasswordDialog = true">修改密码</button>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <div class="home-body">
      <main class="main-content">
        <!-- Tab 导航栏 -->
        <div class="tab-nav">
          <button :class="['tab-btn', { active: activeTab === 'overview' }]" @click="activeTab = 'overview'">📊 概览签到</button>
          <button :class="['tab-btn', { active: activeTab === 'orders' }]" @click="activeTab = 'orders'">📋 我的课程</button>
          <button :class="['tab-btn', { active: activeTab === 'classes' }]" @click="activeTab = 'classes'">🏫 全部课程</button>
          <button :class="['tab-btn', { active: activeTab === 'equipment' }]" @click="activeTab = 'equipment'">🏋️ 器材状态</button>
          <button :class="['tab-btn', { active: activeTab === 'profile' }]" @click="activeTab = 'profile'">👤 个人信息</button>
        </div>

        <!-- ===== Tab: 概览签到 ===== -->
        <div v-show="activeTab === 'overview'">
          <div class="dashboard">
            <div class="card">
              <div class="card-icon">👤</div>
              <div class="card-content">
                <h3>我的信息</h3>
                <p>账号: {{ userAccount }}</p>
              </div>
            </div>
            <div class="card">
              <div class="card-icon">🎫</div>
              <div class="card-content">
                <h3>剩余天数</h3>
                <p v-if="role === 'MEMBER'">{{ daysRemaining(memberInfo?.cardExpireDate, memberInfo?.cardNextClass) }} 天</p>
                <p v-else>--</p>
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

          <!-- 签到打卡 -->
          <div class="section checkin-section">
            <h2>🏃 签到打卡</h2>
            <div v-if="todayCheckIn" class="checkin-done">
              <span class="checkin-icon">✅</span>
              <div>
                <p class="checkin-time">今日已签到 - {{ formatDate(todayCheckIn.checkInTime) }}</p>
                <p class="checkin-type">{{ todayCheckIn.checkInType === 'CLASS' ? '上课签到' : '自主训练签到' }}</p>
              </div>
            </div>
            <div v-else class="checkin-actions">
              <button class="checkin-btn" @click="handleCheckIn('GYM')">💪 自主训练签到</button>
              <span class="checkin-hint">今日尚未签到，点击上方按钮完成签到</span>
            </div>
          </div>

          <!-- 课程签到 -->
          <div class="section class-checkin-section" v-if="role === 'MEMBER' && signinableOrders.length > 0">
            <h2>📗 课程签到</h2>
            <p class="checkin-hint">以下课程已开课，可在开课 4 小时内签到</p>
            <div class="table-container">
              <div class="search-bar">
                <input v-model="signinableSearch" placeholder="搜索课程名称/教练" class="filter-input" />
              </div>
              <table>
                <thead>
                  <tr><th>订单ID</th><th>课程名称</th><th>教练</th><th>开课时间</th><th>状态</th><th>操作</th></tr>
                </thead>
                <tbody>
                  <tr v-for="o in signinablePaged" :key="o.classOrderId">
                    <td>{{ o.classOrderId }}</td>
                    <td>{{ o.className || getClassName(o.classId) }}</td>
                    <td>{{ o.coach }}</td>
                    <td>{{ formatDate(o.classBegin) }}</td>
                    <td><span :class="['status-tag', orderStatusClass(o.status)]">{{ orderStatusText(o.status) }}</span></td>
                    <td>
                      <button v-if="o.status === 'BOOKED'" class="checkin-btn class-checkin-btn" @click="handleClassCheckIn(o.classOrderId)">课程签到</button>
                      <span v-else style="color:#22c55e;font-weight:600;">已签到</span>
                    </td>
                  </tr>
                  <tr v-if="signinableFiltered.length === 0">
                    <td colspan="6" style="text-align:center;color:#999;padding:24px;">暂无数据</td>
                  </tr>
                </tbody>
              </table>
              <div class="pagination" v-if="signinableFiltered.length > 0">
                <button :disabled="signinablePage === 1" @click="signinablePage--">上一页</button>
                <span class="page-info">{{ signinablePage }}/{{ signinableTotalPages }} 页</span>
                <button :disabled="signinablePage === signinableTotalPages" @click="signinablePage++">下一页</button>
              </div>
            </div>
          </div>
        </div>

        <!-- ===== Tab: 我的课程 ===== -->
        <div v-show="activeTab === 'orders'" class="section">
          <div class="section-header">
            <h2>我的课程订单</h2>
            <div class="filter-actions">
              <select v-model="orderFilterStatus" class="filter-select">
                <option value="">全部状态</option>
                <option value="BOOKED">已预约</option>
                <option value="COMPLETED">已完成</option>
                <option value="CANCELLED">已取消</option>
                <option value="NO_SHOW">旷课</option>
              </select>
              <input v-model="orderFilterCoach" placeholder="按教练筛选" class="filter-input" />
            </div>
          </div>
          <div v-if="upcomingOrders.length > 0" class="table-container">
            <h3 class="sub-title">📅 即将到来（{{ upcomingOrders.length }}）</h3>
            <div class="search-bar">
              <input v-model="upcomingSearch" placeholder="搜索课程名称/教练" class="filter-input" />
            </div>
            <table>
              <thead>
                <tr><th>订单ID</th><th>课程名称</th><th>教练</th><th>开始时间</th><th>状态</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="order in upcomingPaged" :key="order.classOrderId">
                  <td>{{ order.classOrderId }}</td>
                  <td>{{ order.className || getClassName(order.classId) }}</td>
                  <td>{{ order.coach }}</td>
                  <td>{{ formatDate(order.classBegin) }}</td>
                  <td><span :class="['status-tag', orderStatusClass(order.status)]">{{ orderStatusText(order.status) }}</span></td>
                  <td>
                    <button v-if="order.status === 'BOOKED'" class="cancel-btn" @click="handleCancel(order.classOrderId)">取消预约</button>
                    <span v-else style="color:#999;">—</span>
                  </td>
                </tr>
                <tr v-if="upcomingFiltered.length === 0">
                  <td colspan="6" style="text-align:center;color:#999;padding:24px;">暂无数据</td>
                </tr>
              </tbody>
            </table>
            <div class="pagination" v-if="upcomingFiltered.length > 0">
              <button :disabled="upcomingPage === 1" @click="upcomingPage--">上一页</button>
              <span class="page-info">{{ upcomingPage }}/{{ upcomingTotalPages }} 页</span>
              <button :disabled="upcomingPage === upcomingTotalPages" @click="upcomingPage++">下一页</button>
            </div>
          </div>
          <div v-if="historyOrders.length > 0" class="table-container" style="margin-top:20px;">
            <h3 class="sub-title">📚 历史记录（{{ historyOrders.length }}）</h3>
            <div class="search-bar">
              <input v-model="historySearch" placeholder="搜索课程名称/教练/状态" class="filter-input" />
            </div>
            <table>
              <thead>
                <tr><th>订单ID</th><th>课程名称</th><th>教练</th><th>开始时间</th><th>状态</th></tr>
              </thead>
              <tbody>
                <tr v-for="order in historyPaged" :key="order.classOrderId">
                  <td>{{ order.classOrderId }}</td>
                  <td>{{ order.className || getClassName(order.classId) }}</td>
                  <td>{{ order.coach }}</td>
                  <td>{{ formatDate(order.classBegin) }}</td>
                  <td><span :class="['status-tag', orderStatusClass(order.status)]">{{ orderStatusText(order.status) }}</span></td>
                </tr>
                <tr v-if="historyFiltered.length === 0">
                  <td colspan="5" style="text-align:center;color:#999;padding:24px;">暂无数据</td>
                </tr>
              </tbody>
            </table>
            <div class="pagination" v-if="historyFiltered.length > 0">
              <button :disabled="historyPage === 1" @click="historyPage--">上一页</button>
              <span class="page-info">{{ historyPage }}/{{ historyTotalPages }} 页</span>
              <button :disabled="historyPage === historyTotalPages" @click="historyPage++">下一页</button>
            </div>
          </div>
          <div v-if="orders.length === 0" class="empty-state"><p>暂无课程订单</p></div>
        </div>

        <!-- ===== Tab: 全部课程 ===== -->
        <div v-show="activeTab === 'classes'" class="section">
          <h2>全部课程</h2>
          <div class="classes-grid">
            <div v-for="cls in classes" :key="cls.classId" class="class-card">
              <h3>{{ cls.className }}</h3>
              <p>教练: {{ cls.coach }}</p>
              <p>时间: {{ formatDate(cls.classBegin) }}</p>
              <p>时长: {{ cls.classTime }}</p>
              <p v-if="cls.maxCapacity" class="capacity-info">已约: {{ cls.bookedCount || 0 }} / {{ cls.maxCapacity }}</p>
              <button class="order-btn" @click="handleOrder(cls)" :disabled="cardExpired || isClassFull(cls)" :class="{ disabled: cardExpired || isClassFull(cls) }">
                {{ cardExpired ? '会员卡已过期' : (isClassFull(cls) ? '已约满' : '预约课程') }}
              </button>
            </div>
          </div>
        </div>

        <!-- ===== Tab: 器材状态 ===== -->
        <div v-show="activeTab === 'equipment'" class="section">
          <h2>器材状态</h2>
          <div class="equipment-grid">
            <div v-for="equipment in equipments" :key="equipment.equipmentId" :class="['equipment-card', equipment.equipmentStatus === '正常' ? 'normal' : 'broken']">
              <h3>{{ equipment.equipmentName }}</h3>
              <p>位置: {{ equipment.equipmentLocation }}</p>
              <p :class="['status', equipment.equipmentStatus === '正常' ? 'normal' : 'broken']">{{ equipment.equipmentStatus }}</p>
            </div>
          </div>
        </div>

        <!-- ===== Tab: 个人信息 ===== -->
        <div v-show="activeTab === 'profile'">
          <div class="section" v-if="role === 'MEMBER' && memberInfo">
            <div class="section-header">
              <h2>📋 个人信息详情</h2>
              <button class="edit-profile-btn" @click="goProfile">修改个人资料</button>
            </div>
            <div class="detail-grid">
              <div class="detail-item"><span class="detail-label">账号:</span>{{ memberInfo.memberAccount }}</div>
              <div class="detail-item"><span class="detail-label">姓名:</span>{{ memberInfo.memberName }}</div>
              <div class="detail-item"><span class="detail-label">性别:</span>{{ memberInfo.memberGender }}</div>
              <div class="detail-item"><span class="detail-label">年龄:</span>{{ memberInfo.memberAge }} 岁</div>
              <div class="detail-item"><span class="detail-label">身高:</span>{{ memberInfo.memberHeight }} cm</div>
              <div class="detail-item"><span class="detail-label">体重:</span>{{ memberInfo.memberWeight }} kg</div>
              <div class="detail-item"><span class="detail-label">BMI:</span>{{ calcBMI(memberInfo) }} {{ bmiCategory(calcBMI(memberInfo)) }}</div>
              <div class="detail-item"><span class="detail-label">电话:</span>{{ memberInfo.memberPhone }}</div>
              <div class="detail-item"><span class="detail-label">会员卡:</span>{{ cardTypeText(memberInfo.cardClass) }}</div>
              <div class="detail-item"><span class="detail-label">剩余天数:</span>{{ daysRemaining(memberInfo.cardExpireDate, memberInfo.cardNextClass) }} 天</div>
              <div class="detail-item"><span class="detail-label">办卡时间:</span>{{ memberInfo.cardTime }}</div>
              <div class="detail-item"><span class="detail-label">过期时间:</span>{{ memberInfo.cardExpireDate }}</div>
            </div>
          </div>

          <div class="section" v-if="role === 'MEMBER'">
            <h2>📊 签到历史</h2>
            <div class="table-container">
              <table>
                <thead>
                  <tr><th>签到时间</th><th>签到类型</th><th>备注</th></tr>
                </thead>
                <tbody>
                  <tr v-for="ci in checkinHistory.slice(0, 10)" :key="ci.checkInId">
                    <td>{{ formatDate(ci.checkInTime) }}</td>
                    <td>{{ ci.checkInType === 'CLASS' ? '上课签到' : '自主训练' }}</td>
                    <td>{{ ci.remark || '-' }}</td>
                  </tr>
                </tbody>
              </table>
              <div v-if="checkinHistory.length === 0" class="empty-state">暂无签到记录</div>
            </div>
          </div>
        </div>
      </main>

      <aside class="ai-sidebar" v-if="role === 'MEMBER'">
        <AiChat />
      </aside>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="showPasswordDialog" class="modal-overlay" @click.self="showPasswordDialog = false">
      <div class="modal">
        <h3>修改密码</h3>
        <div class="form-row">
          <label>原密码</label>
          <input v-model="passwordForm.oldPassword" type="password" />
        </div>
        <div class="form-row">
          <label>新密码</label>
          <input v-model="passwordForm.newPassword" type="password" />
        </div>
        <div class="form-row">
          <label>确认新密码</label>
          <input v-model="passwordForm.confirmPassword" type="password" />
        </div>
        <div class="modal-actions">
          <button class="logout-btn" @click="showPasswordDialog = false">取消</button>
          <button class="checkin-btn" @click="handleChangePassword">确认修改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import NotificationBell from '../components/NotificationBell.vue'
import AiChat from '../components/AiChat.vue'

const router = useRouter()
const userName = ref(sessionStorage.getItem('name') || '')
const userAccount = ref(sessionStorage.getItem('account') || '')
const role = ref(sessionStorage.getItem('role') || '')
const orders = ref([])
const classes = ref([])
const equipments = ref([])
const memberInfo = ref(null)
const todayCheckIn = ref(null)
const checkinHistory = ref([])
const showPasswordDialog = ref(false)
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const orderFilterStatus = ref('')
const orderFilterCoach = ref('')

// Tab 标签页当前激活项
const activeTab = ref('overview')

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getClassName = (classId) => {
  const cls = classes.value.find(c => c.classId === classId)
  return cls ? cls.className : '未知课程'
}

const handleLogout = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('name')
  sessionStorage.removeItem('account')
  window.location.href = '/'
}

const goProfile = () => {
  router.push('/profile')
}

const goPlan = () => {
  router.push('/plan')
}

const cardTypeText = (cardClass) => {
  return { 1: '月卡', 2: '季卡', 3: '年卡' }[cardClass] || '-'
}

const calcBMI = (m) => {
  if (!m || !m.memberHeight || !m.memberWeight) return '-'
  const h = m.memberHeight / 100
  return (m.memberWeight / (h * h)).toFixed(1)
}

const bmiCategory = (bmi) => {
  if (bmi === '-') return ''
  const v = parseFloat(bmi)
  if (v < 18.5) return '(偏瘦)'
  if (v < 24) return '(正常)'
  if (v < 28) return '(偏胖)'
  return '(肥胖)'
}

const daysRemaining = (expireDate, cardNextClass) => {
  if (cardNextClass != null) {
    return cardNextClass > 0 ? cardNextClass : 0
  }
  if (!expireDate) return '-'
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const expire = new Date(expireDate)
  const diff = Math.ceil((expire - today) / (1000 * 60 * 60 * 24))
  return diff > 0 ? diff : 0
}

const fetchMemberInfo = async () => {
  if (role.value !== 'MEMBER') return
  try {
    const response = await axios.get(`/api/member/${userAccount.value}`)
    if (response.data.code === 200) {
      memberInfo.value = response.data.data
    }
  } catch (error) {
    console.error('Fetch member info error:', error)
  }
}

const fetchTodayCheckIn = async () => {
  try {
    const response = await axios.get(`/api/checkin/today/${userAccount.value}`)
    if (response.data.code === 200) {
      todayCheckIn.value = response.data.data
    }
  } catch (error) {
    console.error('Fetch checkin error:', error)
  }
}

const fetchOrders = async () => {
  if (role.value !== 'MEMBER') return
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
    const response = await axios.get('/api/class/available')
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

const handleCheckIn = async (type) => {
  try {
    const response = await axios.post('/api/checkin', {
      memberAccount: Number(userAccount.value),
      memberName: userName.value,
      checkInType: type
    })
    if (response.data.code === 200) {
      alert('签到成功！')
      fetchTodayCheckIn()
    } else {
      alert(response.data.message)
    }
  } catch (error) {
    console.error('Checkin error:', error)
    alert('签到失败')
  }
}

const handleClassCheckIn = async (classOrderId) => {
  try {
    const response = await axios.post('/api/checkin', {
      memberAccount: Number(userAccount.value),
      memberName: userName.value,
      classOrderId,
      checkInType: 'CLASS'
    })
    if (response.data.code === 200) {
      alert('课程签到成功！')
      fetchOrders()
    } else {
      alert(response.data.message)
    }
  } catch (error) {
    console.error('Class checkin error:', error)
    alert('签到失败')
  }
}

const isClassFull = (cls) => {
  return cls.maxCapacity && (cls.bookedCount || 0) >= cls.maxCapacity
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
      fetchMemberInfo()
    } else {
      alert('预约失败: ' + response.data.message)
    }
  } catch (error) {
    console.error('Order error:', error)
    alert('预约失败')
  }
}

const handleCancel = async (orderId) => {
  if (!confirm('确定要取消该预约吗？课时将退回。')) return
  try {
    const response = await axios.post(`/api/order/cancel/${orderId}`)
    if (response.data.code === 200) {
      alert('取消成功，课时已退回')
      fetchOrders()
      fetchMemberInfo()
    } else {
      alert('取消失败: ' + response.data.message)
    }
  } catch (error) {
    console.error('Cancel error:', error)
    alert('取消失败')
  }
}

const availableEquipment = computed(() => {
  return equipments.value.filter(e => e.equipmentStatus === '正常').length
})

const cardExpired = computed(() => {
  if (!memberInfo.value) return true
  if (memberInfo.value.cardNextClass != null) {
    return memberInfo.value.cardNextClass <= 0
  }
  if (!memberInfo.value.cardExpireDate) return true
  return daysRemaining(memberInfo.value.cardExpireDate) <= 0
})

const signinableOrders = computed(() => {
  const now = Date.now()
  const fourHours = 4 * 60 * 60 * 1000
  return orders.value.filter(o => {
    if (!o.classBegin) return false
    const begin = new Date(o.classBegin).getTime()
    return (o.status === 'BOOKED' || o.status === 'CHECKED_IN')
      && begin <= now && (now - begin) <= fourHours
  })
})

const filteredOrders = computed(() => {
  return orders.value.filter(o => {
    const matchStatus = !orderFilterStatus.value || o.status === orderFilterStatus.value
    const matchCoach = !orderFilterCoach.value || (o.coach || '').includes(orderFilterCoach.value)
    return matchStatus && matchCoach
  })
})

const upcomingOrders = computed(() => {
  const now = Date.now()
  return filteredOrders.value.filter(o =>
    o.status === 'BOOKED' && o.classBegin && new Date(o.classBegin).getTime() >= now
  )
})

const historyOrders = computed(() => {
  const now = Date.now()
  return filteredOrders.value.filter(o =>
    o.status !== 'BOOKED' || (o.classBegin && new Date(o.classBegin).getTime() < now)
  )
})

const orderStatusText = (status) => {
  switch (status) {
    case 'BOOKED': return '已预约'
    case 'CHECKED_IN': return '已签到'
    case 'COMPLETED': return '已完成'
    case 'CANCELLED': return '已取消'
    case 'NO_SHOW': return '旷课'
    default: return status || '-'
  }
}

const orderStatusClass = (status) => {
  switch (status) {
    case 'BOOKED': return 'st-booked'
    case 'CHECKED_IN': return 'st-checked'
    case 'COMPLETED': return 'st-completed'
    case 'CANCELLED': return 'st-cancelled'
    case 'NO_SHOW': return 'st-noshow'
    default: return ''
  }
}

const PAGE_SIZE = 5

const signinableSearch = ref('')
const signinablePage = ref(1)
const signinableFiltered = computed(() => {
  const kw = signinableSearch.value.trim().toLowerCase()
  if (!kw) return signinableOrders.value
  return signinableOrders.value.filter(o => {
    const name = (o.className || getClassName(o.classId) || '').toLowerCase()
    const coach = (o.coach || '').toLowerCase()
    return name.includes(kw) || coach.includes(kw)
  })
})
const signinableTotalPages = computed(() => Math.max(Math.ceil(signinableFiltered.value.length / PAGE_SIZE), 1))
const signinablePaged = computed(() => {
  const start = (signinablePage.value - 1) * PAGE_SIZE
  return signinableFiltered.value.slice(start, start + PAGE_SIZE)
})

const upcomingSearch = ref('')
const upcomingPage = ref(1)
const upcomingFiltered = computed(() => {
  const kw = upcomingSearch.value.trim().toLowerCase()
  if (!kw) return upcomingOrders.value
  return upcomingOrders.value.filter(o => {
    const name = (o.className || getClassName(o.classId) || '').toLowerCase()
    const coach = (o.coach || '').toLowerCase()
    return name.includes(kw) || coach.includes(kw)
  })
})
const upcomingTotalPages = computed(() => Math.max(Math.ceil(upcomingFiltered.value.length / PAGE_SIZE), 1))
const upcomingPaged = computed(() => {
  const start = (upcomingPage.value - 1) * PAGE_SIZE
  return upcomingFiltered.value.slice(start, start + PAGE_SIZE)
})

const historySearch = ref('')
const historyPage = ref(1)
const historyFiltered = computed(() => {
  const kw = historySearch.value.trim().toLowerCase()
  if (!kw) return historyOrders.value
  return historyOrders.value.filter(o => {
    const name = (o.className || getClassName(o.classId) || '').toLowerCase()
    const coach = (o.coach || '').toLowerCase()
    const statusText = (orderStatusText(o.status) || '').toLowerCase()
    return name.includes(kw) || coach.includes(kw) || statusText.includes(kw)
  })
})
const historyTotalPages = computed(() => Math.max(Math.ceil(historyFiltered.value.length / PAGE_SIZE), 1))
const historyPaged = computed(() => {
  const start = (historyPage.value - 1) * PAGE_SIZE
  return historyFiltered.value.slice(start, start + PAGE_SIZE)
})

watch(signinableSearch, () => { signinablePage.value = 1 })
watch(upcomingSearch, () => { upcomingPage.value = 1 })
watch(historySearch, () => { historyPage.value = 1 })

const fetchCheckinHistory = async () => {
  if (role.value !== 'MEMBER') return
  try {
    const response = await axios.get(`/api/checkin/member/${userAccount.value}`)
    if (response.data.code === 200) {
      checkinHistory.value = response.data.data || []
    }
  } catch (error) {
    console.error('Fetch checkin history error:', error)
  }
}

const handleChangePassword = async () => {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value
  if (!oldPassword || !newPassword) {
    alert('请填写原密码和新密码')
    return
  }
  if (newPassword !== confirmPassword) {
    alert('两次输入的新密码不一致')
    return
  }
  if (newPassword.length < 6) {
    alert('新密码长度至少6位')
    return
  }
  try {
    const response = await axios.put('/api/member/password', {
      memberAccount: Number(userAccount.value),
      oldPassword,
      newPassword
    })
    if (response.data.code === 200) {
      alert('密码修改成功，请重新登录')
      showPasswordDialog.value = false
      handleLogout()
    } else {
      alert(response.data.message)
    }
  } catch (error) {
    console.error('Change password error:', error)
    alert('密码修改失败')
  }
}

onMounted(() => {
  fetchMemberInfo()
  fetchOrders()
  fetchClasses()
  fetchEquipments()
  fetchTodayCheckIn()
  fetchCheckinHistory()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.home-body {
  display: flex;
  flex: 1;
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

.plan-btn {
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
  border-color: #22c55e;
}

.main-content {
  flex: 1;
  padding: 24px 40px;
}

.ai-sidebar {
  width: 320px;
  padding: 30px 20px;
  background: #f1f5f9;
}

/* Tab 导航栏 */
.tab-nav {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  background: white;
  padding: 8px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.tab-btn {
  flex: 1;
  padding: 12px 16px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 15px;
  color: #666;
  transition: all 0.25s;
  white-space: nowrap;
}

.tab-btn:hover {
  background: #f0f9ff;
  color: #0ea5e9;
}

.tab-btn.active {
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  font-weight: 600;
}

.dashboard {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
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
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.section h2 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 20px;
}

.checkin-section {
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
  border: 2px solid #22c55e;
}

.checkin-done {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 0;
}

.checkin-icon {
  font-size: 48px;
}

.checkin-time {
  font-size: 18px;
  font-weight: 600;
  color: #16a34a;
  margin: 0;
}

.checkin-type {
  color: #666;
  margin: 4px 0 0 0;
}

.checkin-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.checkin-btn {
  padding: 14px 32px;
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  cursor: pointer;
  transition: opacity 0.3s, transform 0.2s;
}

.checkin-btn:hover {
  opacity: 0.9;
  transform: scale(1.03);
}

.checkin-hint {
  color: #888;
  font-size: 14px;
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

.order-btn:hover:not(.disabled) {
  opacity: 0.9;
}

.order-btn.disabled {
  background: #ccc;
  cursor: not-allowed;
}

.cancel-btn {
  padding: 6px 14px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.3s;
}

.cancel-btn:hover {
  background: #dc2626;
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

.sub-title {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: #475569;
  font-weight: 600;
}

.status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.st-booked { background: #dbeafe; color: #1d4ed8; }
.st-checked { background: #fef3c7; color: #b45309; }
.st-completed { background: #dcfce7; color: #15803d; }
.st-cancelled { background: #f3f4f6; color: #6b7280; }
.st-noshow { background: #fee2e2; color: #b91c1c; }

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 { margin: 0; }

.filter-actions { display: flex; gap: 12px; }

.filter-select, .filter-input {
  padding: 8px 14px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.detail-item {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.detail-label {
  color: #666;
  margin-right: 8px;
  font-size: 14px;
}

.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.modal {
  background: white;
  padding: 30px;
  border-radius: 12px;
  width: 420px;
}

.modal h3 { margin: 0 0 20px 0; color: #333; }

.form-row {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.form-row label {
  width: 100px;
  color: #666;
}

.form-row input {
  flex: 1;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.header-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid white;
}

.edit-profile-btn {
  padding: 8px 18px;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.edit-profile-btn:hover { opacity: 0.9; }

.class-checkin-section {
  background: linear-gradient(135deg, #eff6ff 0%, #f0f9ff 100%);
  border: 2px solid #0ea5e9;
}

.class-checkin-btn {
  padding: 6px 16px;
  font-size: 13px;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
}

.search-bar {
  margin-bottom: 12px;
}

.search-bar .filter-input {
  width: 280px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 8px 0;
}

.pagination button {
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  color: #333;
  font-size: 14px;
  transition: all 0.2s;
}

.pagination button:hover:not(:disabled) {
  border-color: #0ea5e9;
  color: #0ea5e9;
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #555;
}
</style>
