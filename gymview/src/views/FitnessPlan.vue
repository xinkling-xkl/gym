<template>
  <div class="plan-container">
    <header class="header">
      <h1>我的健身计划</h1>
      <div class="user-info">
        <NotificationBell />
        <span>欢迎, {{ userName }}</span>
        <button class="logout-btn" @click="goHome">返回首页</button>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <main class="main-content">
      <!-- 顶部操作栏 -->
      <div class="toolbar">
        <div class="filter-bar">
          <button :class="['filter-btn', { active: statusFilter === 'ALL' }]" @click="statusFilter = 'ALL'">全部 ({{ plans.length }})</button>
          <button :class="['filter-btn', { active: statusFilter === 'ACTIVE' }]" @click="statusFilter = 'ACTIVE'">进行中 ({{ countByStatus('ACTIVE') }})</button>
          <button :class="['filter-btn', { active: statusFilter === 'COMPLETED' }]" @click="statusFilter = 'COMPLETED'">已完成 ({{ countByStatus('COMPLETED') }})</button>
          <button :class="['filter-btn', { active: statusFilter === 'ARCHIVED' }]" @click="statusFilter = 'ARCHIVED'">已归档 ({{ countByStatus('ARCHIVED') }})</button>
        </div>
        <button class="btn btn-primary" @click="openPlanDialog()">+ 新建计划</button>
      </div>

      <!-- 计划列表 -->
      <div v-if="filteredPlans.length === 0" class="empty-state">
        <p>暂无健身计划，点击右上角"新建计划"开始制定你的健身目标吧！</p>
      </div>

      <div v-for="plan in filteredPlans" :key="plan.planId" class="plan-card">
        <div class="plan-header" @click="toggleExpand(plan)">
          <div class="plan-info">
            <h3>{{ plan.planName }}</h3>
            <span :class="['status-tag', statusClass(plan.status)]">{{ statusText(plan.status) }}</span>
          </div>
          <div class="plan-meta">
            <span v-if="plan.startDate || plan.endDate">
              {{ plan.startDate || '?' }} ~ {{ plan.endDate || '?' }}
            </span>
            <span class="progress-info">
              {{ completedCount(plan) }}/{{ plan.items?.length || 0 }} 项完成
            </span>
            <span class="expand-icon">{{ expandedId === plan.planId ? '▼' : '▶' }}</span>
          </div>
        </div>

        <div class="plan-goal" v-if="plan.goal">
          <strong>目标：</strong>{{ plan.goal }}
        </div>

        <!-- 训练项明细（展开时显示） -->
        <div v-if="expandedId === plan.planId" class="plan-items">
          <div class="items-toolbar">
            <h4>训练安排</h4>
            <div>
              <button class="btn btn-info btn-sm" @click="syncOrders(plan)">🔄 同步已预约课程</button>
              <button class="btn btn-success btn-sm" @click="openItemDialog(plan.planId)">+ 添加训练项</button>
            </div>
          </div>

          <div v-if="!plan.items || plan.items.length === 0" class="empty-items">
            暂无训练项，可点击"同步已预约课程"自动导入，或手动"添加训练项"
          </div>

          <table v-else>
            <thead>
              <tr>
                <th>星期</th>
                <th>训练项目</th>
                <th>开课时间</th>
                <th>教练</th>
                <th>时长</th>
                <th>组数</th>
                <th>次数</th>
                <th>备注</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in plan.items" :key="item.itemId">
                <td>{{ dayOfWeekText(item.dayOfWeek) }}</td>
                <td>{{ item.exercise }}</td>
                <td>{{ item.scheduledTime ? formatDateTime(item.scheduledTime) : '-' }}</td>
                <td>{{ item.coachName || '-' }}</td>
                <td>{{ item.duration ? item.duration + '分钟' : '-' }}</td>
                <td>{{ item.sets || '-' }}</td>
                <td>{{ item.reps || '-' }}</td>
                <td>{{ item.notes || '-' }}</td>
                <td>
                  <span :class="['status-tag', item.completed ? 'st-completed' : 'st-pending']">
                    {{ item.completed ? '已完成' : '待完成' }}
                  </span>
                </td>
                <td>
                  <button class="btn btn-link btn-sm" @click="toggleItemCompleted(item)">
                    {{ item.completed ? '撤销' : '完成' }}
                  </button>
                  <button class="btn btn-primary btn-sm" @click="openItemDialog(plan.planId, item)">编辑</button>
                  <button class="btn btn-danger btn-sm" @click="deleteItem(item)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- 计划操作 -->
          <div class="plan-actions">
            <button class="btn btn-primary btn-sm" @click="openPlanDialog(plan)">编辑计划</button>
            <button v-if="plan.status === 'ACTIVE'" class="btn btn-success btn-sm" @click="changeStatus(plan, 'COMPLETED')">标记完成</button>
            <button v-if="plan.status !== 'ARCHIVED'" class="btn btn-warning btn-sm" @click="changeStatus(plan, 'ARCHIVED')">归档</button>
            <button v-if="plan.status !== 'ACTIVE'" class="btn btn-success btn-sm" @click="changeStatus(plan, 'ACTIVE')">重新激活</button>
            <button class="btn btn-danger btn-sm" @click="deletePlan(plan)">删除计划</button>
          </div>
        </div>
      </div>
    </main>

    <!-- 计划新增/编辑弹窗 -->
    <div v-if="showPlanDialog" class="modal-overlay" @click.self="showPlanDialog = false">
      <div class="modal">
        <h3>{{ editingPlan ? '编辑计划' : '新建计划' }}</h3>
        <div class="form-row">
          <label>计划名称 *</label>
          <input v-model="planForm.planName" placeholder="如：夏季减脂计划" />
        </div>
        <div class="form-row">
          <label>训练目标</label>
          <input v-model="planForm.goal" placeholder="如：3个月减脂5kg" />
        </div>
        <div class="form-row">
          <label>开始日期</label>
          <input v-model="planForm.startDate" type="date" />
        </div>
        <div class="form-row">
          <label>结束日期</label>
          <input v-model="planForm.endDate" type="date" />
        </div>
        <div class="modal-actions">
          <button class="btn btn-default" @click="showPlanDialog = false">取消</button>
          <button class="btn btn-primary" @click="savePlan">保存</button>
        </div>
      </div>
    </div>

    <!-- 训练项新增/编辑弹窗 -->
    <div v-if="showItemDialog" class="modal-overlay" @click.self="showItemDialog = false">
      <div class="modal">
        <h3>{{ editingItem ? '编辑训练项' : '添加训练项' }}</h3>
        <div class="form-row">
          <label>星期 *</label>
          <select v-model="itemForm.dayOfWeek">
            <option v-for="d in 7" :key="d" :value="d">{{ dayOfWeekText(d) }}</option>
          </select>
        </div>
        <div class="form-row">
          <label>训练项目 *</label>
          <input v-model="itemForm.exercise" placeholder="如：哑铃卧推" />
        </div>
        <div class="form-row">
          <label>时长(分钟)</label>
          <input v-model.number="itemForm.duration" type="number" />
        </div>
        <div class="form-row">
          <label>组数</label>
          <input v-model.number="itemForm.sets" type="number" />
        </div>
        <div class="form-row">
          <label>次数</label>
          <input v-model.number="itemForm.reps" type="number" />
        </div>
        <div class="form-row">
          <label>备注</label>
          <input v-model="itemForm.notes" placeholder="如：注意呼吸节奏" />
        </div>
        <div class="modal-actions">
          <button class="btn btn-default" @click="showItemDialog = false">取消</button>
          <button class="btn btn-primary" @click="saveItem">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import NotificationBell from '../components/NotificationBell.vue'

