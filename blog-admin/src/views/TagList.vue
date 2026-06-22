<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ t('tag.listTitle') }}</h2>
      <el-button type="primary" @click="openDialog()">{{ t('tag.add') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="tags" stripe border style="width: 100%">
      <el-table-column prop="id" :label="t('common.id')" width="60" />
      <el-table-column prop="name" :label="t('common.name')" min-width="150" />
      <el-table-column prop="slug" :label="t('common.slug')" width="150" />
      <el-table-column prop="articleCount" :label="t('tag.articles')" width="80" align="center" />
      <el-table-column :label="t('common.actions')" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">{{ t('common.edit') }}</el-button>
          <el-button size="small" type="danger" @click="confirmDelete(row.id)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && tags.length === 0" :description="t('tag.noData')" />

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('tag.editTitle') : t('tag.addTitle')"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item :label="t('common.name')" prop="name">
          <el-input v-model="form.name" :placeholder="t('tag.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.slug')" prop="slug">
          <el-input v-model="form.slug" :placeholder="t('tag.slugPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from '@/composables/useI18n'
import { getTags, createTag, updateTag, deleteTag } from '@/api/tags'

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const tags = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  slug: ''
})

const rules: FormRules = {
  name: [{ required: true, message: t('tag.nameRequired'), trigger: 'blur' }],
  slug: [{ required: true, message: t('tag.slugRequired'), trigger: 'blur' }]
}

async function fetchTags() {
  loading.value = true
  try {
    const res = await getTags()
    tags.value = res.data || []
  } catch {
    tags.value = []
  } finally {
    loading.value = false
  }
}

function openDialog(row?: any) {
  editingId.value = row?.id ?? null
  form.name = row?.name ?? ''
  form.slug = row?.slug ?? ''
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingId.value) {
      await updateTag(editingId.value, form)
      ElMessage.success(t('tag.updated'))
    } else {
      await createTag(form)
      ElMessage.success(t('tag.created'))
    }
    dialogVisible.value = false
    fetchTags()
  } catch {
    // error shown by interceptor
  } finally {
    saving.value = false
  }
}

async function confirmDelete(id: number) {
  try {
    await ElMessageBox.confirm(t('tag.deleteConfirmText'), t('tag.deleteConfirmTitle'), {
      confirmButtonText: t('common.delete'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
    await deleteTag(id)
    ElMessage.success(t('tag.deleted'))
    fetchTags()
  } catch {
    // cancelled
  }
}

onMounted(fetchTags)
</script>
