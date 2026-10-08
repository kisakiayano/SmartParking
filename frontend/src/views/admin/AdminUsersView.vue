<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { userApi } from '@/api/modules'
import type { PageResponse, User, UserQuery } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime, statusLabel, tagType } from '@/utils/format'

const auth = useAuthStore()
const loading = ref(false)
const tableLoading = ref(false)
const table = ref<PageResponse<User>>({ records: [], total: 0, pageNum: 1, pageSize: 10 })
const query = reactive<UserQuery>({
  pageNum: 1,
  pageSize: 10,
  username: '',
  name: '',
  phone: '',
  role: '',
  status: '',
})
const form = reactive({
  name: auth.user?.name ?? '',
  phone: auth.user?.phone ?? '',
})

async function load() {
  tableLoading.value = true
  try {
    table.value = await userApi.pageAdmin(query)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    tableLoading.value = false
  }
}

async function save() {
  loading.value = true
  try {
    const user = await userApi.updateProfile(form)
    auth.user = user
    window.localStorage.setItem('currentUser', JSON.stringify(user))
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function updateStatus(row: User) {
  const nextStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const action = nextStatus === 'ENABLED' ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认${action}用户 ${row.username}？`, '状态确认', { type: 'warning' })
    await userApi.updateStatus(row.userId, nextStatus)
    ElMessage.success(`${action}成功`)
    load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(apiErrorMessage(error))
    }
  }
}

function search() {
  query.pageNum = 1
  load()
}

onMounted(load)
</script>

<template>
  <section class="page">
    <div class="page-head">
      <div>
        <h1>用户管理</h1>
        <p>查询用户账号，并维护启用状态。</p>
      </div>
      <div class="toolbar">
        <el-input v-model.trim="query.username" class="filter-input" clearable placeholder="用户名" />
        <el-input v-model.trim="query.name" class="filter-input" clearable placeholder="姓名" />
        <el-input v-model.trim="query.phone" class="filter-input" clearable placeholder="手机号" />
        <el-select v-model="query.role" class="filter-select">
          <el-option label="全部角色" value="" />
          <el-option label="管理员" value="ADMIN" />
          <el-option label="用户" value="USER" />
        </el-select>
        <el-select v-model="query.status" class="filter-select">
          <el-option label="全部状态" value="" />
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="two-column">
      <section class="form-panel panel-body" v-loading="loading">
        <h2>当前管理员账号</h2>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户 ID">{{ auth.user?.userId }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ auth.user?.username }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ auth.user?.role }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="tagType(auth.user?.status)" effect="plain">{{ statusLabel(auth.user?.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(auth.user?.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="form-panel panel-body" v-loading="loading">
        <h2>资料维护</h2>
        <el-form class="dialog-form" label-position="top">
          <el-form-item label="姓名">
            <el-input v-model.trim="form.name" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model.trim="form.phone" />
          </el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
        </el-form>
      </section>
    </div>

    <div class="table-panel">
      <el-table v-loading="tableLoading" :data="table.records" stripe>
        <el-table-column prop="userId" label="ID" min-width="190" />
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="role" label="角色" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              :type="row.status === 'ENABLED' ? 'danger' : 'success'"
              :disabled="row.userId === auth.user?.userId && row.status === 'ENABLED'"
              @click="updateStatus(row)"
            >
              {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          layout="total, sizes, prev, pager, next"
          :total="table.total"
          @change="load"
        />
      </div>
    </div>
  </section>
</template>
