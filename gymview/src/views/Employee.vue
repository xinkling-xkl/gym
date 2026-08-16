<template>
  <div class="employee-container">
    <header class="header">
      <h1>健身房管理系统 - 员工工作台</h1>
      <div class="user-info">
        <NotificationBell />
        <span>欢迎, {{ userName }} ({{ staff === '前台' ? '前台' : '教练' }})</span>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <main class="main-content">
      <!-- ============ 前台员工模块 ============ -->
      <template v-if="staff === '前台'">
        <!-- 数据概览 -->
        <div class="dashboard">
          <div class="card">
            <div class="card-icon">👥</div>
            <div class="card-content">
              <h3>会员总数</h3>
              <p>{{ members.length }} 人</p>
            </div>
          </div>
          <div class="card">
            <div class="card-icon">📋</div>
            <div class="card-content">
              <h3>订单总数</h3>
              <p>{{ allOrders.length }} 条</p>
            </div>
          </div>
          <div class="card">
            <div class="card-icon">📅</div>
            <div class="card-content">
              <h3>今日预约</h3>
              <p>{{ todayOrders }} 条</p>
            </div>
          </div>
          <div class="card">
            <div class="card-icon">⚠️</div>
            <div class="card-content">
              <h3>已过期会员</h3>
              <p>{{ expiredMembers }} 人</p>
            </div>
          </div>
        </div>

        <!-- 会员管理（仅添加+查看） -->
        <div class="section">
          <div class="section-header">
            <h2>👥 会员管理</h2>
            <div class="section-actions">
              <input v-model="memberSearch" placeholder="搜索账号/姓名/电话" class="search-input" />
              <button class="btn btn-primary" @click="showAddMemberDialog = true">+ 添加会员</button>
            </div>
          </div>
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>账号</th>
                  <th>姓名</th>
                  <th>性别</th>
                  <th>年龄</th>
                  <th>电话</th>
                  <th>会员卡</th>
                  <th>剩余天数</th>
                  <th>过期时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="m in memberPaged" :key="m.memberAccount">
                  <td>{{ m.memberAccount }}</td>
                  <td>{{ m.memberName }}</td>
                  <td>{{ m.memberGender }}</td>
                  <td>{{ m.memberAge }}</td>
                  <td>{{ m.memberPhone }}</td>
                  <td>{{ cardTypeText(m.cardClass) }}</td>
                  <td>{{ m.cardNextClass ?? '-' }} 天</td>
                  <td>{{ m.cardExpireDate || '-' }}</td>
                  <td>
                    <button class="btn btn-link" @click="viewMember(m)">查看详情</button>
                    <button class="btn btn-primary btn-sm" @click="openRenewDialog(m)">续费</button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div class="pagination">
              <button class="btn btn-default btn-sm" :disabled="memberPage <= 1" @click="memberPage--">上一页</button>
              <span>第 {{ memberPage }} / {{ memberTotalPages }} 页</span>
              <button class="btn btn-default btn-sm" :disabled="memberPage >= memberTotalPages" @click="memberPage++">下一页</button>
            </div>
          </div>
        </div>

        <!-- 订单管理（仅查看） -->
        <div class="section">
          <div class="section-header">
            <h2>📋 订单管理（只读）</h2>
            <div class="section-actions">
              <select v-model="orderStatusFilter" class="search-input">
                <option value="">全部状态</option>
                <option value="BOOKED">已预约</option>
                <option value="CHECKED_IN">已签到</option>
                <option value="COMPLETED">已完成</option>
                <option value="CANCELLED">已取消</option>
                <option value="NO_SHOW">旷课</option>
              </select>
              <input v-model="orderCoachFilter" placeholder="按教练筛选" class="search-input" />
            </div>
          </div>
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>订单ID</th>
                  <th>课程</th>
                  <th>教练</th>
                  <th>会员</th>
                  <th>开始时间</th>
                  <th>状态</th>
                  <th>创建时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in allOrderPaged" :key="o.classOrderId">
                  <td>{{ o.classOrderId }}</td>
                  <td>{{ o.className }}</td>
                  <td>{{ o.coach }}</td>
                  <td>{{ o.memberName }} ({{ o.memberAccount }})</td>
                  <td>{{ formatDate(o.classBegin) }}</td>
                  <td><span :class="['status-tag', statusClass(o.status)]">{{ statusText(o.status) }}</span></td>
                  <td>{{ formatDate(o.createTime) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="filteredAllOrders.length === 0" class="empty-state">暂无订单</div>
            <div class="pagination" v-if="filteredAllOrders.length > 0">
              <button class="btn btn-default btn-sm" :disabled="allOrderPage <= 1" @click="allOrderPage--">上一页</button>
              <span>第 {{ allOrderPage }} / {{ allOrderTotalPages }} 页</span>
              <button class="btn btn-default btn-sm" :disabled="allOrderPage >= allOrderTotalPages" @click="allOrderPage++">下一页</button>
            </div>
          </div>
        </div>
      </template>

      <!-- ============ 教练模块 ============ -->
      <template v-else-if="staff === '教练'">
        <!-- 教练数据概览 -->
        <div class="dashboard">
          <div class="card">
            <div class="card-icon">📚</div>
            <div class="card-content">
              <h3>我的课程</h3>
              <p>{{ myClasses.length }} 门</p>
            </div>
          </div>
          <div class="card">
            <div class="card-icon">📝</div>
            <div class="card-content">
              <h3>学员预约</h3>
              <p>{{ myOrders.length }} 条</p>
            </div>
          </div>
          <div class="card">
            <div class="card-icon">✅</div>
            <div class="card-content">
              <h3>已完成</h3>
              <p>{{ myOrders.filter(o => o.status === 'COMPLETED').length }} 条</p>
            </div>
          </div>
          <div class="card">
            <div class="card-icon">📊</div>
            <div class="card-content">
              <h3>出勤率</h3>
              <p>{{ attendanceRate }}%</p>
            </div>
          </div>
        </div>

        <!-- 我的课程 -->
        <div class="section">
          <div class="section-header">
            <h2>📚 我的课程</h2>
            <div class="section-actions">
              <input v-model="myClassSearch" placeholder="搜索课程名称/教练" class="search-input" />
              <button class="btn btn-primary" @click="openClassDialog()">+ 发布课程</button>
            </div>
          </div>
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>课程名称</th>
                  <th>开课时间</th>
                  <th>课程时长</th>
                  <th>人数上限</th>
                  <th>已预约</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="cls in myClassPaged" :key="cls.classId">
                  <td>{{ cls.className }}</td>
                  <td>{{ formatDate(cls.classBegin) }}</td>
                  <td>{{ cls.classTime }}</td>
                  <td>{{ cls.maxCapacity || '不限' }}</td>
                  <td>{{ bookedCount(cls.classId) }}</td>
                  <td>
                    <button class="btn btn-primary btn-sm" @click="openClassDialog(cls)">编辑</button>
                    <button class="btn btn-danger btn-sm" @click="deleteClass(cls)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="myClassFiltered.length === 0" class="empty-state">暂无课程，点击"发布课程"添加</div>
            <div class="pagination" v-if="myClassFiltered.length > 0">
              <button class="btn btn-default btn-sm" :disabled="myClassPage <= 1" @click="myClassPage--">上一页</button>
              <span>第 {{ myClassPage }} / {{ myClassTotalPages }} 页</span>
              <button class="btn btn-default btn-sm" :disabled="myClassPage >= myClassTotalPages" @click="myClassPage++">下一页</button>
            </div>
          </div>
        </div>

        <!-- 学员订单（可操作） -->
        <div class="section">
          <div class="section-header">
            <h2>📝 学员订单管理</h2>
            <div class="section-actions">
              <select v-model="myOrderStatusFilter" class="search-input">
                <option value="">全部状态</option>
                <option value="BOOKED">已预约</option>
                <option value="CHECKED_IN">已签到</option>
                <option value="COMPLETED">已完成</option>
                <option value="CANCELLED">已取消</option>
                <option value="NO_SHOW">旷课</option>
              </select>
            </div>
          </div>
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>订单ID</th>
                  <th>课程</th>
                  <th>会员</th>
                  <th>开始时间</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in myOrderPaged" :key="o.classOrderId">
                  <td>{{ o.classOrderId }}</td>
                  <td>{{ o.className }}</td>
                  <td>{{ o.memberName }} ({{ o.memberAccount }})</td>
                  <td>{{ formatDate(o.classBegin) }}</td>
                  <td><span :class="['status-tag', statusClass(o.status)]">{{ statusText(o.status) }}</span></td>
                  <td>
                    <template v-if="o.status === 'BOOKED' || o.status === 'CHECKED_IN'">
                      <button class="btn btn-success btn-sm" @click="completeOrder(o.classOrderId)">完成</button>
                      <button class="btn btn-danger btn-sm" @click="noshowOrder(o.classOrderId)">旷课</button>
                      <button class="btn btn-warning btn-sm" @click="cancelOrder(o.classOrderId)">取消</button>
                    </template>
                    <span v-else style="color:#999;">—</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="filteredMyOrders.length === 0" class="empty-state">暂无订单</div>
            <div class="pagination" v-if="filteredMyOrders.length > 0">
              <button class="btn btn-default btn-sm" :disabled="myOrderPage <= 1" @click="myOrderPage--">上一页</button>
              <span>第 {{ myOrderPage }} / {{ myOrderTotalPages }} 页</span>
              <button class="btn btn-default btn-sm" :disabled="myOrderPage >= myOrderTotalPages" @click="myOrderPage++">下一页</button>
            </div>
          </div>
        </div>

        <!-- 课程统计 -->
        <div class="section">
          <div class="section-header">
            <h2>📊 课程统计</h2>
          </div>
          <div class="stats-grid">
            <div v-for="cls in myClassStats" :key="cls.classId" class="stats-card">
              <h3>{{ cls.className }}</h3>
              <div class="stats-row">
                <span class="stats-label">总预约:</span>
                <span class="stats-value">{{ cls.total }}</span>
              </div>
              <div class="stats-row">
                <span class="stats-label">已完成:</span>
                <span class="stats-value stats-success">{{ cls.completed }}</span>
              </div>
              <div class="stats-row">
                <span class="stats-label">旷课:</span>
                <span class="stats-value stats-danger">{{ cls.noshow }}</span>
              </div>
              <div class="stats-row">
                <span class="stats-label">出勤率:</span>
                <span class="stats-value">{{ cls.rate }}%</span>
              </div>
            </div>
          </div>
          <div v-if="myClassStats.length === 0" class="empty-state">暂无统计数据</div>
        </div>
      </template>
    </main>

    <!-- 添加会员弹窗 -->
    <div v-if="showAddMemberDialog" class="modal-overlay" @click.self="showAddMemberDialog = false">
      <div class="modal">
        <h3>添加会员</h3>
        <div class="form-row">
          <label>账号</label>
          <input v-model.number="newMember.memberAccount" type="number" />
        </div>
        <div class="form-row">
          <label>密码</label>
          <input v-model="newMember.memberPassword" type="text" />
        </div>
        <div class="form-row">
          <label>姓名</label>
          <input v-model="newMember.memberName" />
        </div>
        <div class="form-row">
          <label>性别</label>
          <select v-model="newMember.memberGender">
            <option value="男">男</option>
            <option value="女">女</option>
          </select>
        </div>
        <div class="form-row">
          <label>年龄</label>
          <input v-model.number="newMember.memberAge" type="number" />
        </div>
        <div class="form-row">
          <label>身高(cm)</label>
          <input v-model.number="newMember.memberHeight" type="number" step="0.1" />
        </div>
        <div class="form-row">
          <label>体重(kg)</label>
          <input v-model.number="newMember.memberWeight" type="number" step="0.1" />
        </div>
        <div class="form-row">
          <label>电话</label>
          <input v-model.number="newMember.memberPhone" type="number" />
        </div>
        <div class="form-row">
          <label>会员卡类型</label>
          <select v-model.number="newMember.cardClass">
            <option :value="1">月卡</option>
            <option :value="2">季卡</option>
            <option :value="3">年卡</option>
          </select>
        </div>
        <div class="modal-actions">
          <button class="btn btn-default" @click="showAddMemberDialog = false">取消</button>
          <button class="btn btn-primary" @click="addMember">确认添加</button>
        </div>
      </div>
    </div>

    <!-- 会员详情弹窗 -->
    <div v-if="viewingMember" class="modal-overlay" @click.self="viewingMember = null">
      <div class="modal">
        <h3>会员详情</h3>
        <div class="detail-grid">
          <div class="detail-item"><span>账号:</span>{{ viewingMember.memberAccount }}</div>
          <div class="detail-item"><span>姓名:</span>{{ viewingMember.memberName }}</div>
          <div class="detail-item"><span>性别:</span>{{ viewingMember.memberGender }}</div>
          <div class="detail-item"><span>年龄:</span>{{ viewingMember.memberAge }}</div>
          <div class="detail-item"><span>身高:</span>{{ viewingMember.memberHeight }} cm</div>
          <div class="detail-item"><span>体重:</span>{{ viewingMember.memberWeight }} kg</div>
          <div class="detail-item"><span>电话:</span>{{ viewingMember.memberPhone }}</div>
          <div class="detail-item"><span>会员卡:</span>{{ cardTypeText(viewingMember.cardClass) }}</div>
          <div class="detail-item"><span>剩余天数:</span>{{ viewingMember.cardNextClass ?? '-' }} 天</div>
          <div class="detail-item"><span>办卡时间:</span>{{ viewingMember.cardTime }}</div>
          <div class="detail-item"><span>过期时间:</span>{{ viewingMember.cardExpireDate }}</div>
          <div class="detail-item"><span>BMI:</span>{{ calcBMI(viewingMember) }}</div>
        </div>
        <div class="modal-actions">
          <button class="btn btn-default" @click="viewingMember = null">关闭</button>
        </div>
      </div>
    </div>

    <!-- 续费弹窗 -->
    <div v-if="renewingMember" class="modal-overlay" @click.self="renewingMember = null">
      <div class="modal">
        <h3>会员续费 - {{ renewingMember.memberName }} ({{ renewingMember.memberAccount }})</h3>
        <div class="detail-item"><span>当前卡类型:</span>{{ cardTypeText(renewingMember.cardClass) }}</div>
        <div class="detail-item"><span>当前过期时间:</span>{{ renewingMember.cardExpireDate || '-' }}</div>
        <div class="detail-item"><span>剩余天数:</span>{{ renewingMember.cardNextClass ?? '-' }} 天</div>
        <div class="form-row">
          <label>续费方式</label>
          <select v-model="renewMode">
            <option value="card">按卡类型续费</option>
            <option value="days">自定义天数续费</option>
          </select>
        </div>
        <div class="form-row" v-if="renewMode === 'card'">
          <label>卡类型</label>
          <select v-model.number="renewForm.cardClass">
            <option :value="1">月卡</option>
            <option :value="2">季卡</option>
            <option :value="3">年卡</option>
          </select>
        </div>
        <div class="form-row" v-else>
          <label>续费天数</label>
          <input v-model.number="renewForm.addDays" type="number" placeholder="如 30" />
        </div>
        <div class="modal-actions">
          <button class="btn btn-default" @click="renewingMember = null">取消</button>
          <button class="btn btn-primary" @click="submitRenew">确认续费</button>
        </div>
      </div>
    </div>

    <!-- 课程发布/编辑弹窗 -->
    <div v-if="showClassForm" class="modal-overlay" @click.self="closeClassForm">
      <div class="modal">
        <h3>{{ editingClass ? '编辑课程' : '发布课程' }}</h3>
        <div class="form-row">
          <label>课程名称</label>
          <input v-model="classForm.className" placeholder="如：动感单车" />
        </div>
        <div class="form-row">
          <label>开课时间</label>
          <input type="datetime-local" v-model="classForm.classBegin" />
        </div>
        <div class="form-row">
          <label>课程时长</label>
          <input v-model="classForm.classTime" placeholder="如：一个小时" />
        </div>
        <div class="form-row">
          <label>人数上限</label>
          <input type="number" v-model="classForm.maxCapacity" min="1" placeholder="留空表示不限" />
        </div>
        <div class="modal-actions">
          <button class="btn btn-default" @click="closeClassForm">取消</button>
          <button class="btn btn-primary" @click="saveClass">确认</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, watch } from 'vue'
import axios from 'axios'
import NotificationBell from '../components/NotificationBell.vue'

// 分页配置
const PAGE_SIZE = 5

const userName = ref(sessionStorage.getItem('name') || '')
const userAccount = ref(sessionStorage.getItem('account') || '')
const staff = ref(sessionStorage.getItem('staff') || '')

// 前台数据
const members = ref([])
const allOrders = ref([])
const memberSearch = ref('')
const orderStatusFilter = ref('')
const orderCoachFilter = ref('')

// 教练数据
const myClasses = ref([])
const myOrders = ref([])
const myOrderStatusFilter = ref('')

// 课程管理（教练对自己课程的增删改查）
const showClassForm = ref(false)
const editingClass = ref(null)
const classForm = ref({
  classId: '',
  className: '',
  classBegin: '',
  classTime: '',
  coach: '',
  maxCapacity: ''
})

// 弹窗
const showAddMemberDialog = ref(false)
const viewingMember = ref(null)
const newMember = reactive({
  memberAccount: null,
  memberPassword: '123456',
  memberName: '',
  memberGender: '男',
  memberAge: null,
  memberHeight: null,
  memberWeight: null,
  memberPhone: null,
  cardClass: 1
})

const handleLogout = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('name')
  sessionStorage.removeItem('account')
  sessionStorage.removeItem('staff')
  window.location.href = '/'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const cardTypeText = (cardClass) => {
  return { 1: '月卡', 2: '季卡', 3: '年卡' }[cardClass] || '-'
}

const statusText = (status) => ({
  BOOKED: '已预约', CHECKED_IN: '已签到', COMPLETED: '已完成',
  CANCELLED: '已取消', NO_SHOW: '旷课'
}[status] || status || '-')

const statusClass = (status) => ({
  BOOKED: 'st-booked', CHECKED_IN: 'st-checked', COMPLETED: 'st-completed',
  CANCELLED: 'st-cancelled', NO_SHOW: 'st-noshow'
}[status] || '')

const calcBMI = (m) => {
  if (!m || !m.memberHeight || !m.memberWeight) return '-'
  const h = m.memberHeight / 100
  return (m.memberWeight / (h * h)).toFixed(1)
}

const daysRemaining = (expireDate) => {
  if (!expireDate) return '-'
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const expire = new Date(expireDate)
  const diff = Math.ceil((expire - today) / (1000 * 60 * 60 * 24))
  return diff > 0 ? diff : 0
}

// ============ 前台方法 ============
const filteredMembers = computed(() => {
  const s = memberSearch.value.trim()
  if (!s) return members.value
  return members.value.filter(m =>
    String(m.memberAccount).includes(s) ||
    (m.memberName || '').includes(s) ||
    String(m.memberPhone || '').includes(s)
  )
})

// 会员分页
const memberPage = ref(1)
const memberPaged = computed(() => filteredMembers.value.slice((memberPage.value - 1) * PAGE_SIZE, memberPage.value * PAGE_SIZE))
const memberTotalPages = computed(() => Math.ceil(filteredMembers.value.length / PAGE_SIZE) || 1)
watch(memberSearch, () => { memberPage.value = 1 })

const filteredAllOrders = computed(() => {
  return allOrders.value.filter(o => {
    const matchStatus = !orderStatusFilter.value || o.status === orderStatusFilter.value
    const matchCoach = !orderCoachFilter.value || (o.coach || '').includes(orderCoachFilter.value)
    return matchStatus && matchCoach
  })
})

// 全部订单分页
const allOrderPage = ref(1)
const allOrderPaged = computed(() => filteredAllOrders.value.slice((allOrderPage.value - 1) * PAGE_SIZE, allOrderPage.value * PAGE_SIZE))
const allOrderTotalPages = computed(() => Math.ceil(filteredAllOrders.value.length / PAGE_SIZE) || 1)
watch([orderStatusFilter, orderCoachFilter], () => { allOrderPage.value = 1 })

const todayOrders = computed(() => {
  const today = new Date().toDateString()
  return allOrders.value.filter(o =>
    o.createTime && new Date(o.createTime).toDateString() === today
  ).length
})

const expiredMembers = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return members.value.filter(m => m.cardExpireDate && m.cardExpireDate < today).length
})

