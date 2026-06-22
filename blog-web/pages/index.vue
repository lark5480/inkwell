<template>
  <div class="landing-page">
    <!-- ============================================ -->
    <!-- 1. Hero Section (compact) -->
    <!-- ============================================ -->
    <section class="hero-section">
      <div class="container hero-content">
        <div class="hero-badge">
          <svg class="hero-badge-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/><path d="M2 2l7.586 7.586"/><circle cx="11" cy="11" r="2"/></svg>
          {{ t('home.badge') }}
        </div>
        <h1 class="hero-title">{{ siteTitle }}</h1>
        <p class="hero-subtitle">{{ siteDescription }}</p>
        <div class="hero-stats-inline">
          <span>{{ siteInfo.articleCount }} {{ t('home.statArticles') }}</span>
          <span class="hero-dot">·</span>
          <span>{{ siteInfo.categoryCount }} {{ t('home.statCategories') }}</span>
          <span class="hero-dot">·</span>
          <span>{{ siteInfo.tagCount }} {{ t('home.statTags') }}</span>
        </div>
      </div>
    </section>

    <!-- ============================================ -->
    <!-- 2. Featured Articles -->
    <!-- ============================================ -->
    <section v-if="featured.length > 0" class="featured-section">
      <div class="container">
        <div class="section-header">
          <span class="section-label">{{ t('home.featuredLabel') }}</span>
          <h2 class="section-title">{{ t('home.featuredTitlePrefix') }}<span class="gradient-text">{{ t('home.featuredTitleHighlight') }}</span>{{ t('home.featuredTitleSuffix') }}</h2>
          <p class="section-desc">{{ t('home.featuredDesc') }}</p>
        </div>

        <div class="featured-grid">
          <NuxtLink
            v-for="article in featured"
            :key="article.id"
            :to="`/article/${article.slug}`"
            class="featured-card clay-card"
          >
            <div v-if="article.coverImage" class="featured-cover">
              <img :src="resolve(article.coverImage)" :alt="article.title" loading="lazy" />
            </div>
            <div class="featured-body">
              <span v-if="article.categoryName" class="featured-cat">{{ article.categoryName }}</span>
              <h3 class="featured-title">{{ article.title }}</h3>
              <p v-if="article.summary" class="featured-summary">{{ article.summary }}</p>
              <div class="featured-meta">
                <span>{{ article.viewCount }} {{ t('home.views') }}</span>
                <span class="hero-dot">·</span>
                <span>{{ article.likeCount }} {{ t('home.likes') }}</span>
              </div>
            </div>
          </NuxtLink>
          <!-- 导航卡片：浏览全部文章 -->
          <NuxtLink to="/archive" class="featured-card clay-card">
            <div class="featured-cover featured-cover--nav">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
            </div>
            <div class="featured-body">
              <span class="featured-cat">{{ t('home.explore') }}</span>
              <h3 class="featured-title">{{ t('home.viewAll') }}</h3>
              <p class="featured-summary">{{ t('home.emptyCatDesc') }}</p>
            </div>
          </NuxtLink>
          <!-- 导航卡片：关于我 -->
          <NuxtLink to="/about" class="featured-card clay-card">
            <div class="featured-cover featured-cover--about">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <div class="featured-body">
              <span class="featured-cat">ABOUT</span>
              <h3 class="featured-title">{{ t('home.ctaAbout') }}</h3>
              <p class="featured-summary">{{ t('home.ctaDesc') }}</p>
            </div>
          </NuxtLink>
        </div>
      </div>
    </section>

    <!-- ============================================ -->
    <!-- 3. Latest Articles -->
    <!-- ============================================ -->
    <section id="articles" class="blog-section">
      <div class="container">
        <div class="section-header">
          <span class="section-label">{{ t('home.blogLabel') }}</span>
          <h2 class="section-title">{{ t('home.blogTitlePrefix') }}<span class="gradient-text">{{ t('home.blogTitleHighlight') }}</span>{{ t('home.blogTitleSuffix') }}</h2>
          <p class="section-desc">{{ t('home.blogDesc') }}</p>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="article-grid">
          <div v-for="i in 3" :key="i" class="skeleton-card clay-card">
            <div class="skeleton" style="height: 180px; width: 100%; border-radius: 17px 17px 0 0;"></div>
            <div class="skeleton-card-body">
              <div class="skeleton" style="height: 14px; width: 40%; margin-bottom: 12px;"></div>
              <div class="skeleton" style="height: 24px; width: 80%; margin-bottom: 8px;"></div>
              <div class="skeleton" style="height: 16px; width: 100%; margin-bottom: 4px;"></div>
              <div class="skeleton" style="height: 16px; width: 70%;"></div>
            </div>
          </div>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="error-state">
          <div class="error-code">:(</div>
          <p class="error-message">{{ t('home.errorMsg') }}</p>
          <button class="btn btn-primary" @click="fetchArticles">{{ t('common.retry') }}</button>
        </div>

        <!-- Empty State -->
        <div v-else-if="articles.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/></svg>
          </div>
          <div class="empty-title">{{ t('home.emptyArtTitle') }}</div>
          <div class="empty-desc">{{ t('home.emptyArtDesc') }}</div>
        </div>

        <!-- Articles Grid -->
        <div v-else class="article-grid">
          <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
          <!-- 标签云：填充空位 -->
          <div v-if="sidebarTags.length > 0" class="grid-tagcloud">
            <h4 class="grid-tagcloud-title">{{ t('nav.tags') }}</h4>
            <div class="grid-tagcloud-tags">
              <NuxtLink
                v-for="tag in sidebarTags"
                :key="tag.slug"
                :to="`/tag/${tag.slug}`"
                class="grid-tagchip"
              >#{{ tag.name }}</NuxtLink>
            </div>
          </div>
        </div>

        <!-- View All -->
        <div v-if="articles.length > 0" class="view-all-wrap">
          <NuxtLink to="/archive" class="btn btn-outline btn-lg view-all-btn">
            {{ t('home.viewAll') }}
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
          </NuxtLink>
        </div>
      </div>
    </section>

    <!-- ============================================ -->
    <!-- 4. Categories -->
    <!-- ============================================ -->
    <section id="categories" class="categories-section">
      <div class="container">
        <div class="section-header">
          <span class="section-label">{{ t('home.catLabel') }}</span>
          <h2 class="section-title">{{ t('home.catTitlePrefix') }}<span class="gradient-text">{{ t('home.catTitleHighlight') }}</span>{{ t('home.catTitleSuffix') }}</h2>
          <p class="section-desc">{{ t('home.catDesc') }}</p>
        </div>

        <!-- Loading State -->
        <div v-if="categoriesLoading" class="categories-skeleton">
          <div v-for="i in 6" :key="i" class="skeleton-category">
            <div class="skeleton" style="width: 48px; height: 48px; border-radius: 14px; margin-bottom: 12px;"></div>
            <div class="skeleton" style="width: 80px; height: 18px; margin: 0 auto;"></div>
          </div>
        </div>

        <!-- Categories Grid -->
        <div v-else-if="categories.length > 0" class="courses-grid">
          <div
            v-for="(cat, index) in categories"
            :key="cat.id"
            class="course-card"
            :class="[`course-card--${index % 6}`]"
          >
            <div class="course-card-icon" v-html="categoryIcons[index % categoryIcons.length]"></div>
            <h3 class="course-card-title">{{ cat.name }}</h3>
            <p class="course-card-count">{{ cat.articleCount }} {{ cat.articleCount === 1 ? t('common.article') : t('common.articles') }}</p>
            <NuxtLink :to="`/category/${cat.slug}`" class="course-card-link">
              {{ t('home.explore') }}
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            </NuxtLink>
          </div>
        </div>

        <!-- Empty State -->
        <div v-else class="empty-state">
          <div class="empty-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
          </div>
          <div class="empty-title">{{ t('home.emptyCatTitle') }}</div>
          <div class="empty-desc">{{ t('home.emptyCatDesc') }}</div>
        </div>
      </div>
    </section>

    <!-- ============================================ -->
    <!-- 5. CTA Section -->
    <!-- ============================================ -->
    <section class="cta-section">
      <div class="cta-bg-shapes">
        <div class="shape shape-4"></div>
        <div class="shape shape-5"></div>
      </div>
      <div class="container">
        <div class="cta-card">
          <h2 class="cta-title">{{ t('home.ctaTitle') }}</h2>
          <p class="cta-desc">{{ t('home.ctaDesc') }}</p>
          <div class="cta-actions">
            <NuxtLink to="/about" class="btn btn-accent btn-lg cta-btn">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </NuxtLink>

          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { ArticleWebResponse, TagDTO } from '~/composables/useBlogApi'

const { getArticles, getFeaturedArticles, getCategories, getTags, getSiteInfo } = useBlogApi()
const { t, locale, toggleLocale } = useI18n()
const { resolve } = useMedia()

// ---------- Site Info ----------
const siteTitle = ref('Inkwell')
const siteDescription = ref('Sharing thoughts, tutorials, and experiences.')
const siteInfo = ref({ articleCount: 0, categoryCount: 0, tagCount: 0 })

async function fetchSiteInfo() {
  try {
    const info = await getSiteInfo()
    siteTitle.value = info.siteTitle || 'Inkwell'
    siteDescription.value = info.siteDescription || t('home.subtitleFallback')
    siteInfo.value = {
      articleCount: info.articleCount ?? 0,
      categoryCount: info.categoryCount ?? 0,
      tagCount: info.tagCount ?? 0,
    }
  } catch (e) {
    console.error('Failed to fetch site info:', String(e))
  }
}

// ---------- Articles ----------
const articles = ref<ArticleWebResponse[]>([])
const featured = ref<ArticleWebResponse[]>([])
const loading = ref(true)
const error = ref(false)
const featuredLoading = ref(true)

// 精选文章：后端按手动精选 + 热度排序返回，最多 6 篇
async function fetchFeatured() {
  featuredLoading.value = true
  try {
    featured.value = await getFeaturedArticles()
  } catch {
    featured.value = []
  } finally {
    featuredLoading.value = false
  }
}

async function fetchArticles() {
  loading.value = true
  error.value = false
  try {
    const result = await getArticles({ page: 1, pageSize: 10 })
    articles.value = result.records
  } catch (e) {
    console.error('Failed to fetch articles:', String(e))
    error.value = true
  } finally {
    loading.value = false
  }
}

// ---------- Tags (for tag cloud) ----------
const sidebarTags = ref<TagDTO[]>([])

async function fetchSidebarTags() {
  try {
    const result = await getTags()
    sidebarTags.value = (result || []).slice(0, 10)
  } catch {
    sidebarTags.value = []
  }
}

// ---------- Categories ----------
const categories = ref<any[]>([])
const categoriesLoading = ref(true)

async function fetchCategories() {
  categoriesLoading.value = true
  try {
    const result = await getCategories()
    categories.value = result || []
  } catch {
    categories.value = []
  } finally {
    categoriesLoading.value = false
  }
}

await Promise.all([fetchFeatured(), fetchArticles(), fetchCategories(), fetchSiteInfo(), fetchSidebarTags()])

useSeoMeta({
  title: 'Home',
  ogTitle: siteTitle.value,
  description: siteDescription.value,
  ogDescription: siteDescription.value,
})

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
/* =============================================
   General Section Styles
   ============================================= */
.landing-page {
  overflow: hidden;
}

.section-header {
  text-align: center;
  max-width: 650px;
  margin: 0 auto var(--space-12);
}

.section-label {
  display: inline-block;
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--primary);
  background: var(--bg-tertiary);
  padding: 0.25rem 1rem;
  border-radius: 999px;
  margin-bottom: var(--space-3);
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.section-title {
  font-size: var(--font-size-4xl);
  font-weight: 800;
  color: var(--text);
  margin-bottom: var(--space-3);
}

.section-desc {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  line-height: 1.7;
}

/* =============================================
   1. Hero Section (compact)
   ============================================= */
.hero-section {
  position: relative;
  padding: var(--space-16) 0 var(--space-10);
  background: var(--bg-secondary);
  overflow: hidden;
}

@media (prefers-color-scheme: dark) {
  :root:not(.light) .hero-section {
    background: linear-gradient(180deg, #0F0A2E 0%, #1E1245 100%);
  }
}

:global(html.dark) .hero-section {
  background: linear-gradient(180deg, #0F0A2E 0%, #1E1245 100%);
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 700px;
  margin: 0 auto;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--primary);
  background: var(--bg-card);
  border: var(--clay-border);
  padding: 0.4rem 1rem;
  border-radius: 999px;
  box-shadow: var(--clay-shadow);
  margin-bottom: var(--space-4);
}

.hero-badge-icon {
  width: 16px;
  height: 16px;
  color: var(--primary);
}

.hero-title {
  font-size: 2.75rem;
  font-weight: 800;
  line-height: 1.15;
  color: var(--text);
  margin-bottom: var(--space-3);
  letter-spacing: -0.02em;
}

.hero-subtitle {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  line-height: 1.6;
  max-width: 540px;
  margin: 0 auto var(--space-4);
}

.hero-stats-inline {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  font-family: var(--font-heading);
  font-weight: 500;
}

.hero-dot {
  color: var(--text-muted);
  opacity: 0.5;
}

/* =============================================
   2. Featured Section
   ============================================= */
.featured-section {
  padding: var(--space-12) 0;
  background: var(--bg);
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-5);
}

.featured-card {
  overflow: hidden;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.featured-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--clay-shadow-hover);
}

