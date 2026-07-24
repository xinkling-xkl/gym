<template>
  <div class="admin-container">
    <header class="header">
      <h1>健身房管理系统 - 管理员后台</h1>
      <div class="user-info">
        <NotificationBell />
        <span>欢迎, {{ userName }}</span>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <div class="main-layout">
      <aside class="sidebar">
        <div 
          v-for="menu in menus" 
          :key="menu.key"
          :class="['menu-item', { active: activeMenu === menu.key }]"
          @click="activeMenu = menu.key"
        >
          {{ menu.label }}
        </div>
      </aside>

      <main class="content">
        <div v-show="activeMenu === 'member'">
          <div class="section-header">
            <h2>会员管理</h2>
            <button class="add-btn" @click="showMemberForm = true">添加会员</button>
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
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="member in members" :key="member.memberAccount">
                  <td>{{ member.memberAccount }}</td>
                  <td>{{ member.memberName }}</td>
                  <td>{{ member.memberGender }}</td>
                  <td>{{ member.memberAge }}</td>
                  <td>{{ member.memberPhone }}</td>
                  <td>
                    <button class="edit-btn" @click="editMember(member)">编辑</button>
                    <button class="delete-btn" @click="deleteMember(member.memberAccount)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-show="activeMenu === 'employee'">
          <div class="section-header">
            <h2>员工管理</h2>
            <button class="add-btn" @click="showEmployeeForm = true">添加员工</button>
          </div>
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>账号</th>
                  <th>姓名</th>
                  <th>性别</th>
                  <th>年龄</th>
                  <th>职务</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="employee in employees" :key="employee.employeeAccount">
                  <td>{{ employee.employeeAccount }}</td>
                  <td>{{ employee.employeeName }}</td>
                  <td>{{ employee.employeeGender }}</td>
                  <td>{{ employee.employeeAge }}</td>
                  <td>{{ employee.staff }}</td>
                  <td>
                    <button class="edit-btn" @click="editEmployee(employee)">编辑</button>
                    <button class="delete-btn" @click="deleteEmployee(employee.employeeAccount)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-show="activeMenu === 'equipment'">
          <div class="section-header">
            <h2>器材管理</h2>
            <button class="add-btn" @click="showEquipmentForm = true">添加器材</button>
          </div>
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>名称</th>
                  <th>位置</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="equipment in equipments" :key="equipment.equipmentId">
                  <td>{{ equipment.equipmentId }}</td>
                  <td>{{ equipment.equipmentName }}</td>
                  <td>{{ equipment.equipmentLocation }}</td>
                  <td :class="['status', equipment.equipmentStatus === '正常' ? 'normal' : 'broken']">
                    {{ equipment.equipmentStatus }}
                  </td>
                  <td>
                    <button class="edit-btn" @click="editEquipment(equipment)">编辑</button>
                    <button class="delete-btn" @click="deleteEquipment(equipment.equipmentId)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-show="activeMenu === 'class'">
          <div class="section-header">
            <h2>课程管理</h2>
            <button class="add-btn" @click="showClassForm = true">添加课程</button>
          </div>
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>课程名称</th>
                  <th>教练</th>
                  <th>开始时间</th>
                  <th>时长</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="cls in classes" :key="cls.classId">
                  <td>{{ cls.classId }}</td>
                  <td>{{ cls.className }}</td>
                  <td>{{ cls.coach }}</td>
                  <td>{{ formatDate(cls.classBegin) }}</td>
                  <td>{{ cls.classTime }}</td>
                  <td>
                    <button class="edit-btn" @click="editClass(cls)">编辑</button>
                    <button class="delete-btn" @click="deleteClass(cls.classId)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-show="activeMenu === 'order'">
          <div class="section-header">
            <h2>订单管理</h2>
          </div>
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>订单ID</th>
                  <th>会员账号</th>
                  <th>会员姓名</th>
                  <th>课程名称</th>
                  <th>教练</th>
                  <th>开始时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in orders" :key="order.classOrderId">
                  <td>{{ order.classOrderId }}</td>
                  <td>{{ order.memberAccount }}</td>
                  <td>{{ order.memberName }}</td>
                  <td>{{ getClassName(order.classId) }}</td>
                  <td>{{ order.coach }}</td>
                  <td>{{ formatDate(order.classBegin) }}</td>
                  <td>
                    <button class="delete-btn" @click="deleteOrder(order.classOrderId)">取消订单</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </main>

      <aside class="ai-sidebar">
        <AiChat />
      </aside>
    </div>

    <div v-if="showMemberForm" class="modal">
      <div class="modal-content">
        <h3>{{ editingMember ? '编辑会员' : '添加会员' }}</h3>
        <form @submit.prevent="saveMember">
          <div class="form-row">
            <div class="form-group">
              <label>账号</label>
              <input type="number" v-model="memberForm.memberAccount" :disabled="!!editingMember" required />
            </div>
            <div class="form-group">
              <label>密码</label>
              <input type="password" v-model="memberForm.memberPassword" required />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>姓名</label>
              <input type="text" v-model="memberForm.memberName" required />
            </div>
            <div class="form-group">
              <label>性别</label>
              <select v-model="memberForm.memberGender">
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>年龄</label>
              <input type="number" v-model="memberForm.memberAge" />
            </div>
            <div class="form-group">
              <label>电话</label>
              <input type="number" v-model="memberForm.memberPhone" />
            </div>
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="closeMemberForm">取消</button>
            <button type="submit" class="submit-btn">保存</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showEmployeeForm" class="modal">
      <div class="modal-content">
        <h3>{{ editingEmployee ? '编辑员工' : '添加员工' }}</h3>
        <form @submit.prevent="saveEmployee">
          <div class="form-row">
            <div class="form-group">
              <label>账号</label>
              <input type="number" v-model="employeeForm.employeeAccount" :disabled="!!editingEmployee" required />
            </div>
            <div class="form-group">
              <label>姓名</label>
              <input type="text" v-model="employeeForm.employeeName" required />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>性别</label>
              <select v-model="employeeForm.employeeGender">
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
            <div class="form-group">
              <label>年龄</label>
              <input type="number" v-model="employeeForm.employeeAge" />
            </div>
          </div>
          <div class="form-group">
            <label>职务</label>
            <input type="text" v-model="employeeForm.staff" />
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="closeEmployeeForm">取消</button>
            <button type="submit" class="submit-btn">保存</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showEquipmentForm" class="modal">
      <div class="modal-content">
        <h3>{{ editingEquipment ? '编辑器材' : '添加器材' }}</h3>
        <form @submit.prevent="saveEquipment">
          <div class="form-group" v-if="editingEquipment">
            <label>ID</label>
            <input type="number" v-model="equipmentForm.equipmentId" disabled />
          </div>
          <div class="form-group">
            <label>名称</label>
            <input type="text" v-model="equipmentForm.equipmentName" required />
          </div>
          <div class="form-group">
            <label>位置</label>
            <input type="text" v-model="equipmentForm.equipmentLocation" />
          </div>
          <div class="form-group">
            <label>状态</label>
            <select v-model="equipmentForm.equipmentStatus">
              <option value="正常">正常</option>
              <option value="损坏">损坏</option>
              <option value="维修中">维修中</option>
            </select>
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="closeEquipmentForm">取消</button>
            <button type="submit" class="submit-btn">保存</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showClassForm" class="modal">
      <div class="modal-content">
        <h3>{{ editingClass ? '编辑课程' : '添加课程' }}</h3>
        <form @submit.prevent="saveClass">
          <div class="form-group" v-if="editingClass">
            <label>ID</label>
            <input type="number" v-model="classForm.classId" disabled />
          </div>
          <div class="form-group">
            <label>课程名称</label>
            <input type="text" v-model="classForm.className" required />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>教练</label>
              <input type="text" v-model="classForm.coach" />
            </div>
            <div class="form-group">
              <label>时长</label>
              <input type="text" v-model="classForm.classTime" />
            </div>
          </div>
          <div class="form-group">
            <label>开始时间</label>
            <input type="datetime-local" v-model="classForm.classBegin" />
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="closeClassForm">取消</button>
            <button type="submit" class="submit-btn">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import NotificationBell from '../components/NotificationBell.vue'