const viewMember = (m) => {
  viewingMember.value = m
}

// ============ 前台续费 ============
const renewingMember = ref(null)
const renewMode = ref('card')
const renewForm = reactive({ cardClass: null, addDays: null })

const openRenewDialog = (m) => {
  renewingMember.value = m
  renewMode.value = 'card'
  renewForm.cardClass = m.cardClass || 1
  renewForm.addDays = null
}

const submitRenew = async () => {
  if (!renewingMember.value) return
  const body = { memberAccount: renewingMember.value.memberAccount }
  if (renewMode.value === 'card') {
    if (!renewForm.cardClass) {
      alert('请选择卡类型')
      return
    }
    body.cardClass = renewForm.cardClass
  } else {
    if (!renewForm.addDays || renewForm.addDays <= 0) {
      alert('请填写有效的续费天数')
      return
    }
    body.addDays = renewForm.addDays
  }
  try {
    const res = await axios.put('/api/member/renew', body)
    if (res.data.code === 200) {
      alert('续费成功')
      renewingMember.value = null
      fetchMembers()
    } else {
      alert('续费失败: ' + res.data.message)
    }
  } catch (e) {
    console.error('renew error:', e)
    alert('续费失败: ' + (e.response?.data?.message || e.message))
  }
}

const addMember = async () => {
  if (!newMember.memberAccount || !newMember.memberName) {
    alert('账号和姓名必填')
    return
  }
  try {
    const response = await axios.post('/api/member/add', newMember)
    if (response.data.code === 200) {
      alert('添加成功')
      showAddMemberDialog.value = false
      fetchMembers()
      Object.assign(newMember, {
        memberAccount: null, memberPassword: '123456', memberName: '',
        memberGender: '男', memberAge: null, memberHeight: null,
        memberWeight: null, memberPhone: null, cardClass: 1
      })
    } else {
      alert('添加失败: ' + response.data.message)
    }
  } catch (e) {
    alert('添加失败')
    console.error(e)
  }
}