const router = useRouter()
const userName = ref(sessionStorage.getItem('name') || '')
const userAccount = ref(sessionStorage.getItem('account') || '')
const plans = ref([])
const statusFilter = ref('ALL')
const expandedId = ref(null)

// 计划弹窗
const showPlanDialog = ref(false)
const editingPlan = ref(null)
const planForm = ref({ planName: '', goal: '', startDate: '', endDate: '' })

// 训练项弹窗
const showItemDialog = ref(false)
const editingItem = ref(null)
const itemForm = ref({ dayOfWeek: 1, exercise: '', duration: null, sets: null, reps: null, notes: '' })
const currentPlanId = ref(null)

const filteredPlans = computed(() => {
  if (statusFilter.value === 'ALL') return plans.value
  return plans.value.filter(p => p.status === statusFilter.value)
})

const countByStatus = (status) => plans.value.filter(p => p.status === status).length

const completedCount = (plan) => {
  if (!plan.items) return 0
  return plan.items.filter(i => i.completed === 1).length
}

const statusText = (status) => ({ ACTIVE: '进行中', COMPLETED: '已完成', ARCHIVED: '已归档' }[status] || status)
const statusClass = (status) => ({ ACTIVE: 'st-active', COMPLETED: 'st-completed', ARCHIVED: 'st-archived' }[status] || '')

