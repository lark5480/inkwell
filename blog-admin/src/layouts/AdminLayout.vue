<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="admin-sidebar">
      <div class="logo" :class="{ collapsed: isCollapsed }">
        <svg v-if="!isCollapsed" class="logo-svg" width="28" height="28" viewBox="0 0 32 32" fill="none">
          <rect width="32" height="32" rx="8" fill="url(#admin-logo)"/>
          <path d="M16 7l9 5v10l-9 5-9-5V12l9-5z" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
          <path d="M16 7v20" stroke="white" stroke-width="1.6" stroke-linecap="round"/>
          <path d="M7 12l9 5 9-5" stroke="white" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
          <defs><linearGradient id="admin-logo" x1="0" y1="0" x2="32" y2="32"><stop offset="0%" stop-color="#818CF8"/><stop offset="100%" stop-color="#A78BFA"/></linearGradient></defs>
        </svg>
        <span v-if="!isCollapsed">{{ t('brand') }}</span>
        <span v-else class="logo-mini">IA</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        background-color="#1E1245"
        text-color="#A5B4FC"
        active-text-color="#FFFFFF"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>{{ t('menu.dashboard') }}</template>
        </el-menu-item>
        <el-menu-item index="/articles">
          <el-icon><Document /></el-icon>
          <template #title>{{ t('menu.articles') }}</template>
        </el-menu-item>
        <el-menu-item index="/categories">
          <el-icon><Folder /></el-icon>
          <template #title>{{ t('menu.categories') }}</template>
        </el-menu-item>
        <el-menu-item index="/tags">
          <el-icon><PriceTag /></el-icon>
          <template #title>{{ t('menu.tags') }}</template>
        </el-menu-item>
        <el-menu-item index="/comments">
          <el-icon><ChatDotSquare /></el-icon>
          <template #title>{{ t('menu.comments') }}</template>
        </el-menu-item>
        <el-menu-item index="/reports">
          <el-icon><WarningFilled /></el-icon>
          <template #title>{{ t('menu.reports') }}</template>
        </el-menu-item>
        <el-menu-item index="/links">
          <el-icon><Link /></el-icon>
          <template #title>{{ t('menu.links') }}</template>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <template #title>{{ t('menu.users') }}</template>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <template #title>{{ t('menu.settings') }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapsed = !isCollapsed" :size="20">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">{{ t('header.home') }}</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-button text size="small" class="lang-toggle-btn" @click="toggleLocale" :title="t('header.switchLang')">
            {{ locale === 'zh' ? '中文' : 'EN' }}
          </el-button>
          <span class="user-name">{{ userStore.userInfo?.username || t('header.admin') }}</span>
          <el-button type="danger" size="small" @click="handleLogout">{{ t('header.logout') }}</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view v-slot="{ Component }">
          <transition name="slide-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useI18n } from '@/composables/useI18n'
import {
  DataAnalysis, Document, Folder, PriceTag, ChatDotSquare,
  Link, Setting, Fold, Expand, User, WarningFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()
const { t, locale, toggleLocale } = useI18n()

const isCollapsed = ref(false)

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/articles')) return '/articles'
  return path
})

const currentTitle = computed(() => {
  const meta = route.meta as any
  return meta?.title || ''
})

async function handleLogout() {
  await userStore.logout()
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.admin-sidebar {
  background: #1E1245;
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  font-family: 'Baloo 2', 'Inter', sans-serif;
  font-size: 18px;
  font-weight: 700;
  background: rgba(0, 0, 0, 0.2);
  white-space: nowrap;
  overflow: hidden;
}

.logo.collapsed {
  font-size: 14px;
}

.logo-svg {
  flex-shrink: 0;
}

.logo-mini {
  font-size: 16px;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 3px solid #E0E7FF;
  padding: 0 20px;
  height: var(--header-height);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #4F46E5;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #1E1B4B;
}

.lang-toggle-btn {
  color: #4F46E5;
  font-weight: 600;
}

.lang-toggle-btn:hover {
  color: #7C3AED;
}

.admin-main {
  background: #F5F3FF;
  padding: 24px;
  overflow-y: auto;
}

.el-menu {
  border-right: none;
}

/* Override Element Plus menu active styles */
:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #4F46E5, #7C3AED) !important;
  border-radius: 8px;
  margin: 4px 8px;
  width: auto;
}
</style>
