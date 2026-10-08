<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { vehicleApi } from '@/api/modules'
import type { PageResponse, Vehicle, VehicleQuery } from '@/api/types'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const table = ref<PageResponse<Vehicle>>({ records: [], total: 0, pageNum: 1, pageSize: 10 })
const query = reactive<VehicleQuery>({
  pageNum: 1,
  pageSize: 10,
  plateNumber: '',
  vehicleType: '',
  sortBy: 'createTime',
  order: 'desc',
})

async function load() {
  loading.value = true
  try {
    table.value = await vehicleApi.pageAdmin(query)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
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
        <h1>车辆管理</h1>
        <p>按车牌、类型和用户维度查看车辆。</p>
      </div>
      <div class="toolbar">
        <el-input v-model.trim="query.plateNumber" class="filter-input" clearable placeholder="车牌号" />
        <el-input v-model.trim="query.vehicleType" class="filter-input" clearable placeholder="车辆类型" />
        <el-input v-model.trim="query.userId" class="filter-input" clearable placeholder="用户 ID" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table v-loading="loading" :data="table.records" stripe>
        <el-table-column prop="vehicleId" label="ID" width="90" />
        <el-table-column prop="plateNumber" label="车牌号" min-width="140" />
        <el-table-column prop="vehicleType" label="类型" min-width="120" />
        <el-table-column prop="userId" label="用户 ID" width="110" />
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
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
