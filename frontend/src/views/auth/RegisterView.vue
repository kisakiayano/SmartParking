<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { apiErrorMessage } from '@/api/http'
import type { RegisterRequest } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const form = reactive<RegisterRequest>({
  username: '',
  password: '',
  name: '',
  phone: '',
})

async function submit() {
  if (!form.username || !form.password || !form.name) {
    ElMessage.warning('请填写用户名、密码和姓名')
    return
  }
  loading.value = true
  try {
    await auth.register(form)
    ElMessage.success('注册成功')
    router.push({ path: '/login', query: { username: form.username } })
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel compact">
      <div class="auth-copy">
        <p class="eyebrow">User Register</p>
        <h1>创建用户账号</h1>
      </div>
      <el-form class="auth-form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model.trim="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model.trim="form.name" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model.trim="form.phone" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-button class="wide-button" type="primary" :loading="loading" @click="submit">注册</el-button>
        <div class="auth-actions">
          <RouterLink to="/login">返回登录</RouterLink>
        </div>
      </el-form>
    </section>
  </main>
</template>
