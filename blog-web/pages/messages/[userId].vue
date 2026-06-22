<template>
  <div class="chat-page">
    <div class="chat-header">
      <NuxtLink to="/messages" class="chat-back">← {{ t('messages') }}</NuxtLink>
      <span class="chat-peer">{{ peerName }}</span>
    </div>

    <div class="chat-messages" ref="messagesRef">
      <div v-if="loading" class="chat-loading">{{ t('common.loading') }}</div>

      <div v-for="msg in messages" :key="msg.id" class="chat-msg" :class="{ 'chat-msg-own': msg.fromUserId === myId }">
        <div class="msg-bubble">{{ msg.content }}</div>
        <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
      </div>

      <div v-if="!loading && messages.length === 0" class="chat-empty">{{ t('noMessages') }}</div>
    </div>

    <div class="chat-input-bar">
      <input
        v-model="inputContent"
        :placeholder="t('messagePlaceholder')"
        class="chat-input"
        @keydown.enter="handleSend"
        :disabled="sending"
      />
      <button class="btn btn-primary btn-sm" @click="handleSend" :disabled="sending || !inputContent.trim()">
        {{ sending ? t('common.loading') : t('sendMessage') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'default', middleware: 'auth' })

const { t } = useI18n()
const { locale } = useI18n()
const { currentUser } = useAuth()
const { getMessages, sendMessage, markAllMessagesRead, getUserProfile } = useBlogApi()

const route = useRoute()
const peerUserId = Number(route.params.userId)
const myId = computed(() => currentUser.value?.id)

const loading = ref(true)
const messages = ref<any[]>([])
const inputContent = ref('')
const sending = ref(false)
const peerName = ref('')
const messagesRef = ref<HTMLElement | null>(null)

async function loadMessages() {
  loading.value = true
  try {
    const [profile, result] = await Promise.all([
      getUserProfile(peerUserId),
      getMessages(peerUserId, { page: 1, pageSize: 50 }),
    ])
    peerName.value = profile?.nickname || ''
    messages.value = (result.records || []).reverse()
    // 标记已读
    await markAllMessagesRead(peerUserId)
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function handleSend() {
  if (!inputContent.value.trim() || sending.value) return
  sending.value = true
  try {
    const msg = await sendMessage(peerUserId, inputContent.value.trim())
    messages.value.push(msg)
    if (!peerName.value) peerName.value = msg.fromUserName || ''
    inputContent.value = ''
    scrollToBottom()
  } catch { /* ignore */ }
  finally { sending.value = false }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleTimeString(locale.value, { hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  loadMessages().then(scrollToBottom)
})
</script>

<style scoped>
.chat-page {
  max-width: 680px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-6);
  border-bottom: 2px solid var(--border-light);
  font-family: var(--font-heading);
}

.chat-back {
  font-size: var(--font-size-sm);
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
}
.chat-back:hover { text-decoration: underline; }

.chat-peer {
  font-size: var(--font-size-lg);
  font-weight: 700;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4) var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.chat-loading, .chat-empty {
  text-align: center;
  padding: var(--space-12) 0;
  color: var(--text-muted);
}

.chat-msg {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}

.chat-msg-own {
  align-self: flex-end;
  align-items: flex-end;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: var(--radius-lg);
  font-size: var(--font-size-sm);
  line-height: 1.5;
  word-break: break-word;
  background: var(--bg-tertiary);
  color: var(--text);
}

.chat-msg-own .msg-bubble {
  background: var(--primary);
  color: #fff;
}

.msg-time {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-top: 2px;
  padding: 0 4px;
}

.chat-input-bar {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-6);
  border-top: 2px solid var(--border-light);
  background: var(--bg);
}

.chat-input {
  flex: 1;
  padding: 10px 14px;
  border: 3px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg);
  color: var(--text);
  font-size: var(--font-size-sm);
  outline: none;
  transition: all var(--transition-fast);
}
.chat-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}
.chat-input::placeholder { color: var(--text-muted); }
</style>
