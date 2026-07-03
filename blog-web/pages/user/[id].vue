<template>
  <div class="container">
    <div v-if="loading" class="loading-state">{{ t('common.loading') }}</div>

    <div v-else-if="error" class="error-state">{{ error }}</div>

    <template v-else-if="profile">
      <!-- Profile Card -->
      <div class="profile-section">
        <div class="profile-card">
          <div class="profile-avatar">
            <img v-if="profileAvatarUrl" :src="profileAvatarUrl" :alt="profile.nickname" />
            <div v-else class="avatar-placeholder">{{ (profile.nickname || '?').charAt(0).toUpperCase() }}</div>
          </div>
          <div class="profile-info">
            <h1 class="profile-name">{{ profile.nickname || 'Anonymous' }}</h1>
            <p v-if="profile.bio" class="profile-bio">{{ profile.bio }}</p>
            <div class="profile-stats">
              <span>{{ profile.articleCount }} {{ t('common.articles') }}</span>
              <span class="stat-click" @click="switchTab('followers')">{{ profile.followerCount }} {{ t('user.followers') || 'followers' }}</span>
              <span class="stat-click" @click="switchTab('following')">{{ profile.followingCount }} {{ t('user.following') || 'following' }}</span>
              <span>{{ t('user.joined') }} {{ formatDate(profile.createTime) }}</span>
            </div>
          </div>
          <button
            v-if="isLoggedIn && !isOwner"
            :class="['follow-btn', { followed: isFollowing }]"
            @click="handleToggleFollow"
            :disabled="followLoading"
          >{{ isFollowing ? (t('user.following') || 'Following') : (t('user.follow') || 'Follow') }}</button>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs-section">
        <div class="tabs">
          <button
            :class="['tab-btn', { active: activeTab === 'articles' }]"
            @click="switchTab('articles')"
          >{{ t('user.tabArticles') }}</button>
          <button
            :class="['tab-btn', { active: activeTab === 'followers' }]"
            @click="switchTab('followers')"
          >{{ t('user.followers') || 'Followers' }}</button>
          <button
            :class="['tab-btn', { active: activeTab === 'following' }]"
            @click="switchTab('following')"
          >{{ t('user.following') || 'Following' }}</button>
          <button
            v-if="isOwner"
            :class="['tab-btn', { active: activeTab === 'history' }]"
            @click="switchTab('history')"
          >{{ t('user.tabHistory') }}</button>
          <button
            v-if="isOwner"
            :class="['tab-btn', { active: activeTab === 'likes' }]"
            @click="switchTab('likes')"
          >{{ t('user.tabLikes') }}</button>
          <button
            v-if="isOwner"
            :class="['tab-btn', { active: activeTab === 'blocked' }]"
            @click="switchTab('blocked')"
          >{{ t('user.blocked') || 'Blocked' }}</button>
        </div>

        <!-- Articles Tab -->
        <div v-if="activeTab === 'articles'" class="tab-content">
          <div v-if="articlesLoading" class="loading-state">{{ t('common.loading') }}</div>
          <div v-else-if="!articles.length" class="empty-state">{{ t('myArticles.empty') }}</div>
          <div v-else class="article-list">
            <article v-for="article in articles" :key="article.id" class="article-card">
              <div v-if="article.coverImage" class="article-cover">
                <img :src="article.coverImage" :alt="article.title || ''" />
              </div>
              <div class="article-body">
                <h3 class="article-title">
                  <NuxtLink :to="`/article/${article.slug}`">{{ article.title }}</NuxtLink>
                </h3>
                <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
                <div class="article-meta">
                  <span v-if="article.publishedAt">{{ formatDate(article.publishedAt) }}</span>
                  <span>{{ article.viewCount }} {{ t('home.views') }}</span>
                  <span>{{ article.likeCount }} {{ t('home.likes') }}</span>
                </div>
              </div>
            </article>
          </div>
          <div v-if="articleTotalPages > 1" class="pagination">
            <button :disabled="articlePage <= 1" @click="loadArticles(articlePage - 1)">&laquo; {{ t('pagination.prev') }}</button>
            <span class="page-info">{{ articlePage }} / {{ articleTotalPages }}</span>
            <button :disabled="articlePage >= articleTotalPages" @click="loadArticles(articlePage + 1)">{{ t('pagination.next') }} &raquo;</button>
          </div>
        </div>

        <!-- History Tab -->
        <div v-if="activeTab === 'history'" class="tab-content">
          <div class="history-toolbar">
            <input
              v-model="historyKeyword"
              type="text"
              class="search-input"
              :placeholder="t('myArticles.searchPlaceholder')"
              @input="onHistorySearch"
            />
          </div>
          <div v-if="historyLoading" class="loading-state">{{ t('common.loading') }}</div>
          <div v-else-if="!historyItems.length" class="empty-state">{{ t('myArticles.empty') }}</div>
          <div v-else class="history-list">
            <div v-for="item in historyItems" :key="item.id" class="history-item">
              <NuxtLink :to="`/article/${item.articleSlug}`" class="history-link">
                {{ item.articleTitle }}
              </NuxtLink>
              <div class="history-right">
                <span class="history-time">{{ formatDate(item.viewedAt) }}</span>
                <button class="btn-delete" :title="t('common.delete')" @click="handleDeleteHistory(item.id)">&times;</button>
              </div>
            </div>
          </div>
          <div v-if="historyTotalPages > 1" class="pagination">
            <button :disabled="historyPage <= 1" @click="loadHistory(historyPage - 1)">&laquo; {{ t('pagination.prev') }}</button>
            <span class="page-info">{{ historyPage }} / {{ historyTotalPages }}</span>
            <button :disabled="historyPage >= historyTotalPages" @click="loadHistory(historyPage + 1)">{{ t('pagination.next') }} &raquo;</button>
          </div>
        </div>

        <!-- Likes Tab -->
        <div v-if="activeTab === 'likes'" class="tab-content">
          <div v-if="likesLoading" class="loading-state">{{ t('common.loading') }}</div>
          <div v-else-if="!likeItems.length" class="empty-state">{{ t('myArticles.empty') }}</div>
          <div v-else class="history-list">
            <div v-for="item in likeItems" :key="item.id" class="history-item">
              <NuxtLink :to="`/article/${item.articleSlug}`" class="history-link">
                {{ item.articleTitle }}
              </NuxtLink>
              <span class="history-time">{{ formatDate(item.viewedAt) }}</span>
            </div>
          </div>
          <div v-if="likesTotalPages > 1" class="pagination">
            <button :disabled="likesPage <= 1" @click="loadLikes(likesPage - 1)">&laquo; {{ t('pagination.prev') }}</button>
            <span class="page-info">{{ likesPage }} / {{ likesTotalPages }}</span>
            <button :disabled="likesPage >= likesTotalPages" @click="loadLikes(likesPage + 1)">{{ t('pagination.next') }} &raquo;</button>
          </div>
        </div>

        <!-- Blocked Users Tab -->
        <div v-if="activeTab === 'blocked'" class="tab-content">
          <div v-if="blockedLoading" class="loading-state">{{ t('common.loading') }}</div>
          <div v-else-if="blockedUsers.length === 0" class="empty-state">{{ t('user.noBlocked') || 'No blocked users' }}</div>
          <div v-else class="user-list">
            <div v-for="u in blockedUsers" :key="u.userId" class="user-item">
              <NuxtLink :to="`/user/${u.userId}`" class="user-item-link">
                <UserAvatar :user="{ id: u.userId, nickname: u.nickname, avatar: u.avatar }" :size="40" />
                <div class="user-item-info">
                  <span class="user-item-name">{{ u.nickname }}</span>
                  <span v-if="u.bio" class="user-item-bio">{{ u.bio }}</span>
                </div>
              </NuxtLink>
              <div class="user-item-actions">
                <button
                  class="follow-btn followed"
                  @click="handleUnblock(u.userId)"
                  :disabled="unblockingIds.has(u.userId)"
                >{{ unblockingIds.has(u.userId) ? '...' : (t('user.unblock') || 'Unblock') }}</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Followers Tab -->
        <div v-if="activeTab === 'followers'" class="tab-content">
          <div v-if="followersLoading" class="loading-state">{{ t('common.loading') }}</div>
          <div v-else-if="!followers.length" class="empty-state">{{ t('user.noFollowers') || 'No followers yet' }}</div>
          <div v-else class="user-list">
            <div v-for="f in followers" :key="f.userId" class="user-item">
              <NuxtLink :to="`/user/${f.userId}`" class="user-item-link">
                <UserAvatar :user="{ id: f.userId, nickname: f.nickname, avatar: f.avatar }" :size="40" />
                <div class="user-item-info">
                  <span class="user-item-name">{{ f.nickname }}</span>
                  <span v-if="f.bio" class="user-item-bio">{{ f.bio }}</span>
                </div>
              </NuxtLink>
              <div v-if="isOwner" class="user-item-actions">
                <NuxtLink :to="`/messages/${f.userId}`" class="follow-btn">{{ t('user.sendMessage') }}</NuxtLink>
              </div>
            </div>
          </div>
          <div v-if="followersTotalPages > 1" class="pagination">
            <button :disabled="followersPage <= 1" @click="loadFollowers(followersPage - 1)">&laquo; {{ t('pagination.prev') }}</button>
            <span class="page-info">{{ followersPage }} / {{ followersTotalPages }}</span>
            <button :disabled="followersPage >= followersTotalPages" @click="loadFollowers(followersPage + 1)">{{ t('pagination.next') }} &raquo;</button>
          </div>
        </div>

        <!-- Following Tab -->
        <div v-if="activeTab === 'following'" class="tab-content">
          <div v-if="followingLoading" class="loading-state">{{ t('common.loading') }}</div>
          <div v-else-if="!followingList.length" class="empty-state">{{ t('user.noFollowing') || 'Not following anyone yet' }}</div>
          <div v-else class="user-list">
            <div v-for="f in followingList" :key="f.userId" class="user-item">
              <NuxtLink :to="`/user/${f.userId}`" class="user-item-link">
                <UserAvatar :user="{ id: f.userId, nickname: f.nickname, avatar: f.avatar }" :size="40" />
                <div class="user-item-info">
                  <span class="user-item-name">{{ f.nickname }}</span>
                  <span v-if="f.bio" class="user-item-bio">{{ f.bio }}</span>
                </div>
              </NuxtLink>
              <div v-if="isOwner" class="user-item-actions">
                <NuxtLink :to="`/messages/${f.userId}`" class="follow-btn">{{ t('user.sendMessage') }}</NuxtLink>
                <button class="follow-btn followed" @click="handleUnfollow(f.userId)" :disabled="followLoadingSet.has(f.userId)">
                  {{ followLoadingSet.has(f.userId) ? '...' : t('user.following') }}
                </button>
              </div>
            </div>
          </div>
          <div v-if="followingTotalPages > 1" class="pagination">
            <button :disabled="followingPage <= 1" @click="loadFollowing(followingPage - 1)">&laquo; {{ t('pagination.prev') }}</button>
            <span class="page-info">{{ followingPage }} / {{ followingTotalPages }}</span>
            <button :disabled="followingPage >= followingTotalPages" @click="loadFollowing(followingPage + 1)">{{ t('pagination.next') }} &raquo;</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { UserPublicResponse, ArticleWebResponse, HistoryItemResponse, FollowerResponse, FollowingResponse, BlockedUserResponse } from '~/composables/useBlogApi'