// ============ 教练方法 ============
// 课程管理：发布/编辑/删除自己的课程（coach 锁定为当前教练名字）
const openClassDialog = (cls) => {
  if (cls) {
    editingClass.value = cls
    classForm.value = { ...cls }
    if (cls.classBegin) {
      classForm.value.classBegin = new Date(cls.classBegin).toISOString().slice(0, 16)
    }
  } else {
    editingClass.value = null
    classForm.value = {
      classId: '',
      className: '',
      classBegin: '',
      classTime: '',
      coach: userName.value,
      maxCapacity: ''
    }
  }
  showClassForm.value = true
}

const closeClassForm = () => {
  showClassForm.value = false
  editingClass.value = null
  classForm.value = { classId: '', className: '', classBegin: '', classTime: '', coach: '', maxCapacity: '' }
}

const saveClass = async () => {
  if (!classForm.value.className) { alert('请填写课程名称'); return }
  // 教练只能管理自己的课程，coach 锁定为当前教练名字
  classForm.value.coach = userName.value
  try {
    if (editingClass.value) {
      const res = await axios.put('/api/class/update', classForm.value)
      if (res.data.code !== 200) { alert('更新失败: ' + res.data.message); return }
    } else {
      const res = await axios.post('/api/class/add', classForm.value)
      if (res.data.code !== 200) { alert('发布失败: ' + res.data.message); return }
    }
    closeClassForm()
    fetchMyClasses()
  } catch (e) {
    alert('操作失败: ' + (e.response?.data?.message || e.message))
  }
}

