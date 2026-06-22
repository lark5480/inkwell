<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ t('settings.title') }}</h2>
      <el-button type="primary" :loading="saving" @click="handleSave">{{ t('settings.save') }}</el-button>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && settings.length === 0" :description="t('settings.noData')" />

      <el-form
        v-else
        ref="formRef"
        :model="formData"
        label-width="200px"
        style="max-width: 800px"
      >
        <el-form-item
          v-for="setting in settings"
          :key="setting.settingKey"
          :label="setting.settingKey"
          :prop="setting.settingKey"
        >
          <MdEditor
            v-if="setting.settingKey === 'about_content'"
            v-model="formData[setting.settingKey]"
            :toolbars="toolbars"
            language="zh-CN"
            :theme="editorTheme"
            :height="editorHeight"
            :placeholder="t('settings.aboutPlaceholder')"
          />
          <el-input
            v-else
            v-model="formData[setting.settingKey]"
            :placeholder="setting.settingKey"
            :type="isLongText(setting.settingValue) ? 'textarea' : 'text'"
            :rows="isLongText(setting.settingValue) ? 4 : 1"
          />
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames, Themes } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { getSettings, updateSettings } from '@/api/settings'
import { useI18n } from '@/composables/useI18n'
import type { Setting } from '@/types'

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const settings = ref<Setting[]>([])
const formData = reactive<Record<string, string>>({})
const formRef = ref<FormInstance>()
const editorHeight = ref('60vh')
const editorTheme = computed<Themes>(() => 'light')
const toolbars: ToolbarNames[] = [
  'bold', 'underline', 'italic', 'strikeThrough', '-',
  'title', 'quote', 'unorderedList', 'orderedList', 'task', 'codeRow', 'code', 'table',
  '-',
  'link', 'image', 'preview', 'catalog',
  '=',
]

function isLongText(value: string): boolean {
  return value ? value.length > 100 : false
}

async function fetchSettings() {
  loading.value = true
  try {
    const res = await getSettings()
    settings.value = res.data || []
    // Populate form data
    settings.value.forEach((s: Setting) => {
      formData[s.settingKey] = s.settingValue ?? ''
    })
  } catch {
    settings.value = []
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateSettings(formData)
    ElMessage.success(t('settings.saved'))
  } catch {
    // error shown by interceptor
  } finally {
    saving.value = false
  }
}

onMounted(fetchSettings)
</script>
