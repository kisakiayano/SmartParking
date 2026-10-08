<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { aiApi } from '@/api/modules'
import type { AiChatMessage, AiChatSession, AiChatSessionQuery, PageResponse } from '@/api/types'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const loadingMessages = ref(false)
const drawerVisible = ref(false)
const currentSession = ref<AiChatSession | null>(null)
const messages = ref<AiChatMessage[]>([])
const table = ref<PageResponse<AiChatSession>>({ records: [], total: 0, pageNum: 1, pageSize: 10 })
const query = reactive<AiChatSessionQuery>({ pageNum: 1, pageSize: 10, userId: '' })

async function load() {
  loading.value = true
  try {
    table.value = await aiApi.pageAdminSessions(query)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}

async function openMessages(row: AiChatSession) {
  currentSession.value = row
  drawerVisible.value = true
  loadingMessages.value = true
  try {
    messages.value = await aiApi.adminMessages(row.sessionId)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loadingMessages.value = false
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
        <h1>AI 对话记录</h1>
        <p>查看用户与 AI 助手的问答记录，用于服务质量和问题跟进。</p>
      </div>
      <div class="toolbar">
        <el-input v-model.trim="query.userId" class="filter-input" clearable placeholder="用户 ID" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table v-loading="loading" :data="table.records" stripe>
        <el-table-column prop="sessionId" label="会话 ID" width="120" />
        <el-table-column prop="userId" label="用户 ID" width="110" />
        <el-table-column prop="title" label="会话标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="provider" label="回答引擎" width="120" />
        <el-table-column prop="messageCount" label="消息数" width="90" />
        <el-table-column label="最近消息时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastMessageTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openMessages(row)">查看对话</el-button>
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

    <el-drawer v-model="drawerVisible" :title="currentSession?.title || '会话记录'" size="520px">
      <div v-loading="loadingMessages" class="drawer-messages">
        <el-empty v-if="!messages.length" description="暂无消息" :image-size="80" />
        <div
          v-for="message in messages"
          :key="message.messageId"
          class="drawer-message"
          :class="message.role === 'USER' ? 'from-user' : 'from-assistant'"
        >
          <div class="message-role">{{ message.role === 'USER' ? '用户' : 'AI 助手' }}</div>
          <div class="message-content">{{ message.content }}</div>
          <div v-if="message.referencedData?.length" class="message-refs">
            <el-tag v-for="reference in message.referencedData" :key="reference" size="small" type="info" effect="plain">
              {{ reference }}
            </el-tag>
          </div>
          <div class="message-time">{{ formatDateTime(message.createTime) }}</div>
        </div>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.drawer-messages {
  display: grid;
  gap: 14px;
}

.drawer-message {
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.drawer-message.from-user {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.message-role {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.from-user .message-role {
  color: #2563eb;
}

.message-content {
  margin-top: 6px;
  color: #1e293b;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-refs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.message-time {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
}
</style>
