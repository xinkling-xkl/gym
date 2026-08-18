<template>
  <div class="ai-chat-container" :class="{ expanded: isExpanded }">
    <div class="chat-header" @click="toggleExpand">
      <div class="header-left">
        <span class="ai-icon">🤖</span>
        <span class="chat-title">AI助手</span>
      </div>
      <div class="header-right">
        <button class="clear-btn" @click.stop="clearChat">🗑️</button>
        <span class="expand-icon">{{ isExpanded ? '▼' : '▲' }}</span>
      </div>
    </div>

    <div v-show="isExpanded" class="chat-body">
      <div class="messages-container" ref="messagesContainer">
        <div class="welcome-message">
          <span class="ai-icon">🤖</span>
          <p>您好！我是您的健身助手，请问有什么可以帮助您的？</p>
          <p class="hint">您可以问我关于健身计划、营养建议、课程信息等问题。</p>
        </div>

        <div v-for="(msg, index) in messages" :key="index" :class="['message-item', msg.role]">
          <span class="msg-icon">{{ msg.role === 'user' ? '👤' : '🤖' }}</span>
          <div class="msg-content">
            <p>{{ msg.content }}</p>
          </div>
        </div>

        <div v-if="isLoading" class="loading-message">
          <span class="loading-icon">⏳</span>
          <span>AI正在思考中...</span>
        </div>
      </div>

      <div class="input-container">
        <input 
          type="text" 
          v-model="inputMessage" 
          @keyup.enter="sendMessage"
          :disabled="isLoading"
          placeholder="输入您的问题..."
          class="message-input"
        />
        <button 
          class="send-btn" 
          @click="sendMessage" 
          :disabled="isLoading || !inputMessage.trim()"
        >
          {{ isLoading ? '发送中...' : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import axios from 'axios'

const isExpanded = ref(true)
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const messagesContainer = ref(null)

const userId = ref(sessionStorage.getItem('account') || 1)

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const sendMessage = async () => {
  const message = inputMessage.value.trim()
  if (!message || isLoading.value) return

  // 添加用户消息
  messages.value.push({ role: 'user', content: message })
  inputMessage.value = ''
  isLoading.value = true
  await scrollToBottom()

  try {
    const response = await axios.post('/api/ai/chat', {
      message: message,
      userId: userId.value
    })

    if (response.data.code === 200) {
      messages.value.push({ role: 'assistant', content: response.data.data.reply })
    } else {
      // 显示后端返回的具体错误原因，方便排查
      messages.value.push({ role: 'assistant', content: response.data.message || '抱歉，服务暂时不可用。' })
    }
  } catch (error) {
    console.error('AI chat error:', error)
    const reason = error.response?.data?.message || error.message || '网络异常'
    messages.value.push({ role: 'assistant', content: `AI服务异常：${reason}` })
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}

// 加载历史对话（从 Redis 恢复）
const loadHistory = async () => {
  try {
    const response = await axios.get('/api/ai/history', {
      params: { userId: userId.value }
    })
    if (response.data.code === 200 && response.data.data) {
      messages.value = response.data.data
      await scrollToBottom()
    }
  } catch (error) {
    console.error('Load history error:', error)
  }
}

const clearChat = async () => {
  if (!confirm('确定要清空聊天记录吗？')) return

  try {
    await axios.post('/api/ai/clear', { userId: userId.value })
    messages.value = []
  } catch (error) {
    console.error('Clear chat error:', error)
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.ai-chat-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  cursor: pointer;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-icon {
  font-size: 24px;
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.clear-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  padding: 6px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
}

.clear-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.expand-icon {
  font-size: 12px;
}

.chat-body {
  display: flex;
  flex-direction: column;
  height: 400px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
}

.welcome-message {
  text-align: center;
  padding: 20px;
  background: white;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.welcome-message p {
  margin: 8px 0;
  color: #666;
}

.welcome-message .hint {
  font-size: 12px;
  color: #999;
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-item.user .msg-content {
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  border-radius: 12px 12px 0 12px;
}

.message-item.assistant .msg-content {
  background: white;
  color: #333;
  border-radius: 12px 12px 12px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.msg-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.msg-content {
  max-width: 75%;
  padding: 12px 16px;
}

.msg-content p {
  margin: 0;
  line-height: 1.5;
}

.loading-message {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #999;
  padding: 12px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.input-container {
  display: flex;
  gap: 10px;
  padding: 16px;
  background: white;
  border-top: 1px solid #eee;
}

.message-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 24px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.message-input:focus {
  border-color: #0ea5e9;
}

.message-input:disabled {
  background: #f5f5f5;
  color: #999;
}

.send-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  border: none;
  border-radius: 24px;
  cursor: pointer;
  font-size: 14px;
  transition: opacity 0.3s;
}

.send-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>