const deleteClass = async (cls) => {
  if (!confirm(`确认删除课程「${cls.className}」？`)) return
  try {
    const res = await axios.delete(`/api/class/delete/${cls.classId}`)
    if (res.data.code === 200) {
      fetchMyClasses()
    } else {
      alert('删除失败: ' + res.data.message)
    }
  } catch (e) {
    alert('删除失败: ' + (e.response?.data?.message || e.message))
  }
}

// 我的课程搜索（按课程名称/教练）
const myClassSearch = ref('')
const myClassFiltered = computed(() => myClasses.value.filter(c =>
  !myClassSearch.value ||
  (c.className || '').includes(myClassSearch.value) ||
  (c.coach || '').includes(myClassSearch.value)
))
// 我的课程分页
const myClassPage = ref(1)
const myClassPaged = computed(() => myClassFiltered.value.slice((myClassPage.value - 1) * PAGE_SIZE, myClassPage.value * PAGE_SIZE))
const myClassTotalPages = computed(() => Math.ceil(myClassFiltered.value.length / PAGE_SIZE) || 1)
watch(myClassSearch, () => { myClassPage.value = 1 })

const filteredMyOrders = computed(() => {
  if (!myOrderStatusFilter.value) return myOrders.value
  return myOrders.value.filter(o => o.status === myOrderStatusFilter.value)
})

