<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { dashboardApi } from '@/api/modules'
import type { DashboardStatistics, TrendPoint } from '@/api/types'
import { formatMoney } from '@/utils/format'

const loading = ref(false)
const statistics = ref<DashboardStatistics | null>(null)

const stats = computed(() => [
  { label: '今日收入', value: `¥${formatMoney(statistics.value?.todayIncome)}` },
  { label: '今日停车次数', value: statistics.value?.todayParkingCount ?? 0 },
  { label: '车位总数', value: statistics.value?.totalSpaces ?? 0 },
  { label: '当前占用率', value: `${formatMoney(statistics.value?.currentOccupancyRate)}%` },
  { label: '空闲车位', value: statistics.value?.freeSpaces ?? 0 },
  { label: '预约车位', value: statistics.value?.reservedSpaces ?? 0 },
  { label: '占用车位', value: statistics.value?.occupiedSpaces ?? 0 },
])

function maxValue(points: TrendPoint[]) {
  return Math.max(1, ...points.map((item) => Number(item.value) || 0))
}

function barWidth(item: TrendPoint, points: TrendPoint[]) {
  const value = Number(item.value) || 0
  return `${Math.max(2, Math.round((value / maxValue(points)) * 100))}%`
}

async function load() {
  loading.value = true
  try {
    statistics.value = await dashboardApi.statistics()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1>数据统计</h1>
        <p>停车场收入、车流和车位使用情况。</p>
      </div>
      <el-button type="primary" @click="load">刷新</el-button>
    </div>

    <div class="stats-grid">
      <div v-for="item in stats" :key="item.label" class="stat-card">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value }}</div>
      </div>
    </div>

    <div class="trend-grid">
      <section class="stats-panel panel-body">
        <h2>近 7 天收入</h2>
        <div class="trend-list">
          <div v-for="item in statistics?.revenueTrend7Days || []" :key="`r7-${item.date}`" class="trend-row">
            <span>{{ item.date }}</span>
            <span class="trend-track"><span class="trend-bar" :style="{ width: barWidth(item, statistics?.revenueTrend7Days || []) }" /></span>
            <strong>{{ formatMoney(item.value) }}</strong>
          </div>
        </div>
      </section>
      <section class="stats-panel panel-body">
        <h2>近 7 天车流</h2>
        <div class="trend-list">
          <div v-for="item in statistics?.trafficTrend7Days || []" :key="`t7-${item.date}`" class="trend-row">
            <span>{{ item.date }}</span>
            <span class="trend-track"><span class="trend-bar" :style="{ width: barWidth(item, statistics?.trafficTrend7Days || []) }" /></span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>
