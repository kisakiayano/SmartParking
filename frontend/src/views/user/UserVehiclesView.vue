<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { vehicleApi } from '@/api/modules'
import type { Vehicle, VehiclePayload } from '@/api/types'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const vehicles = ref<Vehicle[]>([])
const form = reactive<VehiclePayload>({
  plateNumber: '',
  vehicleType: 'SMALL',
})

async function load() {
  loading.value = true
  try {
    vehicles.value = await vehicleApi.listMine()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

function resetForm(row?: Vehicle) {
  editingId.value = row?.vehicleId ?? null
  form.plateNumber = row?.plateNumber ?? ''
  form.vehicleType = row?.vehicleType ?? 'SMALL'
  dialogVisible.value = true
}

async function save() {
  if (!form.plateNumber || !form.vehicleType) {
    ElMessage.warning('请填写车牌号和车辆类型')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await vehicleApi.update(editingId.value, form)
    } else {
      await vehicleApi.create(form)
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

async function remove(row: Vehicle) {
  try {
    await ElMessageBox.confirm(`确认删除车辆 ${row.plateNumber}？`, '删除确认', { type: 'warning' })
    await vehicleApi.delete(row.vehicleId)
    ElMessage.success('删除成功')
    load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(apiErrorMessage(error))
    }
  }
}

onMounted(load)
</script>

<template>
  <section class="page">
    <div class="page-head">
      <div>
        <h1>我的车辆</h1>
        <p>维护个人车辆，用于在线预约和停车记录关联。</p>
      </div>
      <el-button type="primary" @click="resetForm()">新增车辆</el-button>
    </div>

    <div class="table-panel">
      <el-table v-loading="loading" :data="vehicles" stripe>
        <el-table-column prop="vehicleId" label="ID" width="90" />
        <el-table-column prop="plateNumber" label="车牌号" min-width="140" />
        <el-table-column prop="vehicleType" label="类型" min-width="120" />
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="resetForm(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑车辆' : '新增车辆'" width="460px">
      <el-form class="dialog-form" label-position="top">
        <el-form-item label="车牌号">
          <el-input v-model.trim="form.plateNumber" />
        </el-form-item>
        <el-form-item label="车辆类型">
          <el-select v-model="form.vehicleType" class="wide-button" filterable allow-create>
            <el-option label="小型车" value="SMALL" />
            <el-option label="大型车" value="LARGE" />
            <el-option label="新能源车" value="NEW_ENERGY" />
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
