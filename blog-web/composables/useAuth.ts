import { computed, getCurrentInstance, onMounted } from 'vue'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string | null
  role: string
}

const TOKEN_KEY = 'blog_token'
const USER_KEY = 'blog_user_info'

export const useAuth = () => {
  const config = useRuntimeConfig()
  // 跟 useBlogApi 保持一致: SSR 直连 8080，浏览器端动态取主机名
  const apiBase = import.meta.server
    ? config.apiBase + '/api/web/auth'
    : `http://${location.hostname}:8080/api/web/auth`

  const token = useState<string>(TOKEN_KEY, () => '')
  const user = useState<UserInfo | null>('blog_user', () => null)

  // Defer ALL localStorage restoration to onMounted to avoid SSR hydration mismatch.
  // Only register when called from a component setup context (not middleware).
  if (import.meta.client && getCurrentInstance()) {
    onMounted(() => {
      if (!token.value) {
        const saved = localStorage.getItem(TOKEN_KEY)
        if (saved) token.value = saved
      }
      if (!user.value) {
        try {
          const saved = localStorage.getItem(USER_KEY)
          if (saved) user.value = JSON.parse(saved)
        } catch {}
      }
      if (token.value) {
        fetchMe()
      }
    })
  }

  function saveToken(t: string) {
    token.value = t
    if (import.meta.client) localStorage.setItem(TOKEN_KEY, t)
  }

  function saveUser(u: UserInfo) {
    user.value = u
    if (import.meta.client) localStorage.setItem(USER_KEY, JSON.stringify(u))
  }

  function clearToken() {
    token.value = ''
    user.value = null
    if (import.meta.client) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }

  const isLoggedIn = computed(() => !!token.value)
  const currentUser = computed(() => user.value)

  async function login(username: string, password: string): Promise<UserInfo> {
    const res = await $fetch(`${apiBase}/login`, {
      method: 'POST',
      body: { username, password },
    })
    const data = res as any
    if (data.code !== 200) throw new Error(data.message || 'Login failed')
    saveToken(data.data.token)
    saveUser(data.data.user)
    return data.data.user
  }

  async function register(username: string, password: string, nickname?: string, email?: string): Promise<UserInfo> {
    const res = await $fetch(`${apiBase}/register`, {
      method: 'POST',
      body: { username, password, nickname, email },
    })
    const data = res as any
    if (data.code !== 200) throw new Error(data.message || 'Registration failed')
    saveToken(data.data.token)
    saveUser(data.data.user)
    return data.data.user
  }

  async function fetchMe(): Promise<UserInfo | null> {
    if (!token.value) return null
    try {
      const res = await $fetch(`${apiBase}/me`, {
        headers: { Authorization: `Bearer ${token.value}` },
      })
      const data = res as any
      if (data.code !== 200) {
        clearToken()
        return null
      }
      saveUser(data.data)
      return data.data
    } catch {
      clearToken()
      return null
    }
  }

  function logout() {
    clearToken()
  }

  return {
    token,
    user,
    isLoggedIn,
    currentUser,
    login,
    register,
    logout,
    fetchMe,
  }
}
