<template>
  <div class="page-wrapper">
    <div class="container search-page">
      <header class="page-header">
        <h1 class="page-title">{{ t('search.title') }}</h1>
      </header>

      <!-- Search Input -->
      <div class="search-form">
        <div class="search-input-wrapper">
          <span class="search-input-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </span>
          <input
            ref="searchInput"
            v-model="keyword"
            type="text"
            :placeholder="t('search.placeholder')"
            class="search-input"
            @keydown.enter="doSearch"
          />
          <button
            v-if="keyword"
            class="search-clear"
            @click="clearSearch"
            :aria-label="t('search.clear')"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <button class="btn btn-primary" @click="doSearch" :disabled="!keyword.trim()">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          {{ t('search.search') }}
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="search-status">
        <div v-for="i in 3" :key="i" class="skeleton" style="height: 200px; width: 100%; margin-bottom: 1rem; border-radius: 20px;"></div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="error-state">
        <div class="error-code">:(</div>
        <p class="error-message">{{ t('search.failed') }}</p>
      </div>

      <!-- Empty / No Query -->
      <div v-else-if="!hasSearched" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        </div>
        <p class="empty-title">{{ t('search.emptyTitle') }}</p>
        <p class="empty-desc">{{ t('search.emptyDesc') }}</p>
      </div>

      <!-- No Results (both empty) -->
      <div v-else-if="articles.length === 0 && users.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
        </div>
        <p class="empty-title">{{ t('search.noResultsTitle') }}</p>
        <p class="empty-desc">{{ t('search.noResultsDesc', { q: query }) }}</p>
      </div>

      <!-- Results -->
      <div v-else class="search-results">
        <!-- Tabs -->
        <div class="search-tabs">
          <button
            :class="['tab-btn', { active: activeTab === 'articles' }]"
            @click="activeTab = 'articles'"
          >
            {{ t('search.tabArticles') }}
            <span v-if="total > 0" class="tab-count">{{ total }}</span>
          </button>
          <button
            :class="['tab-btn', { active: activeTab === 'users' }]"
            @click="activeTab = 'users'"
          >
            {{ t('search.tabUsers') }}
            <span v-if="users.length > 0" class="tab-count">{{ users.length }}</span>
          </button>
        </div>

        <!-- Articles Tab -->
        <div v-if="activeTab === 'articles'" class="tab-content">
          <div v-if="articles.length === 0" class="tab-empty">
            <p>{{ t('search.noArticles') }}</p>
          </div>
          <template v-else>
            <p class="results-count">{{ t('search.resultsCount', { total: String(total), q: query }) }}</p>
            <div class="article-list">
              <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
            </div>
            <Pagination
              :current-page="currentPage"
              :total-pages="totalPages"
              @page-change="handlePageChange"
            />
          </template>
        </div>

        <!-- Users Tab -->
        <div v-if="activeTab === 'users'" class="tab-content">
          <div v-if="users.length === 0" class="tab-empty">
            <p>{{ t('search.noUsers') }}</p>
          </div>
          <template v-else>
            <p class="results-count">{{ t('search.usersCount', { total: String(users.length) }) }}</p>
            <div class="user-list">
              <div v-for="u in users" :key="u.id" class="user-card">
                <NuxtLink :to="`/user/${u.id}`" class="user-card-link">
                  <UserAvatar :user="{ id: u.id, nickname: u.nickname, avatar: u.avatar }" :size="48" />
                  <div class="user-info">
                    <div class="user-name">{{ u.nickname || 'Anonymous' }}</div>
                    <div v-if="u.bio" class="user-bio">{{ u.bio }}</div>
                    <div class="user-meta">{{ u.followerCount }} {{ t('user.followers') }}</div>
                  </div>
                </NuxtLink>
                <button
                  v-if="isLoggedIn && currentUser?.id !== u.id"
                  :class="['follow-btn', { followed: followedSet.has(u.id) }]"
                  @click="handleToggleFollow(u)"
                  :disabled="followLoadingSet.has(u.id)"
                >{{ followedSet.has(u.id) ? t('user.following') : t('user.follow') }}</button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ArticleWebResponse, UserSearchResponse } from '~/composables/useBlogApi'
import UserAvatar from '~/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const { searchArticles, searchUsers, toggleFollow, getFollowStatus } = useBlogApi()
const { t } = useI18n()
const { isLoggedIn, currentUser, token } = useAuth()

const searchInput = ref<HTMLInputElement | null>(null)
const keyword = ref('')
const articles = ref<ArticleWebResponse[]>([])
const users = ref<UserSearchResponse[]>([])
const loading = ref(false)
const error = ref(false)
const hasSearched = ref(false)
const currentPage = ref(1)
const activeTab = ref<'articles' | 'users'>('articles')
const pageSize = 10
const total = ref(0)

const followedSet = ref<Set<number>>(new Set())
const followLoadingSet = ref<Set<number>>(new Set())

const query = computed(() => keyword.value)
const totalPages = computed(() => Math.ceil(total.value / pageSize) || 1)

useSeoMeta({
  title: computed(() => (query.value ? `${t('search.title')}: ${query.value}` : t('search.title'))),
  description: computed(() => t('search.seoDesc')),
})

onMounted(async () => {
  if (route.query.q) {
    keyword.value = route.query.q as string
    if (keyword.value.trim()) {
      await doSearch()
    }
  }
  searchInput.value?.focus()
})