.featured-cover {
  width: 100%;
  height: 140px;
  overflow: hidden;
}

.featured-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.featured-card:hover .featured-cover img {
  transform: scale(1.05);
}

/* 导航卡片封面：渐变背景 + 居中图标 */
.featured-cover--nav,
.featured-cover--about {
  display: flex;
  align-items: center;
  justify-content: center;
}

.featured-cover--nav {
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
}

.featured-cover--about {
  background: linear-gradient(135deg, #F97316, #FB923C);
}

.featured-cover--nav svg,
.featured-cover--about svg {
  color: rgba(255, 255, 255, 0.85);
  width: 48px;
  height: 48px;
}

.featured-body {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  flex: 1;
}

.featured-cat {
  display: inline-block;
  align-self: flex-start;
  font-family: var(--font-heading);
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--primary);
  background: var(--bg-tertiary);
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.featured-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 700;
  color: var(--text);
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.featured-summary {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.featured-meta {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-top: auto;
}

/* =============================================
   2. Categories Section
   ============================================= */
.categories-section {
  padding: var(--space-20) 0;
  background: var(--bg-secondary);
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6);
}

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

.course-card--0 {
  box-shadow: 6px 6px 14px rgba(79, 70, 229, 0.15), -4px -4px 12px rgba(255,255,255,0.7);
  border-color: #C7D2FE;
  &:hover { box-shadow: 8px 8px 20px rgba(79, 70, 229, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }
}

.course-card--1 {
  box-shadow: 6px 6px 14px rgba(249, 115, 22, 0.15), -4px -4px 12px rgba(255,255,255,0.7);
  border-color: #FED7AA;
  &:hover { box-shadow: 8px 8px 20px rgba(249, 115, 22, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }
}

.course-card--2 {
  box-shadow: 6px 6px 14px rgba(236, 72, 153, 0.15), -4px -4px 12px rgba(255,255,255,0.7);
  border-color: #FBCFE8;
  &:hover { box-shadow: 8px 8px 20px rgba(236, 72, 153, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }
}

.course-card--3 {
  box-shadow: 6px 6px 14px rgba(16, 185, 129, 0.15), -4px -4px 12px rgba(255,255,255,0.7);
  border-color: #A7F3D0;
  &:hover { box-shadow: 8px 8px 20px rgba(16, 185, 129, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }
}

.course-card--4 {
  box-shadow: 6px 6px 14px rgba(124, 58, 237, 0.15), -4px -4px 12px rgba(255,255,255,0.7);
  border-color: #DDD6FE;
  &:hover { box-shadow: 8px 8px 20px rgba(124, 58, 237, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }
}

.course-card--5 {
  box-shadow: 6px 6px 14px rgba(59, 130, 246, 0.15), -4px -4px 12px rgba(255,255,255,0.7);
  border-color: #BFDBFE;
  &:hover { box-shadow: 8px 8px 20px rgba(59, 130, 246, 0.25), -6px -6px 16px rgba(255,255,255,0.9); }
}

.course-card-icon {
  position: relative;
  z-index: 1;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
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
  position: relative;
  z-index: 1;
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--text);
  margin-bottom: var(--space-1);
}

.course-card-count {
  position: relative;
  z-index: 1;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-4);
}

.course-card-link {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
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

/* Categories skeleton */
.categories-skeleton {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6);
}

.skeleton-category {
  padding: var(--space-8);
  border-radius: var(--clay-border-radius);
  border: var(--clay-border);
  text-align: center;
  background: var(--bg-card);
}

.skeleton-card-body {
  padding: var(--space-6);
}

/* =============================================
   3. Blog Section
   ============================================= */
.blog-section {
  padding: var(--space-20) 0;
  background: var(--bg);
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6);
}

/* 标签云（填充文章网格空位） */
.grid-tagcloud {
  grid-column: span 2;
  padding: var(--space-6);
  background: var(--bg-card);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
  box-shadow: var(--clay-shadow);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.grid-tagcloud-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 700;
  color: var(--text);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 var(--space-3);
}

.grid-tagcloud-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.grid-tagchip {
  display: inline-block;
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--bg-tertiary);
  text-decoration: none;
  transition: all var(--transition-fast);
}

.grid-tagchip:hover {
  background: var(--primary);
  color: var(--text-inverse);
}

.view-all-wrap {
  text-align: center;
  margin-top: var(--space-10);
}

.view-all-btn {
  border-width: 3px;
  gap: var(--space-2);
  text-decoration: none;
}

/* =============================================
   4. CTA Section
   ============================================= */
.cta-section {
  position: relative;
  padding: var(--space-20) 0;
  background: linear-gradient(135deg, #EEF2FF 0%, #FFF7ED 100%);
  overflow: hidden;
}

@media (prefers-color-scheme: dark) {
  :root:not(.light) .cta-section {
    background: linear-gradient(135deg, #1E1245 0%, #2D1B69 100%);
  }
}

:global(html.dark) .cta-section {
  background: linear-gradient(135deg, #1E1245 0%, #2D1B69 100%);
}

.cta-bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.shape-4 {
  width: 400px; height: 400px;
  background: var(--primary);
  top: -100px; left: -100px;
  opacity: 0.08;
  animation: float 9s ease-in-out infinite;
}

.shape-5 {
  width: 250px; height: 250px;
  background: var(--accent);
  bottom: -50px; right: -50px;
  opacity: 0.08;
  animation: float 11s ease-in-out infinite reverse;
}

.cta-card {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 600px;
  margin: 0 auto;
  padding: var(--space-12) var(--space-8);
  background: var(--bg-card);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
  box-shadow: var(--clay-shadow);
}

.cta-title {
  font-size: var(--font-size-3xl);
  font-weight: 800;
  color: var(--text);
  margin-bottom: var(--space-4);
}

.cta-desc {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  line-height: 1.7;
  margin-bottom: var(--space-8);
  max-width: 480px;
  margin-left: auto;
  margin-right: auto;
}

.cta-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
}

.cta-btn {
  padding: 1rem 2.5rem;
  font-size: var(--font-size-base);
  font-weight: 700;
  border-radius: var(--clay-border-radius);
  gap: var(--space-2);
  text-decoration: none;
}

.cta-btn-secondary {
  padding: 1rem 2.5rem;
  font-size: var(--font-size-base);
  font-weight: 600;
  border-radius: var(--clay-border-radius);
  gap: var(--space-2);
  border-width: 3px;
  text-decoration: none;
}

/* =============================================
   Reduced Motion
   ============================================= */
@media (prefers-reduced-motion: reduce) {
  .shape, .shape-1, .shape-2, .shape-3, .shape-4, .shape-5 {
    animation: none !important;
  }

  .course-card {
    transition: none !important;
  }
}

/* =============================================
   Responsive
   ============================================= */
@media (max-width: 1024px) {
  .featured-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .courses-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .article-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .grid-tagcloud {
    grid-column: span 2;
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2rem;
  }

  .featured-grid {
    grid-template-columns: 1fr;
  }

  .courses-grid {
    grid-template-columns: 1fr;
  }

  .article-grid {
    grid-template-columns: 1fr;
  }

  .cta-actions {
    flex-direction: column;
  }

  .section-title {
    font-size: var(--font-size-3xl);
  }
}

@media (max-width: 480px) {
  .hero-title {
    font-size: 1.75rem;
  }

  .hero-section {
    padding: var(--space-10) 0 var(--space-8);
  }

  .featured-section,
  .categories-section,
  .blog-section {
    padding: var(--space-10) 0;
  }

  .categories-skeleton {
    grid-template-columns: 1fr;
  }
}
</style>
