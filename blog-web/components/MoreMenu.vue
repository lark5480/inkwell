<template>
  <div class="more-menu-wrapper" ref="menuRef">
    <button class="more-trigger" @click="toggle" :title="t('common.more')">
      <span class="dots">⋯</span>
    </button>
    <Transition name="fade">
      <div v-if="open" class="more-dropdown">
        <button
          v-for="item in items"
          :key="item.key"
          class="menu-item"
          :class="{ 'menu-item-danger': item.danger }"
          @click="handleItemClick(item)"
        >
          <span class="menu-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </button>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
const { t } = useI18n()

export interface MenuItem {
  key: string
  label: string
  icon: string
  danger?: boolean
}

const props = defineProps<{
  items: MenuItem[]
}>()

const emit = defineEmits<{
  action: [key: string]
}>()

const open = ref(false)
const menuRef = ref<HTMLElement | null>(null)

function onDocumentClick(e: MouseEvent) {
  if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
    open.value = false
  }
}

function toggle() {
  open.value = !open.value
}

function handleItemClick(item: MenuItem) {
  open.value = false
  emit('action', item.key)
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
})

onUnmounted(() => {
  document.removeEventListener('click', onDocumentClick)
})
</script>

<style scoped>
.more-menu-wrapper {
  position: relative;
  display: inline-block;
}

.more-trigger {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  color: var(--text-muted);
  font-size: 18px;
  line-height: 1;
  transition: all var(--transition-fast);
}

.more-trigger:hover {
  background: var(--bg-tertiary);
  color: var(--text);
}

.dots {
  letter-spacing: 1px;
  font-weight: 700;
}

.more-dropdown {
  position: absolute;
  right: 0;
  top: 100%;
  z-index: 50;
  min-width: 150px;
  background: var(--bg);
  border: 2px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  margin-top: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
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

.menu-item:hover {
  background: var(--bg-tertiary);
}

.menu-item-danger {
  color: var(--error);
}

.menu-icon {
  font-size: 14px;
  width: 18px;
  text-align: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
