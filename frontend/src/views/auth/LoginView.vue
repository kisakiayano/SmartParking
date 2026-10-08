<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { apiErrorMessage } from '@/api/http'
import type { LoginRequest } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const form = reactive<LoginRequest>({
  username: '',
  password: '',
})

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : auth.homePath
    ElMessage.success('登录成功')
    router.push(redirect)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel">
      <div class="auth-copy">
        <p class="eyebrow">Smart Parking</p>
        <h1>智慧停车管理系统</h1>
        <p>统一入口会根据账号角色进入对应工作台。</p>
      </div>
      <el-form class="auth-form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model.trim="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button class="wide-button" type="primary" :loading="loading" @click="submit">登录</el-button>
        <div class="auth-actions">
          <RouterLink to="/register">注册用户账号</RouterLink>
        </div>
      </el-form>
    </section>
  </main>
</template>
