export default defineNuxtRouteMiddleware((to) => {
  if (import.meta.server) return

  const { isLoggedIn } = useAuth()
  if (!isLoggedIn.value) {
    return navigateTo({
      path: '/login',
      query: { redirect: to.fullPath },
    })
  }
})
