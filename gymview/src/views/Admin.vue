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
        <!-- 会员管理 -->
        <div v-show="activeMenu === 'member'">
          <div class="section-header">
            <h2>会员管理</h2>
            <button class="add-btn" @click="showMemberForm = true">添加会员</button>
          </div>
          <div class="search-bar">
            <input type="text" v-model="memberSearch" placeholder="搜索会员..." />
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
                  <th>身高</th>
                  <th>体重</th>
                  <th>卡类型</th>
                  <th>办卡时间</th>
                  <th>过期时间</th>
                  <th>剩余天数</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="member in pagedMembers" :key="member.memberAccount">
                  <td>{{ member.memberAccount }}</td>
                  <td>{{ member.memberName }}</td>
                  <td>{{ member.memberGender }}</td>
                  <td>{{ member.memberAge }}</td>
                  <td>{{ member.memberPhone }}</td>
                  <td>{{ member.memberHeight != null ? member.memberHeight + 'cm' : '-' }}</td>
                  <td>{{ member.memberWeight != null ? member.memberWeight + 'kg' : '-' }}</td>
                  <td>{{ getCardClassName(member.cardClass) }}</td>
                  <td>{{ formatDate(member.cardTime) }}</td>
                  <td>{{ formatDate(member.cardExpireDate) }}</td>
                  <td :class="{ expired: (member.cardNextClass != null ? member.cardNextClass : getRemainingDays(member.cardExpireDate)) <= 0 }">
                    {{ getRemainingDaysText(member.cardExpireDate, member.cardNextClass) }}
                  </td>
                  <td>
                    <button class="edit-btn" @click="editMember(member)">编辑</button>
                    <button class="delete-btn" @click="deleteMember(member.memberAccount)">删除</button>
                  </td>
                </tr>
                <tr v-if="pagedMembers.length === 0">
                  <td colspan="12" style="text-align:center;color:#999;">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination" v-if="memberTotalPages > 1">
            <button :disabled="memberPage === 1" @click="memberPage--">上一页</button>
            <span 
              v-for="p in memberTotalPages" 
              :key="p" 
              :class="['page-num', { active: memberPage === p }]"
              @click="memberPage = p"
            >{{ p }}</span>
            <button :disabled="memberPage === memberTotalPages" @click="memberPage++">下一页</button>
          </div>
        </div>

        <!-- 员工管理 -->
        <div v-show="activeMenu === 'employee'">
          <div class="section-header">
            <h2>员工管理</h2>
            <button class="add-btn" @click="showEmployeeForm = true">添加员工</button>
          </div>
          <div class="search-bar">
            <input type="text" v-model="employeeSearch" placeholder="搜索员工..." />
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
                  <th>入职时间</th>
                  <th>备注</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="employee in pagedEmployees" :key="employee.employeeAccount">
                  <td>{{ employee.employeeAccount }}</td>
                  <td>{{ employee.employeeName }}</td>
                  <td>{{ employee.employeeGender }}</td>
                  <td>{{ employee.employeeAge }}</td>
                  <td>{{ employee.staff }}</td>
                  <td>{{ formatDate(employee.entryTime) }}</td>
                  <td>{{ employee.employeeMessage || '-' }}</td>
                  <td>
                    <button class="edit-btn" @click="editEmployee(employee)">编辑</button>
                    <button class="delete-btn" @click="deleteEmployee(employee.employeeAccount)">删除</button>
                  </td>
                </tr>
                <tr v-if="pagedEmployees.length === 0">
                  <td colspan="8" style="text-align:center;color:#999;">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination" v-if="employeeTotalPages > 1">
            <button :disabled="employeePage === 1" @click="employeePage--">上一页</button>
            <span 
              v-for="p in employeeTotalPages" 
              :key="p" 
              :class="['page-num', { active: employeePage === p }]"
              @click="employeePage = p"
            >{{ p }}</span>
            <button :disabled="employeePage === employeeTotalPages" @click="employeePage++">下一页</button>
          </div>
        </div>

        <!-- 器材管理 -->
        <div v-show="activeMenu === 'equipment'">
          <div class="section-header">
            <h2>器材管理</h2>
            <button class="add-btn" @click="showEquipmentForm = true">添加器材</button>
          </div>
          <div class="search-bar">
            <input type="text" v-model="equipmentSearch" placeholder="搜索器材..." />
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
                <tr v-for="equipment in pagedEquipments" :key="equipment.equipmentId">
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
                <tr v-if="pagedEquipments.length === 0">
                  <td colspan="5" style="text-align:center;color:#999;">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination" v-if="equipmentTotalPages > 1">
            <button :disabled="equipmentPage === 1" @click="equipmentPage--">上一页</button>
            <span 
              v-for="p in equipmentTotalPages" 
              :key="p" 
              :class="['page-num', { active: equipmentPage === p }]"
              @click="equipmentPage = p"
            >{{ p }}</span>
            <button :disabled="equipmentPage === equipmentTotalPages" @click="equipmentPage++">下一页</button>
          </div>
        </div>

        <!-- 课程管理 -->
        <div v-show="activeMenu === 'class'">
          <div class="section-header">
            <h2>课程管理</h2>
            <button class="add-btn" @click="showClassForm = true">添加课程</button>
          </div>
          <div class="search-bar">
            <input type="text" v-model="classSearch" placeholder="搜索课程..." />
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
                  <th>人数</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="cls in pagedClasses" :key="cls.classId">
                  <td>{{ cls.classId }}</td>
                  <td>{{ cls.className }}</td>
                  <td>{{ cls.coach }}</td>
                  <td>{{ formatDate(cls.classBegin) }}</td>
                  <td>{{ cls.classTime }}</td>
                  <td>{{ cls.maxCapacity || '不限' }}</td>
                  <td>
                    <button class="edit-btn" @click="editClass(cls)">编辑</button>
                    <button class="delete-btn" @click="cancelClassOrders(cls.classId, cls.className)">取消预约</button>
                    <button class="delete-btn" @click="deleteClass(cls.classId)">删除</button>
                  </td>
                </tr>
                <tr v-if="pagedClasses.length === 0">
                  <td colspan="7" style="text-align:center;color:#999;">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination" v-if="classTotalPages > 1">
            <button :disabled="classPage === 1" @click="classPage--">上一页</button>
            <span 
              v-for="p in classTotalPages" 
              :key="p" 
              :class="['page-num', { active: classPage === p }]"
              @click="classPage = p"
            >{{ p }}</span>
            <button :disabled="classPage === classTotalPages" @click="classPage++">下一页</button>
          </div>
        </div>

        <!-- 订单管理 -->
        <div v-show="activeMenu === 'order'">
          <div class="section-header">
            <h2>订单管理</h2>
          </div>
          <div class="search-bar">
            <input type="text" v-model="orderSearch" placeholder="搜索订单..." />
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
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in pagedOrders" :key="order.classOrderId">
                  <td>{{ order.classOrderId }}</td>
                  <td>{{ order.memberAccount }}</td>
                  <td>{{ order.memberName }}</td>
                  <td>{{ order.className || getClassName(order.classId) }}</td>
                  <td>{{ order.coach }}</td>
                  <td>{{ formatDate(order.classBegin) }}</td>
                  <td><span :class="['status-tag', orderStatusClass(order.status)]">{{ orderStatusText(order.status) }}</span></td>
                  <td>
                    <button v-if="order.status === 'BOOKED' || order.status === 'CHECKED_IN'"
                            class="edit-btn" @click="completeOrder(order.classOrderId)">完成</button>
                    <button v-if="order.status === 'BOOKED'"
                            class="delete-btn" @click="markNoShow(order.classOrderId)">旷课</button>
                    <button v-if="order.status === 'BOOKED' || order.status === 'CHECKED_IN'"
                            class="delete-btn" @click="adminCancelOrder(order.classOrderId)">强制取消</button>
                    <button class="delete-btn" @click="deleteOrder(order.classOrderId)">删除</button>
                  </td>
                </tr>
                <tr v-if="pagedOrders.length === 0">
                  <td colspan="8" style="text-align:center;color:#999;">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination" v-if="orderTotalPages > 1">
            <button :disabled="orderPage === 1" @click="orderPage--">上一页</button>
            <span 
              v-for="p in orderTotalPages" 
              :key="p" 
              :class="['page-num', { active: orderPage === p }]"
              @click="orderPage = p"
            >{{ p }}</span>
            <button :disabled="orderPage === orderTotalPages" @click="orderPage++">下一页</button>
          </div>
        </div>
      </main>
    </div>

    <!-- 会员表单弹窗 -->
    <div v-if="showMemberForm" class="modal">
      <div class="modal-content">
        <h3>{{ editingMember ? '编辑会员' : '添加会员' }}</h3>
        <form @submit.prevent="saveMember">
          <div class="form-row">
            <div class="form-group">
              <label>账号</label>
              <input type="text" v-model="memberForm.memberAccount" :disabled="!!editingMember" required />
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
              <input type="text" v-model="memberForm.memberAge" />
            </div>
            <div class="form-group">
              <label>电话</label>
              <input type="text" v-model="memberForm.memberPhone" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>身高 (cm)</label>
              <input type="text" v-model="memberForm.memberHeight" placeholder="如 175.5" />
            </div>
            <div class="form-group">
              <label>体重 (kg)</label>
              <input type="text" v-model="memberForm.memberWeight" placeholder="如 70.5" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>卡类型</label>
              <select v-model="memberForm.cardClass">
                <option value="1">月卡</option>
                <option value="2">季卡</option>
                <option value="3">年卡</option>
              </select>
            </div>
            <div class="form-group">
              <label>会员卡时间</label>
              <input type="date" v-model="memberForm.cardTime" />
            </div>
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="closeMemberForm">取消</button>
            <button type="submit" class="submit-btn">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 员工表单弹窗 -->
    <div v-if="showEmployeeForm" class="modal">
      <div class="modal-content">
        <h3>{{ editingEmployee ? '编辑员工' : '添加员工' }}</h3>
        <form @submit.prevent="saveEmployee">
          <div class="form-row">
            <div class="form-group">
              <label>账号</label>
              <input type="text" v-model="employeeForm.employeeAccount" :disabled="!!editingEmployee" required />
            </div>
            <div class="form-group">
              <label>姓名</label>
              <input type="text" v-model="employeeForm.employeeName" required />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>密码</label>
              <input type="password" v-model="employeeForm.employeePassword" />
            </div>
            <div class="form-group">
              <label>性别</label>
              <select v-model="employeeForm.employeeGender">
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>年龄</label>
              <input type="text" v-model="employeeForm.employeeAge" />
            </div>
            <div class="form-group">
              <label>职务</label>
              <input type="text" v-model="employeeForm.staff" />
            </div>
          </div>
          <div class="form-group">
            <label>入职时间</label>
            <input type="date" v-model="employeeForm.entryTime" />
          </div>
          <div class="form-group">
            <label>备注</label>
            <input type="text" v-model="employeeForm.employeeMessage" />
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="closeEmployeeForm">取消</button>
            <button type="submit" class="submit-btn">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 器材表单弹窗 -->
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
            <input type="text" v-model="equipmentForm.equipmentStatus" placeholder="正常/损坏/维修中" />
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="closeEquipmentForm">取消</button>
            <button type="submit" class="submit-btn">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 课程表单弹窗 -->
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
          <div class="form-row">
            <div class="form-group">
              <label>开始时间</label>
              <input type="datetime-local" v-model="classForm.classBegin" />
            </div>
            <div class="form-group">
              <label>最大人数</label>
              <input type="number" v-model="classForm.maxCapacity" min="1" placeholder="如：15" />
            </div>
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
import { ref, computed, onMounted, watch } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import NotificationBell from '../components/NotificationBell.vue'

