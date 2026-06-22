<template>
  <section class="comment-section">
    <h3 class="comment-section-title">{{ t('comment.title') }} ({{ comments.length }})</h3>

    <!-- Loading -->
    <div v-if="loading" class="comment-loading">
      <div class="skeleton" style="height: 80px; margin-bottom: 1rem; border-radius: 12px;" v-for="i in 3" :key="i"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="empty-state">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      </div>
      <p class="empty-desc">{{ t('comment.loadFailed') }}</p>
      <button class="btn btn-outline btn-sm" @click="fetchComments">{{ t('comment.retry') }}</button>
    </div>

    <!-- Empty -->
    <div v-else-if="comments.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
      </div>
      <p class="empty-desc">{{ t('comment.empty') }}</p>
    </div>

    <!-- Comment List -->
    <div v-else class="comment-list">
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        :article-id="articleId"
        @replied="fetchComments"
      />
    </div>

    <!-- Comment Form -->
    <div class="comment-form-wrapper clay-card">
      <h4 class="form-title">{{ t('comment.formTitle') }}</h4>

      <!-- Logged in: textarea only -->
      <div v-if="isLoggedIn">
        <div v-if="currentUser" class="user-badge">
          <div class="user-avatar-sm">{{ currentUser.nickname.charAt(0).toUpperCase() }}</div>
          <span>{{ currentUser.nickname }}</span>
        </div>
        <form @submit.prevent="handleSubmit" class="comment-form">
          <div class="form-group">
            <textarea
              v-model="form.content"
              :placeholder="t('comment.placeholder')"
              rows="4"
              required
              class="form-input form-textarea"
            ></textarea>
          </div>
          <div v-if="submitError" class="form-error">{{ submitError }}</div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              {{ submitting ? t('comment.submitting') : t('comment.submit') }}
            </button>
          </div>
        </form>
      </div>

      <!-- Not logged in: prompt only -->
      <div v-else class="form-login-prompt">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
        <p><NuxtLink to="/login" class="auth-link">{{ t('comment.login') }}</NuxtLink>{{ t('comment.or') }}<NuxtLink to="/register" class="auth-link">{{ t('comment.register') }}</NuxtLink> to leave a comment</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { CommentResponse } from '~/composables/useBlogApi'

const { t } = useI18n()

const props = defineProps<{
  articleId: number
}>()

const comments = ref<CommentResponse[]>([])
const loading = ref(true)
const error = ref(false)
const submitting = ref(false)
const submitError = ref('')

const form = reactive({
  content: '',
})

const { getComments, createComment } = useBlogApi()
const { isLoggedIn, currentUser, token } = useAuth()

async function fetchComments() {
  loading.value = true
  error.value = false
  try {
    const data = await getComments(props.articleId)
    comments.value = data || []
  } catch (e) {
    console.error('Failed to fetch comments:', String(e))
    error.value = true
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!form.content.trim()) return
  submitting.value = true
  submitError.value = ''
  try {
    await createComment({
      articleId: props.articleId,
      content: form.content.trim(),
    }, token.value)
    form.content = ''
    await fetchComments()
  } catch (e: any) {
    submitError.value = e?.message || t('comment.submitFailed')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchComments()
})
</script>

<style scoped>
.comment-section {
  margin-top: var(--space-10);
  padding-top: var(--space-8);
  border-top: 3px solid var(--border);
}

.comment-section-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;
  margin-bottom: var(--space-6);
  color: var(--text);
}

.comment-loading {
  margin-bottom: var(--space-6);
}

.comment-list {
  margin-bottom: var(--space-8);
}

.comment-form-wrapper {
  padding: var(--space-6);
}

.form-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
  margin-bottom: var(--space-4);
  color: var(--text);
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  margin-top: var(--space-3);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.form-input {
  padding: 0.625rem 0.875rem;
  border: 3px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg);
  color: var(--text);
  font-size: var(--font-size-sm);
  transition: all var(--transition-fast);
  outline: none;
}

.form-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-input::placeholder {
  color: var(--text-muted);
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.form-error {
  color: var(--error);
  font-size: var(--font-size-sm);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.user-badge {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text);
  margin-bottom: var(--space-2);
}

.user-avatar-sm {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
}

.form-login-prompt {
  text-align: center;
  padding: var(--space-8) 0;
  color: var(--text-muted);
}

.form-login-prompt svg {
  margin-bottom: var(--space-3);
  opacity: 0.4;
}

.form-login-prompt p {
  font-size: var(--font-size-sm);
  margin: 0;
}

.form-login-prompt .auth-link {
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
}

.form-login-prompt .auth-link:hover {
  text-decoration: underline;
}
</style>
