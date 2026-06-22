<template>
  <nav v-if="totalPages > 1" class="pagination" aria-label="Pagination">
    <!-- First page -->
    <button
      class="page-btn page-edge"
      :disabled="currentPage <= 1"
      @click="handlePageChange(1)"
      :aria-label="t('pagination.first')"
      :title="t('pagination.first')"
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="11 17 6 12 11 7"/><polyline points="18 17 13 12 18 7"/></svg>
    </button>

    <!-- Prev -->
    <button
      class="page-btn"
      :disabled="currentPage <= 1"
      @click="handlePageChange(currentPage - 1)"
      :aria-label="t('pagination.prev')"
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
    </button>

    <!-- Page numbers -->
    <template v-for="page in displayedPages" :key="page">
      <button
        v-if="page === '...'"
        class="page-btn page-ellipsis"
        disabled
      >
        ...
      </button>
      <button
        v-else
        class="page-btn"
        :class="{ active: page === currentPage }"
        @click="handlePageChange(page as number)"
      >
        {{ page }}
      </button>
    </template>

    <!-- Next -->
    <button
      class="page-btn"
      :disabled="currentPage >= totalPages"
      @click="handlePageChange(currentPage + 1)"
      :aria-label="t('pagination.next')"
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
    </button>

    <!-- Last page -->
    <button
      class="page-btn page-edge"
      :disabled="currentPage >= totalPages"
      @click="handlePageChange(totalPages)"
      :aria-label="t('pagination.last')"
      :title="t('pagination.last')"
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="13 17 18 12 13 7"/><polyline points="6 17 11 12 6 7"/></svg>
    </button>

    <!-- Jump to page -->
    <div class="page-jump">
      <span class="page-jump-label">{{ t('pagination.jumpTo') }}</span>
      <input
        v-model.number="jumpValue"
        type="number"
        class="page-jump-input"
        :min="1"
        :max="totalPages"
        @keyup.enter="handleJump"
      />
      <span class="page-jump-label">{{ t('pagination.page') }}</span>
      <button class="page-btn page-jump-btn" @click="handleJump">{{ t('pagination.go') }}</button>
    </div>
  </nav>
</template>

<script setup lang="ts">
const props = defineProps<{
  currentPage: number
  totalPages: number
}>()

const emit = defineEmits<{
  'page-change': [page: number]
}>()

const { t } = useI18n()

const jumpValue = ref<number | ''>('')

// 当外部 currentPage 变化时，清空输入框
watch(() => props.currentPage, () => {
  jumpValue.value = ''
})

const displayedPages = computed(() => {
  const pages: (number | string)[] = []
  const total = props.totalPages
  const current = props.currentPage

  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
    return pages
  }

  pages.push(1)

  if (current > 3) pages.push('...')

  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)

  for (let i = start; i <= end; i++) pages.push(i)

  if (current < total - 2) pages.push('...')

  pages.push(total)

  return pages
})

function handlePageChange(page: number) {
  if (page < 1 || page > props.totalPages || page === props.currentPage) return
  emit('page-change', page)
}

function handleJump() {
  const page = Number(jumpValue.value)
  if (!page || isNaN(page)) return
  handlePageChange(page)
  jumpValue.value = ''
}
</script>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-8) 0;
  flex-wrap: wrap;
}

.page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  min-width: 42px;
  height: 42px;
  padding: 0 var(--space-3);
  border: 3px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.page-btn:hover:not(:disabled):not(.active) {
  border-color: var(--primary);
  color: var(--primary);
  transform: translateY(-2px);
  box-shadow: var(--clay-shadow);
}

.page-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: var(--text-inverse);
  box-shadow: 4px 4px 10px rgba(79, 70, 229, 0.25);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

.page-ellipsis {
  border: none;
  background: none;
  cursor: default;
  min-width: auto;
}

.page-edge {
  padding: 0 var(--space-2);
}

/* Jump section */
.page-jump {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-left: var(--space-3);
}

.page-jump-label {
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  white-space: nowrap;
}

.page-jump-input {
  width: 56px;
  height: 42px;
  padding: 0 var(--space-2);
  border: 3px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  color: var(--text);
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  text-align: center;
  outline: none;
  transition: all var(--transition-fast);
  /* hide spinner */
  appearance: textfield;
  -moz-appearance: textfield;
}

.page-jump-input::-webkit-outer-spin-button,
.page-jump-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.page-jump-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.page-jump-btn {
  min-width: auto;
  height: 42px;
  padding: 0 var(--space-3);
}

@media (max-width: 768px) {
  .page-jump {
    width: 100%;
    justify-content: center;
    margin-left: 0;
    margin-top: var(--space-2);
  }
}
</style>
