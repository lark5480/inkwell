<template>
  <div class="page-wrapper">
    <div class="container about-page">
      <header class="page-header">
        <h1 class="page-title">{{ t('nav.about') }}</h1>
      </header>

      <!-- Loading -->
      <div v-if="loading" class="loading-state">
        <div class="skeleton" style="height: 16px; width: 80%; margin-bottom: 1rem; border-radius: 8px;" v-for="i in 8" :key="i"></div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="error-state">
        <div class="error-code">:(</div>
        <p class="error-message">{{ t('comment.loadFailed') }}</p>
        <button class="btn btn-primary" @click="fetchAbout">{{ t('common.retry') }}</button>
      </div>

      <!-- Empty / No about content -->
      <div v-else-if="!content" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </div>
        <p class="empty-title">{{ t('home.emptyArtTitle') }}</p>
        <p class="empty-desc">{{ t('home.emptyArtDesc') }}</p>
      </div>

      <!-- About Content -->
      <div v-else class="about-content article-content" v-html="content"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
const { t } = useI18n()

useSeoMeta({
  title: t('nav.about'),
  description: t('home.blogDesc'),
})

const { getSiteInfo } = useBlogApi()

const content = ref('')
const loading = ref(true)
const error = ref(false)

async function fetchAbout() {
  loading.value = true
  error.value = false
  try {
    const info = await getSiteInfo()
    content.value = info?.aboutContent || ''
  } catch (e) {
    console.error('Failed to load about page:', String(e))
    error.value = true
  } finally {
    loading.value = false
  }
}

await fetchAbout()
</script>

<style scoped>
.about-page {
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
}

.loading-state {
  padding: var(--space-4) 0;
}

@media (max-width: 768px) {
  .page-title {
    font-size: var(--font-size-2xl);
  }
}
</style>
