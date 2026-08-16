import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue')
  },
  {
    path: '/employee',
    name: 'Employee',
    component: () => import('../views/Employee.vue')
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue')
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue')
  },
  {
    path: '/plan',
    name: 'FitnessPlan',
    component: () => import('../views/FitnessPlan.vue')
  },
  {
    path: '/notifications',
    name: 'NotificationCenter',
    component: () => import('../views/NotificationCenter.vue')
  },
  {
    path: '/notifications/:id',
    name: 'NotificationDetail',
    component: () => import('../views/NotificationDetail.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')
  const role = sessionStorage.getItem('role')

  if (to.path === '/') {
    next()
  } else if (to.path === '/home' && token && role === 'MEMBER') {
    next()
  } else if (to.path === '/profile' && token && role === 'MEMBER') {
    next()
  } else if (to.path === '/plan' && token && role === 'MEMBER') {
    next()
  } else if (to.path === '/employee' && token && role === 'EMPLOYEE') {
    next()
  } else if ((to.path === '/admin' || to.path === '/dashboard') && token && role === 'ADMIN') {
    next()
  } else if ((to.path === '/notifications' || to.path.startsWith('/notifications/')) && token) {
    next()
  } else {
    next('/')
  }
})

export default router