import UserAvatar from '~/components/UserAvatar.vue'

import { resolveImageUrl } from '~/composables/useImageUrl'

const route = useRoute()
const userId = Number(route.params.id)
const { getUserProfile, getUserArticles, getMyHistory, getMyLikes, deleteMyHistory, toggleFollow, getFollowers, getFollowing, getFollowStatus, getBlockedUsers, unblockUser } = useBlogApi()
const { user, isLoggedIn } = useAuth()
const { confirm } = useModal()
const { t } = useI18n()

const profile = ref<UserPublicResponse | null>(null)
const profileAvatarUrl = computed(() => resolveImageUrl(profile.value?.avatar))
const loading = ref(true)
const error = ref('')

// Follow state
const isFollowing = ref(false)
const followLoading = ref(false)

async function handleToggleFollow() {
  followLoading.value = true
  try {
    const res = await toggleFollow(userId)
    isFollowing.value = res.followed
    if (profile.value) {
      profile.value.followerCount = res.followerCount
      profile.value.followingCount = res.followingCount
    }
  } catch {} finally {
    followLoading.value = false
  }
}

// Tab state
const activeTab = ref<'articles' | 'history' | 'likes' | 'followers' | 'following'>('articles')
const isOwner = computed(() => user.value?.id === userId)