const dayOfWeekText = (d) => ({ 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }[d] || '-')

const toggleExpand = (plan) => {
  expandedId.value = expandedId.value === plan.planId ? null : plan.planId
}

const fetchPlans = async () => {
  try {
    const res = await axios.get(`/api/plan/member/${userAccount.value}`)
    if (res.data.code === 200) {
      plans.value = res.data.data || []
      // 自动展开第一个进行中的计划
      if (expandedId.value === null && plans.value.length > 0) {
        const active = plans.value.find(p => p.status === 'ACTIVE')
        expandedId.value = active ? active.planId : plans.value[0].planId
      }
    }
  } catch (e) {
    console.error('Fetch plans error:', e)
  }
}

const openPlanDialog = (plan = null) => {
  editingPlan.value = plan
  if (plan) {
    planForm.value = {
      planName: plan.planName, goal: plan.goal || '',
      startDate: plan.startDate || '', endDate: plan.endDate || ''
    }
  } else {
    planForm.value = { planName: '', goal: '', startDate: '', endDate: '' }
  }
  showPlanDialog.value = true
}

const savePlan = async () => {
  if (!planForm.value.planName.trim()) {
    alert('请填写计划名称')
    return
  }
  try {
    if (editingPlan.value) {
      const res = await axios.put('/api/plan/update', {
        planId: editingPlan.value.planId,
        planName: planForm.value.planName,
        goal: planForm.value.goal || null,
        startDate: planForm.value.startDate || null,
        endDate: planForm.value.endDate || null,
        status: editingPlan.value.status
      })
      if (res.data.code === 200) { alert('更新成功'); showPlanDialog.value = false; fetchPlans() }
      else alert(res.data.message)
    } else {
      const res = await axios.post('/api/plan/add', {
        memberAccount: Number(userAccount.value),
        planName: planForm.value.planName,
        goal: planForm.value.goal || null,
        startDate: planForm.value.startDate || null,
        endDate: planForm.value.endDate || null
      })
      if (res.data.code === 200) { alert('创建成功'); showPlanDialog.value = false; fetchPlans() }
      else alert(res.data.message)
    }
  } catch (e) {
    alert('操作失败: ' + (e.response?.data?.message || e.message))
  }
}

const deletePlan = async (plan) => {
  if (!confirm(`确定要删除计划【${plan.planName}】吗？所有训练项将一并删除。`)) return
  try {
    const res = await axios.delete(`/api/plan/delete/${plan.planId}`)
    if (res.data.code === 200) { alert('删除成功'); fetchPlans() }
    else alert(res.data.message)
  } catch (e) {
    alert('删除失败: ' + (e.response?.data?.message || e.message))
  }
}

