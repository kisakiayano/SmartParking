<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const menuItems = computed(() => {
  if (auth.user?.role === 'ADMIN') {
    return [
      { path: '/admin/dashboard', label: '数据统计' },
      { path: '/admin/users', label: '用户管理' },
      { path: '/admin/vehicles', label: '车辆管理' },
      { path: '/admin/spaces', label: '车位管理' },
      { path: '/admin/reservations', label: '预约管理' },
      { path: '/admin/records', label: '停车记录' },
      { path: '/admin/fees', label: '计费规则' },
      { path: '/admin/suggestions', label: '用户建议' },
      { path: '/admin/ai-chats', label: 'AI 对话记录' },
      { path: '/admin/ai-prompts', label: 'AI 提示词' },
    ]
  }
  return [
    { path: '/user/vehicles', label: '我的车辆' },
    { path: '/user/spaces', label: '查询车位' },
    { path: '/user/reserve', label: '在线预约' },
    { path: '/user/reservations', label: '我的预约' },
    { path: '/user/records', label: '停车记录' },
    { path: '/user/assistant', label: 'AI 智能助手' },
    { path: '/user/suggestions', label: '意见反馈' },
  ]
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout-shell">
    <el-aside width="236px" class="layout-aside">
      <div class="brand">
        <div class="brand-mark">P</div>
        <div>
          <strong>智慧停车</strong>
          <span>{{ auth.user?.role === 'ADMIN' ? '管理端' : '用户端' }}</span>
        </div>
      </div>
      <el-menu class="side-menu" :default-active="route.path" router>
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-title">{{ route.meta.title }}</div>
        <div class="header-user">
          <span>{{ auth.user?.name || auth.user?.username }}</span>
          <el-tag size="small" effect="plain">{{ auth.user?.role }}</el-tag>
          <el-button text @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>
