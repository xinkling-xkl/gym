<template>
  <div class="notification-center">
    <div class="page-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h2>通知中心</h2>
      <button class="mark-all-btn" v-if="unreadCount > 0" @click="markAllAsRead">全部已读</button>
    </div>

    <div class="filter-bar">
      <button :class="['filter-btn', { active: filter === 'all' }]" @click="filter = 'all'">全部</button>
      <button :class="['filter-btn', { active: filter === 'unread' }]" @click="filter = 'unread'">未读 ({{ unreadCount }})</button>
      <button :class="['filter-btn', { active: filter === 'read' }]" @click="filter = 'read'">已读</button>
    </div>

    <div class="notification-list">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="filteredNotifications.length === 0" class="empty-state">
        <span class="empty-icon">📭</span>
        <p>暂无通知</p>
      </div>
      <div v-else v-for="notification in filteredNotifications"
           :key="notification.id"
           :class="['notification-item', { unread: !notification.read }]"
           @click="goToDetail(notification)">
        <span class="notification-icon">{{ getNotificationIcon(notification.type) }}</span>
        <div class="notification-content">
          <div class="content-header">
            <h4>{{ notification.title }}</h4>
            <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
          </div>
          <p>{{ notification.content }}</p>
        </div>
        <div class="item-actions">
          <button v-if="!notification.read" class="read-btn" @click.stop="markAsRead(notification)">已读</button>
          <button class="delete-btn" @click.stop="deleteNotification(notification)">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const notifications = ref([])
const loading = ref(true)
const filter = ref('all')
const userAccount = ref(sessionStorage.getItem('account') || '')

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

const filteredNotifications = computed(() => {
  if (filter.value === 'unread') return notifications.value.filter(n => !n.read)
  if (filter.value === 'read') return notifications.value.filter(n => n.read)
  return notifications.value
})

const goBack = () => {
  const role = sessionStorage.getItem('role')
  const rolePageMap = { MEMBER: '/home', EMPLOYEE: '/employee', ADMIN: '/admin' }
  router.push(rolePageMap[role] || '/')
}

const goToDetail = (notification) => {
  router.push(`/notifications/${notification.id}`)
}

const getNotificationIcon = (type) => {
  const icons = {
    SYSTEM: '📢', ORDER: '📋', CHECKIN: '✅', PROMOTION: '🎁',
    BOOKING_SUCCESS: '📅', BOOKING_CANCELLED: '❌', COURSE_CANCEL: '🚫',
    COURSE_CHANGE: '📝', MEMBER_EXPIRE: '⏰', DEFAULT: '🔔'
  }
  return icons[type] || icons.DEFAULT
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const fetchNotifications = async () => {
  if (!userAccount.value) return
  loading.value = true
  try {
    const response = await axios.get(`/api/notification/all/${userAccount.value}`)
    if (response.data.code === 200) {
      notifications.value = response.data.data || []
    }
  } catch (error) {
    console.error('Fetch notifications error:', error)
  } finally {
    loading.value = false
  }
}

const markAsRead = async (notification) => {
  try {
    await axios.post(`/api/notification/read/${notification.id}`)
    notification.read = true
  } catch (error) {
    console.error('Mark read error:', error)
    alert('标记已读失败')
  }
}

const markAllAsRead = async () => {
  try {
    await axios.post(`/api/notification/read-all/${userAccount.value}`)
    notifications.value.forEach(n => { n.read = true })
  } catch (error) {
    console.error('Mark all read error:', error)
    alert('全部已读失败')
  }
}

const deleteNotification = async (notification) => {
  if (!confirm('确定要删除这条通知吗？')) return
  try {
    await axios.delete(`/api/notification/${notification.id}`)
    notifications.value = notifications.value.filter(n => n.id !== notification.id)
  } catch (error) {
    console.error('Delete notification error:', error)
    alert('删除失败')
  }
}

onMounted(() => {
  fetchNotifications()
})
</script>

<style scoped>
.notification-center {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 20px 40px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  background: white;
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.page-header h2 {
  margin: 0;
  flex: 1;
  font-size: 20px;
  color: #333;
}

.back-btn {
  background: none;
  border: 1px solid #ddd;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  font-size: 14px;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #f5f5f5;
  border-color: #0ea5e9;
  color: #0ea5e9;
}

.mark-all-btn {
  background: #0ea5e9;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.mark-all-btn:hover {
  background: #0284c7;
}

.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.filter-btn {
  background: white;
  border: 1px solid #ddd;
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}

.filter-btn.active {
  background: #0ea5e9;
  color: white;
  border-color: #0ea5e9;
}

.notification-list {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 10px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 24px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.2s;
}

.notification-item:hover {
  background: #f8fafc;
}

.notification-item.unread {
  background: #fef9c3;
}

.notification-item.unread:hover {
  background: #fef3c7;
}

.notification-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  overflow: hidden;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.content-header h4 {
  margin: 0;
  font-size: 15px;
  color: #333;
}

.notification-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 12px;
}

.notification-content p {
  margin: 0;
  font-size: 13px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.read-btn {
  background: #f0f9ff;
  color: #0ea5e9;
  border: 1px solid #bae6fd;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}

.read-btn:hover {
  background: #e0f2fe;
}

.delete-btn {
  background: #fef2f2;
  color: #ef4444;
  border: 1px solid #fecaca;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}

.delete-btn:hover {
  background: #fee2e2;
}
</style>
