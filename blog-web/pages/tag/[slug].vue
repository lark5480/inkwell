<template>
  <div class="page-wrapper">
    <div class="container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <div class="skeleton" style="height: 36px; width: 200px; border-radius: 8px; margin-bottom: 0.5rem;"></div>
        <div class="skeleton" style="height: 16px; width: 400px; border-radius: 8px; margin-bottom: 2rem;"></div>
        <div v-for="i in 3" :key="i" class="skeleton" style="height: 200px; width: 100%; border-radius: 20px; margin-bottom: 1rem;"></div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="error-state">
        <div class="error-code">:(</div>
        <p class="error-message">Failed to load articles for this tag.</p>
        <NuxtLink to="/" class="btn btn-primary">Back to Home</NuxtLink>
      </div>

      <!-- Empty State -->
      <div v-else-if="articles.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><line x1="4.9" y1="4.9" x2="14.1" y2="14.1"/><circle cx="6" cy="6" r="3"/><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/></svg>
        </div>
        <p class="empty-title">No articles with this tag</p>
        <p class="empty-desc">Check back later or browse other tags.</p>
        <NuxtLink to="/" class="btn btn-primary" style="margin-top: 1rem;">Back to Home</NuxtLink>
      </div>

      <!-- Content -->
      <template v-else>
        <header class="tag-header">
          <h1 class="tag-name">#{{ tagName }}</h1>
          <p class="tag-count">{{ total }} article{{ total !== 1 ? 's' : '' }}</p>
        </header>

        <div class="content-layout">
          <div class="main-content">
            <div class="article-list">
              <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
              <Pagination
                :current-page="currentPage"
                :total-pages="totalPages"
                @page-change="handlePageChange"
              />
            </div>
          </div>
          <Sidebar />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ArticleWebResponse } from '~/composables/useBlogApi'

const route = useRoute()
const slug = route.params.slug as string

const { getArticlesByTag } = useBlogApi()

const articles = ref<ArticleWebResponse[]>([])
const tagName = ref(slug)
const loading = ref(true)
const error = ref(false)
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)

const totalPages = computed(() => Math.ceil(total.value / pageSize) || 1)

useSeoMeta({
  title: computed(() => `#${tagName.value} Tag`),
  description: computed(() => `Browse all articles tagged with "${tagName.value}".`),
})

async function fetchArticles() {
  loading.value = true
  error.value = false
  try {
    const result = await getArticlesByTag(slug, { page: currentPage.value, pageSize })
    articles.value = result.records
    total.value = result.total
    if (result.records.length > 0) {
      const matchingTag = result.records[0].tags?.find(
        (t) => t.slug.toLowerCase() === slug.toLowerCase()
      )
      if (matchingTag) tagName.value = matchingTag.name
    }
  } catch (e) {
    console.error('Failed to fetch tag articles:', String(e))
    error.value = true
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  window.scrollTo({ top: 0, behavior: 'smooth' })
  fetchArticles()
}

await fetchArticles()
</script>

<style scoped>
.loading-state {
  padding: var(--space-8) 0;
}

.tag-header {
  padding: var(--space-8) 0 var(--space-6);
  display: flex;
  align-items: baseline;
  gap: var(--space-3);
}

.tag-name {
  font-family: var(--font-heading);
  font-size: var(--font-size-3xl);
  font-weight: 800;
  color: var(--primary);
}

.tag-count {
  font-size: var(--font-size-base);
  color: var(--text-muted);
}

.content-layout {
  display: grid;
  grid-template-columns: 1fr var(--sidebar-width);
  gap: var(--space-8);
  align-items: start;
}

.main-content {
  min-width: 0;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

@media (max-width: 1024px) {
  .content-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .tag-name {
    font-size: var(--font-size-2xl);
  }
}
</style>
