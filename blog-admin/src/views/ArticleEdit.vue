<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ isEdit ? t('article.editTitle') : t('article.createTitle') }}</h2>
    </div>

    <div v-loading="loading">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="max-width: 900px"
      >
        <el-form-item :label="t('article.title')" prop="title">
          <el-input v-model="form.title" :placeholder="t('article.title')" />
        </el-form-item>

        <el-form-item :label="t('article.slug')" prop="slug">
          <el-input v-model="form.slug" :placeholder="t('article.slugPlaceholder')">
            <template #append>
              <el-button @click="generateSlug">{{ t('article.generate') }}</el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item :label="t('article.category')">
          <el-select v-model="form.categoryId" :placeholder="t('article.selectCategory')" clearable style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('article.selectTags')">
          <el-select
            v-model="form.tagIds"
            multiple
            :placeholder="t('article.selectTags')"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('article.coverImage')">
          <el-input v-model="form.coverImage" :placeholder="t('article.coverPlaceholder')">
            <template #append>
              <el-button @click="showImageUpload = !showImageUpload">{{ t('article.upload') }}</el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="showImageUpload" label="&nbsp;">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :show-file-list="false"
            accept="image/*"
          >
            <el-button type="primary">{{ t('article.chooseImage') }}</el-button>
          </el-upload>
        </el-form-item>

        <el-form-item :label="t('article.summary')">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            :placeholder="t('article.summaryPlaceholder')"
          />
        </el-form-item>

        <el-form-item :label="t('article.content')" prop="content" class="editor-item">
          <div class="editor-wrapper">
            <MdEditor
              v-model="form.content"
              :toolbars="toolbars"
              language="zh-CN"
              :theme="editorTheme"
              :height="editorHeight"
              :placeholder="t('article.contentPlaceholder')"
            />
          </div>
        </el-form-item>

        <el-form-item :label="t('article.status')">
          <el-select v-model="form.status" style="width: 200px">
            <el-option :label="t('article.draft')" value="DRAFT" />
            <el-option :label="t('article.publishedOpt')" value="PUBLISHED" />
            <el-option :label="t('article.hiddenOpt')" value="HIDDEN" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('article.featured')">
          <el-switch v-model="form.isFeatured" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSubmit">
            {{ isEdit ? t('article.update') : t('article.createBtn') }}
          </el-button>
          <el-button @click="goBack">{{ t('article.cancel') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from '@/composables/useI18n'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames, Themes } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { getArticle, createArticle, updateArticle } from '@/api/articles'
import { getCategories } from '@/api/categories'
import { getTags } from '@/api/tags'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const articleId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : null
})
const isEdit = computed(() => !!articleId.value)

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const showImageUpload = ref(false)

const form = reactive({
  title: '',
  slug: '',
  categoryId: null as number | null,
  tagIds: [] as number[],
  coverImage: '',
  summary: '',
  content: '',
  status: 'DRAFT',
  isFeatured: false
})

const hasChanges = ref(false)

watch(form, () => {
  hasChanges.value = true
})

const editorHeight = ref('70vh')
const editorTheme = computed<Themes>(() => 'light')
const toolbars: ToolbarNames[] = [
  'bold', 'underline', 'italic', 'strikeThrough', '-',
  'title', 'quote', 'unorderedList', 'orderedList', 'task', 'codeRow', 'code', 'table',
  '-',
  'link', 'image', 'preview', 'catalog',
  '=',
]

const rules: FormRules = {
  title: [{ required: true, message: t('article.titleRequired'), trigger: 'blur' }],
  content: [{ required: true, message: t('article.contentRequired'), trigger: 'blur' }]
}

const uploadUrl = '/api/admin/articles/upload-image'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

function generateSlug() {
  const title = form.title.trim()
  if (!title) {
    ElMessage.warning(t('article.generateWarn'))
    return
  }
  const slug = title
    .toLowerCase()
    .replace(/[^\w\s-]/g, '')
    .replace(/[\s_]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .substring(0, 80)
  const suffix = Math.random().toString(36).substring(2, 6)
  form.slug = `${slug}-${suffix}`
}

function handleUploadSuccess(response: any) {
  if (response.code === 200) {
    form.coverImage = response.data.url
    ElMessage.success(t('article.uploaded'))
  } else {
    ElMessage.error(response.message || t('article.uploadFailed'))
  }
}

function handleUploadError() {
  ElMessage.error(t('article.imageUploadFailed'))
}

async function loadArticle() {
  if (!articleId.value) return
  loading.value = true
  try {
    const res = await getArticle(articleId.value)
    const data = res.data
    form.title = data.title || ''
    form.slug = data.slug || ''
    form.categoryId = data.categoryId ?? null
    form.tagIds = data.tags?.map((t: any) => t.id) || []
    form.coverImage = data.coverImage || ''
    form.summary = data.summary || ''
    form.content = data.content || ''
    form.status = (data.status || 'DRAFT').toUpperCase()
    form.isFeatured = !!data.isFeatured
  } catch {
    ElMessage.error(t('article.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [catRes, tagRes] = await Promise.all([getCategories(), getTags()])
    categories.value = catRes.data || []
    tags.value = tagRes.data || []
  } catch {
    // ignore
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = { ...form }
    if (isEdit.value) {
      await updateArticle(articleId.value!, payload)
      ElMessage.success(t('article.updated'))
    } else {
      await createArticle(payload)
      ElMessage.success(t('article.created'))
    }
    hasChanges.value = false
    router.push('/articles')
  } catch {
    // message shown by interceptor
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/articles')
}

onBeforeRouteLeave((to, from, next) => {
  if (hasChanges.value) {
    ElMessageBox.confirm(t('article.unsavedText'), t('article.unsavedTitle'), {
      confirmButtonText: t('article.unsavedLeave'),
      cancelButtonText: t('article.unsavedStay'),
      type: 'warning',
    }).then(() => next()).catch(() => next(false))
  } else {
    next()
  }
})

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (hasChanges.value) {
    e.preventDefault()
  }
}

onMounted(() => {
  loadOptions()
  if (isEdit.value) {
    loadArticle()
  }
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.page-header h2 {
  margin: 0;
}

.editor-item :deep(.el-form-item__label) {
  align-self: flex-start;
  padding-top: 12px;
}

.editor-item :deep(.el-form-item__content) {
  flex: 1;
  min-width: 0;
}

.editor-wrapper {
  width: 100%;
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: var(--el-border-radius-base, 4px);
  overflow: hidden;
}

.editor-wrapper:focus-within {
  border-color: var(--el-color-primary, #409eff);
}
</style>
