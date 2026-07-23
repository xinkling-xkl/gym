import './assets/main.css'

import { createApp } from 'vue'
import axios from 'axios'
import App from './App.vue'
import router from './router/index.js'

// Axios 拦截器：自动携带 JWT Token
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => {
  return Promise.reject(error)
})

// Axios 拦截器：401 自动跳转登录页
axios.interceptors.response.use(response => {
  return response
}, error => {
  if (error.response && error.response.status === 401) {
    localStorage.clear()
    router.push('/')
  }
  return Promise.reject(error)
})

const app = createApp(App)
app.use(router)
app.mount('#app')
