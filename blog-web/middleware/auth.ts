export default defineNuxtRouteMiddleware((to) => {
  if (import.meta.server) return

  const { isLoggedIn, token } = useAuth()
  // token.value may not be restored yet (onMounted hasn't fired).
  // Check localStorage directly as fallback to avoid false redirects.
  const hasToken = isLoggedIn.value || !!token.value || !!(import.meta.client && localStorage.getItem('blog_token'))
  if (!hasToken) {
    return navigateTo({
      path: '/login',
      query: { redirect: to.fullPath },
    })
  }
})
