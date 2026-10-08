<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { parkingSpaceApi } from '@/api/modules'
import type { PageResponse, ParkingSpace, ParkingSpaceQuery } from '@/api/types'
import { formatDateTime, statusLabel, tagType } from '@/utils/format'

const loading = ref(false)
const table = ref<PageResponse<ParkingSpace>>({ records: [], total: 0, pageNum: 1, pageSize: 10 })
const query = reactive<ParkingSpaceQuery>({
  pageNum: 1,
  pageSize: 10,
  area: '',
  type: '',
  status: '',
})

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
        <h1>查询车位</h1>
        <p>查看可用车位和当前状态。</p>
      </div>
      <div class="toolbar">
        <el-input v-model.trim="query.area" class="filter-input" clearable placeholder="区域" />
        <el-input v-model.trim="query.type" class="filter-input" clearable placeholder="类型" />
        <el-select v-model="query.status" class="filter-select">
          <el-option label="全部状态" value="" />
          <el-option label="空闲" value="FREE" />
          <el-option label="已预约" value="RESERVED" />
          <el-option label="占用" value="OCCUPIED" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table v-loading="loading" :data="table.records" stripe>
        <el-table-column prop="spaceId" label="车位 ID" width="110" />
        <el-table-column prop="area" label="区域" min-width="120" />
        <el-table-column prop="type" label="类型" min-width="130" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
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
