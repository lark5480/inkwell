<template>
  <div class="article-editor">
    <div class="editor-form">
      <!-- Title -->
      <div class="form-group">
        <label class="form-label">{{ t('editor.title') }}</label>
        <input v-model="form.title" type="text" class="form-input" :placeholder="t('editor.titlePlaceholder')" />
      </div>

      <!-- Slug -->
      <div class="form-group form-row">
        <div class="form-field">
          <label class="form-label">{{ t('editor.slug') }}</label>
          <input v-model="form.slug" type="text" class="form-input" :placeholder="t('editor.slugPlaceholder')" />
        </div>
        <div class="form-field form-field-sm">
          <label class="form-label">{{ t('editor.status') }}</label>
          <select v-model="form.status" class="form-select">
            <option value="DRAFT">{{ t('editor.draft') }}</option>
            <option value="PUBLISHED">{{ t('editor.published') }}</option>
          </select>
        </div>
      </div>

      <!-- Category & Tags -->
      <div class="form-group form-row">
        <div class="form-field">
          <label class="form-label">{{ t('editor.category') }}</label>
          <select v-model="form.categoryId" class="form-select">
            <option :value="null">{{ t('editor.none') }}</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
        </div>
        <div class="form-field">
          <label class="form-label">{{ t('editor.tags') }}</label>
          <div class="tags-select">
            <button
              v-for="tag in allTags"
              :key="tag.id"
              type="button"
              class="tag-chip"
              :class="{ active: form.tags.includes(tag.id) }"
              @click="toggleTag(tag.id)"
            >{{ tag.name }}</button>
          </div>
        </div>
      </div>

      <!-- Summary -->
      <div class="form-group">
        <label class="form-label">{{ t('editor.summary') }}</label>
        <textarea v-model="form.summary" class="form-textarea" rows="3" :placeholder="t('editor.summaryPlaceholder')"></textarea>
      </div>

      <!-- Cover Image -->
      <div class="form-group">
        <label class="form-label">{{ t('editor.coverImage') }}</label>
        <div class="cover-input-row">
          <input v-model="form.coverImage" type="text" class="form-input" :placeholder="t('editor.coverPlaceholder')" />
          <button type="button" class="btn btn-secondary btn-upload" :disabled="uploading" @click="triggerUpload">
            <svg v-if="uploading" class="spin" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            {{ uploading ? t('editor.uploading') : t('editor.upload') }}
          </button>
          <input ref="fileInput" type="file" accept="image/*" class="file-hidden" @change="handleFileChange" />
        </div>
        <div v-if="form.coverImage" class="cover-preview">
          <img :src="form.coverImage" :alt="t('editor.coverImage')" />
        </div>
      </div>

      <!-- Markdown Editor -->
      <div class="form-group">
        <label class="form-label">{{ t('editor.content') }}</label>
        <MdEditor
          v-model="form.content"
          :toolbars="toolbars"
          :language="mdLang"
          :style="{ height: '500px' }"
        />
      </div>

      <!-- Actions -->
      <div class="editor-actions">
        <button type="button" class="btn btn-secondary" @click="$emit('cancel')">{{ t('editor.cancel') }}</button>
        <button type="button" class="btn btn-primary" :disabled="loading" @click="handleSubmit">
          {{ loading ? t('editor.saving') : submitLabel }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import type { ArticleDetailResponse, ArticleCreateRequest, CategoryDTO, TagDTO } from '~/composables/useBlogApi'

const props = defineProps<{
  initialData?: ArticleDetailResponse
  submitLabel: string
  loading?: boolean
}>()

const emit = defineEmits<{
  submit: [data: ArticleCreateRequest]
  cancel: []
}>()

const { getCategories, getTags, uploadArticleImage } = useBlogApi()
const { t, locale } = useI18n()
const { alert } = useModal()

const mdLang = computed(() => (locale.value === 'zh' ? 'zh-CN' : 'en-US'))

const categories = ref<CategoryDTO[]>([])
const allTags = ref<TagDTO[]>([])

const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const res = await uploadArticleImage(file)
    form.coverImage = res.url
  } catch (err: any) {
    alert({ message: err?.message || t('editor.uploadFailed'), type: 'error' })
  } finally {
    uploading.value = false
    target.value = ''
  }
}

