<template>
  <ClientOnly>
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="state.visible" class="modal-mask" @click.self="handleCancel">
          <div class="modal-box" :class="`modal-${state.options.type || 'info'}`">
            <div class="modal-icon-wrap">
              <svg v-if="state.options.type === 'warning'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
              <svg v-else-if="state.options.type === 'error'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              <svg v-else width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            </div>
            <h3 v-if="state.options.title" class="modal-title">{{ state.options.title }}</h3>
            <p class="modal-message">{{ state.options.message }}</p>
            <div class="modal-actions">
              <button v-if="!isAlert" class="modal-btn modal-btn-cancel" @click="handleCancel">
                {{ state.options.cancelText || t('common.cancel') }}
              </button>
              <button class="modal-btn modal-btn-confirm" @click="handleConfirm">
                {{ confirmLabel }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </ClientOnly>
</template>

<script setup lang="ts">
const { state, close } = useModal()
const { t } = useI18n()

const isAlert = computed(() => state.value.options.type === 'info' && !state.value.options.cancelText)

const confirmLabel = computed(() => {
  if (state.value.options.confirmText) return state.value.options.confirmText
  if (state.value.options.type === 'error') return t('common.ok')
  return isAlert.value ? t('common.ok') : t('common.confirm')
})

function handleConfirm() {
  close(true)
}

function handleCancel() {
  close(false)
}
</script>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  padding: 1rem;
}

.modal-box {
  width: 100%;
  max-width: 420px;
  background: var(--bg-card, #fff);
  border: 3px solid var(--border, #e5e7eb);
  border-radius: var(--clay-border-radius, 20px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 1.75rem;
  text-align: center;
}

.modal-icon-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 0.75rem;
}

.modal-warning .modal-icon-wrap { color: #f59e0b; }
.modal-error .modal-icon-wrap { color: #ef4444; }
.modal-info .modal-icon-wrap { color: var(--primary, #4f46e5); }

.modal-title {
  font-family: var(--font-heading, sans-serif);
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text, #1f2937);
  margin: 0 0 0.5rem;
}

.modal-message {
  font-size: 0.95rem;
  color: var(--text-secondary, #6b7280);
  line-height: 1.6;
  margin: 0 0 1.5rem;
}

.modal-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.modal-btn {
  padding: 0.5rem 1.5rem;
  border-radius: var(--clay-border-radius, 12px);
  font-family: var(--font-heading, sans-serif);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.modal-btn-cancel {
  background: var(--bg-tertiary, #f3f4f6);
  color: var(--text-secondary, #6b7280);
  border-color: var(--border, #e5e7eb);
}

.modal-btn-cancel:hover {
  background: var(--bg-secondary, #e5e7eb);
  color: var(--text, #1f2937);
}

.modal-btn-confirm {
  background: var(--primary, #4f46e5);
  color: #fff;
  border-color: var(--primary, #4f46e5);
}

.modal-btn-confirm:hover {
  background: var(--primary-dark, #4338ca);
  transform: translateY(-1px);
}

.modal-error .modal-btn-confirm {
  background: #ef4444;
  border-color: #ef4444;
}

.modal-error .modal-btn-confirm:hover {
  background: #dc2626;
}

.modal-warning .modal-btn-confirm {
  background: #f59e0b;
  border-color: #f59e0b;
}

.modal-warning .modal-btn-confirm:hover {
  background: #d97706;
}

/* Transition */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .modal-box,
.modal-leave-active .modal-box {
  transition: transform 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-box,
.modal-leave-to .modal-box {
  transform: scale(0.95) translateY(-10px);
}
</style>