// 我的订单分页
const myOrderPage = ref(1)
const myOrderPaged = computed(() => filteredMyOrders.value.slice((myOrderPage.value - 1) * PAGE_SIZE, myOrderPage.value * PAGE_SIZE))
const myOrderTotalPages = computed(() => Math.ceil(filteredMyOrders.value.length / PAGE_SIZE) || 1)
watch(myOrderStatusFilter, () => { myOrderPage.value = 1 })

const bookedCount = (classId) => {
  return myOrders.value.filter(o => o.classId === classId && o.status === 'BOOKED').length
}

const attendanceRate = computed(() => {
  const finished = myOrders.value.filter(o => o.status === 'COMPLETED' || o.status === 'NO_SHOW')
  if (finished.length === 0) return 0
  const completed = finished.filter(o => o.status === 'COMPLETED').length
  return Math.round((completed / finished.length) * 100)
})

const myClassStats = computed(() => {
  return myClasses.value.map(cls => {
    const orders = myOrders.value.filter(o => o.classId === cls.classId)
    const total = orders.length
    const completed = orders.filter(o => o.status === 'COMPLETED').length
    const noshow = orders.filter(o => o.status === 'NO_SHOW').length
    const finished = completed + noshow
    const rate = finished === 0 ? 0 : Math.round((completed / finished) * 100)
    return { classId: cls.classId, className: cls.className, total, completed, noshow, rate }
  })
})

