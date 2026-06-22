<template>
  <div class="page-wrapper">
    <div class="container article-layout">
      <!-- Main Content -->
      <div class="article-main">
        <!-- Loading State -->
        <div v-if="loading" class="loading-state">
          <div class="skeleton" style="height: 36px; width: 70%; margin-bottom: 1rem; border-radius: 8px;"></div>
          <div class="skeleton" style="height: 16px; width: 40%; margin-bottom: 2rem; border-radius: 8px;"></div>
          <div class="skeleton" style="height: 300px; width: 100%; margin-bottom: 1rem; border-radius: 20px;"></div>
          <div v-for="i in 6" :key="i" class="skeleton" style="height: 16px; width: 100%; margin-bottom: 0.75rem; border-radius: 8px;"></div>
        </div>

        <!-- 404 / Not Found -->
        <div v-else-if="notFound" class="error-state">
          <div class="error-code">404</div>
          <p class="error-message">{{ t('article.notFound') }}</p>
          <NuxtLink to="/" class="btn btn-primary">{{ t('article.backHome') }}</NuxtLink>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="error-state">
          <div class="error-code">:(</div>
          <p class="error-message">{{ t('article.loadFailed') }}</p>
          <NuxtLink to="/" class="btn btn-primary">{{ t('article.backHome') }}</NuxtLink>
        </div>

        <!-- Article Content -->
        <template v-else-if="article">
          <article class="article-detail">
            <!-- Cover Image -->
            <div v-if="article.coverImage" class="article-cover">
              <img :src="coverUrl" :alt="article.title" />
            </div>

            <!-- Header -->
            <header class="article-header">
              <div class="article-meta">
                <span v-if="article.categoryName" class="article-category-badge">{{ article.categoryName }}</span>
                <time class="article-published">{{ formatDate(article.publishedAt) }}</time>
                <span class="article-reading-time" v-if="readingTime">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                  {{ readingTime }} {{ t('article.readingTime') }}
                </span>
              </div>

              <h1 class="article-title">{{ article.title }}</h1>

              <div v-if="article.tags && article.tags.length > 0" class="article-tags">
                <NuxtLink
                  v-for="tag in article.tags"
                  :key="tag.slug"
                  :to="`/tag/${tag.slug}`"
                  class="article-tag-badge"
                >
                  #{{ tag.name }}
                </NuxtLink>
              </div>

              <!-- Stats + Like Button -->
              <div class="article-stats">
                <span class="stat-item">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  {{ article.viewCount }} {{ t('home.views') }}
                </span>
                <button
                  class="like-button"
                  :class="{ 'liked': isLiked, 'animating': isAnimating }"
                  @click="handleLike"
                  :disabled="liking"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" :fill="isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                  <span>{{ likeCount }} {{ t('home.likes') }}</span>
                  <span v-if="isAnimating" class="like-burst">
                    <span v-for="i in 6" :key="i" class="burst-dot"></span>
                  </span>
                </button>
                <span class="stat-item">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                  {{ article.commentCount }} {{ t('myArticles.comments') }}
                </span>
              </div>
            </header>

            <!-- Author Info -->
            <div v-if="article.authorId" class="author-info">
              <div class="author-row">
                <NuxtLink :to="`/user/${article.authorId}`" class="author-link">
                  <img v-if="article.authorAvatar" :src="article.authorAvatar" :alt="article.authorName || ''" class="author-avatar" />
                  <div v-else class="author-avatar-placeholder">{{ (article.authorName || '?').charAt(0).toUpperCase() }}</div>
                  <div class="author-text">
                    <span class="author-name">{{ article.authorName }}</span>
                    <span v-if="authorProfile" class="author-stats">
                      {{ authorProfile.articleCount }} {{ t('common.articles') }} · {{ authorProfile.followerCount }} {{ t('user.followers') }}
                    </span>
                  </div>
                </NuxtLink>
                <button
                  v-if="isLoggedIn && !isOwnArticle"
                  :class="['follow-btn', { followed: isFollowing }]"
                  @click="handleToggleFollow"
                  :disabled="followLoading"
                >{{ isFollowing ? t('user.following') : t('user.follow') }}</button>
              </div>
            </div>

            <!-- Article Body -->
            <div class="article-content" v-html="article.content"></div>
          </article>

          <!-- Share Section -->
          <div class="share-section">
            <span class="share-label">{{ t('article.share') }}</span>
            <div class="share-buttons">
              <button class="share-btn" @click="copyLink" :class="{ copied: linkCopied }">
                <svg v-if="!linkCopied" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                {{ linkCopied ? t('article.copied') : t('article.copyLink') }}
              </button>
              <a class="share-btn" :href="twitterUrl" target="_blank" rel="noopener noreferrer">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
                Twitter
              </a>
              <a class="share-btn" :href="weiboUrl" target="_blank" rel="noopener noreferrer">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M10.098 20.323c-3.977.391-7.414-1.406-7.672-4.02-.259-2.609 2.759-5.047 6.74-5.441 3.979-.394 7.413 1.406 7.671 4.018.259 2.6-2.759 5.049-6.739 5.443zM9.05 17.219c-.384.616-1.208.884-1.829.602-.612-.279-.793-.991-.406-1.593.379-.595 1.176-.861 1.793-.583.631.283.82.971.442 1.574zm1.27-1.627c-.141.237-.449.353-.689.253-.236-.095-.313-.361-.177-.586.138-.227.436-.346.672-.24.239.09.321.344.194.573zm.176-2.719c-1.906-.496-4.063.475-4.883 2.187-.837 1.746.042 3.726 1.985 4.392 2.013.69 4.391-.312 5.2-2.145.801-1.793-.239-3.772-2.302-4.434zM17.401 14.486c-.244-.093-.412-.157-.286-.565.274-.884.302-1.646.006-2.191-.554-1.02-2.069-1.029-3.812-.029 0 0-.546.237-.407-.193.266-.849.226-1.561-.189-1.975-1.163-1.157-4.257.045-6.906 2.685C3.576 14.449 2.4 16.827 2.4 18.876c0 3.929 5.034 6.319 9.954 6.319 6.446 0 10.737-3.747 10.737-6.727 0-1.798-1.507-2.823-3.09-3.397zm3.844-6.426c-1.226-1.363-3.034-1.883-4.679-1.569l-.054.012c-.34.078-.552.414-.473.753.078.338.414.549.753.471l.052-.012c1.144-.218 2.4.143 3.252 1.088.854.949 1.057 2.236.638 3.329l-.018.052c-.112.33.066.689.396.801.33.112.689-.066.801-.396l.02-.056c.558-1.453.29-3.163-.688-4.473zM18.9 4.627c-2.153-2.394-5.329-3.305-8.22-2.756l-.068.015c-.393.09-.639.48-.549.873.09.393.48.639.873.549l.066-.015c2.352-.447 4.935.295 6.694 2.25 1.761 1.957 2.249 4.583 1.478 6.86l-.025.068c-.135.381.066.8.447.935.381.135.8-.066.935-.447l.026-.073c.953-2.807.351-6.045-1.657-8.259z"/></svg>
                Weibo
              </a>
            </div>
          </div>

          <!-- Prev / Next Navigation -->
          <nav v-if="prevNext.prev || prevNext.next" class="prev-next-nav">
            <NuxtLink
              v-if="prevNext.prev"
              :to="`/article/${prevNext.prev.slug}`"
              class="prev-next-link prev-link"
            >
              <span class="prev-next-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
                {{ t('article.prevArticle') }}
              </span>
              <span class="prev-next-title">{{ prevNext.prev.title }}</span>
            </NuxtLink>
            <div v-else></div>
            <NuxtLink
              v-if="prevNext.next"
              :to="`/article/${prevNext.next.slug}`"
              class="prev-next-link next-link"
            >
              <span class="prev-next-label">
                {{ t('article.nextArticle') }}
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
              </span>
              <span class="prev-next-title">{{ prevNext.next.title }}</span>
            </NuxtLink>
          </nav>

          <!-- Comment Section -->
          <CommentSection :article-id="article.id" />
        </template>
      </div>

      <!-- TOC Sidebar (hidden on mobile) -->
      <aside v-if="article && !loading && !error && !notFound" class="toc-sidebar">
        <TocBlock :html="article.content" />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ArticleDetailResponse, PrevNextDTO, UserPublicResponse } from '~/composables/useBlogApi'

