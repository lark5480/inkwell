<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="auth-card clay-card">
        <h1 class="auth-title">{{ t('auth.loginTitle') }}</h1>
        <p class="auth-desc">{{ t('auth.loginDesc') }}</p>

        <form @submit.prevent="handleLogin" class="auth-form">
          <div class="form-group">
            <label for="username">{{ t('auth.username') }}</label>
            <input id="username" v-model="username" type="text" :placeholder="t('auth.enterUsername')" required class="form-input" />
          </div>
          <div class="form-group">
            <label for="password">{{ t('auth.password') }}</label>
            <div class="password-wrapper">
              <input id="password" v-model="password" :type="showPassword ? 'text' : 'password'" :placeholder="t('auth.enterPassword')" required class="form-input" />
              <button type="button" class="password-toggle" @click="showPassword = !showPassword" tabindex="-1" :aria-label="showPassword ? t('auth.hidePassword') : t('auth.showPassword')">
                <!-- Eye open -->
                <svg v-if="showPassword" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                <!-- Eye closed -->
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
              </button>
            </div>
          </div>

          <div v-if="error" class="form-error">{{ error }}</div>

          <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
            {{ loading ? t('auth.signingIn') : t('auth.signIn') }}
          </button>
        </form>

        <p class="auth-footer">
          {{ t('auth.noAccount') }} <NuxtLink to="/register" class="auth-link">{{ t('auth.createOne') }}</NuxtLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const { login } = useAuth()
const { t } = useI18n()
const router = useRouter()

const username = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  if (!username.value.trim() || !password.value.trim()) return
  loading.value = true
  error.value = ''
  try {
    await login(username.value.trim(), password.value.trim())
    router.push('/')
  } catch (e: any) {
    error.value = e?.message || t('auth.loginFailed')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-wrapper {
  padding: var(--space-10) 0;
  min-height: 60vh;
  display: flex;
  align-items: center;
}

.container {
  max-width: 440px;
  margin: 0 auto;
  width: 100%;
  padding: 0 var(--space-4);
}

.auth-card {
  padding: var(--space-8);
}

.auth-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-3xl);
  font-weight: 800;
  text-align: center;
  margin-bottom: var(--space-2);
  color: var(--text);
}

.auth-desc {
  text-align: center;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-6);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.form-group label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-secondary);
}

.form-input {
  padding: 0.75rem 1rem;
  border: 3px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg);
  color: var(--text);
  font-size: var(--font-size-sm);
  transition: all var(--transition-fast);
  outline: none;
}

.form-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.password-wrapper {
  position: relative;
  width: 100%;
}

.password-wrapper > .form-input {
  width: 100%;
}

.password-toggle {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  padding: 2px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: color var(--transition-fast);
}

.password-toggle:hover {
  color: var(--text);
}

.form-error {
  color: var(--error);
  font-size: var(--font-size-sm);
  text-align: center;
}

.btn-block {
  width: 100%;
  justify-content: center;
  padding: 0.75rem;
  font-size: var(--font-size-base);
}

.auth-footer {
  text-align: center;
  margin-top: var(--space-6);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.auth-link {
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
}

.auth-link:hover {
  text-decoration: underline;
}
</style>
