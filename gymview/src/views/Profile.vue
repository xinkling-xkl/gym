<template>
  <div class="profile-container">
    <header class="header">
      <h1>个人资料</h1>
      <div class="user-info">
        <button class="back-btn" @click="goBack">返回主页</button>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <main class="main-content">
      <div class="profile-card">
        <!-- 头像区域 -->
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <img v-if="avatarUrl" :src="avatarUrl" class="avatar" alt="头像" />
            <div v-else class="avatar avatar-placeholder">{{ form.memberName?.charAt(0) || '?' }}</div>
          </div>
          <div class="avatar-actions">
            <label class="upload-btn">
              选择头像
              <input type="file" accept="image/*" @change="handleAvatarChange" hidden />
            </label>
            <span class="avatar-hint">支持 jpg/png，建议正方形</span>
          </div>
        </div>

        <!-- 资料表单 -->
        <form @submit.prevent="saveProfile" class="profile-form">
          <div class="form-row">
            <div class="form-group">
              <label>账号</label>
              <input :value="form.memberAccount" disabled />
            </div>
            <div class="form-group">
              <label>姓名</label>
              <input v-model="form.memberName" required />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>性别</label>
              <select v-model="form.memberGender">
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
            <div class="form-group">
              <label>年龄</label>
              <input v-model.number="form.memberAge" type="number" min="1" max="120" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>身高 (cm)</label>
              <input v-model.number="form.memberHeight" type="number" step="0.1" placeholder="如 175.5" />
            </div>
            <div class="form-group">
              <label>体重 (kg)</label>
              <input v-model.number="form.memberWeight" type="number" step="0.1" placeholder="如 70.5" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>电话</label>
              <input v-model="form.memberPhone" placeholder="如 13800138000" />
            </div>
            <div class="form-group">
              <label>BMI</label>
              <input :value="bmiText" disabled />
            </div>
          </div>

          <!-- 卡信息只读 -->
          <div class="card-info">
            <h3>会员卡信息（不可自行修改，如需变更请联系前台）</h3>
            <div class="card-info-grid">
              <div><span>卡类型:</span>{{ cardTypeText(form.cardClass) }}</div>
              <div><span>过期时间:</span>{{ form.cardExpireDate || '-' }}</div>
              <div><span>剩余天数:</span>{{ daysRemaining }} 天</div>
              <div><span>办卡时间:</span>{{ form.cardTime || '-' }}</div>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="goBack">取消</button>
            <button type="submit" class="submit-btn">保存修改</button>
          </div>
        </form>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const account = ref(sessionStorage.getItem('account') || '')
const form = ref({
  memberAccount: null,
  memberName: '',
  memberGender: '',
  memberAge: null,
  memberHeight: null,
  memberWeight: null,
  memberPhone: null,
  cardClass: null,
  cardTime: '',
  cardExpireDate: '',
  avatar: ''
})

const goBack = () => router.push('/home')

const handleLogout = () => {
  sessionStorage.clear()
  window.location.href = '/'
}

const cardTypeText = (cardClass) => {
  return { 1: '月卡', 2: '季卡', 3: '年卡' }[cardClass] || '-'
}

const avatarUrl = computed(() => {
  if (!form.value.avatar) return ''
  // DB 存的是相对路径如 /avatar/xxx.jpg，通过网关 /pictures 访问
  return '/pictures' + form.value.avatar
})

const bmiText = computed(() => {
  const h = form.value.memberHeight
  const w = form.value.memberWeight
  if (!h || !w) return '-'
  const bmi = w / ((h / 100) * (h / 100))
  const cat = bmi < 18.5 ? '(偏瘦)' : bmi < 24 ? '(正常)' : bmi < 28 ? '(偏胖)' : '(肥胖)'
  return bmi.toFixed(1) + ' ' + cat
})

const daysRemaining = computed(() => {
  if (form.value.cardNextClass == null) return '-'
  return form.value.cardNextClass > 0 ? form.value.cardNextClass : 0
})

const fetchMember = async () => {
  try {
    const res = await axios.get(`/api/member/${account.value}`)
    if (res.data.code === 200) {
      form.value = { ...res.data.data }
    }
  } catch (e) {
    console.error('fetch member error:', e)
    alert('加载资料失败')
  }
}

const handleAvatarChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  formData.append('folder', 'avatar')
  try {
    const res = await axios.post('/api/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.code === 200) {
      form.value.avatar = res.data.data.url
    } else {
      alert('头像上传失败: ' + res.data.message)
    }
  } catch (err) {
    console.error('upload avatar error:', err)
    alert('头像上传失败')
  }
}

const saveProfile = async () => {
  try {
    const body = {
      memberAccount: form.value.memberAccount,
      memberName: form.value.memberName,
      memberGender: form.value.memberGender,
      memberAge: form.value.memberAge,
      memberHeight: form.value.memberHeight,
      memberWeight: form.value.memberWeight,
      memberPhone: form.value.memberPhone,
      avatar: form.value.avatar
    }
    const res = await axios.put('/api/member/profile', body)
    if (res.data.code === 200) {
      alert('资料修改成功')
      sessionStorage.setItem('name', form.value.memberName)
      goBack()
    } else {
      alert('修改失败: ' + res.data.message)
    }
  } catch (e) {
    console.error('save profile error:', e)
    alert('修改失败: ' + (e.response?.data?.message || e.message))
  }
}

onMounted(() => {
  fetchMember()
})
</script>

<style scoped>
.profile-container {
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

.user-info { display: flex; gap: 12px; }

.back-btn, .logout-btn {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid white;
  color: white;
  border-radius: 6px;
  cursor: pointer;
}

.back-btn:hover, .logout-btn:hover { background: rgba(255, 255, 255, 0.3); }

.main-content { padding: 40px; }

.profile-card {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  max-width: 720px;
  margin: 0 auto;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 30px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eee;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #0ea5e9;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  font-size: 48px;
  font-weight: 600;
}

.avatar-actions { display: flex; flex-direction: column; gap: 8px; }

.upload-btn {
  display: inline-block;
  padding: 10px 24px;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  text-align: center;
}

.upload-btn:hover { opacity: 0.9; }

.avatar-hint { color: #888; font-size: 13px; }

.profile-form .form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-group { margin-bottom: 18px; }

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 14px;
}

.form-group input, .form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:disabled, .form-group select:disabled { background: #f5f5f5; }

.card-info {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin: 24px 0;
}

.card-info h3 { margin: 0 0 14px 0; color: #333; font-size: 15px; }

.card-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.card-info-grid div { color: #333; }

.card-info-grid span { color: #666; margin-right: 8px; }

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.cancel-btn {
  padding: 10px 24px;
  background: #e5e7eb;
  color: #333;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.submit-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #0ea5e9 0%, #f97316 100%);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
</style>
