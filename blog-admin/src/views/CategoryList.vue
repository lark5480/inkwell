<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ t('category.listTitle') }}</h2>
      <el-button type="primary" @click="openDialog()">{{ t('category.add') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="categories" stripe border style="width: 100%">
      <el-table-column prop="id" :label="t('common.id')" width="60" />
      <el-table-column prop="name" :label="t('common.name')" min-width="150" />
      <el-table-column prop="slug" :label="t('common.slug')" width="150" />
      <el-table-column prop="description" :label="t('common.description')" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sort" :label="t('common.sort')" width="80" align="center" />
      <el-table-column prop="articleCount" :label="t('category.articles')" width="80" align="center" />
      <el-table-column :label="t('common.actions')" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">{{ t('common.edit') }}</el-button>
          <el-button size="small" type="danger" @click="confirmDelete(row.id)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && categories.length === 0" :description="t('category.noData')" />

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('category.editTitle') : t('category.addTitle')"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('common.name')" prop="name">
          <el-input v-model="form.name" :placeholder="t('category.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.slug')" prop="slug">
          <el-input v-model="form.slug" :placeholder="t('category.slugPlaceholder')">
            <template #append>
              <el-button @click="form.slug = form.name.toLowerCase().replace(/\\s+/g, '-').replace(/[^\\w-]/g, '')">{{ t('category.auto') }}</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('common.description')">
          <el-input v-model="form.description" type="textarea" :rows="3" :placeholder="t('category.descPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.sort')">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
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
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/api/categories'

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const categories = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  slug: '',
  description: '',
  sort: 0
})

const rules: FormRules = {
  name: [{ required: true, message: t('category.nameRequired'), trigger: 'blur' }],
  slug: [{ required: true, message: t('category.slugRequired'), trigger: 'blur' }]
}

async function fetchCategories() {
  loading.value = true
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch {
    categories.value = []
  } finally {
    loading.value = false
  }
}

function openDialog(row?: any) {
  editingId.value = row?.id ?? null
  form.name = row?.name ?? ''
  form.slug = row?.slug ?? ''
  form.description = row?.description ?? ''
  form.sort = row?.sort ?? 0
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, form)
      ElMessage.success(t('category.updated'))
    } else {
      await createCategory(form)
      ElMessage.success(t('category.created'))
    }
    dialogVisible.value = false
    fetchCategories()
  } catch {
    // error shown by interceptor
  } finally {
    saving.value = false
  }
}

async function confirmDelete(id: number) {
  try {
    await ElMessageBox.confirm(t('category.deleteConfirmText'), t('category.deleteConfirmTitle'), {
      confirmButtonText: t('common.delete'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
    await deleteCategory(id)
    ElMessage.success(t('category.deleted'))
    fetchCategories()
  } catch {
    // cancelled
  }
}

onMounted(fetchCategories)
</script>
