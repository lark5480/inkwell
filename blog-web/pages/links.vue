<template>
  <div class="page-wrapper">
    <div class="container links-page">
      <header class="page-header">
        <h1 class="page-title">{{ t('nav.links') }}</h1>
        <p class="page-desc">{{ t('home.blogDesc') }}</p>
      </header>

      <!-- Loading -->
      <div v-if="loading" class="loading-state">
        <div class="links-grid">
          <div v-for="i in 6" :key="i" class="skeleton" style="height: 80px; border-radius: 16px;"></div>
        </div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="error-state">
        <div class="error-code">:(</div>
        <p class="error-message">{{ t('comment.loadFailed') }}</p>
        <button class="btn btn-primary" @click="fetchLinks">{{ t('common.retry') }}</button>
      </div>

      <!-- Empty -->
      <div v-else-if="links.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
        </div>
        <p class="empty-title">{{ t('home.emptyArtTitle') }}</p>
        <p class="empty-desc">{{ t('home.emptyArtDesc') }}</p>
      </div>

      <!-- Links Grid -->
      <div v-else class="links-grid">
        <a
          v-for="link in links"
          :key="link.id"
          :href="link.url"
          target="_blank"
          rel="noopener noreferrer"
          class="link-card clay-card"
        >
          <img
            v-if="link.avatar"
            :src="link.avatar"
            :alt="link.name"
            class="link-avatar"
            loading="lazy"
          />
          <div v-else class="link-avatar-placeholder">{{ link.name.charAt(0) }}</div>
          <div class="link-info">
            <h3 class="link-name">{{ link.name }}</h3>
            <p class="link-desc">{{ link.description }}</p>
          </div>
        </a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LinkDTO } from '~/composables/useBlogApi'

const { t } = useI18n()

useSeoMeta({
  title: t('nav.links'),
  description: t('home.blogDesc'),
})

const { getLinks } = useBlogApi()

const links = ref<LinkDTO[]>([])
const loading = ref(true)
const error = ref(false)

async function fetchLinks() {
  loading.value = true
  error.value = false
  try {
    const data = await getLinks()
    links.value = data || []
  } catch (e) {
    console.error('Failed to fetch links:', String(e))
    error.value = true
  } finally {
    loading.value = false
  }
}

await fetchLinks()
</script>

<style scoped>
.links-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  padding-bottom: var(--space-8);
  margin-bottom: var(--space-8);
  border-bottom: 3px solid var(--border);
}

.page-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-4xl);
  font-weight: 800;
  color: var(--text);
  margin-bottom: var(--space-2);
}

.page-desc {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
}

.loading-state {
  padding: var(--space-4) 0;
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-4);
}

.link-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5);
  text-decoration: none;
  color: inherit;
  transition: all var(--transition-base);
}

.link-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--clay-shadow-hover);
}

.link-avatar {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  object-fit: cover;
  flex-shrink: 0;
}

.link-avatar-placeholder {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--primary), var(--secondary));
  color: var(--text-inverse);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;
  flex-shrink: 0;
}

.link-info {
  flex: 1;
  min-width: 0;
}

.link-name {
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 700;
  color: var(--text);
  margin-bottom: var(--space-1);
}

.link-desc {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 768px) {
  .page-title {
    font-size: var(--font-size-2xl);
  }

  .links-grid {
    grid-template-columns: 1fr;
  }
}
</style>
