<template>
  <div class="page-wrapper">
    <div class="container archive-page">
      <header class="page-header">
        <h1 class="page-title">{{ t('nav.archive') }}</h1>
        <p class="page-desc">{{ t('home.blogDesc') }}</p>
      </header>

      <!-- Loading -->
      <div v-if="loading" class="loading-state">
        <div v-for="i in 4" :key="i" class="skeleton" style="height: 60px; width: 100%; margin-bottom: 1rem; border-radius: 12px;"></div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="error-state">
        <div class="error-code">:(</div>
        <p class="error-message">{{ t('comment.loadFailed') }}</p>
        <button class="btn btn-primary" @click="fetchArchives">{{ t('common.retry') }}</button>
      </div>

      <!-- Empty -->
      <div v-else-if="archives.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
        </div>
        <p class="empty-title">{{ t('home.emptyArtTitle') }}</p>
        <p class="empty-desc">{{ t('home.emptyArtDesc') }}</p>
      </div>

      <!-- Archive Timeline -->
      <div v-else class="archive-timeline">
        <div v-for="yearGroup in groupedArchives" :key="yearGroup.year" class="archive-year">
          <h2 class="year-title">{{ yearGroup.year }}</h2>
          <div v-for="monthGroup in yearGroup.months" :key="monthGroup.month" class="archive-month">
            <h3 class="month-title">{{ monthGroup.monthName }}</h3>
            <ul class="archive-list">
              <li v-for="article in monthGroup.articles" :key="article.id" class="archive-item">
                <time class="archive-day">{{ getDay(article.publishedAt) }}</time>
                <NuxtLink :to="`/article/${article.slug}`" class="archive-link">
                  {{ article.title }}
                </NuxtLink>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ArchiveItem, ArchiveArticle } from '~/composables/useBlogApi'

const { t } = useI18n()

useSeoMeta({
  title: t('nav.archive'),
  description: t('home.blogDesc'),
})

const { getArchives } = useBlogApi()

const archives = ref<ArchiveItem[]>([])
const loading = ref(true)
const error = ref(false)

interface MonthGroup {
  month: number
  monthName: string
  articles: ArchiveArticle[]
}

interface YearGroup {
  year: number
  months: MonthGroup[]
}

const groupedArchives = computed(() => {
  const years: Record<number, Record<number, ArchiveArticle[]>> = {}

  for (const item of archives.value) {
    const [yearStr, monthStr] = item.yearMonth.split('-')
    const year = parseInt(yearStr)
    const month = parseInt(monthStr)

    if (!years[year]) years[year] = {}
    if (!years[year][month]) years[year][month] = []
    years[year][month].push(...item.articles)
  }

  const result: YearGroup[] = []
  const sortedYears = Object.keys(years)
    .map(Number)
    .sort((a, b) => b - a)

  for (const year of sortedYears) {
    const months: MonthGroup[] = Object.keys(years[year])
      .map(Number)
      .sort((a, b) => b - a)
      .map((month) => ({
        month,
        monthName: getMonthName(month),
        articles: years[year][month].sort(
          (a, b) => new Date(b.publishedAt).getTime() - new Date(a.publishedAt).getTime()
        ),
      }))
    result.push({ year, months })
  }

  return result
})

function getMonthName(month: number): string {
  const date = new Date()
  date.setMonth(month - 1)
  const { locale } = useI18n()
  return date.toLocaleDateString(locale.value, { month: 'long' })
}

function getDay(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).getDate().toString().padStart(2, '0')
}

async function fetchArchives() {
  loading.value = true
  error.value = false
  try {
    const data = await getArchives()
    archives.value = data || []
  } catch (e) {
    console.error('Failed to fetch archives:', String(e))
    error.value = true
  } finally {
    loading.value = false
  }
}

await fetchArchives()
</script>

<style scoped>
.archive-page {
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
  padding: var(--space-8) 0;
}

.archive-year {
  margin-bottom: var(--space-10);
}

.year-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-3xl);
  font-weight: 800;
  color: var(--primary);
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-2);
  border-bottom: 3px solid var(--primary);
}

.archive-month {
  margin-bottom: var(--space-6);
  padding-left: var(--space-4);
}

.month-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--text);
  margin-bottom: var(--space-3);
}

.archive-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.archive-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
}

.archive-item:hover {
  background: var(--bg-tertiary);
}

.archive-day {
  flex-shrink: 0;
  width: 28px;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-muted);
  text-align: right;
}

.archive-link {
  font-size: var(--font-size-base);
  color: var(--text);
  text-decoration: none;
  font-weight: 500;
  transition: color var(--transition-fast);
}

.archive-link:hover {
  color: var(--primary);
}

@media (max-width: 768px) {
  .page-title {
    font-size: var(--font-size-2xl);
  }

  .year-title {
    font-size: var(--font-size-2xl);
  }

  .archive-month {
    padding-left: var(--space-2);
  }

  .archive-item {
    padding: var(--space-2);
  }
}
</style>
