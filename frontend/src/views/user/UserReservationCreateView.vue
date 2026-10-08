<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { apiErrorMessage } from '@/api/http'
import { parkingSpaceApi, reservationApi, vehicleApi } from '@/api/modules'
import type { ParkingSpace, ReservationPayload, Vehicle } from '@/api/types'
import { formatDateTime, statusLabel, tagType } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const vehicles = ref<Vehicle[]>([])
const spaces = ref<ParkingSpace[]>([])
const form = reactive<ReservationPayload>({
  vehicleId: '',
  spaceId: '',
  arriveTime: '',
})

const freeSpaces = computed(() => spaces.value.filter((item) => item.status === 'FREE'))

async function load() {
  loading.value = true
  try {
    const [vehicleList, spacePage] = await Promise.all([
      vehicleApi.listMine(),
      parkingSpaceApi.page({ pageNum: 1, pageSize: 100, status: 'FREE' }),
    ])
    vehicles.value = vehicleList
    spaces.value = spacePage.records
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.vehicleId || !form.spaceId || !form.arriveTime) {
    ElMessage.warning('请选择车辆、车位和预计到达时间')
    return
  }
  saving.value = true
  try {
    await reservationApi.create(form)
    ElMessage.success('预约成功')
    router.push('/user/reservations')
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page">
    <div class="page-head">
      <div>
        <h1>在线预约</h1>
        <p>选择车辆和空闲车位，提交预计到达时间。</p>
      </div>
      <el-button @click="load">刷新车位</el-button>
    </div>

    <div class="two-column" v-loading="loading">
      <section class="form-panel panel-body">
        <h2>预约信息</h2>
        <el-form class="dialog-form" label-position="top">
          <el-form-item label="车辆">
            <el-select v-model="form.vehicleId" class="wide-button" filterable>
              <el-option
                v-for="item in vehicles"
                :key="item.vehicleId"
                :label="`${item.plateNumber} / ${item.vehicleType}`"
                :value="item.vehicleId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="车位">
            <el-select v-model="form.spaceId" class="wide-button" filterable>
              <el-option
                v-for="item in freeSpaces"
                :key="item.spaceId"
                :label="`${item.area}-${item.spaceId} / ${item.type}`"
                :value="item.spaceId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="预计到达时间">
            <el-date-picker
              v-model="form.arriveTime"
              class="wide-button"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
            />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">提交预约</el-button>
        </el-form>
      </section>

      <section class="table-panel">
        <el-table :data="freeSpaces" stripe max-height="420">
          <el-table-column prop="spaceId" label="车位 ID" width="110" />
          <el-table-column prop="area" label="区域" min-width="100" />
          <el-table-column prop="type" label="类型" min-width="120" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="tagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </section>
</template>
