<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { suggestionApi } from '@/api/modules'
import type { PageResponse, Suggestion, SuggestionHandlePayload, SuggestionQuery } from '@/api/types'
import {
  formatDateTime,
  suggestionCategoryLabel,
  suggestionStatusLabel,
  suggestionStatusTag,
} from '@/utils/format'

const categories = [
  { value: 'FUNCTION', label: '功能建议' },
  { value: 'EXPERIENCE', label: '体验优化' },
  { value: 'BUG', label: '问题反馈' },
  { value: 'CHARGE', label: '收费相关' },
  { value: 'OTHER', label: '其他' },
]

const handleStatuses = [
  { value: 'PROCESSING', label: '受理处理中' },
  { value: 'RESOLVED', label: '标记已解决' },
  { value: 'REJECTED', label: '驳回建议' },
]

const loading = ref(false)
const handling = ref(false)
const handleVisible = ref(false)
const current = ref<Suggestion | null>(null)
const table = ref<PageResponse<Suggestion>>({ records: [], total: 0, pageNum: 1, pageSize: 10 })
const query = reactive<SuggestionQuery>({
  pageNum: 1,
  pageSize: 10,
  status: '',
  category: '',
  keyword: '',
})
const handleForm = reactive<SuggestionHandlePayload>({ status: 'PROCESSING', reply: '' })

async function load() {
  loading.value = true
  try {
    table.value = await suggestionApi.pageAdmin(query)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

function openHandle(row: Suggestion) {
  current.value = row
  handleForm.status = row.status === 'PENDING' ? 'PROCESSING' : 'RESOLVED'
  handleForm.reply = ''
  handleVisible.value = true
}

async function submitHandle() {
  if (!current.value) {
    return
  }
  if ((handleForm.status === 'RESOLVED' || handleForm.status === 'REJECTED') && !handleForm.reply?.trim()) {
    ElMessage.warning('标记为已解决或已驳回时请填写回复内容')
    return
  }
  handling.value = true
  try {
    await suggestionApi.handle(current.value.suggestionId, {
      status: handleForm.status,
      reply: handleForm.reply?.trim() || undefined,
    })
    ElMessage.success('处理完成')
    handleVisible.value = false
    load()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    handling.value = false
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
        <h1>用户建议管理</h1>
        <p>查看用户提交的建议与反馈，登记处理进度并回复用户。</p>
      </div>
      <div class="toolbar">
        <el-select v-model="query.status" class="filter-select" clearable placeholder="全部状态">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-select v-model="query.category" class="filter-select" clearable placeholder="全部分类">
          <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model.trim="query.keyword" class="filter-input" clearable placeholder="标题或内容关键字" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table v-loading="loading" :data="table.records" stripe>
        <el-table-column prop="suggestionId" label="ID" width="100" />
        <el-table-column prop="userId" label="用户 ID" width="110" />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ suggestionCategoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="suggestionStatusTag(row.status)" effect="plain">{{ suggestionStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="处理人" width="110">
          <template #default="{ row }">{{ row.handlerId || '-' }}</template>
        </el-table-column>
        <el-table-column label="处理时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.handleTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING' || row.status === 'PROCESSING'" text type="primary" @click="openHandle(row)">
              处理
            </el-button>
            <el-button v-else text type="info" @click="openHandle(row)">查看</el-button>
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

    <el-dialog v-model="handleVisible" title="建议详情与处理" width="580px">
      <el-descriptions v-if="current" :column="2" border class="suggestion-detail">
        <el-descriptions-item label="提交用户">{{ current.userId }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ suggestionCategoryLabel(current.category) }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">
          <span class="detail-content">{{ current.content }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ current.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatDateTime(current.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="current.reply" label="已有回复" :span="2">{{ current.reply }}</el-descriptions-item>
      </el-descriptions>

      <el-form v-if="current && current.status !== 'RESOLVED' && current.status !== 'REJECTED'" class="dialog-form handle-form" label-position="top">
        <el-form-item label="处理结果">
          <el-select v-model="handleForm.status" class="wide-button">
            <el-option v-for="item in handleStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input
            v-model="handleForm.reply"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="标记为已解决或已驳回时必填，用户可在意见反馈页面查看"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleVisible = false">关闭</el-button>
        <el-button
          v-if="current && current.status !== 'RESOLVED' && current.status !== 'REJECTED'"
          type="primary"
          :loading="handling"
          @click="submitHandle"
        >
          提交处理
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.detail-content {
  white-space: pre-wrap;
}

.suggestion-detail {
  margin-bottom: 16px;
}

.handle-form {
  margin-top: 4px;
}
</style>
