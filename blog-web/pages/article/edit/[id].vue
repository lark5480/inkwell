<template>
  <div class="container">
    <div class="page-header">
      <h1 class="page-title">{{ t('editor.editTitle') }}</h1>
    </div>
    <div v-if="loading" class="loading-state">Loading...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <ArticleEditor
      ref="editorRef"
      v-else-if="article"
      :initial-data="article"
      :submit-label="t('common.save')"
      :loading="saving"
      @submit="handleUpdate"
      @cancel="navigateTo('/my-articles')"
    />
  </div>
</template>

<script setup lang="ts">
import type { ArticleDetailResponse, ArticleCreateRequest } from '~/composables/useBlogApi'

definePageMeta({ middleware: ['auth'] })

const route = useRoute()
const articleId = Number(route.params.id)
const { getMyArticle, updateArticle } = useBlogApi()
const { alert } = useModal()
const { t } = useI18n()

const article = ref<ArticleDetailResponse | null>(null)
const loading = ref(true)
const saving = ref(false)
const error = ref('')

const editorRef = ref<{ hasChanges: boolean } | null>(null)

onBeforeRouteLeave((to) => {
  if (editorRef.value?.hasChanges) {
    const answer = window.confirm('You have unsaved changes. Leave anyway?')
    if (!answer) return false
  }
})

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (editorRef.value?.hasChanges) {
    e.preventDefault()
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

useHead({ title: t('editor.editTitle') })

async function loadArticle() {
  try {
    article.value = await getMyArticle(articleId)
  } catch (e: any) {
    error.value = e.message || 'Failed to load article'
  } finally {
    loading.value = false
  }
}

async function handleUpdate(data: ArticleCreateRequest) {
  saving.value = true
  try {
    await updateArticle(articleId, data)
    navigateTo('/my-articles')
  } catch (e: any) {
    alert({ message: e.message || t('myArticles.updateFailed'), type: 'error' })
  } finally {
    saving.value = false
  }
}

onMounted(loadArticle)
</script>

<style scoped>
.page-header {
  padding: var(--space-8) 0 var(--space-4);
}

.page-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--text);
}

.loading-state,
.error-state {
  text-align: center;
  padding: var(--space-16) 0;
  color: var(--text-secondary);
  font-family: var(--font-heading);
}

.error-state {
  color: var(--error);
}
</style>