const route = useRoute()
const slug = route.params.slug as string

const { t } = useI18n()
const { getArticle, getPrevNext, likeArticle, getUserProfile, toggleFollow, getFollowStatus } = useBlogApi()
const { isLoggedIn, currentUser, token } = useAuth()
const { resolve } = useMedia()

const article = ref<ArticleDetailResponse | null>(null)
const prevNext = ref<PrevNextDTO>({ prev: null, next: null })
const loading = ref(true)
const error = ref(false)
const notFound = ref(false)

// Author profile + follow state
const authorProfile = ref<UserPublicResponse | null>(null)
const isFollowing = ref(false)
const followLoading = ref(false)

const isOwnArticle = computed(() =>
  isLoggedIn.value && currentUser.value && article.value?.authorId
    ? currentUser.value.id === article.value.authorId
    : false
)

const coverUrl = computed(() => resolve(article.value?.coverImage))

// Like state
const likeCount = ref(0)
const isLiked = ref(false)
const liking = ref(false)
const isAnimating = ref(false)

// Share state
const linkCopied = ref(false)

const currentUrl = computed(() => {
  if (import.meta.server) return ''
  return window.location.href
})

const twitterUrl = computed(() => {
  if (!article.value) return '#'
  const url = encodeURIComponent(currentUrl.value)
  const text = encodeURIComponent(article.value.title)
  return `https://twitter.com/intent/tweet?url=${url}&text=${text}`
})