const cancelOrder = async (id) => {
  if (!confirm('确定要取消该预约吗？')) return
  try {
    const res = await axios.post(`/api/order/adminCancel/${id}`)
    if (res.data.code === 200) {
      alert('已取消预约')
      fetchMyOrders()
    } else {
      alert('操作失败: ' + res.data.message)
    }
  } catch (e) {
    alert('操作失败: ' + (e.response?.data?.message || e.message))
  }
}

const completeOrder = async (id) => {
  if (!confirm('确认标记此订单为已完成？')) return
  try {
    const res = await axios.post(`/api/order/complete/${id}`)
    if (res.data.code === 200) {
      alert('已标记完成')
      fetchMyOrders()
    } else {
      alert('操作失败: ' + res.data.message)
    }
  } catch (e) {
    alert('操作失败')
  }
}

const noshowOrder = async (id) => {
  if (!confirm('确认标记此订单为旷课？（课时不退回）')) return
  try {
    const res = await axios.post(`/api/order/noshow/${id}`)
    if (res.data.code === 200) {
      alert('已标记旷课')
      fetchMyOrders()
    } else {
      alert('操作失败: ' + res.data.message)
    }
  } catch (e) {
    alert('操作失败')
  }
}

// ============ 数据加载 ============
const fetchMembers = async () => {
  try {
    const res = await axios.get('/api/member/list')
    if (res.data.code === 200) members.value = res.data.data
  } catch (e) { console.error('fetchMembers:', e) }
}

