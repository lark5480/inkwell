<template>
  <button
    class="vote-btn"
    :class="{ active: isActive, disliked: isDislike }"
    :disabled="disabled"
    @click="handleClick"
    :title="label"
  >
    <span class="vote-icon">{{ isDislike ? '👎' : '👍' }}</span>
    <span v-if="count > 0" class="vote-count">{{ count }}</span>
  </button>
</template>

<script setup lang="ts">
const props = defineProps<{
  count: number
  isActive: boolean
  isDislike?: boolean
  disabled?: boolean
  label?: string
}>()

const emit = defineEmits<{
  vote: []
}>()

function handleClick() {
  emit('vote')
}
</script>

<style scoped>
.vote-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
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

.vote-btn:hover {
  background: var(--bg-tertiary);
  color: var(--primary);
}

.vote-btn.active {
  color: var(--primary);
}

.vote-btn.disliked.active {
  color: var(--error);
}

.vote-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.vote-icon {
  font-size: 13px;
  line-height: 1;
}

.vote-count {
  font-weight: 600;
  font-size: 12px;
}
</style>