const weiboUrl = computed(() => {
  if (!article.value) return '#'
  const url = encodeURIComponent(currentUrl.value)
  const title = encodeURIComponent(article.value.title)
  return `https://service.weibo.com/share/share.php?url=${url}&title=${title}`
})

async function copyLink() {
  try {
    await navigator.clipboard.writeText(currentUrl.value)
    linkCopied.value = true
    setTimeout(() => { linkCopied.value = false }, 2000)
  } catch {}
}

const readingTime = computed(() => {
  if (!article.value?.content) return 0
  const words = article.value.content.replace(/<[^>]+>/g, '').split(/\s+/).length
  return Math.max(1, Math.ceil(words / 200))
})

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

// Like handler (toggle)
async function handleLike() {
  if (liking.value) return
  liking.value = true
  try {
    const result = await likeArticle(slug)
    likeCount.value = result.likeCount
    isLiked.value = result.liked
    if (result.liked) {
      isAnimating.value = true
      setTimeout(() => { isAnimating.value = false }, 600)
    }
  } catch (e) {
    console.error('Failed to like article:', String(e))
  } finally {
    liking.value = false
  }
}

// SEO
useSeoMeta({
  title: computed(() => article.value?.title || 'Article'),
  description: computed(() => article.value?.summary || ''),
  ogTitle: computed(() => article.value?.title || ''),
  ogDescription: computed(() => article.value?.summary || ''),
  ogImage: computed(() => article.value?.coverImage || ''),
  ogType: 'article',
})

// JSON-LD Structured Data
useHead({
  script: computed(() => {
    if (!article.value) return []
    return [
      {
        type: 'application/ld+json',
        innerHTML: JSON.stringify({
          '@context': 'https://schema.org',
          '@type': 'Article',
          headline: article.value.title,
          description: article.value.summary,
          image: article.value.coverImage,
          datePublished: article.value.publishedAt,
          author: {
            '@type': 'Person',
            name: article.value.authorName || 'Author',
          },
        }),
      },
    ]
  }),
})

