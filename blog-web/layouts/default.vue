<template>
  <div class="layout">
    <!-- Header -->
    <header class="header">
      <div class="container header-inner">
        <NuxtLink to="/" class="logo">
          <svg class="logo-icon" width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="32" height="32" rx="8" fill="url(#logo-gradient)"/>
            <path d="M16 7l9 5v10l-9 5-9-5V12l9-5z" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            <path d="M16 7v20" stroke="white" stroke-width="1.6" stroke-linecap="round"/>
            <path d="M7 12l9 5 9-5" stroke="white" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="logo-gradient" x1="0" y1="0" x2="32" y2="32">
                <stop offset="0%" stop-color="#4F46E5"/>
                <stop offset="100%" stop-color="#7C3AED"/>
              </linearGradient>
            </defs>
          </svg>
          <span class="logo-text">{{ siteName }}</span>
        </NuxtLink>
        <button class="menu-toggle" @click="mobileMenuOpen = !mobileMenuOpen" aria-label="Toggle menu">
          <span class="hamburger" :class="{ active: mobileMenuOpen }"></span>
        </button>
        <nav class="nav" :class="{ open: mobileMenuOpen }">
          <NuxtLink to="/" class="nav-link" @click="mobileMenuOpen = false">{{ t('nav.home') }}</NuxtLink>
          <NuxtLink to="/categories" class="nav-link" @click="mobileMenuOpen = false">{{ t('nav.categories') }}</NuxtLink>
          <NuxtLink to="/archive" class="nav-link" @click="mobileMenuOpen = false">{{ t('nav.archive') }}</NuxtLink>
          <NuxtLink to="/about" class="nav-link" @click="mobileMenuOpen = false">{{ t('nav.about') }}</NuxtLink>
          <NuxtLink to="/links" class="nav-link" @click="mobileMenuOpen = false">{{ t('nav.links') }}</NuxtLink>
          <NuxtLink to="/search" class="nav-link nav-link-search" @click="mobileMenuOpen = false">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </NuxtLink>
          <button class="nav-link nav-theme-toggle" @click="toggleLocale" :aria-label="t('nav.switchLang')" :title="t('nav.switchLang')">
            <span class="lang-label">{{ locale === 'zh' ? '中' : 'EN' }}</span>
          </button>
          <button class="nav-link nav-theme-toggle" @click="toggleDark" :aria-label="t('nav.switchTheme')" :title="t('nav.switchTheme')">
            <!-- Sun icon (shown in dark mode) -->
            <svg v-if="isDark" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
            <!-- Moon icon (shown in light mode) -->
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
          </button>
          <!-- Auth -->
          <template v-if="isLoggedIn">
            <NuxtLink to="/write" class="nav-link nav-write-btn" @click="mobileMenuOpen = false">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              {{ t('nav.write') }}
            </NuxtLink>
            <!-- Notification Bell (消息 + 通知入口) -->
            <NuxtLink to="/messages" class="nav-link nav-notif-bell" @click="mobileMenuOpen = false">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
              <span v-if="unreadCount > 0" class="notif-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </NuxtLink>
            <div class="nav-user-menu" @click.stop>
              <button class="nav-user-btn" @click="userMenuOpen = !userMenuOpen">
                <img v-if="user?.avatar" :src="user.avatar" :alt="user.nickname" class="nav-user-avatar" />
                <div v-else class="nav-user-avatar-placeholder">{{ (user?.nickname || user?.username || '?').charAt(0).toUpperCase() }}</div>
              </button>
              <div v-if="userMenuOpen" class="nav-dropdown">
                <NuxtLink :to="`/user/${user?.id}`" class="nav-dropdown-item" @click="userMenuOpen = false">{{ t('nav.myProfile') }}</NuxtLink>
                <NuxtLink to="/my-articles" class="nav-dropdown-item" @click="userMenuOpen = false">{{ t('nav.myArticles') }}</NuxtLink>
                <NuxtLink to="/settings" class="nav-dropdown-item" @click="userMenuOpen = false">{{ t('settings.title') }}</NuxtLink>
                <hr class="nav-dropdown-divider" />
                <button class="nav-dropdown-item nav-dropdown-logout" @click="handleLogout">{{ t('nav.logout') }}</button>
              </div>
            </div>
          </template>
          <template v-else>
            <NuxtLink to="/login" class="nav-link" @click="mobileMenuOpen = false">{{ t('nav.login') }}</NuxtLink>
          </template>
        </nav>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main">
      <slot />
    </main>

    <!-- Footer -->
    <Footer />

    <!-- Global Modal -->
    <AppModal />
  </div>
