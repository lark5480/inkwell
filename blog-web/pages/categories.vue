<template>
  <div class="page-wrapper">
    <div class="container categories-page">
      <header class="page-header">
        <h1 class="page-title">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
          All <span class="gradient-text">{{ t('nav.categories') }}</span>
        </h1>
        <p class="page-desc">{{ t('home.catDesc') }}</p>
      </header>

      <!-- Loading -->
      <div v-if="loading" class="categories-skeleton">
        <div v-for="i in 6" :key="i" class="skeleton-card">
          <div class="skeleton" style="width: 56px; height: 56px; border-radius: 14px; margin: 0 auto 12px;"></div>
          <div class="skeleton" style="width: 80px; height: 20px; margin: 0 auto 8px;"></div>
          <div class="skeleton" style="width: 140px; height: 14px; margin: 0 auto 24px;"></div>
          <div class="skeleton" style="width: 90px; height: 16px; margin: 0 auto;"></div>
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="!categories.length" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
        </div>
        <div class="empty-title">{{ t('home.emptyCatTitle') }}</div>
        <div class="empty-desc">{{ t('home.emptyCatDesc') }}</div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        </div>
        <div class="empty-title">{{ t('common.retry') }}</div>
        <div class="empty-desc">{{ error }}</div>
      </div>

      <!-- Categories Grid -->
      <div v-else class="courses-grid">
        <div
          v-for="(cat, index) in categories"
          :key="cat.id"
          class="course-card"
          :class="[`course-card--${index % 6}`]"
        >
          <div class="course-card-icon" v-html="categoryIcons[index % categoryIcons.length]"></div>
          <h3 class="course-card-title">{{ cat.name }}</h3>
          <p v-if="cat.description" class="course-card-desc">{{ cat.description }}</p>
          <p class="course-card-count">{{ cat.articleCount }} {{ cat.articleCount === 1 ? t('common.article') : t('common.articles') }}</p>
          <NuxtLink :to="`/category/${cat.slug}`" class="course-card-link">
            {{ t('home.explore') }}
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
          </NuxtLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CategoryDTO } from '~/composables/useBlogApi'

const { t } = useI18n()

useSeoMeta({
  title: t('nav.categories'),
  description: t('home.catDesc'),
})

const { getCategories } = useBlogApi()

const categories = ref<CategoryDTO[]>([])
const loading = ref(true)
const error = ref('')

try {
  const result = await getCategories()
  categories.value = result || []
} catch (e: any) {
  error.value = e.message || t('common.loading')
  categories.value = []
} finally {
  loading.value = false
}

// ---------- Category Icons (SVG) ----------
const categoryIcons = [
  '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>',
  '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>',
  '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
  '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>',
  '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>',
  '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>',
]
</script>

<style scoped>
.categories-page {
  max-width: 1000px;
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

/* ---------- Grid ---------- */
.courses-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6);
}

/* ---------- Cards ---------- */
.course-card {
  position: relative;
  padding: var(--space-8) var(--space-6);
  border-radius: var(--clay-border-radius);
  border: var(--clay-border);
  text-align: center;
  transition: all var(--transition-base);
  cursor: pointer;
  overflow: hidden;
}

.course-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--bg-card);
  z-index: 0;
}

.course-card:hover {
  transform: translateY(-4px) scale(1.01);
}

/* Color variants */
.course-card--0 { box-shadow: 6px 6px 14px rgba(79, 70, 229, 0.15), -4px -4px 12px rgba(255,255,255,0.7); border-color: #C7D2FE; }
.course-card--0:hover { box-shadow: 8px 8px 20px rgba(79, 70, 229, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }
.course-card--1 { box-shadow: 6px 6px 14px rgba(249, 115, 22, 0.15), -4px -4px 12px rgba(255,255,255,0.7); border-color: #FED7AA; }
.course-card--1:hover { box-shadow: 8px 8px 20px rgba(249, 115, 22, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }
.course-card--2 { box-shadow: 6px 6px 14px rgba(236, 72, 153, 0.15), -4px -4px 12px rgba(255,255,255,0.7); border-color: #FBCFE8; }
.course-card--2:hover { box-shadow: 8px 8px 20px rgba(236, 72, 153, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }
.course-card--3 { box-shadow: 6px 6px 14px rgba(16, 185, 129, 0.15), -4px -4px 12px rgba(255,255,255,0.7); border-color: #A7F3D0; }
.course-card--3:hover { box-shadow: 8px 8px 20px rgba(16, 185, 129, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }
.course-card--4 { box-shadow: 6px 6px 14px rgba(124, 58, 237, 0.15), -4px -4px 12px rgba(255,255,255,0.7); border-color: #DDD6FE; }
.course-card--4:hover { box-shadow: 8px 8px 20px rgba(124, 58, 237, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }
.course-card--5 { box-shadow: 6px 6px 14px rgba(59, 130, 246, 0.15), -4px -4px 12px rgba(255,255,255,0.7); border-color: #BFDBFE; }
.course-card--5:hover { box-shadow: 8px 8px 20px rgba(59, 130, 246, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }

.course-card-icon {
  position: relative; z-index: 1;
  width: 56px; height: 56px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 14px;
  margin: 0 auto var(--space-3);
  color: var(--bg-card);
}

.course-card--0 .course-card-icon { background: linear-gradient(135deg, #4F46E5, #7C3AED); }
.course-card--1 .course-card-icon { background: linear-gradient(135deg, #F97316, #FB923C); }
.course-card--2 .course-card-icon { background: linear-gradient(135deg, #EC4899, #F472B6); }
.course-card--3 .course-card-icon { background: linear-gradient(135deg, #10B981, #34D399); }
.course-card--4 .course-card-icon { background: linear-gradient(135deg, #7C3AED, #A78BFA); }
.course-card--5 .course-card-icon { background: linear-gradient(135deg, #3B82F6, #60A5FA); }

.course-card-title {
  position: relative; z-index: 1;
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--text);
  margin-bottom: var(--space-1);
}

.course-card-desc {
  position: relative; z-index: 1;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-2);
  line-height: 1.5;
}

.course-card-count {
  position: relative; z-index: 1;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-4);
}

.course-card-link {
  position: relative; z-index: 1;
  display: inline-flex; align-items: center;
  gap: var(--space-1);
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--primary);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  text-decoration: none;
}

.course-card-link:hover {
  background: var(--bg-tertiary);
  gap: var(--space-2);
}

/* ---------- Skeleton ---------- */
.categories-skeleton {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6);
}

.skeleton-card {
  padding: var(--space-8);
  border-radius: var(--clay-border-radius);
  border: var(--clay-border);
  text-align: center;
  background: var(--bg-card);
}

/* ---------- States ---------- */
.empty-state {
  text-align: center;
  padding: var(--space-16) 0;
  color: var(--text-secondary);
}

.empty-icon {
  margin-bottom: var(--space-4);
  color: var(--text-muted);
}

.empty-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--text);
  margin-bottom: var(--space-2);
}

.empty-desc {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

/* ---------- Responsive ---------- */
@media (max-width: 1024px) {
  .courses-grid,
  .categories-skeleton {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: var(--font-size-2xl);
  }

  .courses-grid,
  .categories-skeleton {
    grid-template-columns: 1fr;
  }
}
</style>