async function handleToggleFollow() {
  if (!article.value?.authorId || followLoading.value) return
  followLoading.value = true
  try {
    const res = await toggleFollow(article.value.authorId)
    isFollowing.value = res.followed
    if (authorProfile.value) {
      authorProfile.value = {
        ...authorProfile.value,
        followerCount: res.followerCount,
      }
    }
  } catch (e) {
    console.error('Follow toggle failed:', String(e))
  } finally {
    followLoading.value = false
  }
}

async function loadAuthorProfile(authorId: number) {
  try {
    authorProfile.value = await getUserProfile(authorId)
  } catch {}
  if (token.value) {
    try {
      const status = await getFollowStatus(authorId)
      isFollowing.value = status.following
    } catch {}
  }
}

try {
  const [articleData, prevNextData] = await Promise.all([
    getArticle(slug),
    getPrevNext(slug).catch(() => ({ prev: null, next: null } as PrevNextDTO)),
  ])
  article.value = articleData
  likeCount.value = articleData.likeCount ?? 0
  isLiked.value = articleData.isLiked ?? false
  prevNext.value = prevNextData
  if (articleData.authorId) {
    loadAuthorProfile(articleData.authorId)
  }
} catch (e: any) {
  if (e?.statusCode === 404 || e?.status === 404) {
    notFound.value = true
  } else {
    error.value = true
  }
  console.error('Failed to load article:', String(e))
} finally {
  loading.value = false
}
</script>

<style scoped>
/* Layout: two-column on desktop */
.article-layout {
  display: grid;
  grid-template-columns: 1fr 220px;
  gap: var(--space-8);
  max-width: 1100px;
  margin: 0 auto;
  align-items: start;
}

.article-main {
  min-width: 0;
  max-width: 800px;
}

.toc-sidebar {
  display: block;
}

/* Loading */
.loading-state {
  padding: var(--space-8) 0;
}

.article-cover {
  width: 100%;
  max-height: 400px;
  overflow: hidden;
  border-radius: var(--clay-border-radius);
  margin-bottom: var(--space-8);
  border: var(--clay-border);
  box-shadow: var(--clay-shadow);
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-header {
  margin-bottom: var(--space-8);
}

.author-info {
  margin-bottom: var(--space-8);
  padding: var(--space-4);
  background: var(--bg-card);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
}

.author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.author-link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  text-decoration: none;
  color: inherit;
  transition: opacity var(--transition-fast);
  flex: 1;
  min-width: 0;
}

.author-link:hover {
  opacity: 0.8;
}

.author-avatar,
.author-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.author-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary);
  color: white;
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 700;
}

.author-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.author-name {
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text);
}

.author-stats {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-top: 0.15rem;
}

.follow-btn {
  flex-shrink: 0;
  padding: 0.35rem 1rem;
  font-size: var(--font-size-xs);
  font-weight: 600;
  border-radius: 999px;
  border: 2px solid var(--primary);
  background: var(--primary);
  color: white;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.follow-btn:hover {
  opacity: 0.9;
}

.follow-btn.followed {
  background: transparent;
  color: var(--primary);
}

.follow-btn.followed:hover {
  background: var(--primary);
  color: white;
}

.follow-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
}

.article-category-badge {
  display: inline-block;
  font-family: var(--font-heading);
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--primary);
  background: var(--bg-tertiary);
  padding: 0.2rem 0.75rem;
  border-radius: 999px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.article-published {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.article-reading-time {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.article-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-4xl);
  font-weight: 800;
  color: var(--text);
  line-height: 1.15;
  margin-bottom: var(--space-4);
  letter-spacing: -0.02em;
}

.article-tags {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
  margin-bottom: var(--space-4);
}

