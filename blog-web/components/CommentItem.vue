<template>
  <div class="comment-item" :class="{ 'comment-deleted': isDeleted }">
    <!-- 根评论：完整布局 -->
    <template v-if="!isReply">
      <UserAvatar
        :user="{ id: comment.userId, nickname: comment.userNickname, avatar: comment.userAvatar }"
        :size="36"
      />
      <div class="comment-content">
        <div class="comment-header">
          <span class="comment-author">{{ comment.authorName }}</span>
          <time class="comment-date">{{ formatDate(comment.createdAt) }}</time>
        </div>

        <p v-if="isDeleted" class="comment-deleted-text">{{ t('comment.deleted') }}</p>
        <p v-else class="comment-text">{{ comment.content }}</p>

        <CommentActionBar
          v-if="!isDeleted"
          ref="actionBarRef"
          :comment="comment"
          :is-own-comment="isOwnComment"
          @vote="handleVote"
          @reply="toggleReplyForm('root')"
          @copy="handleCopy"
          @delete="handleDelete"
          @report="handleReportClick"
          @block="handleBlock"
        />
        <!-- Inline report reason selector -->
        <div v-if="showReportOptions" class="report-options">
          <button
            v-for="r in reportReasons"
            :key="r.value"
            class="report-option-btn"
            @click="handleReportSubmit(r.value)"
          >{{ r.label }}</button>
          <button class="report-option-btn report-option-cancel" @click="showReportOptions = false">{{ t('common.cancel') }}</button>
        </div>

        <!-- 未登录提示 -->
        <div v-if="showRootReplyForm && !isLoggedIn" class="reply-login-prompt">
          <p><NuxtLink to="/login" class="auth-link">{{ t('comment.login') }}</NuxtLink>{{ t('comment.or') }}<NuxtLink to="/register" class="auth-link">{{ t('comment.register') }}</NuxtLink>{{ t('comment.loginRequiredReply') }}</p>
        </div>

        <!-- 回复表单（根评论） -->
        <form v-if="showRootReplyForm && isLoggedIn" @submit.prevent="handleReply(null)" class="reply-form clay-card">
          <textarea v-model="replyContent" :placeholder="t('comment.replyPlaceholder')" rows="3" required class="form-input form-textarea"></textarea>
          <div v-if="replyError" class="form-error">{{ replyError }}</div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary btn-sm" :disabled="replySubmitting">
              {{ replySubmitting ? t('comment.replySubmitting') : t('comment.replySubmit') }}
            </button>
          </div>
        </form>

        <!-- 回复区：扁平化内联风格 -->
        <div v-if="comment.replies && comment.replies.length > 0" class="replies-section">
          <div v-for="reply in visibleReplies" :key="reply.id" class="reply-item">
            <div class="reply-body">
              <span class="reply-author">{{ reply.authorName }}</span>
              <span class="reply-text">{{ reply.content }}</span>
            </div>
            <div class="reply-actions">
              <button class="reply-action-btn" :class="{ active: reply.likedByCurrentUser }" @click="handleReplyVote(reply, 'LIKE')">
                👍 {{ reply.likeCount || '' }}
              </button>
              <button class="reply-action-btn" :class="{ active: reply.dislikedByCurrentUser }" @click="handleReplyVote(reply, 'DISLIKE')">
                👎 {{ reply.dislikeCount || '' }}
              </button>
              <button class="reply-action-btn" @click="toggleReplyForm(reply.id)">💬 {{ t('comment.reply') }}</button>

              <!-- 回复的子回复表单 -->
              <form v-if="activeReplyForm === reply.id" @submit.prevent="handleReply(reply.id)" class="reply-inline-form">
                <input v-model="replyContent" :placeholder="t('comment.replyPlaceholder')" class="reply-inline-input" />
                <button type="submit" class="btn btn-primary btn-xs" :disabled="replySubmitting">{{ replySubmitting ? t('comment.replySubmitting') : t('comment.replySubmit') }}</button>
              </form>
            </div>
          </div>

          <!-- 折叠/展开 -->
          <button v-if="comment.replies.length > 3" class="toggle-replies-btn" @click="repliesExpanded = !repliesExpanded">
            <template v-if="repliesExpanded">{{ t('comment.collapseReplies') }}</template>
            <template v-else>{{ t('comment.viewAllReplies', { count: comment.replies.length }) }}</template>
          </button>
        </div>
      </div>
    </template>

    <!-- 扁平回复不再使用 isReply 模式，已内联渲染到根评论下面 -->
  </div>
</template>

<script setup lang="ts">
import type { CommentResponse, VoteResult } from '~/composables/useBlogApi'
import UserAvatar from '~/components/UserAvatar.vue'

