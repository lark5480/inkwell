<template>
  <div class="login-container">
    <button class="lang-switch" @click="toggleLocale" :title="t('header.switchLang')">
      {{ locale === 'zh' ? '中文' : 'EN' }}
    </button>
    <div class="login-card">
      <div class="login-header">
        <svg width="48" height="48" viewBox="0 0 32 32" fill="none">
          <rect width="32" height="32" rx="8" fill="url(#login-logo)"/>
          <path d="M16 7l9 5v10l-9 5-9-5V12l9-5z" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
          <path d="M16 7v20" stroke="white" stroke-width="1.6" stroke-linecap="round"/>
          <path d="M7 12l9 5 9-5" stroke="white" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
          <defs><linearGradient id="login-logo" x1="0" y1="0" x2="32" y2="32"><stop offset="0%" stop-color="#4F46E5"/><stop offset="100%" stop-color="#7C3AED"/></linearGradient></defs>
        </svg>
        <h2 class="login-title">{{ t('brand') }}</h2>
        <p class="login-desc">{{ t('login.subtitle') }}</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            :placeholder="t('login.usernamePlaceholder')"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('login.passwordPlaceholder')"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            size="large"
            @click="handleLogin"
          >
            {{ loading ? t('login.signingIn') : t('login.submit') }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useI18n } from '@/composables/useI18n'

const router = useRouter()
const userStore = useUserStore()
const { t, locale, toggleLocale } = useI18n()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = computed<FormRules>(() => ({
  username: [
    { required: true, message: t('login.errorUser'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('login.errorPass'), trigger: 'blur' },
    { min: 6, message: t('login.errorMin'), trigger: 'blur' }
  ]
}))

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success(t('login.success'))
    router.push('/dashboard')
  } catch (err: any) {
    ElMessage.error(err.message || t('login.failed'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #EEF2FF 0%, #F5F3FF 30%, #FFF7ED 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: rgba(79, 70, 229, 0.06);
  top: -150px;
  right: -100px;
}

.login-container::after {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: rgba(249, 115, 22, 0.06);
  bottom: -80px;
  left: -80px;
}

.login-card {
  position: relative;
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  border: 3px solid #C7D2FE;
  box-shadow: 6px 6px 14px rgba(79, 70, 229, 0.12), -4px -4px 12px rgba(255, 255, 255, 0.7);
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-title {
  font-family: 'Baloo 2', 'Inter', sans-serif;
  margin: 16px 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: #1E1B4B;
}

.login-desc {
  font-size: 14px;
  color: #818CF8;
  margin: 0;
}

.lang-switch {
  position: absolute;
  top: 24px;
  right: 24px;
  z-index: 2;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #4F46E5;
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid #C7D2FE;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.lang-switch:hover {
  background: #fff;
  border-color: #4F46E5;
}
</style>