const changeStatus = async (plan, status) => {
  try {
    const res = await axios.put(`/api/plan/status/${plan.planId}?status=${status}`)
    if (res.data.code === 200) { fetchPlans() }
    else alert(res.data.message)
  } catch (e) {
    alert('操作失败')
  }
}

const openItemDialog = (planId, item = null) => {
  currentPlanId.value = planId
  editingItem.value = item
  if (item) {
    itemForm.value = {
      dayOfWeek: item.dayOfWeek || 1, exercise: item.exercise,
      duration: item.duration, sets: item.sets, reps: item.reps, notes: item.notes || ''
    }
  } else {
    itemForm.value = { dayOfWeek: 1, exercise: '', duration: null, sets: null, reps: null, notes: '' }
  }
  showItemDialog.value = true
}

const saveItem = async () => {
  if (!itemForm.value.exercise.trim()) {
    alert('请填写训练项目')
    return
  }
  try {
    if (editingItem.value) {
      const res = await axios.put('/api/plan/item/update', {
        itemId: editingItem.value.itemId,
        planId: currentPlanId.value,
        dayOfWeek: itemForm.value.dayOfWeek,
        exercise: itemForm.value.exercise,
        duration: itemForm.value.duration,
        sets: itemForm.value.sets,
        reps: itemForm.value.reps,
        notes: itemForm.value.notes || null
      })
      if (res.data.code === 200) { alert('更新成功'); showItemDialog.value = false; fetchPlans() }
      else alert(res.data.message)
    } else {
      const res = await axios.post('/api/plan/item/add', {
        planId: currentPlanId.value,
        dayOfWeek: itemForm.value.dayOfWeek,
        exercise: itemForm.value.exercise,
        duration: itemForm.value.duration,
        sets: itemForm.value.sets,
        reps: itemForm.value.reps,
        notes: itemForm.value.notes || null
      })
      if (res.data.code === 200) { alert('添加成功'); showItemDialog.value = false; fetchPlans() }
      else alert(res.data.message)
    }
  } catch (e) {
    alert('操作失败: ' + (e.response?.data?.message || e.message))
  }
}

const deleteItem = async (item) => {
  if (!confirm('确定要删除该训练项吗？')) return
  try {
    const res = await axios.delete(`/api/plan/item/delete/${item.itemId}`)
    if (res.data.code === 200) { fetchPlans() }
    else alert(res.data.message)
  } catch (e) {
    alert('删除失败')
  }
}

const toggleItemCompleted = async (item) => {
  try {
    const newCompleted = item.completed === 1 ? 0 : 1
    const res = await axios.put(`/api/plan/item/toggle/${item.itemId}?completed=${newCompleted}`)
    if (res.data.code === 200) { fetchPlans() }
    else alert(res.data.message)
  } catch (e) {
    alert('操作失败')
  }
}

// 一键同步已预约课程到当前计划
const syncOrders = async (plan) => {
  if (!confirm(`确定要同步课程预约到计划【${plan.planName}】吗？\n新预约的课程将加入计划，已取消预约的课程训练项将被移除。`)) return
  try {
    const res = await axios.post(`/api/plan/sync/${plan.planId}/${userAccount.value}`)
    if (res.data.code === 200) {
      alert(res.data.message)
      fetchPlans()
    } else {
      alert(res.data.message)
    }
  } catch (e) {
    alert('同步失败: ' + (e.response?.data?.message || e.message))
  }
}

// 格式化日期时间
const formatDateTime = (dt) => {
  if (!dt) return '-'
  // 兼容后端返回的 LocalDateTime 数组或字符串
  if (typeof dt === 'string') {
    return dt.replace('T', ' ')
  }
  return dt
}

const goHome = () => router.push('/home')

const handleLogout = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('name')
  sessionStorage.removeItem('account')
  window.location.href = '/'
}

onMounted(() => { fetchPlans() })
</script>

