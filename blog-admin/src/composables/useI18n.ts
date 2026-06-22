import { ref, computed } from 'vue'
import en from '@/locales/en'
import zh from '@/locales/zh'

const dictionaries = { en, zh } as const
export type Locale = keyof typeof dictionaries

// 全局语言状态（SPA 单例）
const currentLocale = ref<Locale>('zh')

// 初始化：从 localStorage 恢复
if (typeof window !== 'undefined') {
  const stored = localStorage.getItem('locale')
  if (stored === 'zh' || stored === 'en') {
    currentLocale.value = stored
  }
}

export const useI18n = () => {
  const locale = computed(() => currentLocale.value)

  function t(key: string, vars?: Record<string, string>): string {
    const dict = dictionaries[currentLocale.value] as any
    const parts = key.split('.')
    let val: any = dict
    for (const k of parts) {
      val = val?.[k]
      if (val === undefined) return key
    }
    if (typeof val !== 'string') return key
    if (vars) {
      for (const [k, v] of Object.entries(vars)) {
        val = val.replaceAll(`{${k}}`, v)
      }
    }
    return val
  }

  function setLocale(loc: Locale) {
    currentLocale.value = loc
    if (typeof window !== 'undefined') {
      localStorage.setItem('locale', loc)
      document.documentElement.lang = loc === 'zh' ? 'zh-CN' : 'en'
    }
  }

  function toggleLocale() {
    setLocale(currentLocale.value === 'en' ? 'zh' : 'en')
  }

  return { locale, t, setLocale, toggleLocale }
}
