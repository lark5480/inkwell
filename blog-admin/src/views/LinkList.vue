<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ t('link.listTitle') }}</h2>
      <el-button type="primary" @click="openDialog()">{{ t('link.add') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="links" stripe border style="width: 100%">
      <el-table-column prop="id" :label="t('common.id')" width="60" />
      <el-table-column prop="name" :label="t('common.name')" min-width="150" />
      <el-table-column prop="url" :label="t('link.url')" min-width="250" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link :href="row.url" target="_blank" type="primary">{{ row.url }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="description" :label="t('common.description')" min-width="180" show-overflow-tooltip />
      <el-table-column prop="sort" :label="t('common.sort')" width="70" align="center" />
      <el-table-column prop="status" :label="t('common.status')" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? t('link.enabled') : t('link.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">{{ t('common.edit') }}</el-button>
          <el-button size="small" type="danger" @click="confirmDelete(row.id)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && links.length === 0" :description="t('link.noData')" />

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('link.editTitle') : t('link.addTitle')"
      width="550px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('common.name')" prop="name">
          <el-input v-model="form.name" :placeholder="t('link.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('link.url')" prop="url">
          <el-input v-model="form.url" :placeholder="t('link.urlPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.description')">
          <el-input v-model="form.description" :placeholder="t('link.descPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.sort')">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
          />
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
import { getLinks, createLink, updateLink, deleteLink } from '@/api/links'

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const links = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  url: '',
  description: '',
  sort: 0,
  status: 1
})

const rules: FormRules = {
  name: [{ required: true, message: t('link.nameRequired'), trigger: 'blur' }],
  url: [
    { required: true, message: t('link.urlRequired'), trigger: 'blur' },
    { type: 'url', message: t('link.urlInvalid'), trigger: 'blur' }
  ]
}

async function fetchLinks() {
  loading.value = true
  try {
    const res = await getLinks()
    links.value = res.data || []
  } catch {
    links.value = []
  } finally {
    loading.value = false
  }
}

function openDialog(row?: any) {
  editingId.value = row?.id ?? null
  form.name = row?.name ?? ''
  form.url = row?.url ?? ''
  form.description = row?.description ?? ''
  form.sort = row?.sort ?? 0
  form.status = row?.status ?? 1
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingId.value) {
      await updateLink(editingId.value, form)
      ElMessage.success(t('link.updated'))
    } else {
      await createLink(form)
      ElMessage.success(t('link.created'))
    }
    dialogVisible.value = false
    fetchLinks()
  } catch {
    // error shown by interceptor
  } finally {
    saving.value = false
  }
}

async function confirmDelete(id: number) {
  try {
    await ElMessageBox.confirm(t('link.deleteConfirmText'), t('link.deleteConfirmTitle'), {
      confirmButtonText: t('common.delete'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
    await deleteLink(id)
    ElMessage.success(t('link.deleted'))
    fetchLinks()
  } catch {
    // cancelled
  }
}

onMounted(fetchLinks)
</script>
