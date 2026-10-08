<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { feeRuleApi } from '@/api/modules'
import type { FeeRule, FeeRulePayload, FeeRuleQuery, PageResponse } from '@/api/types'
import { formatDateTime, formatMoney } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const table = ref<PageResponse<FeeRule>>({ records: [], total: 0, pageNum: 1, pageSize: 10 })
const query = reactive<FeeRuleQuery>({
  pageNum: 1,
  pageSize: 10,
  ruleName: '',
  enabled: undefined,
})
const form = reactive<FeeRulePayload>({
  ruleName: '',
  unitPrice: 5,
  freeMinute: 15,
  maxDaily: 60,
  enabled: false,
})

function resetForm(row?: FeeRule) {
  editingId.value = row?.ruleId ?? null
  form.ruleName = row?.ruleName ?? ''
  form.unitPrice = row?.unitPrice ?? 5
  form.freeMinute = row?.freeMinute ?? 15
  form.maxDaily = row?.maxDaily ?? 60
  form.enabled = row?.enabled ?? false
  dialogVisible.value = true
}

async function load() {
  loading.value = true
  try {
    table.value = await feeRuleApi.pageAdmin(query)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.ruleName) {
    ElMessage.warning('请填写规则名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await feeRuleApi.updateAdmin(editingId.value, form)
    } else {
      await feeRuleApi.createAdmin(form)
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

async function enable(row: FeeRule) {
  saving.value = true
  try {
    await feeRuleApi.enable(row.ruleId)
    ElMessage.success('已启用')
    load()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    saving.value = false
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
        <h1>计费规则管理</h1>
        <p>维护单价、免费时长、日封顶和启用规则。</p>
      </div>
      <div class="toolbar">
        <el-input v-model.trim="query.ruleName" class="filter-input" clearable placeholder="规则名称" />
        <el-select v-model="query.enabled" class="filter-select" clearable placeholder="状态">
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
        <el-button @click="search">查询</el-button>
        <el-button type="primary" @click="resetForm()">新增规则</el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table v-loading="loading" :data="table.records" stripe>
        <el-table-column prop="ruleId" label="ID" width="90" />
        <el-table-column prop="ruleName" label="规则名称" min-width="170" />
        <el-table-column label="每小时单价" min-width="130">
          <template #default="{ row }">¥{{ formatMoney(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column prop="freeMinute" label="免费分钟" width="110" />
        <el-table-column label="日封顶" min-width="110">
          <template #default="{ row }">¥{{ formatMoney(row.maxDaily) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="resetForm(row)">编辑</el-button>
            <el-button text type="success" :disabled="row.enabled" @click="enable(row)">启用</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑计费规则' : '新增计费规则'" width="500px">
      <el-form class="dialog-form" label-position="top">
        <el-form-item label="规则名称">
          <el-input v-model.trim="form.ruleName" />
        </el-form-item>
        <el-form-item label="每小时单价">
          <el-input-number v-model="form.unitPrice" :min="0" :precision="2" class="wide-button" />
        </el-form-item>
        <el-form-item label="免费分钟">
          <el-input-number v-model="form.freeMinute" :min="0" class="wide-button" />
        </el-form-item>
        <el-form-item label="日封顶">
          <el-input-number v-model="form.maxDaily" :min="0" :precision="2" class="wide-button" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
