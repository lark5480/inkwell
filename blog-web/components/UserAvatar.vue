<template>
  <component
    :is="link && user.id ? 'NuxtLink' : 'span'"
    :to="link && user.id ? `/user/${user.id}` : undefined"
    class="user-avatar"
    :class="[`size-${size}`, { clickable: link && user.id }]"
    :style="{ width: size + 'px', height: size + 'px' }"
  >
    <img
      v-if="avatarUrl"
      :src="avatarUrl"
      :alt="user.nickname || ''"
      class="avatar-img"
      @error="onImgError"
    />
    <span
      v-else
      class="avatar-placeholder"
      :style="{ background: placeholderColor }"
    >{{ initial }}</span>
  </component>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(defineProps<{
  user: { id?: number | null; nickname?: string | null; avatar?: string | null }
  size?: number
  link?: boolean
}>(), {
  size: 40,
  link: true,
})

const imgFailed = ref(false)

// 解析头像 URL：相对路径（/ 开头）补全为后端绝对 URL，支持局域网 IP 访问
const avatarUrl = computed(() => {
  if (!props.user.avatar) return null
  if (props.user.avatar.startsWith('/')) {
    const base = import.meta.client
      ? `http://${location.hostname}:8080`
      : 'http://localhost:8080'
    return base + props.user.avatar
  }
  return props.user.avatar
})

const initial = computed(() => {
  const name = props.user.nickname || props.user.id?.toString() || '?'
  return name.charAt(0).toUpperCase()
})

const placeholderColor = computed(() => {
  const id = props.user.id || 0
  const palette = [
    'linear-gradient(135deg, #6366f1, #a855f7)',
    'linear-gradient(135deg, #ec4899, #f43f5e)',
    'linear-gradient(135deg, #22c55e, #16a34a)',
    'linear-gradient(135deg, #f59e0b, #d97706)',
    'linear-gradient(135deg, #3b82f6, #2563eb)',
    'linear-gradient(135deg, #14b8a6, #0d9488)',
    'linear-gradient(135deg, #8b5cf6, #7c3aed)',
    'linear-gradient(135deg, #ef4444, #dc2626)',
  ]
  return palette[Math.abs(id) % palette.length]
})

function onImgError() {
  imgFailed.value = true
}
</script>

<style scoped>
.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  text-decoration: none;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.user-avatar.clickable:hover {
  transform: scale(1.05);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: white;
  font-family: var(--font-heading);
  font-weight: 700;
  user-select: none;
}

.size-24 { font-size: 10px; }
.size-28 { font-size: 11px; }
.size-32 { font-size: 12px; }
.size-36 { font-size: 13px; }
.size-40 { font-size: 14px; }
.size-48 { font-size: 17px; }
.size-64 { font-size: 22px; }
.size-80 { font-size: 28px; }
</style>