<style scoped>
.plan-container { min-height: 100vh; background: #f5f7fa; }

.header {
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white; padding: 20px 40px;
  display: flex; justify-content: space-between; align-items: center;
}
.header h1 { margin: 0; font-size: 24px; }
.user-info { display: flex; align-items: center; gap: 20px; }
.logout-btn {
  padding: 8px 20px; background: rgba(255,255,255,0.2);
  border: 1px solid white; color: white; border-radius: 6px; cursor: pointer;
}
.logout-btn:hover { background: rgba(255,255,255,0.3); }

.main-content { max-width: 1000px; margin: 0 auto; padding: 30px 20px; }

.toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 24px;
}
.filter-bar { display: flex; gap: 8px; }
.filter-btn {
  padding: 8px 16px; border: 1px solid #ddd; background: white;
  border-radius: 6px; cursor: pointer; font-size: 14px; color: #666;
  transition: all 0.2s;
}
.filter-btn.active {
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white; border-color: transparent;
}

.btn {
  padding: 8px 18px; border: none; border-radius: 6px; cursor: pointer;
  font-size: 14px; transition: opacity 0.2s;
}
.btn:hover { opacity: 0.85; }
.btn-primary { background: #0ea5e9; color: white; }
.btn-info { background: #6366f1; color: white; }
.btn-success { background: #22c55e; color: white; }
.btn-warning { background: #f59e0b; color: white; }
.btn-danger { background: #ef4444; color: white; }
.btn-default { background: #f1f5f9; color: #666; }
.btn-link { background: transparent; color: #0ea5e9; }
.btn-sm { padding: 5px 12px; font-size: 13px; }

.empty-state {
  text-align: center; padding: 80px 20px; color: #999;
  background: white; border-radius: 12px;
}

.plan-card {
  background: white; border-radius: 12px; margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08); overflow: hidden;
}
.plan-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px; cursor: pointer; transition: background 0.2s;
}
.plan-header:hover { background: #f8f9fa; }
.plan-info { display: flex; align-items: center; gap: 12px; }
.plan-info h3 { margin: 0; font-size: 18px; color: #333; }
.plan-meta { display: flex; align-items: center; gap: 16px; font-size: 14px; color: #888; }
.progress-info { color: #0ea5e9; font-weight: 600; }
.expand-icon { font-size: 12px; }

.plan-goal {
  padding: 0 24px 12px; color: #666; font-size: 14px;
}

.plan-items { padding: 0 24px 20px; border-top: 1px solid #f0f0f0; }
.items-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 0;
}
.items-toolbar h4 { margin: 0; color: #555; }

.empty-items {
  text-align: center; padding: 30px; color: #aaa; font-size: 14px;
}

table { width: 100%; border-collapse: collapse; }
th, td { padding: 10px; text-align: left; border-bottom: 1px solid #eee; font-size: 14px; }
th { background: #f8f9fa; color: #666; font-weight: 600; }

.plan-actions {
  display: flex; gap: 8px; margin-top: 16px; padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.status-tag {
  display: inline-block; padding: 2px 10px; border-radius: 12px;
  font-size: 12px; font-weight: 600;
}
.st-active { background: #dbeafe; color: #1d4ed8; }
.st-completed { background: #dcfce7; color: #15803d; }
.st-archived { background: #f3f4f6; color: #6b7280; }
.st-pending { background: #fef3c7; color: #b45309; }

.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex;
  justify-content: center; align-items: center; z-index: 999;
}
.modal { background: white; padding: 30px; border-radius: 12px; width: 480px; }
.modal h3 { margin: 0 0 20px 0; color: #333; }
.form-row { display: flex; align-items: center; margin-bottom: 14px; }
.form-row label { width: 100px; color: #666; font-size: 14px; }
.form-row input, .form-row select {
  flex: 1; padding: 8px 10px; border: 1px solid #ddd; border-radius: 6px;
}
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px; }
</style>
