<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { parkingRecordApi } from '@/api/modules'
import type { PageResponse, ParkingRecord, ParkingRecordQuery } from '@/api/types'
import { formatDateTime, formatDuration, formatMoney, statusLabel, tagType } from '@/utils/format'

const loading = ref(false)
const actionLoading = ref(false)
const table = ref<PageResponse<ParkingRecord>>({ records: [], total: 0, pageNum: 1, pageSize: 10 })
const query = reactive<ParkingRecordQuery>({
  pageNum: 1,
  pageSize: 10,
  status: '',
  vehicleId: '',
  spaceId: '',
})
const entryForm = reactive({ plateNumber: '', spaceId: '' })
const exitForm = reactive({ plateNumber: '' })

async function load() {
  loading.value = true
  try {
    table.value = await parkingRecordApi.pageAdmin(query)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function manualEntry() {
  if (!entryForm.plateNumber || !entryForm.spaceId) {
    ElMessage.warning('请填写入场车牌和车位 ID')
    return
  }
  actionLoading.value = true
  try {
    await parkingRecordApi.manualEntry({ plateNumber: entryForm.plateNumber, spaceId: entryForm.spaceId })
    ElMessage.success('入场登记成功')
    entryForm.plateNumber = ''
    entryForm.spaceId = ''
    load()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    actionLoading.value = false
  }
}

async function manualExit() {
  if (!exitForm.plateNumber) {
    ElMessage.warning('请填写出场车牌')
    return
  }
  actionLoading.value = true
  try {
    await parkingRecordApi.manualExit({ plateNumber: exitForm.plateNumber })
    ElMessage.success('出场登记成功')
    exitForm.plateNumber = ''
    load()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    actionLoading.value = false
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
        <h1>停车记录管理</h1>
        <p>查看停车明细，并支持人工入场和出场。</p>
      </div>
    </div>

    <div class="stats-panel panel-body">
      <div class="action-row">
        <el-input v-model.trim="entryForm.plateNumber" class="filter-input" clearable placeholder="入场车牌" />
        <el-input v-model.trim="entryForm.spaceId" class="filter-input" clearable placeholder="车位 ID" />
        <el-button type="primary" :loading="actionLoading" @click="manualEntry">登记入场</el-button>
        <el-input v-model.trim="exitForm.plateNumber" class="filter-input" clearable placeholder="出场车牌" />
        <el-button :loading="actionLoading" @click="manualExit">登记出场</el-button>
      </div>
    </div>

    <div class="page-head">
      <div class="toolbar">
        <el-select v-model="query.status" class="filter-select">
          <el-option label="全部状态" value="" />
          <el-option label="停车中" value="PARKING" />
          <el-option label="已完成" value="FINISHED" />
        </el-select>
        <el-input v-model.trim="query.vehicleId" class="filter-input" clearable placeholder="车辆 ID" />
        <el-input v-model.trim="query.spaceId" class="filter-input" clearable placeholder="车位 ID" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table v-loading="loading" :data="table.records" stripe>
        <el-table-column prop="recordId" label="ID" width="90" />
        <el-table-column prop="vehicleId" label="车辆 ID" width="110" />
        <el-table-column prop="spaceId" label="车位 ID" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入场时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.entryTime) }}</template>
        </el-table-column>
        <el-table-column label="出场时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.exitTime) }}</template>
        </el-table-column>
        <el-table-column label="时长" min-width="130">
          <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
        </el-table-column>
        <el-table-column label="费用" min-width="110">
          <template #default="{ row }">¥{{ formatMoney(row.fee) }}</template>
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
