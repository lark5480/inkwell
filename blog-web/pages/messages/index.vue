<template>
  <div class="page-container messages-page">
    <div class="page-header-row">
      <button v-if="activeView !== 'conversations'" class="back-btn" @click="activeView = 'conversations'">← {{ t('messages') }}</button>
      <h1 v-else class="page-title">{{ t('messages') }}</h1>
    </div>

    <!-- Three notification type icons with unread badges -->
    <div class="notif-icons">
      <button class="notif-icon-btn" :class="{ active: activeView === 'replies' }" @click="activeView = 'replies'" :title="t('tabReplies')">
        <span class="notif-icon">💬</span>
        <span class="notif-icon-label">{{ t('tabReplies') }}</span>
        <span v-if="unread.comment > 0" class="notif-icon-badge">{{ unread.comment > 99 ? '99+' : unread.comment }}</span>
      </button>
      <button class="notif-icon-btn" :class="{ active: activeView === 'likes' }" @click="activeView = 'likes'" :title="t('tabLikes')">
        <span class="notif-icon">❤️</span>
        <span class="notif-icon-label">{{ t('tabLikes') }}</span>
        <span v-if="unread.like > 0" class="notif-icon-badge">{{ unread.like > 99 ? '99+' : unread.like }}</span>
      </button>
      <button class="notif-icon-btn" :class="{ active: activeView === 'followers' }" @click="activeView = 'followers'" :title="t('tabFollowers')">
        <span class="notif-icon">👤</span>
        <span class="notif-icon-label">{{ t('tabFollowers') }}</span>
        <span v-if="unread.follow > 0" class="notif-icon-badge">{{ unread.follow > 99 ? '99+' : unread.follow }}</span>
      </button>
    </div>

    <!-- Default: conversations list -->
    <template v-if="activeView === 'conversations'">
      <div v-if="convLoading" class="empty-state"><p>{{ t('common.loading') }}</p></div>
      <div v-else-if="conversations.length === 0" class="empty-state"><p>{{ t('noConversations') }}</p></div>
      <div v-else class="conversation-list">
        <NuxtLink v-for="conv in conversations" :key="conv.userId" :to="`/messages/${conv.userId}`" class="conversation-item">
          <UserAvatar
            :user="{ id: conv.userId, nickname: conv.userName, avatar: conv.userAvatar }"
            :size="40"
          />
          <div class="conv-info">
            <div class="conv-header">
              <span class="conv-name">{{ conv.userName || t('common.unknown') }}</span>
              <span v-if="conv.lastMessageTime" class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>
            <div class="conv-last-msg">{{ conv.lastMessage || t('noMessages') }}</div>
          </div>
          <span v-if="conv.unreadCount > 0" class="conv-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
        </NuxtLink>
      </div>
    </template>

    <!-- Replies (COMMENT notifications) -->
    <template v-else-if="activeView === 'replies'">
      <NotificationList :notifications="replies" :loading="repliesLoading" :page="repliesPage" :total-pages="repliesTotalPages" @page-change="loadReplies" />
    </template>

    <!-- Likes (LIKE notifications) -->
    <template v-else-if="activeView === 'likes'">
      <NotificationList :notifications="likes" :loading="likesLoading" :page="likesPage" :total-pages="likesTotalPages" @page-change="loadLikes" />
    </template>

    <!-- Followers -->
    <template v-else-if="activeView === 'followers'">
      <div v-if="followersLoading" class="empty-state">{{ t('common.loading') }}</div>
      <div v-else-if="followers.length === 0" class="empty-state">{{ t('notification.empty') }}</div>
      <div v-else class="follower-list">
        <div v-for="f in followers" :key="f.userId" class="follower-item">
          <NuxtLink :to="`/user/${f.userId}`" class="follower-info">
            <UserAvatar :user="{ id: f.userId, nickname: f.nickname, avatar: f.avatar }" :size="40" />
            <div>
              <div class="follower-name">{{ f.nickname }}</div>
              <div v-if="f.bio" class="follower-bio">{{ f.bio }}</div>
            </div>
          </NuxtLink>
          <button v-if="f.userId !== currentUser?.id" class="btn btn-sm" :class="followedSet.has(f.userId) ? 'btn-outline' : 'btn-primary'"
            @click="handleFollowToggle(f.userId)" :disabled="followLoadingSet.has(f.userId)">
            {{ followedSet.has(f.userId) ? t('user.following') : t('user.follow') }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { NotificationResponse, FollowerResponse } from '~/composables/useBlogApi'
import UserAvatar from '~/components/UserAvatar.vue'

definePageMeta({ layout: 'default', middleware: 'auth' })

const { t } = useI18n()
const { locale } = useI18n()
const route = useRoute()
const { currentUser } = useAuth()
const { getConversations, getNotifications, getFollowers, toggleFollow, getFollowStatus, markNotificationTypeAsRead, getUnreadCount } = useBlogApi()
const unreadCount = useState<number>('notif_unread_count', () => 0)
const unread = reactive({ comment: 0, like: 0, follow: 0 })

const activeView = ref<'conversations' | 'replies' | 'likes' | 'followers'>('conversations')

// Conversations
const convLoading = ref(true)
const conversations = ref<any[]>([])

// Replies
const replies = ref<NotificationResponse[]>([])
const repliesLoading = ref(false)
const repliesPage = ref(1)
const repliesTotal = ref(0)
const repliesTotalPages = computed(() => Math.ceil(repliesTotal.value / 20) || 1)

// Likes
const likes = ref<NotificationResponse[]>([])
const likesLoading = ref(false)
const likesPage = ref(1)
const likesTotal = ref(0)
const likesTotalPages = computed(() => Math.ceil(likesTotal.value / 20) || 1)

// Followers
const followers = ref<FollowerResponse[]>([])
const followersLoading = ref(false)
const followedSet = ref<Set<number>>(new Set())
const followLoadingSet = ref<Set<number>>(new Set())

async function loadConversations() {
  convLoading.value = true
  try { conversations.value = await getConversations() } catch { conversations.value = [] }
  finally { convLoading.value = false }
}

async function loadReplies(p: number) {
  repliesLoading.value = true
  repliesPage.value = p
  try {
    const res = await getNotifications({ page: p, pageSize: 20, type: 'COMMENT' })
    replies.value = res.records
    repliesTotal.value = res.total
    markNotificationTypeAsRead('COMMENT').catch(() => {})
  } catch { replies.value = [] }
  finally { repliesLoading.value = false }
}

async function loadLikes(p: number) {
  likesLoading.value = true
  likesPage.value = p
  try {
    const res = await getNotifications({ page: p, pageSize: 20, type: 'LIKE' })
    likes.value = res.records
    likesTotal.value = res.total
    markNotificationTypeAsRead('LIKE').catch(() => {})
  } catch { likes.value = [] }
  finally { likesLoading.value = false }
}

async function loadFollowers_() {
  if (!currentUser.value) return
  followersLoading.value = true
  try {
    const res = await getFollowers(currentUser.value.id, { page: 1, pageSize: 50 })
    followers.value = res.records
    const statuses = await Promise.all(
      res.records.map(f => getFollowStatus(f.userId).catch(() => ({ following: false })))
    )
    followedSet.value = new Set(res.records.filter((_, i) => statuses[i]?.following).map(f => f.userId))
    markNotificationTypeAsRead('FOLLOW').catch(() => {})
  } catch { followers.value = [] }
  finally { followersLoading.value = false }
}

async function handleFollowToggle(userId: number) {
  followLoadingSet.value = new Set([...followLoadingSet.value, userId])
  try {
    const res = await toggleFollow(userId)
    const s = new Set(followedSet.value)
    res.followed ? s.add(userId) : s.delete(userId)
    followedSet.value = s
  } catch { /* ignore */ }
  finally {
    const s = new Set(followLoadingSet.value)
    s.delete(userId)
    followLoadingSet.value = s
  }
}

watch(activeView, (v) => {
  if (v === 'replies' && !replies.value.length) loadReplies(1)
  else if (v === 'likes' && !likes.value.length) loadLikes(1)
  else if (v === 'followers' && !followers.value.length) loadFollowers_()
})

// 从对话页返回时刷新会话列表（未读数可能已变化）
watch(() => route.fullPath, () => {
  if (activeView.value === 'conversations') loadConversations()
})

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 3600000) return Math.floor(diff / 60000) + 'm'
  if (diff < 86400000) return Math.floor(diff / 3600000) + 'h'
  return d.toLocaleDateString(locale.value, { month: 'short', day: 'numeric' })
}

