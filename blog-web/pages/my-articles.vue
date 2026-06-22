<template>
  <div class="container">
    <div class="page-header">
      <h1 class="page-title">{{ t('myArticles.title') }}</h1>
      <NuxtLink to="/write" class="btn-write">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        {{ t('nav.write') }}
      </NuxtLink>
    </div>

    <!-- Search -->
    <div class="search-bar">
      <input
        v-model="keyword"
        type="text"
        class="search-input"
        :placeholder="t('myArticles.searchPlaceholder')"
        @input="onSearchInput"
      />
    </div>

    <!-- Status Tabs -->
    <div class="status-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-btn"
        :class="{ active: currentStatus === tab.value }"
        @click="switchTab(tab.value)"
      >{{ tab.label }}</button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">{{ t('common.loading') }}</div>

    <!-- Empty -->
    <div v-else-if="!articles.length" class="empty-state">
      <p>{{ t('myArticles.empty') }}</p>
      <NuxtLink to="/write" class="btn-write-inline">{{ t('myArticles.writeFirst') }}</NuxtLink>
    </div>

    <!-- Article List -->
    <div v-else class="article-list">
      <div v-for="article in articles" :key="article.id" class="article-card">
        <div class="article-card-body">
          <div class="article-meta">
            <span class="status-badge" :class="getStatusClass(article)">{{ getStatusLabel(article) }}</span>
            <span v-if="article.publishedAt" class="article-date">{{ formatDate(article.publishedAt) }}</span>
          </div>
          <h2 class="article-title">
            <NuxtLink v-if="article.slug" :to="`/article/${article.slug}`">{{ article.title }}</NuxtLink>
            <span v-else>{{ article.title }}</span>
          </h2>
          <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
          <div class="article-stats">
            <span>{{ article.viewCount }} {{ t('home.views') }}</span>
            <span>{{ article.likeCount }} {{ t('home.likes') }}</span>
            <span>{{ article.commentCount }} {{ t('myArticles.comments') }}</span>
          </div>
        </div>
        <div class="article-actions">
          <NuxtLink :to="`/article/edit/${article.id}`" class="btn-action btn-edit">{{ t('common.edit') }}</NuxtLink>
          <button class="btn-action btn-delete" @click="handleDelete(article.id)">{{ t('common.delete') }}</button>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <Pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      @page-change="loadPage"
    />
  </div>
</template>

<script setup lang="ts">
import type { ArticleWebResponse } from '~/composables/useBlogApi'

definePageMeta({ middleware: ['auth'] })

const { getMyArticles, deleteArticle } = useBlogApi()
const { confirm, alert } = useModal()
const { t } = useI18n()

useHead({ title: t('myArticles.title') })

const tabs = computed(() => [
  { label: t('myArticles.tabAll'), value: '' },
  { label: t('myArticles.tabPublished'), value: 'PUBLISHED' },
  { label: t('myArticles.tabDrafts'), value: 'DRAFT' },
])

const currentStatus = ref('')
const keyword = ref('')
const articles = ref<ArticleWebResponse[]>([])
const loading = ref(true)
const currentPage = ref(1)
const totalPages = ref(1)

let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadArticles()
  }, 300)
}

function getStatusClass(article: ArticleWebResponse) {
  return article.status === 'PUBLISHED' ? 'published' : 'draft'
}

function getStatusLabel(article: ArticleWebResponse) {
  return article.status === 'PUBLISHED' ? t('myArticles.published') : t('myArticles.draft')
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

async function loadArticles() {
  loading.value = true
  try {
    const result = await getMyArticles({
      page: currentPage.value,
      pageSize: 10,
      status: currentStatus.value || undefined,
      keyword: keyword.value || undefined,
    })
    articles.value = result.records
    totalPages.value = Math.ceil(result.total / result.pageSize)
  } catch (e: any) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function switchTab(status: string) {
  currentStatus.value = status
  currentPage.value = 1
  loadArticles()
}

function loadPage(page: number) {
  currentPage.value = page
  loadArticles()
}

async function handleDelete(id: number) {
  const ok = await confirm({
    title: t('myArticles.deleteTitle'),
    message: t('myArticles.deleteConfirm'),
    type: 'warning',
    confirmText: t('common.delete'),
  })
  if (!ok) return
  try {
    await deleteArticle(id)
    loadArticles()
  } catch (e: any) {
    alert({ message: e.message || t('myArticles.deleteFailed'), type: 'error' })
  }
}

onMounted(loadArticles)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-8) 0 var(--space-4);
}

.page-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--text);
}

.btn-write {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-5);
  background: var(--primary);
  color: white;
  border-radius: var(--clay-border-radius);
  font-family: var(--font-heading);
  font-weight: 600;
  text-decoration: none;
  transition: all var(--transition-fast);
}

.btn-write:hover {
  background: var(--primary-dark);
  transform: translateY(-1px);
}

/* Search */
.search-bar {
  margin-bottom: var(--space-4);
}

.search-input {
  width: 100%;
  padding: var(--space-3) var(--space-4);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
  background: var(--bg-card);
  color: var(--text);
  font-size: var(--font-size-sm);
  outline: none;
  transition: border-color var(--transition-fast);
  box-sizing: border-box;
}

.search-input:focus {
  border-color: var(--primary);
}

.search-input::placeholder {
  color: var(--text-muted);
}

/* Tabs */
.status-tabs {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-6);
  border-bottom: var(--clay-border);
  padding-bottom: var(--space-2);
}

.tab-btn {
  padding: var(--space-2) var(--space-4);
  border: none;
  background: none;
  border-radius: var(--clay-border-radius) var(--clay-border-radius) 0 0;
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tab-btn:hover {
  color: var(--primary);
  background: var(--bg-tertiary);
}

.tab-btn.active {
  color: var(--primary);
  background: var(--bg-tertiary);
}

/* States */
.loading-state,
.empty-state {
  text-align: center;
  padding: var(--space-16) 0;
  color: var(--text-secondary);
  font-family: var(--font-heading);
}

.btn-write-inline {
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
}

.btn-write-inline:hover {
  text-decoration: underline;
}

/* Article Cards */
.article-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.article-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.article-card-body {
  flex: 1;
  min-width: 0;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
}

.status-badge {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: var(--font-size-xs);
  font-weight: 600;
  font-family: var(--font-heading);
}

.status-badge.published {
  background: rgba(34, 197, 94, 0.15);
  color: #16a34a;
}

.status-badge.draft {
  background: rgba(234, 179, 8, 0.15);
  color: #ca8a04;
}

.article-date {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.article-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.article-title a {
  color: inherit;
  text-decoration: none;
}

.article-title a:hover {
  color: var(--primary);
}

.article-summary {
  margin: var(--space-2) 0;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-stats {
  display: flex;
  gap: var(--space-4);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

/* Actions */
.article-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  flex-shrink: 0;
}

.btn-action {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--clay-border-radius);
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  text-decoration: none;
  text-align: center;
  border: var(--clay-border);
  background: var(--bg-card);
  color: var(--text-secondary);
}

.btn-edit:hover {
  color: var(--primary);
  border-color: var(--primary);
}

.btn-delete:hover {
  color: var(--error);
  border-color: var(--error);
}

@media (max-width: 768px) {
  .article-card {
    flex-direction: column;
  }

  .article-actions {
    flex-direction: row;
  }
}
</style>