const router = useRouter()
const userName = ref(sessionStorage.getItem('name') || '')
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

// 搜索关键词
const memberSearch = ref('')
const employeeSearch = ref('')
const equipmentSearch = ref('')
const classSearch = ref('')
const orderSearch = ref('')

// 分页页码
const memberPage = ref(1)
const employeePage = ref(1)
const equipmentPage = ref(1)
const classPage = ref(1)
const orderPage = ref(1)

const pageSize = 5

// 客户端搜索过滤
const filteredMembers = computed(() => {
  if (!memberSearch.value) return members.value
  const kw = memberSearch.value.toLowerCase()
  return members.value.filter(m =>
    (m.memberAccount && String(m.memberAccount).toLowerCase().includes(kw)) ||
    (m.memberName && m.memberName.toLowerCase().includes(kw)) ||
    (m.memberPhone && String(m.memberPhone).toLowerCase().includes(kw)) ||
    (m.cardClass && m.cardClass.toLowerCase().includes(kw))
  )
})

const filteredEmployees = computed(() => {
  if (!employeeSearch.value) return employees.value
  const kw = employeeSearch.value.toLowerCase()
  return employees.value.filter(e =>
    (e.employeeAccount && String(e.employeeAccount).toLowerCase().includes(kw)) ||
    (e.employeeName && e.employeeName.toLowerCase().includes(kw)) ||
    (e.staff && e.staff.toLowerCase().includes(kw))
  )
})

