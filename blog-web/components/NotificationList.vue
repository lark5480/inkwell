<template>
  <div v-if="loading" class="loading-state">{{ t('common.loading') }}</div>
  <div v-else-if="!notifications.length" class="empty-state">{{ t('notification.empty') }}</div>

  <div v-else class="notif-list">
    <div
      v-for="notif in notifications"
      :key="notif.id"
      :class="['notif-item', { unread: !notif.isRead }]"
      @click="handleNotifClick(notif)"
    >
      <div class="notif-avatar">
        <img v-if="notif.fromUserAvatar" :src="notif.fromUserAvatar" alt="" />
        <div v-else class="avatar-placeholder">{{ notifTypeIcon(notif.type) }}</div>
      </div>
      <div class="notif-body">
        <p class="notif-text">
          <strong v-if="notif.fromUserName">{{ notif.fromUserName }}</strong>
          <span v-if="notif.type === 'COMMENT'">{{ t('notification.commented') }}</span>
          <span v-else-if="notif.type === 'LIKE'">{{ t('notification.liked') }}</span>
          <span v-else-if="notif.type === 'FOLLOW'">{{ t('notification.followed') }}</span>
          <span v-else-if="notif.type === 'MESSAGE'">{{ t('notification.message') }}</span>
          <em v-if="notif.articleTitle">{{ notif.articleTitle }}</em>
        </p>
        <p v-if="notif.content" class="notif-content">{{ notif.content }}</p>
        <span class="notif-time">{{ formatTimeAgo(notif.createTime) }}</span>
      </div>
    </div>
  </div>

  <div v-if="totalPages > 1" class="pagination">
    <button :disabled="page <= 1" @click="$emit('pageChange', page - 1)">&laquo; {{ t('pagination.prev') }}</button>
    <span class="page-info">{{ page }} / {{ totalPages }}</span>
    <button :disabled="page >= totalPages" @click="$emit('pageChange', page + 1)">{{ t('pagination.next') }} &raquo;</button>
  </div>
</template>

<script setup lang="ts">
import type { NotificationResponse } from '~/composables/useBlogApi'

const { t } = useI18n()
const { markNotificationRead } = useBlogApi()
const unreadCount = useState<number>('notif_unread_count', () => 0)

const props = defineProps<{
  notifications: NotificationResponse[]
  loading: boolean
  page: number
  totalPages: number
}>()

defineEmits<{ pageChange: [page: number] }>()

function notifTypeIcon(type: string) {
  if (type === 'COMMENT') return '💬'
  if (type === 'LIKE') return '❤️'
  if (type === 'FOLLOW') return '👤'
  if (type === 'MESSAGE') return '✉️'
  return '🔔'
}

function formatTimeAgo(dt: string) {
  const diff = Date.now() - new Date(dt).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return t('common.justNow') || '刚刚'
  if (mins < 60) return `${mins}m`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}d`
  return new Date(dt).toLocaleDateString()
}

async function handleNotifClick(notif: NotificationResponse) {
  if (!notif.isRead) {
    try { await markNotificationRead(notif.id) } catch {}
    notif.isRead = true
    if (unreadCount.value > 0) unreadCount.value--
  }
  if (notif.type === 'MESSAGE' && notif.fromUserId) {
    navigateTo(`/messages/${notif.fromUserId}`)
  } else if (notif.type === 'FOLLOW' && notif.fromUserId) {
    navigateTo(`/user/${notif.fromUserId}`)
  } else if (notif.articleSlug) {
    navigateTo(`/article/${notif.articleSlug}`)
  }
}
</script>

<style scoped>
.notif-list { display: flex; flex-direction: column; gap: var(--space-2); }
.notif-item { display: flex; gap: var(--space-4); padding: var(--space-4); background: var(--bg-card); border: var(--clay-border); border-radius: var(--clay-border-radius); cursor: pointer; transition: all var(--transition-fast); }
.notif-item:hover { background: var(--bg-tertiary); border-color: var(--primary); }
.notif-item.unread { border-left: 3px solid var(--primary); }
.notif-avatar { flex-shrink: 0; width: 40px; height: 40px; }
.notif-avatar img, .avatar-placeholder { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; }
.avatar-placeholder { display: flex; align-items: center; justify-content: center; background: var(--bg-tertiary); font-size: 18px; }
.notif-body { flex: 1; min-width: 0; }
.notif-text { font-size: var(--font-size-sm); color: var(--text); line-height: 1.5; margin: 0; }
.notif-text strong { font-weight: 700; }
.notif-text em { font-style: normal; color: var(--primary); }
.notif-content { font-size: var(--font-size-xs); color: var(--text-secondary); margin: var(--space-1) 0 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notif-time { font-size: var(--font-size-xs); color: var(--text-muted); }
.loading-state, .empty-state { text-align: center; padding: var(--space-12) 0; color: var(--text-secondary); }
.pagination { display: flex; align-items: center; justify-content: center; gap: var(--space-4); margin-top: var(--space-6); }
.pagination button { padding: var(--space-2) var(--space-4); font-size: var(--font-size-sm); font-weight: 600; background: var(--bg-card); border: var(--clay-border); border-radius: var(--clay-border-radius); cursor: pointer; }
.pagination button:hover:not(:disabled) { background: var(--bg-tertiary); border-color: var(--primary); }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: var(--font-size-sm); color: var(--text-secondary); }
</style>
