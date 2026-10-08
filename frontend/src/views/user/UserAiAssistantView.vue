<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { nextTick, onMounted, ref } from 'vue'

import { apiErrorMessage } from '@/api/http'
import { aiApi } from '@/api/modules'
import type { AiChatMessage, AiChatSession, AiFaq } from '@/api/types'
import { formatDateTime } from '@/utils/format'

const sessions = ref<AiChatSession[]>([])
const messages = ref<AiChatMessage[]>([])
const faqs = ref<AiFaq[]>([])
const question = ref('')
const sending = ref(false)
const loadingSessions = ref(false)
const activeSessionId = ref('')
const messageArea = ref<HTMLElement | null>(null)

async function loadSessions() {
  loadingSessions.value = true
  try {
    const page = await aiApi.sessions({ pageNum: 1, pageSize: 20 })
    sessions.value = page.records
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loadingSessions.value = false
  }
}

async function loadFaqs() {
  try {
    faqs.value = await aiApi.faq()
  } catch {
    faqs.value = []
  }
}

async function openSession(session: AiChatSession) {
  activeSessionId.value = session.sessionId
  try {
    messages.value = await aiApi.messages(session.sessionId)
    scrollToBottom()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  }
}

function startNewSession() {
  activeSessionId.value = ''
  messages.value = []
  question.value = ''
}

async function ask(text: string) {
  question.value = text
  await send()
}

async function send() {
  const text = question.value.trim()
  if (!text || sending.value) {
    return
  }
  sending.value = true
  messages.value.push({
    messageId: `local-${Date.now()}`,
    sessionId: activeSessionId.value,
    role: 'USER',
    content: text,
    intent: null,
    provider: null,
    referencedData: [],
    createTime: new Date().toISOString(),
  })
  question.value = ''
  scrollToBottom()
  try {
    const reply = await aiApi.chat({ sessionId: activeSessionId.value || null, question: text })
    activeSessionId.value = reply.sessionId
    messages.value.push({
      messageId: `reply-${Date.now()}`,
      sessionId: reply.sessionId,
      role: 'ASSISTANT',
      content: reply.answer,
      intent: reply.intent,
      provider: reply.provider,
      referencedData: reply.referencedData ?? [],
      createTime: reply.createTime,
    })
    loadSessions()
    scrollToBottom()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    sending.value = false
  }
}

async function removeSession(session: AiChatSession) {
  try {
    await ElMessageBox.confirm(`确认删除会话「${session.title}」吗？`, '删除会话', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await aiApi.deleteSession(session.sessionId)
    if (activeSessionId.value === session.sessionId) {
      startNewSession()
    }
    ElMessage.success('会话已删除')
    loadSessions()
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  }
}

function scrollToBottom() {
  nextTick(() => {
    messageArea.value?.scrollTo({ top: messageArea.value.scrollHeight, behavior: 'smooth' })
  })
}

function providerLabel(provider?: string | null) {
  const labels: Record<string, string> = {
    'rule-based': '内置规则引擎',
    openai: 'OpenAI 兼容大模型',
    python: 'Python RAG 服务',
  }
  return provider ? labels[provider] ?? provider : ''
}

onMounted(() => {
  loadFaqs()
  loadSessions()
})
</script>

<template>
  <section class="page assistant-page">
    <div class="page-head">
      <div>
        <h1>AI 智能助手</h1>
        <p>在线查询车位、车辆、预约、停车记录与计费规则，也可以直接提出建议。</p>
      </div>
      <div class="toolbar">
        <el-button type="primary" @click="startNewSession">新建对话</el-button>
      </div>
    </div>

    <div class="assistant-layout">
      <aside class="table-panel session-panel" v-loading="loadingSessions">
        <div class="panel-body">
          <h3 class="panel-title">历史会话</h3>
          <el-empty v-if="!sessions.length" description="暂无历史会话" :image-size="60" />
          <ul v-else class="session-list">
            <li
              v-for="session in sessions"
              :key="session.sessionId"
              class="session-item"
              :class="{ active: session.sessionId === activeSessionId }"
              @click="openSession(session)"
            >
              <div class="session-main">
                <span class="session-title">{{ session.title }}</span>
                <span class="session-meta">{{ formatDateTime(session.lastMessageTime) }} · {{ session.messageCount }} 条</span>
              </div>
              <el-button text type="danger" size="small" @click.stop="removeSession(session)">删除</el-button>
            </li>
          </ul>
        </div>
      </aside>

      <div class="table-panel chat-panel">
        <div ref="messageArea" class="chat-body">
          <el-empty
            v-if="!messages.length"
            description="选择下方推荐问题或直接输入，开始咨询"
            :image-size="90"
          />
          <div
            v-for="message in messages"
            :key="message.messageId"
            class="chat-message"
            :class="message.role === 'USER' ? 'from-user' : 'from-assistant'"
          >
            <div class="chat-avatar">{{ message.role === 'USER' ? '我' : 'AI' }}</div>
            <div class="chat-bubble">
              <p class="bubble-text">{{ message.content }}</p>
              <div v-if="message.referencedData?.length" class="reference-tags">
                <el-tag v-for="reference in message.referencedData" :key="reference" size="small" type="info" effect="plain">
                  {{ reference }}
                </el-tag>
              </div>
              <div class="bubble-meta">
                <span class="bubble-time">{{ formatDateTime(message.createTime) }}</span>
                <el-tag
                  v-if="message.role !== 'USER' && message.provider"
                  size="small"
                  effect="plain"
                  :type="message.provider === 'rule-based' ? 'warning' : 'success'"
                >
                  {{ providerLabel(message.provider) }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-footer">
          <div class="faq-chips">
            <el-tag
              v-for="item in faqs"
              :key="item.question"
              class="faq-chip"
              effect="plain"
              @click="ask(item.question)"
            >
              {{ item.question }}
            </el-tag>
          </div>
          <div class="chat-input">
            <el-input
              v-model="question"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              resize="none"
              placeholder="请输入问题，例如：还有多少空闲车位？"
              @keydown.enter.exact.prevent="send"
            />
            <el-button type="primary" :loading="sending" :disabled="!question.trim()" @click="send">发送</el-button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.assistant-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
  align-items: stretch;
}

.panel-title {
  margin: 0 0 14px;
  color: #0f172a;
  font-size: 15px;
}

.session-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.session-item {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.session-item:hover,
.session-item.active {
  border-color: #0f766e;
  background: #f0fdfa;
}

.session-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.session-title {
  overflow: hidden;
  color: #0f172a;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  color: #94a3b8;
  font-size: 12px;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  min-height: 560px;
}

.chat-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 14px;
  max-height: 520px;
  padding: 18px;
  overflow-y: auto;
}

.chat-message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.chat-message.from-user {
  flex-direction: row-reverse;
}

.chat-avatar {
  display: flex;
  flex: 0 0 32px;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #0f766e;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
}

.from-user .chat-avatar {
  background: #2563eb;
}

.chat-bubble {
  max-width: 72%;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.from-user .chat-bubble {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.bubble-text {
  margin: 0;
  color: #1e293b;
  line-height: 1.6;
  white-space: pre-wrap;
}

.reference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.bubble-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 6px;
}

.bubble-time {
  color: #94a3b8;
  font-size: 12px;
}

.chat-footer {
  display: grid;
  gap: 12px;
  padding: 14px 18px 18px;
  border-top: 1px solid #eef2f7;
}

.faq-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.faq-chip {
  cursor: pointer;
}

.chat-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: flex-end;
}

@media (max-width: 960px) {
  .assistant-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