const filteredEquipments = computed(() => {
  if (!equipmentSearch.value) return equipments.value
  const kw = equipmentSearch.value.toLowerCase()
  return equipments.value.filter(eq =>
    (eq.equipmentId && String(eq.equipmentId).toLowerCase().includes(kw)) ||
    (eq.equipmentName && eq.equipmentName.toLowerCase().includes(kw)) ||
    (eq.equipmentLocation && eq.equipmentLocation.toLowerCase().includes(kw))
  )
})

const filteredClasses = computed(() => {
  if (!classSearch.value) return classes.value
  const kw = classSearch.value.toLowerCase()
  return classes.value.filter(c =>
    (c.classId && String(c.classId).toLowerCase().includes(kw)) ||
    (c.className && c.className.toLowerCase().includes(kw)) ||
    (c.coach && c.coach.toLowerCase().includes(kw))
  )
})

const filteredOrders = computed(() => {
  if (!orderSearch.value) return orders.value
  const kw = orderSearch.value.toLowerCase()
  return orders.value.filter(o =>
    (o.classOrderId && String(o.classOrderId).toLowerCase().includes(kw)) ||
    (o.memberAccount && String(o.memberAccount).toLowerCase().includes(kw)) ||
    (o.memberName && o.memberName.toLowerCase().includes(kw)) ||
    (o.coach && o.coach.toLowerCase().includes(kw))
  )
})

