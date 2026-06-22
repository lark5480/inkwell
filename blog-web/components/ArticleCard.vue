<template>
  <article class="article-card clay-card">
    <NuxtLink :to="`/article/${article.slug}`" class="article-card-link">
      <div v-if="article.coverImage" class="article-card-cover">
        <img :src="coverUrl" :alt="article.title" loading="lazy" />
      </div>
      <div class="article-card-body">
        <div class="article-card-meta">
          <span v-if="article.categoryName" class="article-category-tag">{{ article.categoryName }}</span>
          <time class="article-date">{{ formatDate(article.publishedAt) }}</time>
        </div>
        <div v-if="article.authorId" class="article-card-author">
          <UserAvatar
            :user="{ id: article.authorId, nickname: article.authorName, avatar: article.authorAvatar }"
            :size="20"
          />
          <span class="author-name">{{ article.authorName }}</span>
        </div>
        <h2 class="article-card-title">{{ article.title }}</h2>
        <p class="article-card-summary">{{ article.summary }}</p>
        <div class="article-card-footer">
          <div class="article-card-tags">
            <span v-for="tag in article.tags" :key="tag.slug" class="article-tag">{{ tag.name }}</span>
          </div>
          <div class="article-card-stats">
            <span class="stat" title="Views">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              {{ article.viewCount }}
            </span>
            <span class="stat" title="Likes">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
              {{ article.likeCount }}
            </span>
            <span class="stat" title="Comments">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              {{ article.commentCount }}
            </span>
          </div>
        </div>
      </div>
    </NuxtLink>
  </article>
</template>

<script setup lang="ts">
import type { ArticleWebResponse } from '~/composables/useBlogApi'
import UserAvatar from '~/components/UserAvatar.vue'

const props = defineProps<{
  article: ArticleWebResponse
}>()

const { resolve } = useMedia()
const coverUrl = computed(() => resolve(props.article.coverImage))

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
</script>

<style scoped>
.article-card {
  overflow: hidden;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
  cursor: pointer;
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--clay-shadow-hover);
}

.article-card:active {
  transform: translateY(-2px);
  box-shadow: var(--clay-shadow-pressed);
}

.article-card-link {
  display: block;
  text-decoration: none;
  color: inherit;
}

.article-card-cover {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.article-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.article-card:hover .article-card-cover img {
  transform: scale(1.05);
}

.article-card-body {
  padding: var(--space-6);
}

.article-card-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.article-category-tag {
  display: inline-block;
  font-family: var(--font-heading);
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--primary);
  background: var(--bg-tertiary);
  padding: 0.15rem 0.625rem;
  border-radius: 999px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.article-date {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.article-card-author {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: var(--space-2);
}

.article-card-author .author-name {
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--text-secondary);
}

.article-card-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--text);
  margin-bottom: var(--space-2);
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-card-summary {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: var(--space-4);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.article-card-tags {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.article-tag {
  display: inline-block;
  font-size: 0.7rem;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--bg-tertiary);
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  transition: all var(--transition-fast);
}

.article-tag:hover {
  background: var(--primary);
  color: var(--text-inverse);
}

.article-card-stats {
  display: flex;
  gap: var(--space-3);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  white-space: nowrap;
}

.stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 768px) {
  .article-card-cover {
    height: 160px;
  }

  .article-card-body {
    padding: var(--space-4);
  }

  .article-card-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