const form = reactive({
  title: props.initialData?.title || '',
  slug: props.initialData?.slug || '',
  content: props.initialData?.content || '',
  summary: props.initialData?.summary || '',
  coverImage: props.initialData?.coverImage || '',
  categoryId: null as number | null,
  tags: [] as number[],
  status: (props.initialData?.status === 'PUBLISHED' ? 'PUBLISHED' : 'DRAFT') as 'DRAFT' | 'PUBLISHED',
})

const hasChanges = ref(false)

watch(form, () => {
  hasChanges.value = true
})

const toolbars = [
  'bold', 'underline', 'italic', 'strikeThrough', '-',
  'title', 'sub', 'sup', 'quote', 'unorderedList', 'orderedList', 'task', '-',
  'codeRow', 'code', 'link', 'image', 'table', 'mermaid', 'katex', '-',
  'revoke', 'next', '=',
  'pageFullscreen', 'preview', 'htmlPreview',
] as any[]

function toggleTag(tagId: number) {
  const idx = form.tags.indexOf(tagId)
  if (idx >= 0) {
    form.tags.splice(idx, 1)
  } else {
    form.tags.push(tagId)
  }
}

function handleSubmit() {
  if (!form.title.trim()) {
    alert({ message: t('editor.titleRequired'), type: 'warning' })
    return
  }
  if (!form.content.trim()) {
    alert({ message: t('editor.contentRequired'), type: 'warning' })
    return
  }

  hasChanges.value = false
  emit('submit', {
    title: form.title,
    slug: form.slug || undefined,
    content: form.content,
    summary: form.summary || undefined,
    coverImage: form.coverImage || undefined,
    categoryId: form.categoryId,
    tags: form.tags.length > 0 ? form.tags : undefined,
    status: form.status,
  })
}

// Load categories and tags on mount
onMounted(async () => {
  try {
    const [cats, tags] = await Promise.all([getCategories(), getTags()])
    categories.value = cats
    allTags.value = tags
  } catch {}
})

defineExpose({ hasChanges })
</script>

<style scoped>
.article-editor {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--space-8) 0;
}

.editor-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-row {
  flex-direction: row;
  gap: var(--space-4);
}

.form-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-field-sm {
  flex: 0 0 160px;
}

.form-label {
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-secondary);
}

.form-input,
.form-select,
.form-textarea {
  padding: var(--space-3) var(--space-4);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
  background: var(--bg-card);
  color: var(--text);
  font-family: var(--font-body);
  font-size: var(--font-size-base);
  transition: all var(--transition-fast);
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.cover-input-row {
  display: flex;
  gap: var(--space-2);
}

.cover-input-row .form-input {
  flex: 1;
}

.btn-upload {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-3) var(--space-4);
}

.btn-upload:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.file-hidden {
  display: none;
}

.cover-preview {
  margin-top: var(--space-2);
  border: var(--clay-border);
  border-radius: var(--clay-border-radius);
  overflow: hidden;
  max-width: 320px;
}

.cover-preview img {
  display: block;
  width: 100%;
  height: auto;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.tags-select {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.tag-chip {
  padding: var(--space-1) var(--space-3);
  border: var(--clay-border);
  border-radius: 20px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tag-chip:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.tag-chip.active {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  padding-top: var(--space-4);
  border-top: var(--clay-border);
}

.btn {
  padding: var(--space-3) var(--space-6);
  border-radius: var(--clay-border-radius);
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: var(--clay-border);
}

.btn-primary {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.btn-primary:hover:not(:disabled) {
  background: var(--primary-dark);
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: var(--bg-card);
  color: var(--text-secondary);
}

.btn-secondary:hover {
  background: var(--bg-tertiary);
  color: var(--text);
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
  }

  .form-field-sm {
    flex: 1;
  }
}
</style>