// 分页计算
const memberTotalPages = computed(() => Math.ceil(filteredMembers.value.length / pageSize))
const employeeTotalPages = computed(() => Math.ceil(filteredEmployees.value.length / pageSize))
const equipmentTotalPages = computed(() => Math.ceil(filteredEquipments.value.length / pageSize))
const classTotalPages = computed(() => Math.ceil(filteredClasses.value.length / pageSize))
const orderTotalPages = computed(() => Math.ceil(filteredOrders.value.length / pageSize))

const pagedMembers = computed(() => {
  const start = (memberPage.value - 1) * pageSize
  return filteredMembers.value.slice(start, start + pageSize)
})
const pagedEmployees = computed(() => {
  const start = (employeePage.value - 1) * pageSize
  return filteredEmployees.value.slice(start, start + pageSize)
})
const pagedEquipments = computed(() => {
  const start = (equipmentPage.value - 1) * pageSize
  return filteredEquipments.value.slice(start, start + pageSize)
})
const pagedClasses = computed(() => {
  const start = (classPage.value - 1) * pageSize
  return filteredClasses.value.slice(start, start + pageSize)
})
const pagedOrders = computed(() => {
  const start = (orderPage.value - 1) * pageSize
  return filteredOrders.value.slice(start, start + pageSize)
})

