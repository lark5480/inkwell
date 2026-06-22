<template>
  <aside class="sidebar">
    <!-- Categories Widget -->
    <div v-if="categories.length > 0" class="widget clay-card">
      <h3 class="widget-title">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
        Categories
      </h3>
      <ul class="category-list">
        <li v-for="cat in categories" :key="cat.id" class="category-item">
          <NuxtLink :to="`/category/${cat.slug}`" class="category-link">
            <span>{{ cat.name }}</span>
            <span class="category-count">{{ cat.articleCount }}</span>
          </NuxtLink>
        </li>
      </ul>
    </div>

    <!-- Tags Widget -->
    <div v-if="tags.length > 0" class="widget clay-card">
      <h3 class="widget-title">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="4.9" y1="4.9" x2="14.1" y2="14.1"/><circle cx="6" cy="6" r="3"/><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/></svg>
        Tags
      </h3>
      <div class="tag-cloud">
        <NuxtLink
          v-for="tag in tags"
          :key="tag.id"
          :to="`/tag/${tag.slug}`"
          class="sidebar-tag"
          :style="{ fontSize: getTagFontSize(tag.articleCount) }"
        >
          {{ tag.name }}
        </NuxtLink>
      </div>
    </div>

    <!-- Latest Articles Widget -->
    <div v-if="latestArticles.length > 0" class="widget clay-card">
      <h3 class="widget-title">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
        Latest
      </h3>
      <ul class="latest-list">
        <li v-for="article in latestArticles" :key="article.id" class="latest-item">
          <NuxtLink :to="`/article/${article.slug}`" class="latest-link">
            <span class="latest-title">{{ article.title }}</span>
            <time class="latest-date">{{ formatDate(article.publishedAt) }}</time>
          </NuxtLink>
        </li>
      </ul>
    </div>
  </aside>
</template>

<script setup lang="ts">
import type { CategoryDTO, TagDTO, ArticleWebResponse } from '~/composables/useBlogApi'

const { getCategories, getTags, getArticles } = useBlogApi()
const { locale } = useI18n()

const categories = ref<CategoryDTO[]>([])
const tags = ref<TagDTO[]>([])
const latestArticles = ref<ArticleWebResponse[]>([])

const { data: catData } = await useAsyncData('sidebar-categories', () => getCategories())
const { data: tagData } = await useAsyncData('sidebar-tags', () => getTags())
const { data: latestData } = await useAsyncData('sidebar-latest', () =>
  getArticles({ page: 1, pageSize: 5 })
)

if (catData.value) categories.value = catData.value
if (tagData.value) tags.value = tagData.value
if (latestData.value) latestArticles.value = latestData.value.records

function getTagFontSize(count: number): string {
  const min = 0.75
  const max = 1.125
  const maxCount = Math.max(...tags.value.map((t) => t.articleCount), 1)
  const size = min + ((count - 1) / (maxCount - 1 || 1)) * (max - min)
  return `${size}rem`
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString(locale.value, { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.widget {
  padding: var(--space-5);
}

.widget-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 700;
  color: var(--text);
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-2);
  border-bottom: 3px solid var(--primary);
}

.widget-title svg {
  color: var(--primary);
  flex-shrink: 0;
}

/* Categories */
.category-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.category-link {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-lg);
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  text-decoration: none;
}

.category-link:hover {
  background: var(--bg-tertiary);
  color: var(--primary);
}

.category-count {
  background: var(--bg-tertiary);
  padding: 0.125rem 0.5rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--primary);
}

/* Tags */
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.sidebar-tag {
  display: inline-block;
  padding: 0.2rem 0.625rem;
  border-radius: 999px;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--bg-tertiary);
  transition: all var(--transition-fast);
  text-decoration: none;
}

.sidebar-tag:hover {
  background: var(--primary);
  color: var(--text-inverse);
}

/* Latest */
.latest-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.latest-item {
  padding-bottom: var(--space-3);
  border-bottom: 2px solid var(--border-light);
}

.latest-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.latest-link {
  text-decoration: none;
  display: block;
}

.latest-title {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text);
  margin-bottom: var(--space-1);
  transition: color var(--transition-fast);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.latest-link:hover .latest-title {
  color: var(--primary);
}

.latest-date {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}
</style>