const { t } = useI18n()
const { locale } = useI18n()
const { isLoggedIn, currentUser, token } = useAuth()
const { voteComment, reportComment, deleteComment, blockUser, createComment } = useBlogApi()
const { confirm, alert } = useModal()

const props = defineProps<{
  comment: CommentResponse
  articleId: number
  isReply?: boolean
}>()

const emit = defineEmits<{
  replied: []
}>()

const isDeleted = ref(false)
const actionBarRef = ref<InstanceType<typeof CommentActionBar> | null>(null)

// 回复状态
const showRootReplyForm = ref(false)
const activeReplyForm = ref<number | null>(null)  // null=根评论, number=回复的ID
const replyContent = ref('')
const replySubmitting = ref(false)
const replyError = ref('')

// 折叠状态
const repliesExpanded = ref(false)

// 当前正在回复的目标昵称（用于 @ 提及）
const replyTargetName = ref('')

// 当前显示的回复（前 3 条或全部）
const visibleReplies = computed(() => {
  if (!props.comment.replies) return []
  if (repliesExpanded.value) return props.comment.replies
  return props.comment.replies.slice(0, 3)
})

const isOwnComment = computed(() => {
  if (!currentUser.value || !props.comment.userId) return false
  return currentUser.value.id === props.comment.userId
})

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString(locale.value, {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

// ===== 回复逻辑 =====

function toggleReplyForm(target: number | 'root') {
  if (target === 'root') {
    showRootReplyForm.value = !showRootReplyForm.value
    activeReplyForm.value = null
    replyTargetName.value = ''
  } else {
    activeReplyForm.value = activeReplyForm.value === target ? null : target
    showRootReplyForm.value = false
    // 找到被回复的回复的昵称
    const reply = props.comment.replies?.find(r => r.id === target)
    replyTargetName.value = reply?.authorName || ''
  }
  replyContent.value = ''
  replyError.value = ''
}

async function handleReply(_parentId: number | null) {
  if (!replyContent.value.trim()) return
  replySubmitting.value = true
  replyError.value = ''
  try {
    // 始终以根评论为 parent（后端只支持 1 层嵌套）
    // @ 提及通过 replyTargetName 在显示时处理
    const prefix = replyTargetName.value ? `回复 @${replyTargetName.value}：` : ''
    await createComment({
      articleId: props.articleId,
      parentId: props.comment.id,  // 始终挂到根评论下
      content: prefix + replyContent.value.trim(),
    }, token.value)
    replyContent.value = ''
    showRootReplyForm.value = false
    activeReplyForm.value = null
    replyTargetName.value = ''
    emit('replied')
  } catch (e: any) {
    replyError.value = e?.message || t('comment.replyFailed')
  } finally {
    replySubmitting.value = false
  }
}

// ===== 投票逻辑 =====

async function handleVote(commentId: number, voteType: string | null) {
  try {
    const result = await voteComment(commentId, voteType)
    Object.assign(props.comment, result)
  } catch { /* ignore */ }
  finally { actionBarRef.value?.resetVoteLoading() }
}

async function handleReplyVote(reply: CommentResponse, voteType: string) {
  const isActive = voteType === 'LIKE' ? reply.likedByCurrentUser : reply.dislikedByCurrentUser
  try {
    const result = await voteComment(reply.id, isActive ? null : voteType)
    reply.likeCount = result.likeCount
    reply.dislikeCount = result.dislikeCount
    reply.likedByCurrentUser = result.likedByCurrentUser
    reply.dislikedByCurrentUser = result.dislikedByCurrentUser
  } catch { /* ignore */ }
}

// ===== 其他操作 =====

async function handleCopy() {
  try { await navigator.clipboard.writeText(props.comment.content) }
  catch {
    const ta = document.createElement('textarea')
    ta.value = props.comment.content
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }
}

async function handleDelete(commentId: number) {
  const ok = await confirm({ title: t('comment.confirmDeleteTitle') || '删除评论', message: t('comment.confirmDelete'), type: 'warning', confirmText: t('common.delete') })
  if (!ok) return
  try { await deleteComment(commentId); isDeleted.value = true }
  catch { /* ignore */ }
}

const showReportOptions = ref(false)
const reportReasons = computed(() => [
  { value: 'SPAM', label: t('comment.reasonSpam') },
  { value: 'ABUSE', label: t('comment.reasonAbuse') },
  { value: 'OTHER', label: t('comment.reasonOther') },
])

async function handleReportClick() {
  showReportOptions.value = !showReportOptions.value
}

async function handleReportSubmit(reason: string) {
  showReportOptions.value = false
  try {
    await reportComment(props.comment.id, reason)
    alert({ title: t('comment.reportSubmitted'), message: t('comment.reportSubmitted'), type: 'info' })
  } catch (e: any) {
    alert({ message: e.message?.includes('Already reported') ? t('comment.alreadyReported') : t('comment.reportSubmitted'), type: 'info' })
  }
}

async function handleBlock(userId: number) {
  const ok = await confirm({ title: t('comment.confirmBlockTitle') || '拉黑用户', message: t('comment.confirmBlock'), type: 'warning', confirmText: t('comment.block') })
  if (!ok) return
  try { await blockUser(userId); isDeleted.value = true }
  catch { /* ignore */ }
}
</script>

<style scoped>
/* ===== 根评论 ===== */
.comment-item {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4) 0;
  border-bottom: 2px solid var(--border-light);
}
.comment-item:last-child { border-bottom: none; }
.comment-deleted { opacity: 0.5; }

.comment-avatar {
  flex-shrink: 0;
  width: 40px; height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--primary), var(--secondary));
  color: var(--text-inverse);
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-heading); font-weight: 700; font-size: var(--font-size-sm);
}
.comment-content { flex: 1; min-width: 0; }
.comment-header { display: flex; align-items: center; gap: var(--space-3); margin-bottom: var(--space-1); }
.comment-author { font-family: var(--font-heading); font-weight: 700; font-size: var(--font-size-sm); color: var(--text); }
.comment-date { font-size: var(--font-size-xs); color: var(--text-muted); }
.comment-text { font-size: var(--font-size-sm); color: var(--text-secondary); line-height: 1.6; margin-bottom: var(--space-2); word-break: break-word; }
.comment-deleted-text { font-size: var(--font-size-sm); color: var(--text-muted); font-style: italic; margin-bottom: var(--space-2); }

