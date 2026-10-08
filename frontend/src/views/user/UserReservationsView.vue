<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { reservationApi } from '@/api/modules'
import type { PageResponse, Reservation, ReservationQuery } from '@/api/types'
import { formatDateTime, statusLabel, tagType } from '@/utils/format'

const loading = ref(false)
const table = ref<PageResponse<Reservation>>({ records: [], total: 0, pageNum: 1, pageSize: 10 })
const query = reactive<ReservationQuery>({
  pageNum: 1,
  pageSize: 10,
  status: '',
  vehicleId: '',
  spaceId: '',
})

async function load() {
  loading.value = true
  try {
    table.value = await reservationApi.pageMine(query)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function cancel(row: Reservation) {
  try {
    await ElMessageBox.confirm(`确认取消预约 ${row.reservationId}？`, '取消确认', { type: 'warning' })
    await reservationApi.cancel(row.reservationId)
    ElMessage.success('已取消')
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
        <h1>我的预约</h1>
        <p>查看预约记录并取消有效预约。</p>
      </div>
      <div class="toolbar">
        <el-select v-model="query.status" class="filter-select">
          <el-option label="全部状态" value="" />
          <el-option label="有效" value="ACTIVE" />
          <el-option label="已使用" value="USED" />
          <el-option label="已过期" value="EXPIRED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-input v-model.trim="query.vehicleId" class="filter-input" clearable placeholder="车辆 ID" />
        <el-input v-model.trim="query.spaceId" class="filter-input" clearable placeholder="车位 ID" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table v-loading="loading" :data="table.records" stripe>
        <el-table-column prop="reservationId" label="ID" width="90" />
        <el-table-column prop="vehicleId" label="车辆 ID" width="110" />
        <el-table-column prop="spaceId" label="车位 ID" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预约时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.reserveTime) }}</template>
        </el-table-column>
        <el-table-column label="预计到达" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.arriveTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="danger" :disabled="row.status !== 'ACTIVE'" @click="cancel(row)">取消</el-button>
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
