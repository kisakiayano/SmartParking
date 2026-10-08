<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { aiApi } from '@/api/modules'
import type { AiPromptTemplate, AiPromptTemplatePayload } from '@/api/types'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const editingIntent = ref<string | null>(null)
const templates = ref<AiPromptTemplate[]>([])
const form = reactive<AiPromptTemplatePayload>({
  templateName: '',
  systemPrompt: '',
  enabled: false,
})

function resetForm(row: AiPromptTemplate) {
  editingId.value = row.templateId
  editingIntent.value = row.intent
  form.templateName = row.templateName
  form.systemPrompt = row.systemPrompt
  form.enabled = row.enabled
  dialogVisible.value = true
}

async function load() {
  loading.value = true
  try {
    templates.value = await aiApi.prompts()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!editingId.value) {
    return
  }
  const templateName = form.templateName.trim()
  const systemPrompt = form.systemPrompt.trim()
  if (!templateName) {
    ElMessage.warning('请填写模板名称')
    return
  }
  if (!systemPrompt) {
    ElMessage.warning('请填写系统提示词')
    return
  }
  saving.value = true
  try {
    await aiApi.updatePrompt(editingId.value, { templateName, systemPrompt, enabled: form.enabled })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
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
        <h1>AI 提示词管理</h1>
        <p>维护 AI 助手各场景的预设提示词，系统会在运行时自动追加实时数据与业务知识。</p>
      </div>
      <div class="toolbar">
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table v-loading="loading" :data="templates" stripe>
        <el-table-column prop="templateId" label="模板 ID" width="180" />
        <el-table-column prop="templateCode" label="模板编码" min-width="170" show-overflow-tooltip />
        <el-table-column prop="templateName" label="模板名称" min-width="170" show-overflow-tooltip />
        <el-table-column label="适用意图" min-width="130">
          <template #default="{ row }">{{ row.intent || '通用' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="resetForm(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="编辑提示词模板" width="640px">
      <el-form class="dialog-form" label-position="top">
        <el-form-item label="模板名称">
          <el-input v-model.trim="form.templateName" maxlength="128" />
        </el-form-item>
        <el-form-item label="适用场景">
          <el-input :model-value="editingIntent || '通用'" disabled />
        </el-form-item>
        <el-form-item label="系统提示词">
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :rows="12"
            maxlength="8000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="系统会在运行时自动追加实时数据与业务知识，无需在此填写。"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