const fetchAllOrders = async () => {
  try {
    const res = await axios.get('/api/order/list')
    if (res.data.code === 200) allOrders.value = res.data.data
  } catch (e) { console.error('fetchAllOrders:', e) }
}

const fetchMyClasses = async () => {
  try {
    const res = await axios.get(`/api/class/coach/${userName.value}`)
    if (res.data.code === 200) myClasses.value = res.data.data
  } catch (e) { console.error('fetchMyClasses:', e) }
}

const fetchMyOrders = async () => {
  try {
    const res = await axios.get(`/api/order/coach/${userName.value}`)
    if (res.data.code === 200) myOrders.value = res.data.data
  } catch (e) { console.error('fetchMyOrders:', e) }
}

onMounted(() => {
  if (staff.value === '前台') {
    fetchMembers()
    fetchAllOrders()
  } else if (staff.value === '教练') {
    fetchMyClasses()
    fetchMyOrders()
  }
})
</script>

<style scoped>
.employee-container {
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

.header h1 { margin: 0; font-size: 24px; }

.user-info { display: flex; align-items: center; gap: 20px; }
.user-info span { font-size: 16px; }

.logout-btn {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid white;
  color: white;
  border-radius: 6px;
  cursor: pointer;
}
.logout-btn:hover { background: rgba(255, 255, 255, 0.3); }

.main-content { padding: 40px; }

.dashboard {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  gap: 20px;
}

.card-icon { font-size: 48px; }
.card-content h3 { margin: 0 0 8px 0; color: #333; font-size: 14px; }
.card-content p { margin: 0; color: #0ea5e9; font-size: 24px; font-weight: 600; }

.section {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  margin-bottom: 30px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 { margin: 0; color: #333; font-size: 20px; }

.section-actions { display: flex; gap: 12px; }

.search-input, .section-actions select {
  padding: 8px 14px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.table-container { overflow-x: auto; }

table { width: 100%; border-collapse: collapse; }
th, td { padding: 12px; text-align: left; border-bottom: 1px solid #eee; }
th { background: #f8f9fa; color: #666; font-weight: 600; }

.empty-state { text-align: center; padding: 40px; color: #999; }

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}
.pagination span { font-size: 14px; color: #666; }
.pagination button:disabled { opacity: 0.5; cursor: not-allowed; }

.classes-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.class-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
}
.class-card h3 { margin: 0 0 10px 0; color: #333; }
.class-card p { margin: 5px 0; color: #666; font-size: 14px; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stats-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
}
.stats-card h3 { margin: 0 0 15px 0; color: #333; }
.stats-row { display: flex; justify-content: space-between; padding: 6px 0; }
.stats-label { color: #666; }
.stats-value { font-weight: 600; color: #333; }
.stats-success { color: #22c55e; }
.stats-danger { color: #ef4444; }

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

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: opacity 0.3s;
}
.btn:hover { opacity: 0.9; }
.btn-primary { background: #0ea5e9; color: white; }
.btn-success { background: #22c55e; color: white; }
.btn-danger { background: #ef4444; color: white; }
.btn-default { background: #e5e7eb; color: #333; }
.btn-link { background: none; color: #0ea5e9; text-decoration: underline; padding: 0; }
.btn-sm { padding: 4px 10px; font-size: 12px; margin-right: 6px; }

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
  width: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal h3 { margin: 0 0 20px 0; color: #333; }

.form-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.form-row label { width: 100px; color: #666; }
.form-row input, .form-row select {
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

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.detail-item {
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}
.detail-item span {
  color: #666;
  margin-right: 8px;
}
</style>
