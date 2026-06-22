<template>
  <div class="action-bar">
    <VoteButton
      :count="comment.likeCount"
      :is-active="comment.likedByCurrentUser"
      :disabled="voteLoading"
      @vote="handleVote('LIKE')"
    />
    <VoteButton
      :count="comment.dislikeCount"
      :is-active="comment.dislikedByCurrentUser"
      :is-dislike="true"
      :disabled="voteLoading"
      @vote="handleVote('DISLIKE')"
    />

    <button class="action-btn" @click="$emit('reply')">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="10 9 5 14 10 19"/><path d="M5 14h11a4 4 0 0 0 4-4V5"/></svg>
      {{ t('comment.reply') }}
    </button>

    <SharePopover :comment-id="comment.id" />

    <MoreMenu :items="menuItems" @action="handleMenuAction" />
  </div>
</template>

<script setup lang="ts">
import type { MenuItem } from './MoreMenu.vue'
import type { CommentResponse } from '~/composables/useBlogApi'

const { t } = useI18n()

const props = defineProps<{
  comment: CommentResponse
  isOwnComment: boolean
}>()

const emit = defineEmits<{
  vote: [commentId: number, voteType: string | null]
  reply: []
  copy: []
  delete: [commentId: number]
  report: [commentId: number]
  block: [userId: number]
}>()

const voteLoading = ref(false)

const menuItems = computed<MenuItem[]>(() => {
  const items: MenuItem[] = [
    { key: 'reply', label: t('comment.reply'), icon: '💬' },
    { key: 'copy', label: t('common.copy'), icon: '📋' },
  ]
  if (props.isOwnComment) {
    items.push({ key: 'delete', label: t('common.delete'), icon: '🗑', danger: true })
  } else {
    items.push(
      { key: 'report', label: t('comment.report'), icon: '⚠️' },
      { key: 'block', label: t('comment.block'), icon: '🚫', danger: true }
    )
  }
  return items
})

function handleVote(voteType: string) {
  if (voteLoading.value) return
  voteLoading.value = true
  // 确定新的 voteType: 点击已激活的按钮则取消，否则设置为该类型
  const isActive = voteType === 'LIKE' ? props.comment.likedByCurrentUser : props.comment.dislikedByCurrentUser
  const newVoteType = isActive ? null : voteType
  emit('vote', props.comment.id, newVoteType)
  // voteLoading 会在父组件更新数据后由父组件重置
}

function handleMenuAction(key: string) {
  switch (key) {
    case 'reply':
      emit('reply')
      break
    case 'copy':
      emit('copy')
      break
    case 'delete':
      emit('delete', props.comment.id)
      break
    case 'report':
      emit('report', props.comment.id)
      break
    case 'block':
      emit('block', props.comment.userId!)
      break
  }
}

// 暴露 resetVoteLoading 以便父组件在异步完成后调用
function resetVoteLoading() {
  voteLoading.value = false
}

defineExpose({ resetVoteLoading })
</script>

<style scoped>
.action-bar {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  background: none;
  border: none;
  padding: 2px 6px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: var(--font-body);
}

.action-btn:hover {
  background: var(--bg-tertiary);
  color: var(--primary);
}
</style>