// 搜索时重置页码
watch(memberSearch, () => { memberPage.value = 1 })
watch(employeeSearch, () => { employeePage.value = 1 })
watch(equipmentSearch, () => { equipmentPage.value = 1 })
watch(classSearch, () => { classPage.value = 1 })
watch(orderSearch, () => { orderPage.value = 1 })

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
  employeePassword: '',
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
  coach: '',
  maxCapacity: ''
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getClassName = (classId) => {
  const cls = classes.value.find(c => c.classId === classId)
  return cls ? cls.className : '未知课程'
}

const getCardClassName = (cardClass) => {
  if (cardClass == null) return '-'
  return cardClass === 1 ? '月卡' : cardClass === 2 ? '季卡' : cardClass === 3 ? '年卡' : cardClass
}

const getRemainingDays = (cardTime, cardNextClass) => {
  if (cardNextClass != null) return cardNextClass
  if (!cardTime) return null
  const now = new Date()
  const expire = new Date(cardTime)
  return Math.ceil((expire - now) / (1000 * 60 * 60 * 24))
}

const getRemainingDaysText = (cardTime, cardNextClass) => {
  const days = getRemainingDays(cardTime, cardNextClass)
  if (days == null) return '-'
  if (days <= 0) return '已过期'
  return days + '天'
}

const cleanForm = (form) => {
  const cleaned = {}
  for (const key of Object.keys(form)) {
    const val = form[key]
    if (val === '' || val === undefined || val === null) continue
    cleaned[key] = val
  }
  return cleaned
}

const toNum = (v) => {
  if (v === '' || v == null) return undefined
  const n = Number(v)
  return isNaN(n) ? undefined : n
}

const buildMemberBody = () => {
  const f = memberForm.value
  return {
    memberAccount: toNum(f.memberAccount),
    memberPassword: f.memberPassword || undefined,
    memberName: f.memberName || undefined,
    memberGender: f.memberGender || undefined,
    memberAge: toNum(f.memberAge),
    memberPhone: toNum(f.memberPhone),
    memberHeight: f.memberHeight ? Number(f.memberHeight) : undefined,
    memberWeight: f.memberWeight ? Number(f.memberWeight) : undefined,
    cardClass: toNum(f.cardClass),
    cardTime: f.cardTime || undefined,
    cardNextClass: toNum(f.cardNextClass)
  }
}

const buildEmployeeBody = () => {
  const f = employeeForm.value
  return {
    employeeAccount: toNum(f.employeeAccount),
    employeePassword: f.employeePassword || undefined,
    employeeName: f.employeeName || undefined,
    employeeGender: f.employeeGender || undefined,
    employeeAge: toNum(f.employeeAge),
    entryTime: f.entryTime || undefined,
    staff: f.staff || undefined,
    employeeMessage: f.employeeMessage || undefined
  }
}

const handleLogout = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('name')
  sessionStorage.removeItem('account')
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
  // select 下拉框需要字符串匹配
  if (memberForm.value.cardClass != null) {
    memberForm.value.cardClass = String(memberForm.value.cardClass)
  }
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
    const data = buildMemberBody()
    if (editingMember.value) {
      await axios.put('/api/member/update', data)
    } else {
      await axios.post('/api/member/add', data)
    }
    closeMemberForm()
    fetchMembers()
  } catch (error) {
    console.error('Save member error:', error)
    alert('保存失败：' + (error.response?.data?.message || error.message))
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
    employeePassword: '',
    employeeGender: '男',
    employeeAge: '',
    entryTime: '',
    staff: '',
    employeeMessage: ''
  }
}

