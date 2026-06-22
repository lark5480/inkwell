<template>
  <div class="container">
    <div class="page-header">
      <h1 class="page-title">{{ t('editor.writeTitle') }}</h1>
    </div>
    <ArticleEditor
      ref="editorRef"
      :submit-label="t('editor.published')"
      :loading="saving"
      @submit="handleCreate"
      @cancel="navigateTo('/my-articles')"
    />
  </div>
</template>

<script setup lang="ts">
import type { ArticleCreateRequest } from '~/composables/useBlogApi'

definePageMeta({ middleware: ['auth'] })

const { createArticle } = useBlogApi()
const { alert } = useModal()
const { t } = useI18n()

useHead({ title: t('nav.write') })
const saving = ref(false)

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

async function handleCreate(data: ArticleCreateRequest) {
  saving.value = true
  try {
    const id = await createArticle(data)
    if (data.status === 'PUBLISHED') {
      // Fetch the article to get its slug
      const article = await useBlogApi().getMyArticle(id)
      navigateTo(`/article/${article.slug}`)
    } else {
      navigateTo('/my-articles')
    }
  } catch (e: any) {
    alert({ message: e.message || t('myArticles.createFailed'), type: 'error' })
  } finally {
    saving.value = false
  }
}
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
</style>
