<template>
  <div class="notification-container">
    <div class="bell-wrapper" @click="toggleDropdown">
      <span class="bell-icon">🔔</span>
      <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
    </div>

    <div v-show="isOpen" class="notification-dropdown">
      <div class="dropdown-header">
        <h3>通知中心</h3>
        <button class="mark-all-read" @click="markAllAsRead" v-if="unreadCount > 0">
          全部已读
        </button>
      </div>

      <div class="notification-list">
        <div v-if="notifications.length === 0" class="empty-state">
          <span class="empty-icon">📭</span>
          <p>暂无通知</p>
        </div>

        <div 
          v-for="notification in notifications" 
          :key="notification.id"
          :class="['notification-item', { unread: !notification.read }]"
          @click="handleNotificationClick(notification)"
        >
          <span class="notification-icon">{{ getNotificationIcon(notification.type) }}</span>
          <div class="notification-content">
            <h4>{{ notification.title }}</h4>
            <p>{{ notification.content }}</p>
            <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 点击外部关闭下拉菜单 -->
    <div v-if="isOpen" class="overlay" @click="isOpen = false"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
const isOpen = ref(false);
const notifications = ref([]);
const unreadCount = ref(0);
const userAccount = ref(localStorage.getItem('account') || '');
const toggleDropdown = () => {
 isOpen.value = !isOpen.value;
 if (isOpen.value) {
 fetchNotifications();
 }
};
const getNotificationIcon = (type) => {
 const icons = {
 SYSTEM: '📢',
 ORDER: '📋',
 CHECKIN: '✅',
 PROMOTION: '🎁',
 DEFAULT: '🔔'
 };
 return icons[type] || icons.DEFAULT;
};
const formatTime = (dateStr) => {
 if (!dateStr)
 return '';
 const date = new Date(dateStr);
 const now = new Date();
 const diff = now.getTime() - date.getTime();
 const minutes = Math.floor(diff / 60000);
 const hours = Math.floor(diff / 3600000);
 const days = Math.floor(diff / 86400000);
 if (minutes < 1)
 return '刚刚';
 if (minutes < 60)
 return `${minutes}分钟前`;
 if (hours < 24)
 return `${hours}小时前`;
 if (days < 7)
 return `${days}天前`;
 return date.toLocaleDateString('zh-CN');
};
const fetchNotifications = async () => {
 if (!userAccount.value)
 return;
 try {
 const response = await axios.get(`/api/notification/all/${userAccount.value}`);
 if (response.data.code === 200) {
 notifications.value = response.data.data;
 unreadCount.value = notifications.value.filter(n => !n.read).length;
 }
 }
 catch (error) {
 console.error('Fetch notifications error:', error);
 }
};
const handleNotificationClick = async (notification) => {
 if (!notification.read) {
 try {
 await axios.post(`/api/notification/read/${notification.id}`);
 notification.read = true;
 unreadCount.value--;
 }
 catch (error) {
 console.error('Mark read error:', error);
 }
 }
};
const markAllAsRead = async () => {
 if (!userAccount.value)
 return;
 try {
 await axios.post(`/api/notification/read-all/${userAccount.value}`);
 notifications.value.forEach(n => n.read = true);
 unreadCount.value = 0;
 }
 catch (error) {
 console.error('Mark all read error:', error);
 }
};
let stompClient = null;
const connectWebSocket = () => {
  if (!userAccount.value) return;
  try {
    const socket = new SockJS('/ws');
 stompClient = Stomp.over(socket);
 stompClient.connect({}, () => {
 stompClient.subscribe(`/topic/notification/${userAccount.value}`, (message) => {
 const notification = JSON.parse(message.body);
 notifications.value.unshift(notification);
 if (!notification.read) {
 unreadCount.value++;
 }
 // 限制最多显示20条
 if (notifications.value.length > 20) {
 notifications.value.pop();
 }
 });
 });
 }
 catch (error) {
 console.error('WebSocket connect error:', error);
 }
};
onMounted(() => {
 fetchNotifications();
 // 尝试连接WebSocket
 if (window.SockJS && window.Stomp) {
 connectWebSocket();
 }
});
onUnmounted(() => {
 if (stompClient) {
 stompClient.disconnect();
 }
});
</script>

<style scoped>
.notification-container {
  position: relative;
  display: inline-block;
}

.bell-wrapper {
  position: relative;
  cursor: pointer;
  padding: 8px;
  font-size: 24px;
}

.badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background: #ef4444;
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.notification-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  width: 320px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.dropdown-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.mark-all-read {
  background: #0ea5e9;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

.mark-all-read:hover {
  background: #0284c7;
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
}

.empty-state {
  text-align: center;
  padding: 30px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 10px;
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.2s;
}

.notification-item:hover {
  background: #f8fafc;
}

.notification-item.unread {
  background: #fef3c7;
}

.notification-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  overflow: hidden;
}

.notification-content h4 {
  margin: 0 0 6px 0;
  font-size: 14px;
  color: #333;
}

.notification-content p {
  margin: 0 0 6px 0;
  font-size: 13px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 999;
}
</style>