// Articles tab
const articles = ref<ArticleWebResponse[]>([])
const articlesLoading = ref(false)
const articlePage = ref(1)
const articleTotalPages = ref(1)

// History tab
const historyItems = ref<HistoryItemResponse[]>([])
const historyLoading = ref(false)
const historyPage = ref(1)
const historyTotalPages = ref(1)
const historyKeyword = ref('')
let historySearchTimer: ReturnType<typeof setTimeout> | null = null

function onHistorySearch() {
  if (historySearchTimer) clearTimeout(historySearchTimer)
  historySearchTimer = setTimeout(() => loadHistory(1), 300)
}

async function handleDeleteHistory(id: number) {
  const ok = await confirm({
    title: t('user.deleteHistoryTitle'),
    message: t('user.deleteHistoryConfirm'),
    type: 'warning',
    confirmText: t('common.delete'),
  })
  if (!ok) return
  try {
    await deleteMyHistory(id)
    historyItems.value = historyItems.value.filter(i => i.id !== id)
  } catch {}
}

// Likes tab
const likeItems = ref<HistoryItemResponse[]>([])
const likesLoading = ref(false)
const likesPage = ref(1)
const likesTotalPages = ref(1)

// Followers tab
const followers = ref<FollowerResponse[]>([])
const followersLoading = ref(false)
const followersPage = ref(1)
const followersTotalPages = ref(1)