async function doSearch() {
  const q = keyword.value.trim()
  if (!q) return

  loading.value = true
  error.value = false
  hasSearched.value = true
  currentPage.value = 1
  articles.value = []
  users.value = []

  router.replace({ query: { q } })

  try {
    const [articleResult, userResult] = await Promise.allSettled([
      searchArticles({ q, page: 1, pageSize }),
      searchUsers(q),
    ])

    if (articleResult.status === 'fulfilled') {
      articles.value = articleResult.value.records
      total.value = articleResult.value.total
    }
    if (userResult.status === 'fulfilled') {
      users.value = userResult.value
    }

    // Auto-switch tab if only users found
    if (articles.value.length === 0 && users.value.length > 0) {
      activeTab.value = 'users'
    } else {
      activeTab.value = 'articles'
    }

    // Load follow status for found users
    if (token.value && users.value.length > 0) {
      loadFollowStatuses()
    }
  } catch (e) {
    console.error('Search failed:', String(e))
    error.value = true
  } finally {
    loading.value = false
  }
}

async function loadFollowStatuses() {
  for (const u of users.value) {
    try {
      const res = await getFollowStatus(u.id)
      if (res.following) {
        followedSet.value.add(u.id)
      }
    } catch {}
  }
}

async function handleToggleFollow(u: UserSearchResponse) {
  if (followLoadingSet.value.has(u.id)) return
  followLoadingSet.value.add(u.id)
  try {
    const res = await toggleFollow(u.id)
    if (res.followed) {
      followedSet.value.add(u.id)
    } else {
      followedSet.value.delete(u.id)
    }
  } catch (e) {
    console.error('Follow toggle failed:', String(e))
  } finally {
    followLoadingSet.value.delete(u.id)
  }
}

async function handlePageChange(page: number) {
  currentPage.value = page
  loading.value = true
  try {
    const q = keyword.value.trim()
    const result = await searchArticles({ q, page, pageSize })
    articles.value = result.records
    total.value = result.total
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (e) {
    console.error('Search pagination failed:', String(e))
  } finally {
    loading.value = false
  }
}

function clearSearch() {
  keyword.value = ''
  articles.value = []
  users.value = []
  hasSearched.value = false
  total.value = 0
  followedSet.value.clear()
  router.replace({ query: {} })
  searchInput.value?.focus()
}
</script>

<style scoped>
.search-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  padding-bottom: var(--space-8);
}

.page-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-4xl);
  font-weight: 800;
  color: var(--text);
  margin-bottom: var(--space-2);
}

.search-form {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-8);
}

.search-input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.search-input-icon {
  position: absolute;
  left: 1rem;
  color: var(--text-muted);
  pointer-events: none;
  display: flex;
}

.search-input {
  width: 100%;
  padding: 0.75rem 2.5rem 0.75rem 3rem;
  border: 3px solid var(--border);
  border-radius: var(--clay-border-radius);
  background: var(--bg-card);
  color: var(--text);
  font-size: var(--font-size-base);
  outline: none;
  transition: all var(--transition-fast);
  box-shadow: var(--clay-shadow);
}

.search-input:focus {
  border-color: var(--primary);
  box-shadow: var(--clay-shadow-hover);
}

.search-input::placeholder {
  color: var(--text-muted);
}

.search-clear {
  position: absolute;
  right: 0.75rem;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: var(--text-muted);
  cursor: pointer;
  background: none;
  border: none;
  transition: all var(--transition-fast);
}

.search-clear:hover {
  background: var(--bg-tertiary);
  color: var(--text);
}

.search-status {
  padding: var(--space-4) 0;
}

.search-tabs {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-6);
  border-bottom: 2px solid var(--border);
  padding-bottom: 0;
}

.tab-btn {
  padding: 0.6rem 1.2rem;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-secondary);
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.tab-btn:hover {
  color: var(--text);
}

.tab-btn.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.tab-count {
  font-size: var(--font-size-xs);
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  font-weight: 500;
}

.tab-btn.active .tab-count {
  background: var(--primary);
  color: white;
}

.tab-empty {
  padding: var(--space-8) 0;
  text-align: center;
  color: var(--text-muted);
}

.results-count {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-4);
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* User cards */
.user-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.user-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--bg-card);
  border-radius: var(--clay-border-radius);
  box-shadow: var(--clay-shadow);
  transition: all var(--transition-fast);
}

.user-card:hover {
  box-shadow: var(--clay-shadow-hover);
}

.user-card-link {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex: 1;
  min-width: 0;
  text-decoration: none;
  color: inherit;
}

.user-avatar {
  flex-shrink: 0;
}

.user-avatar img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  font-weight: 700;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 600;
  color: var(--text);
  font-size: var(--font-size-base);
}

.user-bio {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-top: 0.2rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-meta {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-top: 0.3rem;
}

.follow-btn {
  flex-shrink: 0;
  padding: 0.4rem 1rem;
  font-size: var(--font-size-sm);
  font-weight: 600;
  border-radius: 999px;
  border: 2px solid var(--primary);
  background: var(--primary);
  color: white;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.follow-btn:hover {
  opacity: 0.9;
}

.follow-btn.followed {
  background: transparent;
  color: var(--primary);
}

.follow-btn.followed:hover {
  background: var(--primary);
  color: white;
}

.follow-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .page-title {
    font-size: var(--font-size-2xl);
  }

  .search-form {
    flex-direction: column;
  }

  .user-card {
    padding: var(--space-3);
  }
}
</style>