/* ===== 回复表单 ===== */
.reply-form { margin-top: var(--space-3); padding: var(--space-4); display: flex; flex-direction: column; gap: var(--space-3); }
.form-input { padding: 0.5rem 0.75rem; border: 3px solid var(--border); border-radius: var(--radius-lg); background: var(--bg); color: var(--text); font-size: var(--font-size-sm); outline: none; width: 100%; transition: all var(--transition-fast); }
.form-input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1); }
.form-input::placeholder { color: var(--text-muted); }
.form-textarea { resize: vertical; min-height: 60px; }
.form-error { color: var(--error); font-size: var(--font-size-xs); }
.form-actions { display: flex; justify-content: flex-end; }
.reply-login-prompt { margin-top: var(--space-3); padding: var(--space-4); font-size: var(--font-size-sm); color: var(--text-muted); text-align: center; }
.reply-login-prompt p { margin: 0; }
.reply-login-prompt .auth-link { color: var(--primary); font-weight: 600; text-decoration: none; }

/* ===== 内联回复区（Bilibili 风格） ===== */
.replies-section {
  margin-top: var(--space-3);
  padding-left: var(--space-1);
  border-left: 3px solid var(--border-light);
  padding-left: var(--space-3);
}

.reply-item {
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--border-light);
  font-size: 13px;
}
.reply-item:last-child { border-bottom: none; }

.reply-body {
  line-height: 1.6;
  margin-bottom: 2px;
}

.reply-author {
  font-weight: 700;
  color: var(--primary);
  margin-right: 2px;
}

.reply-to {
  color: var(--text-muted);
  font-size: 12px;
}

.reply-text {
  color: var(--text-secondary);
  word-break: break-word;
}

/* ===== 回复操作栏 ===== */
.reply-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.reply-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--text-muted);
  background: none;
  border: none;
  padding: 2px 6px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: var(--font-body);
}
.reply-action-btn:hover { background: var(--bg-tertiary); color: var(--primary); }
.reply-action-btn.active { color: var(--primary); }

/* ===== 内联回复输入框 ===== */
.reply-inline-form {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  width: 100%;
}
.reply-inline-input {
  flex: 1;
  padding: 4px 8px;
  border: 2px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg);
  color: var(--text);
  font-size: 12px;
  outline: none;
}
.reply-inline-input:focus { border-color: var(--primary); }

/* ===== 折叠/展开按钮 ===== */
.toggle-replies-btn {
  display: block;
  margin-top: var(--space-2);
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 0;
  font-family: var(--font-body);
}
.toggle-replies-btn:hover { text-decoration: underline; }

/* 内联举报原因选择 */
.report-options {
  display: flex;
  gap: 6px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.report-option-btn {
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  background: var(--bg-tertiary);
  border: 2px solid var(--border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: var(--font-body);
}

.report-option-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--bg-card);
}

.report-option-cancel {
  color: var(--text-muted);
}

.report-option-cancel:hover {
  color: var(--error);
  border-color: var(--error);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .replies-section { padding-left: var(--space-2); }
}
</style>
