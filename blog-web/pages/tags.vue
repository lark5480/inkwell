<template>
  <div class="page-wrapper">
    <div class="container tags-page">
      <header class="page-header">
        <h1 class="page-title">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
          All <span class="gradient-text">{{ t('nav.tags') }}</span>
        </h1>
        <p class="page-desc">{{ t('home.blogDesc') }}</p>
      </header>

      <!-- Loading -->
      <div v-if="loading" class="tags-skeleton">
        <div v-for="i in 12" :key="i" class="skeleton" style="height: 36px; border-radius: 999px; width: 100px;"></div>
      </div>

      <!-- Empty -->
      <div v-else-if="tags.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
        </div>
        <div class="empty-title">{{ t('home.emptyArtTitle') }}</div>
        <div class="empty-desc">{{ t('home.emptyArtDesc') }}</div>
      </div>

      <!-- Tags Cloud -->
      <div v-else class="tags-cloud">
        <NuxtLink
          v-for="tag in tags"
          :key="tag.slug"
          :to="`/tag/${tag.slug}`"
          class="tag-chip"
          :style="{ fontSize: tagSize(tag.articleCount) }"
        >
          <span class="tag-hash">#</span>{{ tag.name }}
          <span class="tag-count">{{ tag.articleCount }}</span>
        </NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TagDTO } from '~/composables/useBlogApi'

const { t } = useI18n()

useSeoMeta({
  title: t('nav.tags'),
  description: t('home.blogDesc'),
})

const { getTags } = useBlogApi()

const tags = ref<TagDTO[]>([])
const loading = ref(true)

try {
  tags.value = await getTags() || []
} catch {
  tags.value = []
} finally {
  loading.value = false
}

function tagSize(count: number): string {
  const max = Math.max(...tags.value.map(t => t.articleCount), 1)
  const min = 0.85
  const maxRem = 1.6
  const scale = min + (count / max) * (maxRem - min)
  return `${scale}rem`
}
</script>

<style scoped>
.tags-page {
  max-width: 800px;
  margin: 0 auto;
  padding-top: var(--space-8);
}

.page-header {
  margin-bottom: var(--space-10);
}

.page-title {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--font-size-4xl);
  font-weight: 800;
  color: var(--text);
  margin-bottom: var(--space-3);
}

.page-title svg {
  color: var(--primary);
}

.page-desc {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
}

.tags-skeleton {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0.4rem 1rem;
  border-radius: 999px;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--bg-card);
  border: var(--clay-border);
  box-shadow: var(--clay-shadow);
  transition: all var(--transition-fast);
  text-decoration: none;
  line-height: 1.4;
}

.tag-chip:hover {
  color: var(--primary);
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: var(--clay-shadow-hover);
}

.tag-hash {
  color: var(--text-muted);
  font-weight: 400;
}

.tag-count {
  font-size: 0.7em;
  color: var(--text-muted);
  background: var(--bg-tertiary);
  padding: 0.1rem 0.4rem;
  border-radius: 999px;
  font-weight: 700;
}

@media (max-width: 768px) {
  .page-title {
    font-size: var(--font-size-2xl);
  }
}
</style>
