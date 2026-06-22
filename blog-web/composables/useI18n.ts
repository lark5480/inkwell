import en from '~/locales/en'
import zh from '~/locales/zh'

const dictionaries = { en, zh } as const
export type Locale = keyof typeof dictionaries

/**
 * 轻量 i18n：cookie 持久化（SSR 首屏无闪烁）+ useState 响应式。
 * 用法：const { t, locale, setLocale, toggleLocale } = useI18n()
 * 模板：{{ t('nav.home') }}
 */
export const useI18n = () => {
  const cookie = useCookie<Locale>('locale', {
    default: () => 'en',
    maxAge: 60 * 60 * 24 * 365,
    sameSite: 'lax',
  })
  const locale = useState<Locale>('locale', () => cookie.value)

  function t(key: string, vars?: Record<string, string>): string {
    const dict = dictionaries[locale.value] as any
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
    locale.value = loc
    cookie.value = loc
    if (import.meta.client) {
      document.documentElement.lang = loc === 'zh' ? 'zh-CN' : 'en'
    }
  }

  function toggleLocale() {
    setLocale(locale.value === 'en' ? 'zh' : 'en')
  }

  // 同步 html lang
  if (import.meta.client) {
    document.documentElement.lang = locale.value === 'zh' ? 'zh-CN' : 'en'
  }

  return { locale, t, setLocale, toggleLocale }
}
