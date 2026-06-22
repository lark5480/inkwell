<template>
  <div class="share-wrapper" ref="shareRef">
    <button class="share-trigger" @click="toggle" :title="t('comment.share')">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>
      {{ t('comment.share') }}
    </button>
    <Transition name="fade">
      <div v-if="open" class="share-popover">
        <div class="share-header">{{ t('comment.share') }}</div>
        <div class="share-options">
          <button class="share-option" @click="copyLink">
            <span class="share-icon">📋</span>
            <span>{{ t('comment.copyLink') }}</span>
          </button>
          <button class="share-option" @click="shareTo('twitter')">
            <span class="share-icon">🐦</span>
            <span>Twitter</span>
          </button>
          <button class="share-option" @click="shareTo('weibo')">
            <span class="share-icon">💬</span>
            <span>微博</span>
          </button>
        </div>
        <div v-if="copied" class="share-copied">{{ t('comment.copied') }}</div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
const { t } = useI18n()

const props = defineProps<{
  commentId: number
}>()

const open = ref(false)
const copied = ref(false)
const shareRef = ref<HTMLElement | null>(null)

function onDocumentClick(e: MouseEvent) {
  if (shareRef.value && !shareRef.value.contains(e.target as Node)) {
    open.value = false
  }
}

function toggle() {
  open.value = !open.value
}

async function copyLink() {
  const url = `${window.location.origin}/comments/${props.commentId}`
  try {
    await navigator.clipboard.writeText(url)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    // fallback
    const textarea = document.createElement('textarea')
    textarea.value = url
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  }
}

function shareTo(platform: string) {
  const url = encodeURIComponent(`${window.location.origin}/comments/${props.commentId}`)
  let shareUrl = ''
  if (platform === 'twitter') {
    shareUrl = `https://twitter.com/intent/tweet?url=${url}`
  } else if (platform === 'weibo') {
    shareUrl = `https://service.weibo.com/share/share.php?url=${url}`
  }
  if (shareUrl) {
    window.open(shareUrl, '_blank', 'width=600,height=400')
  }
  open.value = false
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
})

onUnmounted(() => {
  document.removeEventListener('click', onDocumentClick)
})
</script>

<style scoped>
.share-wrapper {
  position: relative;
  display: inline-block;
}

.share-trigger {
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

.share-trigger:hover {
  background: var(--bg-tertiary);
  color: var(--primary);
}

.share-popover {
  position: absolute;
  left: 0;
  bottom: 100%;
  z-index: 50;
  min-width: 180px;
  background: var(--bg);
  border: 2px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  margin-bottom: 4px;
}

.share-header {
  padding: 8px 14px;
  font-size: var(--font-size-xs);
  font-weight: 700;
  color: var(--text-muted);
  border-bottom: 1px solid var(--border-light);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.share-options {
  display: flex;
  flex-direction: column;
}

.share-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  font-size: var(--font-size-sm);
  color: var(--text);
  background: none;
  border: none;
  cursor: pointer;
  transition: background var(--transition-fast);
  font-family: var(--font-body);
  text-align: left;
}

.share-option:hover {
  background: var(--bg-tertiary);
}

.share-icon {
  font-size: 14px;
  width: 18px;
  text-align: center;
}

.share-copied {
  padding: 6px 14px;
  font-size: var(--font-size-xs);
  color: var(--success);
  font-weight: 600;
  border-top: 1px solid var(--border-light);
  text-align: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
</style>