import AiChat from '../components/AiChat.vue'

const router = useRouter()
const userName = ref(localStorage.getItem('name') || '')
const activeMenu = ref('member')

const menus = [
  { key: 'dashboard', label: '📊 数据大屏' },
  { key: 'member', label: '会员管理' },
  { key: 'employee', label: '员工管理' },
  { key: 'equipment', label: '器材管理' },
  { key: 'class', label: '课程管理' },
  { key: 'order', label: '订单管理' }
]

watch(activeMenu, (val) => {
  if (val === 'dashboard') {
    router.push('/dashboard')
  }
})

const members = ref([])
const employees = ref([])
const equipments = ref([])
const classes = ref([])
const orders = ref([])

const showMemberForm = ref(false)
const showEmployeeForm = ref(false)
const showEquipmentForm = ref(false)
const showClassForm = ref(false)

const editingMember = ref(null)
const editingEmployee = ref(null)
const editingEquipment = ref(null)
const editingClass = ref(null)

const memberForm = ref({
  memberAccount: '',
  memberPassword: '',
  memberName: '',
  memberGender: '男',
  memberAge: '',
  memberHeight: '',
  memberWeight: '',
  memberPhone: '',
  cardTime: '',
  cardClass: '',
  cardNextClass: ''
})

const employeeForm = ref({
  employeeAccount: '',
  employeeName: '',
  employeeGender: '男',
  employeeAge: '',
  entryTime: '',
  staff: '',
  employeeMessage: ''
})