// Following tab
const followingList = ref<FollowingResponse[]>([])
const followingLoading = ref(false)
const followingPage = ref(1)
const followingTotalPages = ref(1)
const followLoadingSet = ref(new Set<number>())

async function handleUnfollow(targetId: number) {
  if (followLoadingSet.value.has(targetId)) return
  followLoadingSet.value = new Set([...followLoadingSet.value, targetId])
  try {
    await toggleFollow(targetId)
    followingList.value = followingList.value.filter(f => f.userId !== targetId)
    if (profile.value && profile.value.followingCount > 0) {
      profile.value.followingCount--
    }
  } catch {}
  finally {
    const s = new Set(followLoadingSet.value)
    s.delete(targetId)
    followLoadingSet.value = s
  }
}

// Blocked users tab
const blockedUsers = ref<BlockedUserResponse[]>([])
const blockedLoading = ref(false)
const unblockingIds = ref(new Set<number>())

async function loadBlockedUsers() {
  blockedLoading.value = true
  try {
    blockedUsers.value = await getBlockedUsers()
  } catch {}
  finally { blockedLoading.value = false }
}

async function handleUnblock(userId: number) {
  unblockingIds.value = new Set([...unblockingIds.value, userId])
  try {
    await unblockUser(userId)
    blockedUsers.value = blockedUsers.value.filter(u => u.userId !== userId)
  } catch {}
  finally {
    const s = new Set(unblockingIds.value)
    s.delete(userId)
    unblockingIds.value = s
  }
}

