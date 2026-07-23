<template>
  <div class="login-container">
    <div class="login-box">
      <h2>健身房管理系统</h2>
      
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>账号</label>
          <input 
            type="text" 
            v-model="account" 
            placeholder="请输入账号"
            required
          />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input 
            type="password" 
            v-model="password" 
            placeholder="请输入密码"
            required
          />
        </div>
        <button type="submit" class="login-btn">
          登录
        </button>
      </form>

      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const account = ref('')
const password = ref('')
const errorMessage = ref('')

const handleLogin = async () => {
  errorMessage.value = ''
  try {
    const url = '/api/login'
    
    const response = await axios.post(url, {
      account: account.value,
      password: password.value
    })

    if (response.data.code === 200) {
      const loginData = response.data.data
      localStorage.setItem('token', loginData.token)
      localStorage.setItem('role', loginData.role)
      localStorage.setItem('name', loginData.name)
      localStorage.setItem('account', loginData.account)

      if (loginData.role === 'ADMIN') {
        window.location.href = '/admin'
      } else {
        window.location.href = '/home'
      }
    } else {
      errorMessage.value = response.data.message
    }
  } catch (error) {
    errorMessage.value = '登录失败，请检查网络或服务器状态'
    console.error('Login error:', error)
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
}

.login-box {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 400px;
}

.login-box h2 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  font-size: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 14px;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  box-sizing: border-box;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #0ea5e9;
}

.login-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.login-btn:hover {
  opacity: 0.9;
}

.error-message {
  margin-top: 15px;
  padding: 12px;
  background: #fef2f2;
  color: #dc2626;
  border-radius: 6px;
  text-align: center;
}
</style>
