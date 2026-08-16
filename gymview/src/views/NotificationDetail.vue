<template>
  <div class="notification-detail-page">
    <div class="page-header">
      <button class="back-btn" @click="goBack">← 返回通知中心</button>
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>

    <div v-else-if="notification" class="detail-card">
      <div class="detail-header">
        <span class="detail-icon">{{ getNotificationIcon(notification.type) }}</span>
        <h2>{{ notification.title }}</h2>
      </div>

      <div class="detail-meta">
        <span class="meta-time">{{ formatTime(notification.createTime) }}</span>
        <span :class="['status-tag', notification.read ? 'read' : 'unread']">
          {{ notification.read ? '已读' : '未读' }}
        </span>
      </div>

      <div class="detail-content">{{ notification.content }}</div>

      <div class="detail-actions">
        <button v-if="!notification.read" class="read-btn" @click="markAsRead">标记为已读</button>
        <button class="delete-btn" @click="deleteNotification">删除通知</button>
      </div>
    </div>

    <div v-else class="empty-state">
      <span class="empty-icon">🔍</span>
      <p>通知不存在或已被删除</p>
      <button class="back-btn" @click="goBack">返回通知中心</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const notification = ref(null)
const loading = ref(true)
const userAccount = ref(sessionStorage.getItem('account') || '')

const goBack = () => {
  router.push('/notifications')
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
  return date.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

const fetchNotification = async () => {
  const notificationId = route.params.id
  if (!notificationId || !userAccount.value) {
    loading.value = false
    return
  }
  try {
    const response = await axios.get(`/api/notification/all/${userAccount.value}`)
    if (response.data.code === 200) {
      const all = response.data.data || []
      notification.value = all.find(n => String(n.id) === String(notificationId)) || null
    }
  } catch (error) {
    console.error('Fetch notification error:', error)
  } finally {
    loading.value = false
  }
}

const markAsRead = async () => {
  if (!notification.value) return
  try {
    await axios.post(`/api/notification/read/${notification.value.id}`)
    notification.value.read = true
  } catch (error) {
    console.error('Mark read error:', error)
    alert('标记已读失败')
  }
}

const deleteNotification = async () => {
  if (!notification.value) return
  if (!confirm('确定要删除这条通知吗？此操作不可恢复。')) return
  try {
    await axios.delete(`/api/notification/${notification.value.id}`)
    alert('删除成功')
    router.push('/notifications')
  } catch (error) {
    console.error('Delete notification error:', error)
    alert('删除失败')
  }
}

onMounted(() => {
  fetchNotification()
})
</script>

<style scoped>
.notification-detail-page {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 20px 40px;
}

.page-header {
  margin-bottom: 20px;
  background: white;
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
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

.detail-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 32px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.detail-icon {
  font-size: 40px;
}

.detail-header h2 {
  margin: 0;
  font-size: 22px;
  color: #333;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}

.meta-time {
  font-size: 14px;
  color: #999;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.read {
  background: #f0fdf4;
  color: #16a34a;
}

.status-tag.unread {
  background: #fef9c3;
  color: #ca8a04;
}

.detail-content {
  font-size: 16px;
  line-height: 1.8;
  color: #444;
  padding: 20px 0;
  white-space: pre-wrap;
}

.detail-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.read-btn {
  background: #0ea5e9;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.read-btn:hover {
  background: #0284c7;
}

.delete-btn {
  background: #fef2f2;
  color: #ef4444;
  border: 1px solid #fecaca;
  padding: 10px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.delete-btn:hover {
  background: #fee2e2;
}

.loading-state {
  text-align: center;
  padding: 60px;
  color: #999;
  font-size: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  background: white;
  border-radius: 12px;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 10px;
}

.empty-state .back-btn {
  margin-top: 16px;
  background: #0ea5e9;
  color: white;
  border: none;
  padding: 8px 24px;
}
</style>