function formatDate(dateStr: string) {
  const { locale } = useI18n()
  return new Date(dateStr).toLocaleDateString(locale.value, { year: 'numeric', month: 'short', day: 'numeric' })
}

function switchTab(tab: 'articles' | 'history' | 'likes' | 'followers' | 'following' | 'blocked') {
  activeTab.value = tab
  if (tab === 'articles' && !articles.value.length) loadArticles(1)
  if (tab === 'history' && !historyItems.value.length) loadHistory(1)
  if (tab === 'likes' && !likeItems.value.length) loadLikes(1)
  if (tab === 'followers' && !followers.value.length) loadFollowers(1)
  if (tab === 'blocked' && !blockedUsers.value.length) loadBlockedUsers()
  if (tab === 'following' && !followingList.value.length) loadFollowing(1)
}

async function loadProfile() {
  try {
    profile.value = await getUserProfile(userId)
    useHead({ title: profile.value.nickname || 'User' })
    // Load follow status if logged in and not owner
    if (isLoggedIn.value && user.value?.id !== userId) {
      try {
        const res = await getFollowStatus(userId)
        isFollowing.value = res.following
      } catch {}
    }
    await loadArticles(1)
  } catch (e: any) {
    error.value = e.message || 'User not found'
  } finally {
    loading.value = false
  }
}

async function loadArticles(page: number) {
  articlesLoading.value = true
  articlePage.value = page
  try {
    const result = await getUserArticles(userId, { page, pageSize: 10 })
    articles.value = result.records
    articleTotalPages.value = Math.ceil(result.total / result.pageSize) || 1
  } catch {
    // Ignore
  } finally {
    articlesLoading.value = false
  }
}

async function loadHistory(page: number) {
  historyLoading.value = true
  historyPage.value = page
  try {
    const result = await getMyHistory({ page, pageSize: 20, keyword: historyKeyword.value || undefined })
    historyItems.value = result.records
    historyTotalPages.value = Math.ceil(result.total / result.pageSize) || 1
  } catch {
    // Ignore
  } finally {
    historyLoading.value = false
  }
}

async function loadLikes(page: number) {
  likesLoading.value = true
  likesPage.value = page
  try {
    const result = await getMyLikes({ page, pageSize: 20 })
    likeItems.value = result.records
    likesTotalPages.value = Math.ceil(result.total / result.pageSize) || 1
  } catch {
    // Ignore
  } finally {
    likesLoading.value = false
  }
}

async function loadFollowers(page: number) {
  followersLoading.value = true
  followersPage.value = page
  try {
    const result = await getFollowers(userId, { page, pageSize: 20 })
    followers.value = result.records
    followersTotalPages.value = Math.ceil(result.total / result.pageSize) || 1
  } catch {
    // Ignore
  } finally {
    followersLoading.value = false
  }
}

async function loadFollowing(page: number) {
  followingLoading.value = true
  followingPage.value = page
  try {
    const result = await getFollowing(userId, { page, pageSize: 20 })
    followingList.value = result.records
    followingTotalPages.value = Math.ceil(result.total / result.pageSize) || 1
  } catch {
    // Ignore
  } finally {
    followingLoading.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-section {
  padding: var(--space-8) 0 var(--space-6);
}

.profile-card {
  display: flex;
  align-items: flex-start;
  gap: var(--space-6);
  padding: var(--space-8);
  background: var(--bg-card);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
  box-shadow: var(--clay-shadow);
}

.profile-avatar img,
.avatar-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary);
  color: white;
  font-family: var(--font-heading);
  font-size: var(--font-size-3xl);
  font-weight: 700;
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-family: var(--font-heading);
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--text);
  margin: 0 0 var(--space-2);
}

.profile-bio {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  margin: 0 0 var(--space-3);
  line-height: 1.6;
}

.profile-stats {
  display: flex;
  gap: var(--space-4);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  font-family: var(--font-heading);
  flex-wrap: wrap;
}

.stat-click {
  cursor: pointer;
  transition: color var(--transition-fast);
}