async function fetchPerTypeUnread() {
  try {
    const res = await getUnreadCount()
    unread.comment = res.comment
    unread.like = res.like
    unread.follow = res.follow
  } catch {}
}

onMounted(() => {
  loadConversations()
  fetchPerTypeUnread()
})
</script>

<style scoped>
.messages-page { max-width: 680px; margin: 0 auto; padding: var(--space-6); }
.page-header-row { margin-bottom: var(--space-5); }
.page-title { font-family: var(--font-heading); font-size: var(--font-size-xl); font-weight: 700; margin: 0; }
.back-btn {
  font-size: var(--font-size-sm); font-weight: 600; color: var(--primary);
  background: none; border: none; cursor: pointer; padding: 0;
  font-family: var(--font-body);
}
.back-btn:hover { text-decoration: underline; }

/* Notification type icons */
.notif-icons { display: flex; gap: var(--space-3); margin-bottom: var(--space-6); }
.notif-icon-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px;
  background: var(--bg-tertiary);
  border: 2px solid transparent;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: var(--font-body); font-size: var(--font-size-sm);
  color: var(--text-muted);
}
.notif-icon-btn:hover { background: var(--bg-card); border-color: var(--border); color: var(--text); }
.notif-icon-btn.active { background: var(--bg); border-color: var(--primary); color: var(--primary); font-weight: 600; }
.notif-icon { font-size: 16px; }
.notif-icon-label { white-space: nowrap; }
.notif-icon-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background: var(--error, #ef4444);
  color: white;
  font-size: 10px;
  font-weight: 700;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
.notif-icon-btn { position: relative; }

/* Conversations */
.conversation-list { display: flex; flex-direction: column; gap: 2px; }
.conversation-item {
  display: flex; align-items: center; gap: var(--space-3);
  padding: var(--space-4); border-radius: var(--radius-lg);
  background: var(--bg); text-decoration: none; color: var(--text);
  transition: background var(--transition-fast);
}
.conversation-item:hover { background: var(--bg-tertiary); }
.conv-avatar {
  flex-shrink: 0; width: 44px; height: 44px; border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--secondary));
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: var(--font-size-sm);
}
.conv-info { flex: 1; min-width: 0; }
.conv-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 2px; }
.conv-name { font-weight: 700; font-size: var(--font-size-sm); }
.conv-time { font-size: var(--font-size-xs); color: var(--text-muted); }
.conv-last-msg { font-size: var(--font-size-xs); color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.conv-badge {
  flex-shrink: 0; min-width: 20px; height: 20px; border-radius: 10px;
  background: var(--error, #ef4444); color: #fff;
  font-size: 11px; font-weight: 700; display: flex; align-items: center; justify-content: center; padding: 0 4px;
}

/* Followers */
.follower-list { display: flex; flex-direction: column; gap: 2px; }
.follower-item { display: flex; align-items: center; justify-content: space-between; padding: var(--space-3) var(--space-4); border-radius: var(--radius-lg); }
.follower-item:hover { background: var(--bg-tertiary); }
.follower-info { display: flex; align-items: center; gap: var(--space-3); text-decoration: none; color: var(--text); flex: 1; }
.follower-avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), var(--secondary)); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; flex-shrink: 0; }
.follower-name { font-weight: 700; font-size: var(--font-size-sm); }
.follower-bio { font-size: var(--font-size-xs); color: var(--text-muted); }

.empty-state { text-align: center; padding: var(--space-12) 0; color: var(--text-muted); }
</style>