const saveEmployee = async () => {
  try {
    const data = buildEmployeeBody()
    if (editingEmployee.value) {
      await axios.put('/api/employee/update', data)
    } else {
      await axios.post('/api/employee/add', data)
    }
    closeEmployeeForm()
    fetchEmployees()
  } catch (error) {
    console.error('Save employee error:', error)
    alert('保存失败：' + (error.response?.data?.message || error.message))
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
    coach: '',
    maxCapacity: ''
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

const adminCancelOrder = async (id) => {
  if (!confirm('管理员强制取消该订单？\n此操作不受状态和时间限制。')) return
  try {
    const response = await axios.post(`/api/order/adminCancel/${id}`)
    if (response.data.code === 200) {
      alert(response.data.message || '强制取消成功')
      fetchOrders()
    } else {
      alert(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('Admin cancel error:', error)
    alert('操作失败')
  }
}

const deleteOrder = async (id) => {
  if (!confirm('确定要删除该订单吗？\n此操作不可恢复，将永久移除订单记录。')) return
  try {
    const response = await axios.delete(`/api/order/delete/${id}`)
    if (response.data.code === 200) {
      alert('删除成功')
      fetchOrders()
    } else {
      alert(response.data.message || '删除失败')
    }
  } catch (error) {
    console.error('Delete order error:', error)
    alert('删除失败')
  }
}

const cancelOrder = async (id) => {
  if (!confirm('确定要取消该订单吗？\n开课前2小时内取消不退课时。')) return
  try {
    const response = await axios.post(`/api/order/cancel/${id}`)
    if (response.data.code === 200) {
      alert(response.data.message || '取消成功')
      fetchOrders()
    } else {
      alert(response.data.message || '取消失败')
    }
  } catch (error) {
    console.error('Cancel order error:', error)
    alert('取消失败')
  }
}

const completeOrder = async (id) => {
  if (!confirm('确认标记该订单为已完成？')) return
  try {
    const response = await axios.post(`/api/order/complete/${id}`)
    if (response.data.code === 200) {
      alert('已标记完成')
      fetchOrders()
    } else {
      alert(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('Complete order error:', error)
    alert('操作失败')
  }
}

const markNoShow = async (id) => {
  if (!confirm('确认标记该会员旷课？\n课时不退回。')) return
  try {
    const response = await axios.post(`/api/order/noshow/${id}`)
    if (response.data.code === 200) {
      alert('已标记旷课')
      fetchOrders()
    } else {
      alert(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('NoShow order error:', error)
    alert('操作失败')
  }
}

const cancelClassOrders = async (classId, className) => {
  if (!confirm(`确定要取消课程「${className || classId}」下所有预约吗？\n该操作会自动退回所有会员的课时。`)) return
  try {
    const response = await axios.post(`/api/order/cancelByClass/${classId}`)
    if (response.data.code === 200) {
      alert(response.data.message || '取消成功')
      fetchOrders()
    } else {
      alert(response.data.message || '取消失败')
    }
  } catch (error) {
    console.error('Cancel class orders error:', error)
    alert('取消失败')
  }
}

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

// 选择卡类型时前端预览到期时间
watch(() => memberForm.value.cardClass, (val) => {
  if (!val) return
  const now = new Date()
  if (val === '1') now.setMonth(now.getMonth() + 1)
  else if (val === '2') now.setMonth(now.getMonth() + 3)
  else if (val === '3') now.setFullYear(now.getFullYear() + 1)
  else return
  memberForm.value.cardTime = now.toISOString().slice(0, 10)
})

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

.search-bar {
  margin-bottom: 16px;
}

.search-bar input {
  width: 100%;
  max-width: 360px;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.3s;
}

.search-bar input:focus {
  border-color: #0ea5e9;
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

td.expired {
  color: #ef4444;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 0;
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

.page-num {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: all 0.2s;
}

.page-num:hover {
  border-color: #0ea5e9;
  color: #0ea5e9;
}

.page-num.active {
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  border-color: transparent;
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