</template>

<script setup lang="ts">
const { getSiteInfo, getUnreadCount } = useBlogApi()
const { isLoggedIn, user, logout, token } = useAuth()
const { isDark, toggleDark } = useTheme()
const { t, locale, toggleLocale } = useI18n()
const siteName = ref('Inkwell')
const mobileMenuOpen = ref(false)
const userMenuOpen = ref(false)
// Shared unread count — notifications page can decrement directly
const unreadCount = useState<number>('notif_unread_count', () => 0)

function handleLogout() {
  userMenuOpen.value = false
  logout()
  navigateTo('/')
}

// Close user dropdown on outside click
if (import.meta.client) {
  document.addEventListener('click', () => {
    userMenuOpen.value = false
  })
}

// Fetch site info in background (non-blocking for layout)
if (process.client) {
  getSiteInfo().then((info) => {
    if (info?.siteTitle) siteName.value = info.siteTitle
  }).catch(() => {})
}

// Poll unread count every 30s when logged in
let unreadTimer: ReturnType<typeof setInterval> | null = null
function fetchUnread() {
  if (token.value) {
    getUnreadCount().then(res => { unreadCount.value = res.count }).catch(() => {})
  }
}

if (process.client) {
  fetchUnread()
  unreadTimer = setInterval(fetchUnread, 30000)
}

onBeforeUnmount(() => {
  if (unreadTimer) clearInterval(unreadTimer)
})

// Refresh unread on route change (user may have read notifications)
const route = useRoute()
watch(() => route.fullPath, () => {
  mobileMenuOpen.value = false
  fetchUnread()
})
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* ===== Header ===== */
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: var(--header-height);
  background: rgba(245, 243, 255, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 3px solid var(--border);
}

@media (prefers-color-scheme: dark) {
  :root:not(.light) .header {
    background: rgba(15, 10, 46, 0.92);
  }
}

:global(html.dark) .header {
  background: rgba(15, 10, 46, 0.92);
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--text);
  text-decoration: none;
  transition: opacity var(--transition-fast);
}

.logo:hover {
  opacity: 0.85;
}

.logo-icon {
  flex-shrink: 0;
}

.logo-text {
  font-family: var(--font-heading);
  letter-spacing: -0.01em;
}

/* ===== Hamburger ===== */
.menu-toggle {
  display: none;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border: 3px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
  transition: all var(--transition-fast);
}

.menu-toggle:hover {
  background: var(--bg-tertiary);
}

.hamburger {
  display: block;
  width: 18px;
  height: 2px;
  background-color: var(--text);
  position: relative;
  transition: background-color var(--transition-fast);
  border-radius: 2px;
}

.hamburger::before,
.hamburger::after {
  content: '';
  position: absolute;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: var(--text);
  transition: transform var(--transition-fast);
  border-radius: 2px;
}

.hamburger::before { top: -6px; }
.hamburger::after { top: 6px; }

.hamburger.active {
  background-color: transparent;
}

.hamburger.active::before {
  transform: rotate(45deg);
  top: 0;
}

.hamburger.active::after {
  transform: rotate(-45deg);
  top: 0;
}

