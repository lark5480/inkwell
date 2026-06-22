export const useTheme = () => {
  const theme = ref<'light' | 'dark' | 'system'>('system')
  const isDark = ref(false)

  function resolveIsDark(pref: string): boolean {
    if (pref === 'dark') return true
    if (pref === 'light') return false
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  }

  function applyTheme() {
    isDark.value = resolveIsDark(theme.value)
    document.documentElement.classList.toggle('dark', isDark.value)
  }

  function setTheme(newTheme: 'light' | 'dark' | 'system') {
    theme.value = newTheme
    try { localStorage.setItem('theme', newTheme) } catch {}
    applyTheme()
  }

  function toggleDark() {
    setTheme(isDark.value ? 'light' : 'dark')
  }

  // Init on client
  if (import.meta.client) {
    try {
      const stored = localStorage.getItem('theme')
      if (stored === 'light' || stored === 'dark' || stored === 'system') {
        theme.value = stored
      }
    } catch {}
    isDark.value = resolveIsDark(theme.value)
    document.documentElement.classList.toggle('dark', isDark.value)

    // Listen for system changes
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (theme.value === 'system') applyTheme()
    })
  }

  return { theme, isDark, setTheme, toggleDark }
}
