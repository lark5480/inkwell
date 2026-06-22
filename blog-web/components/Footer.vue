<template>
  <footer class="footer">
    <div class="container footer-inner">
      <div class="footer-content">
        <div class="footer-brand">
          <svg class="footer-logo" width="28" height="28" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="32" height="32" rx="8" fill="url(#footer-logo-grad)"/>
            <path d="M16 7l9 5v10l-9 5-9-5V12l9-5z" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            <path d="M16 7v20" stroke="white" stroke-width="1.6" stroke-linecap="round"/>
            <path d="M7 12l9 5 9-5" stroke="white" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="footer-logo-grad" x1="0" y1="0" x2="32" y2="32">
                <stop offset="0%" stop-color="#4F46E5"/>
                <stop offset="100%" stop-color="#7C3AED"/>
              </linearGradient>
            </defs>
          </svg>
          <span class="footer-brand-text">{{ footerText }}</span>
        </div>
        <p class="footer-copyright">
          &copy; {{ currentYear }} {{ t('footer.rights') }}
        </p>
        <p class="footer-powered">
          {{ t('footer.powered') }} <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          {{ t('footer.using') }} <strong>Nuxt 3</strong> &amp; <strong>Vue 3</strong>
        </p>
      </div>
      <nav class="footer-nav">
        <NuxtLink to="/" class="footer-link">{{ t('nav.home') }}</NuxtLink>
        <NuxtLink to="/categories" class="footer-link">{{ t('nav.categories') }}</NuxtLink>
        <NuxtLink to="/archive" class="footer-link">{{ t('nav.archive') }}</NuxtLink>
        <NuxtLink to="/links" class="footer-link">{{ t('nav.links') }}</NuxtLink>
        <NuxtLink to="/about" class="footer-link">{{ t('nav.about') }}</NuxtLink>
      </nav>
    </div>
  </footer>
</template>

<script setup lang="ts">
const { t } = useI18n()
const currentYear = new Date().getFullYear()
const footerText = ref('Inkwell')

// Optionally fetch dynamic footer text
if (process.client) {
  try {
    const { getSiteInfo } = useBlogApi()
    getSiteInfo().then((info) => {
      if (info?.siteTitle) footerText.value = info.siteTitle
    })
  } catch {}
}
</script>

<style scoped>
.footer {
  margin-top: var(--space-16);
  padding: var(--space-8) 0;
  border-top: 3px solid var(--border);
  background: var(--bg-secondary);
}

.footer-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-6);
}

.footer-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-1);
}

.footer-logo {
  flex-shrink: 0;
}

.footer-brand-text {
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 700;
  color: var(--text);
}

.footer-copyright {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.footer-powered {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.footer-powered svg {
  color: var(--accent);
}

.footer-nav {
  display: flex;
  gap: var(--space-4);
}

.footer-link {
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all var(--transition-fast);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-md);
}

.footer-link:hover {
  color: var(--primary);
  background: var(--bg-tertiary);
}

@media (max-width: 768px) {
  .footer-inner {
    flex-direction: column-reverse;
    text-align: center;
  }

  .footer-content {
    align-items: center;
  }

  .footer-nav {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