.stat-click:hover {
  color: var(--primary);
}

.follow-btn {
  flex-shrink: 0;
  align-self: flex-start;
  padding: var(--space-2) var(--space-5);
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  border-radius: var(--clay-border-radius);
  cursor: pointer;
  transition: all var(--transition-fast);
  background: var(--primary);
  color: white;
  border: none;
}

.follow-btn:hover {
  background: var(--primary-dark);
  transform: translateY(-1px);
}

.follow-btn.followed {
  background: var(--bg-card);
  color: var(--text-secondary);
  border: var(--clay-border);
}

.follow-btn.followed:hover {
  color: var(--error);
  border-color: var(--error);
  background: var(--bg-card);
}

.follow-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* User list (followers/following) */
.user-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.user-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  background: var(--bg-card);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
  transition: all var(--transition-fast);
}

.user-item:hover {
  box-shadow: var(--shadow-sm);
}

.user-item-link {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  text-decoration: none;
}

.user-item-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.user-item-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 700;
  flex-shrink: 0;
}

.user-item-actions {
  flex-shrink: 0;
  margin-left: auto;
}

.user-item-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-item-name {
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text);
}

.user-item-link:hover .user-item-name {
  color: var(--primary);
}

.user-item-bio {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Tabs */
.tabs-section {
  padding-bottom: var(--space-8);
}

.tabs {
  display: flex;
  gap: var(--space-1);
  margin-bottom: var(--space-6);
  border-bottom: 2px solid var(--border-color);
}

.tab-btn {
  padding: var(--space-3) var(--space-5);
  border: none;
  background: none;
  color: var(--text-secondary);
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 600;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all var(--transition-fast);
}

.tab-btn:hover {
  color: var(--primary);
}

.tab-btn.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.tab-content {
  min-height: 200px;
}

/* Articles */
.article-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.article-card {
  display: flex;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--bg-card);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
  transition: all var(--transition-fast);
}

.article-card:hover {
  box-shadow: var(--shadow-md);
}

.article-cover {
  flex-shrink: 0;
  width: 120px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-body {
  flex: 1;
  min-width: 0;
}

.article-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--text);
  margin: 0 0 var(--space-2);
}

.article-title a {
  color: inherit;
  text-decoration: none;
}

.article-title a:hover {
  color: var(--primary);
}

.article-summary {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: 0 0 var(--space-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  gap: var(--space-3);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

/* History / Likes list */
.history-toolbar {
  margin-bottom: var(--space-4);
}

.search-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 3px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg);
  color: var(--text);
  font-size: var(--font-size-sm);
  outline: none;
  box-sizing: border-box;
  transition: border-color var(--transition-fast);
}

.search-input:focus {
  border-color: var(--primary);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  background: var(--bg-card);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
  transition: all var(--transition-fast);
}

.history-item:hover {
  box-shadow: var(--shadow-sm);
}

.history-link {
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text);
  text-decoration: none;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-link:hover {
  color: var(--primary);
}

.history-right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.history-time {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.btn-delete {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 1.25rem;
  line-height: 1;
  padding: 0 4px;
  transition: color var(--transition-fast);
}

.btn-delete:hover {
  color: var(--error);
}

/* States */
.loading-state,
.empty-state,
.error-state {
  text-align: center;
  padding: var(--space-16) 0;
  color: var(--text-secondary);
  font-family: var(--font-heading);
}

.error-state {
  color: var(--error);
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  padding: var(--space-8) 0;
}

.pagination button {
  padding: var(--space-2) var(--space-4);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-family: var(--font-heading);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.pagination button:hover:not(:disabled) {
  color: var(--primary);
  border-color: var(--primary);
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .profile-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .profile-stats {
    justify-content: center;
  }

  .article-card {
    flex-direction: column;
  }

  .article-cover {
    width: 100%;
    height: 160px;
  }

  .history-item {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }

  .history-time {
    margin-left: 0;
  }
}
</style>