const equipmentForm = ref({
  equipmentId: '',
  equipmentName: '',
  equipmentLocation: '',
  equipmentStatus: '正常',
  equipmentMessage: ''
})

const classForm = ref({
  classId: '',
  className: '',
  classBegin: '',
  classTime: '',
  coach: ''
})

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

const fetchMembers = async () => {
  try {
    const response = await axios.get('/api/member/list')
    if (response.data.code === 200) {
      members.value = response.data.data
    }
  } catch (error) {
    console.error('Fetch members error:', error)
  }
}

const fetchEmployees = async () => {
  try {
    const response = await axios.get('/api/employee/list')
    if (response.data.code === 200) {
      employees.value = response.data.data
    }
  } catch (error) {
    console.error('Fetch employees error:', error)
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

const fetchOrders = async () => {
  try {
    const response = await axios.get('/api/order/list')
    if (response.data.code === 200) {
      orders.value = response.data.data
    }
  } catch (error) {
    console.error('Fetch orders error:', error)
  }
}

const editMember = (member) => {
  editingMember.value = member
  memberForm.value = { ...member }
  showMemberForm.value = true
}

const closeMemberForm = () => {
  showMemberForm.value = false
  editingMember.value = null
  memberForm.value = {
    memberAccount: '',
    memberPassword: '',
    memberName: '',
    memberGender: '男',
    memberAge: '',
    memberHeight: '',
    memberWeight: '',
    memberPhone: '',
    cardTime: '',
    cardClass: '',
    cardNextClass: ''
  }
}

const saveMember = async () => {
  try {
    if (editingMember.value) {
      await axios.put('/api/member/update', memberForm.value)
    } else {
      await axios.post('/api/member/add', memberForm.value)
    }
    closeMemberForm()
    fetchMembers()
  } catch (error) {
    console.error('Save member error:', error)
    alert('保存失败')
  }
}

const deleteMember = async (account) => {
  if (!confirm('确定要删除该会员吗？')) return
  try {
    await axios.delete(`/api/member/delete/${account}`)
    fetchMembers()
  } catch (error) {
    console.error('Delete member error:', error)
    alert('删除失败')
  }
}

const editEmployee = (employee) => {
  editingEmployee.value = employee
  employeeForm.value = { ...employee }
  showEmployeeForm.value = true
}

const closeEmployeeForm = () => {
  showEmployeeForm.value = false
  editingEmployee.value = null
  employeeForm.value = {
    employeeAccount: '',
    employeeName: '',
    employeeGender: '男',
    employeeAge: '',
    entryTime: '',
    staff: '',
    employeeMessage: ''
  }
}

const saveEmployee = async () => {
  try {
    if (editingEmployee.value) {
      await axios.put('/api/employee/update', employeeForm.value)
    } else {
      await axios.post('/api/employee/add', employeeForm.value)
    }
    closeEmployeeForm()
    fetchEmployees()
  } catch (error) {
    console.error('Save employee error:', error)
    alert('保存失败')
  }
}

const deleteEmployee = async (account) => {
  if (!confirm('确定要删除该员工吗？')) return
  try {
    await axios.delete(`/api/employee/delete/${account}`)
    fetchEmployees()
  } catch (error) {
    console.error('Delete employee error:', error)
    alert('删除失败')
  }
}

const editEquipment = (equipment) => {
  editingEquipment.value = equipment
  equipmentForm.value = { ...equipment }
  showEquipmentForm.value = true
}

const closeEquipmentForm = () => {
  showEquipmentForm.value = false
  editingEquipment.value = null
  equipmentForm.value = {
    equipmentId: '',
    equipmentName: '',
    equipmentLocation: '',
    equipmentStatus: '正常',
    equipmentMessage: ''
  }
}

const saveEquipment = async () => {
  try {
    if (editingEquipment.value) {
      await axios.put('/api/equipment/update', equipmentForm.value)
    } else {
      await axios.post('/api/equipment/add', equipmentForm.value)
    }
    closeEquipmentForm()
    fetchEquipments()
  } catch (error) {
    console.error('Save equipment error:', error)
    alert('保存失败')
  }
}

const deleteEquipment = async (id) => {
  if (!confirm('确定要删除该器材吗？')) return
  try {
    await axios.delete(`/api/equipment/delete/${id}`)
    fetchEquipments()
  } catch (error) {
    console.error('Delete equipment error:', error)
    alert('删除失败')
  }
}

const editClass = (cls) => {
  editingClass.value = cls
  classForm.value = { ...cls }
  if (cls.classBegin) {
    classForm.value.classBegin = new Date(cls.classBegin).toISOString().slice(0, 16)
  }
  showClassForm.value = true
}

const closeClassForm = () => {
  showClassForm.value = false
  editingClass.value = null
  classForm.value = {
    classId: '',
    className: '',
    classBegin: '',
    classTime: '',
    coach: ''
  }
}

const saveClass = async () => {
  try {
    if (editingClass.value) {
      await axios.put('/api/class/update', classForm.value)
    } else {
      await axios.post('/api/class/add', classForm.value)
    }
    closeClassForm()
    fetchClasses()
  } catch (error) {
    console.error('Save class error:', error)
    alert('保存失败')
  }
}

const deleteClass = async (id) => {
  if (!confirm('确定要删除该课程吗？')) return
  try {
    await axios.delete(`/api/class/delete/${id}`)
    fetchClasses()
  } catch (error) {
    console.error('Delete class error:', error)
    alert('删除失败')
  }
}

const deleteOrder = async (id) => {
  if (!confirm('确定要取消该订单吗？')) return
  try {
    await axios.delete(`/api/order/delete/${id}`)
    fetchOrders()
  } catch (error) {
    console.error('Delete order error:', error)
    alert('取消失败')
  }
}

onMounted(() => {
  fetchMembers()
  fetchEmployees()
  fetchEquipments()
  fetchClasses()
  fetchOrders()
})
</script>

<style scoped>
.admin-container {
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

.logout-btn {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid white;
  color: white;
  border-radius: 6px;
  cursor: pointer;
}

.main-layout {
  display: flex;
}

.sidebar {
  width: 200px;
  background: #2d3748;
  padding: 20px 0;
}

.menu-item {
  padding: 16px 24px;
  color: #e2e8f0;
  cursor: pointer;
  transition: background 0.3s;
}

.menu-item:hover {
  background: #4a5568;
}

.menu-item.active {
  background: #0ea5e9;
  color: white;
}

.content {
  flex: 1;
  padding: 30px;
}

.ai-sidebar {
  width: 320px;
  padding: 30px 20px;
  background: #f1f5f9;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
  color: #333;
}

.add-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.table-container {
  overflow-x: auto;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
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
}

.edit-btn {
  padding: 6px 12px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 8px;
}

.delete-btn {
  padding: 6px 12px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.status.normal {
  color: #22c55e;
}

.status.broken {
  color: #ef4444;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 12px;
  width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h3 {
  margin: 0 0 20px 0;
  color: #333;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 14px;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:disabled {
  background: #f5f5f5;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.cancel-btn {
  padding: 10px 20px;
  background: #ddd;
  color: #333;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.submit-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
</style>