.article-tag-badge {
  display: inline-block;
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--bg-tertiary);
  padding: 0.2rem 0.75rem;
  border-radius: 999px;
  transition: all var(--transition-fast);
  text-decoration: none;
}

.article-tag-badge:hover {
  background: var(--primary);
  color: var(--text-inverse);
}

/* Stats row with like button */
.article-stats {
  display: flex;
  gap: var(--space-6);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  align-items: center;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* Like button */
.like-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  position: relative;
  background: none;
  border: none;
  padding: 4px 8px;
  border-radius: 999px;
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  transition: all var(--transition-fast);
  font-family: inherit;
}

.like-button:hover:not(:disabled) {
  color: #e74c6f;
  background: rgba(231, 76, 111, 0.08);
}

.like-button.liked {
  color: #e74c6f;
  cursor: pointer;
}

.like-button.animating svg {
  animation: like-pop 0.4s ease-out;
}

@keyframes like-pop {
  0% { transform: scale(1); }
  30% { transform: scale(1.35); }
  60% { transform: scale(0.9); }
  100% { transform: scale(1); }
}

/* Burst dots */
.like-burst {
  position: absolute;
  top: 50%;
  left: 12px;
  width: 0;
  height: 0;
}

.burst-dot {
  position: absolute;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #e74c6f;
  animation: burst-out 0.5s ease-out forwards;
}

.burst-dot:nth-child(1) { --angle: 0deg; animation-delay: 0s; }
.burst-dot:nth-child(2) { --angle: 60deg; animation-delay: 0.03s; }
.burst-dot:nth-child(3) { --angle: 120deg; animation-delay: 0.06s; }
.burst-dot:nth-child(4) { --angle: 180deg; animation-delay: 0.09s; }
.burst-dot:nth-child(5) { --angle: 240deg; animation-delay: 0.12s; }
.burst-dot:nth-child(6) { --angle: 300deg; animation-delay: 0.15s; }

@keyframes burst-out {
  0% { transform: rotate(var(--angle)) translateX(0); opacity: 1; }
  100% { transform: rotate(var(--angle)) translateX(18px); opacity: 0; }
}

/* Share Section */
.share-section {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-top: var(--space-8);
  padding: var(--space-5) 0;
  border-top: 2px solid var(--border);
  border-bottom: 2px solid var(--border);
}

.share-label {
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-muted);
  white-space: nowrap;
}

.share-buttons {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.share-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--bg-tertiary);
  border: 2px solid transparent;
  cursor: pointer;
  transition: all var(--transition-fast);
  text-decoration: none;
  font-family: var(--font-heading);
}

.share-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--bg-card);
}

.share-btn.copied {
  color: var(--success);
  border-color: var(--success);
}

/* Prev / Next Navigation */
.prev-next-nav {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
  margin-top: var(--space-10);
  padding-top: var(--space-8);
  border-top: 3px solid var(--border);
}

.prev-next-link {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-5);
  border: 3px solid var(--border);
  border-radius: var(--clay-border-radius);
  text-decoration: none;
  transition: all var(--transition-base);
}

.prev-next-link:hover {
  border-color: var(--primary);
  background: var(--bg-tertiary);
  transform: translateY(-2px);
  box-shadow: var(--clay-shadow);
}

.next-link {
  text-align: right;
}

.prev-next-label {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-family: var(--font-heading);
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  font-weight: 600;
}

.next-link .prev-next-label {
  justify-content: flex-end;
}

.prev-next-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  color: var(--text);
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Responsive */
@media (max-width: 1024px) {
  .article-layout {
    grid-template-columns: 1fr;
  }

  .toc-sidebar {
    display: none;
  }
}

@media (max-width: 768px) {
  .article-title {
    font-size: var(--font-size-2xl);
  }

  .prev-next-nav {
    grid-template-columns: 1fr;
  }

  .article-stats {
    flex-wrap: wrap;
    gap: var(--space-3);
  }
}
</style>
