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
        <p class="error-message">Failed to load this category.</p>
        <NuxtLink to="/" class="btn btn-primary">Back to Home</NuxtLink>
      </div>

      <!-- Empty State -->
      <div v-else-if="articles.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
        </div>
        <p class="empty-title">No articles in this category</p>
        <p class="empty-desc">Check back later or browse other categories.</p>
        <NuxtLink to="/" class="btn btn-primary" style="margin-top: 1rem;">Back to Home</NuxtLink>
      </div>

      <!-- Content -->
      <template v-else>
        <header class="category-header">
          <h1 class="category-name">{{ categoryName }}</h1>
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

const { getArticlesByCategory } = useBlogApi()

const articles = ref<ArticleWebResponse[]>([])
const categoryName = ref(slug)
const loading = ref(true)
const error = ref(false)
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)

const totalPages = computed(() => Math.ceil(total.value / pageSize) || 1)

useSeoMeta({
  title: computed(() => `${categoryName.value || slug} Category`),
  description: computed(() => `Browse all articles in the ${categoryName.value || slug} category.`),
})

async function fetchArticles() {
  loading.value = true
  error.value = false
  try {
    const result = await getArticlesByCategory(slug, { page: currentPage.value, pageSize })
    articles.value = result.records
    total.value = result.total
    if (result.records.length > 0 && result.records[0].categoryName) {
      categoryName.value = result.records[0].categoryName
    } else {
      categoryName.value = slug
    }
  } catch (e) {
    console.error('Failed to fetch category articles:', String(e))
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

.category-header {
  padding: var(--space-8) 0 var(--space-6);
}

.category-name {
  font-family: var(--font-heading);
  font-size: var(--font-size-3xl);
  font-weight: 800;
  color: var(--text);
  text-transform: capitalize;
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
  .category-name {
    font-size: var(--font-size-2xl);
  }
}
</style>