/* ===== Navigation ===== */
.nav {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.nav-link {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--clay-border-radius);
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  text-decoration: none;
}

.nav-link:hover,
.nav-link.router-link-active {
  color: var(--primary);
  background: var(--bg-tertiary);
}

.nav-link-search {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-2);
  width: 40px;
  height: 40px;
  border: var(--clay-border);
  border-radius: 12px;
  background: var(--bg-card);
  margin-left: var(--space-2);
}

.nav-link-search:hover {
  background: var(--bg-tertiary);
  border-color: var(--primary);
}

.nav-theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-2);
  width: 40px;
  height: 40px;
  border: var(--clay-border);
  border-radius: 12px;
  background: var(--bg-card);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.nav-theme-toggle:hover {
  background: var(--bg-tertiary);
  border-color: var(--primary);
  color: var(--primary);
}

.nav-theme-toggle svg {
  transition: transform 0.3s ease;
}

.nav-theme-toggle:hover svg {
  transform: rotate(15deg);
}

.lang-label {
  font-family: var(--font-heading);
  font-size: var(--font-size-xs);
  font-weight: 700;
  letter-spacing: 0.02em;
}

.nav-write-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4) !important;
  background: var(--primary);
  color: white !important;
  border-radius: var(--clay-border-radius);
  border: none;
}

.nav-write-btn:hover {
  background: var(--primary-dark) !important;
  color: white !important;
  transform: translateY(-1px);
}

.nav-notif-bell {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-2) !important;
  width: 40px;
  height: 40px;
  border: var(--clay-border);
  border-radius: 12px;
  background: var(--bg-card);
  margin-left: var(--space-2);
}

.nav-notif-bell:hover {
  background: var(--bg-tertiary);
  border-color: var(--primary);
  color: var(--primary);
}

.notif-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background: var(--error, #ef4444);
  color: white;
  font-size: 11px;
  font-weight: 700;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.nav-user-menu {
  position: relative;
  margin-left: var(--space-2);
}

.nav-user-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
}

.nav-user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
  transition: border-color var(--transition-fast);
}

.nav-user-btn:hover .nav-user-avatar {
  border-color: var(--primary);
}

.nav-user-avatar-placeholder {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 700;
  border: 2px solid transparent;
  transition: border-color var(--transition-fast);
}

.nav-user-btn:hover .nav-user-avatar-placeholder {
  border-color: var(--primary-light);
}

.nav-dropdown {
  position: absolute;
  top: calc(100% + var(--space-2));
  right: 0;
  min-width: 180px;
  background: var(--bg-card);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
  box-shadow: var(--shadow-lg);
  padding: var(--space-2);
  z-index: 200;
}

.nav-dropdown-item {
  display: block;
  width: 100%;
  padding: var(--space-3) var(--space-4);
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-secondary);
  text-decoration: none;
  border-radius: 8px;
  transition: all var(--transition-fast);
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
}

.nav-dropdown-item:hover {
  color: var(--primary);
  background: var(--bg-tertiary);
}

.nav-dropdown-divider {
  margin: var(--space-2) 0;
  border: none;
  border-top: var(--clay-border);
}

.nav-dropdown-logout:hover {
  color: var(--error) !important;
}

/* ===== Main ===== */
.main {
  flex: 1;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .menu-toggle {
    display: flex;
  }

  .nav {
    display: none;
    position: absolute;
    top: var(--header-height);
    left: 0;
    right: 0;
    flex-direction: column;
    background: var(--bg-card);
    border-bottom: 3px solid var(--border);
    padding: var(--space-4);
    gap: var(--space-2);
    box-shadow: var(--clay-shadow);
  }

  .nav.open {
    display: flex;
  }

  .nav-link {
    width: 100%;
    padding: var(--space-3) var(--space-4);
  }

  .nav-link-search {
    width: 100%;
    height: auto;
    justify-content: flex-start;
    gap: var(--space-2);
    margin-left: 0;
  }
}
</style>
