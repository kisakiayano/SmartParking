<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { suggestionApi } from '@/api/modules'
import type { PageResponse, Suggestion, SuggestionPayload, SuggestionQuery } from '@/api/types'
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

const loading = ref(false)
const submitting = ref(false)
const detailVisible = ref(false)
const detail = ref<Suggestion | null>(null)
const table = ref<PageResponse<Suggestion>>({ records: [], total: 0, pageNum: 1, pageSize: 10 })
const query = reactive<SuggestionQuery>({ pageNum: 1, pageSize: 10, status: '' })
const form = reactive<SuggestionPayload>({ category: 'FUNCTION', title: '', content: '', contact: '' })

async function load() {
  loading.value = true
  try {
    table.value = await suggestionApi.pageMine(query)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写建议标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请填写建议内容')
    return
  }
  submitting.value = true
  try {
    await suggestionApi.create({
      category: form.category,
      title: form.title.trim(),
      content: form.content.trim(),
      contact: form.contact?.trim() || undefined,
    })
    ElMessage.success('建议已提交，管理员处理后会在这里回复')
    form.title = ''
    form.content = ''
    form.contact = ''
    query.pageNum = 1
    load()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    submitting.value = false
  }
}

function openDetail(row: Suggestion) {
  detail.value = row
  detailVisible.value = true
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
        <h1>意见反馈</h1>
        <p>提交使用建议或问题反馈，管理员处理后会给出回复。</p>
      </div>
    </div>

    <div class="form-panel">
      <div class="panel-body">
        <el-form class="two-column" label-position="top">
          <el-form-item label="建议分类">
            <el-select v-model="form.category" class="wide-button">
              <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="标题">
            <el-input v-model.trim="form.title" maxlength="128" show-word-limit placeholder="例如：希望支持微信支付" />
          </el-form-item>
          <el-form-item label="建议内容">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="请描述您的建议或遇到的问题"
            />
          </el-form-item>
          <el-form-item label="联系方式（可选）">
            <el-input v-model.trim="form.contact" maxlength="64" placeholder="手机号或邮箱，便于进一步沟通" />
          </el-form-item>
        </el-form>
        <div class="action-row">
          <el-button type="primary" :loading="submitting" @click="submit">提交建议</el-button>
        </div>
      </div>
    </div>

    <div class="table-panel">
      <div class="panel-body table-head">
        <h3 class="panel-title">我的建议</h3>
        <div class="toolbar">
          <el-select v-model="query.status" class="filter-select" clearable placeholder="全部状态">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <el-button @click="search">查询</el-button>
        </div>
      </div>
      <el-table v-loading="loading" :data="table.records" stripe>
        <el-table-column prop="suggestionId" label="ID" width="100" />
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
        <el-table-column label="处理时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.handleTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">查看</el-button>
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

    <el-dialog v-model="detailVisible" title="建议详情" width="560px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="分类">{{ suggestionCategoryLabel(detail.category) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="suggestionStatusTag(detail.status)" effect="plain">{{ suggestionStatusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">
          <span class="detail-content">{{ detail.content }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ detail.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="管理员回复" :span="2">{{ detail.reply || '暂无回复' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间" :span="2">{{ formatDateTime(detail.handleTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.table-head {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0;
}

.panel-title {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
}

.detail-content {
  white-space: pre-wrap;
}
</style>
