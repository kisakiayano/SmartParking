import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: () => {
        const auth = useAuthStore()
        return auth.isAuthenticated ? auth.homePath : '/login'
      },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/admin',
      component: () => import('@/views/layouts/AppLayout.vue'),
      meta: { requiresAuth: true, role: 'ADMIN' },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        { path: 'dashboard', component: () => import('@/views/admin/AdminDashboardView.vue'), meta: { title: '数据统计' } },
        { path: 'users', component: () => import('@/views/admin/AdminUsersView.vue'), meta: { title: '用户管理' } },
        { path: 'vehicles', component: () => import('@/views/admin/AdminVehiclesView.vue'), meta: { title: '车辆管理' } },
        { path: 'spaces', component: () => import('@/views/admin/AdminParkingSpacesView.vue'), meta: { title: '车位管理' } },
        { path: 'reservations', component: () => import('@/views/admin/AdminReservationsView.vue'), meta: { title: '预约管理' } },
        { path: 'records', component: () => import('@/views/admin/AdminParkingRecordsView.vue'), meta: { title: '停车记录管理' } },
        { path: 'fees', component: () => import('@/views/admin/AdminFeeRulesView.vue'), meta: { title: '计费规则管理' } },
        { path: 'suggestions', component: () => import('@/views/admin/AdminSuggestionsView.vue'), meta: { title: '用户建议管理' } },
        { path: 'ai-chats', component: () => import('@/views/admin/AdminAiChatsView.vue'), meta: { title: 'AI 对话记录' } },
        { path: 'ai-prompts', component: () => import('@/views/admin/AdminAiPromptsView.vue'), meta: { title: 'AI 提示词管理' } },
      ],
    },
    {
      path: '/user',
      component: () => import('@/views/layouts/AppLayout.vue'),
      meta: { requiresAuth: true, role: 'USER' },
      children: [
        { path: '', redirect: '/user/spaces' },
        { path: 'vehicles', component: () => import('@/views/user/UserVehiclesView.vue'), meta: { title: '我的车辆' } },
        { path: 'spaces', component: () => import('@/views/user/UserParkingSpacesView.vue'), meta: { title: '查询车位' } },
        { path: 'reserve', component: () => import('@/views/user/UserReservationCreateView.vue'), meta: { title: '在线预约' } },
        { path: 'reservations', component: () => import('@/views/user/UserReservationsView.vue'), meta: { title: '我的预约' } },
        { path: 'records', component: () => import('@/views/user/UserParkingRecordsView.vue'), meta: { title: '停车记录' } },
        { path: 'assistant', component: () => import('@/views/user/UserAiAssistantView.vue'), meta: { title: 'AI 智能助手' } },
        { path: 'suggestions', component: () => import('@/views/user/UserSuggestionsView.vue'), meta: { title: '意见反馈' } },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const requiresAuth = Boolean(to.meta.requiresAuth)
  const guestOnly = Boolean(to.meta.guestOnly)
  const requiredRole = typeof to.meta.role === 'string' ? to.meta.role : ''

  if (requiresAuth && !auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (auth.token && !auth.user) {
    try {
      await auth.loadProfile()
    } catch {
      auth.logout()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (guestOnly && auth.isAuthenticated && auth.user) {
    return auth.homePath
  }

  if (requiredRole && auth.user?.role !== requiredRole) {
    return auth.homePath
  }

  return true
})

export default router
