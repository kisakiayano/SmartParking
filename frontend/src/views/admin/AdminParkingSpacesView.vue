<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { parkingSpaceApi } from '@/api/modules'
import type { PageResponse, ParkingSpace, ParkingSpacePayload, ParkingSpaceQuery } from '@/api/types'
import { formatDateTime, statusLabel, tagType } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const table = ref<PageResponse<ParkingSpace>>({ records: [], total: 0, pageNum: 1, pageSize: 10 })
const query = reactive<ParkingSpaceQuery>({
  pageNum: 1,
  pageSize: 10,
  area: '',
  type: '',
  status: '',
})
const form = reactive<ParkingSpacePayload>({
  area: '',
  type: 'STANDARD',
  status: 'FREE',
  version: undefined,
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '空闲', value: 'FREE' },
  { label: '已预约', value: 'RESERVED' },
  { label: '占用', value: 'OCCUPIED' },
]

function resetForm(row?: ParkingSpace) {
  editingId.value = row?.spaceId ?? null
  form.area = row?.area ?? ''
  form.type = row?.type ?? 'STANDARD'
  form.status = row?.status ?? 'FREE'
  form.version = row?.version
  dialogVisible.value = true
}

async function load() {
  loading.value = true
  try {
    table.value = await parkingSpaceApi.page(query)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.area || !form.type || !form.status) {
    ElMessage.warning('请填写完整车位信息')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await parkingSpaceApi.updateAdmin(editingId.value, {
        area: form.area,
        type: form.type,
        status: form.status,
        version: form.version ?? 0,
      })
    } else {
      await parkingSpaceApi.createAdmin({ area: form.area, type: form.type, status: form.status })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    saving.value = false
  }
}

async function remove(row: ParkingSpace) {
  try {
    await ElMessageBox.confirm(`确认删除车位 ${row.area}-${row.spaceId}？`, '删除确认', { type: 'warning' })
    await parkingSpaceApi.deleteAdmin(row.spaceId)
    ElMessage.success('删除成功')
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
        <h1>车位管理</h1>
        <p>维护车位区域、类型和实时状态。</p>
      </div>
      <div class="toolbar">
        <el-input v-model.trim="query.area" class="filter-input" clearable placeholder="区域" />
        <el-input v-model.trim="query.type" class="filter-input" clearable placeholder="类型" />
        <el-select v-model="query.status" class="filter-select">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button @click="search">查询</el-button>
        <el-button type="primary" @click="resetForm()">新增车位</el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table v-loading="loading" :data="table.records" stripe>
        <el-table-column prop="spaceId" label="ID" width="90" />
        <el-table-column prop="area" label="区域" min-width="120" />
        <el-table-column prop="type" label="类型" min-width="130" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="resetForm(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑车位' : '新增车位'" width="460px">
      <el-form class="dialog-form" label-position="top">
        <el-form-item label="区域">
          <el-input v-model.trim="form.area" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model.trim="form.type" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="wide-button">
            <el-option label="空闲" value="FREE" />
            <el-option label="已预约" value="RESERVED" />
            <el-option label="占用" value="OCCUPIED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
