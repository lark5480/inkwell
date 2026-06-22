<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ t('user.listTitle') }}</h2>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && users.length === 0" :description="t('user.noData')" />

      <el-table v-else :data="users" stripe style="width: 100%">
        <el-table-column prop="id" :label="t('common.id')" width="80" />
        <el-table-column prop="username" :label="t('user.username')" min-width="140" />
        <el-table-column prop="nickname" :label="t('user.nickname')" min-width="140" />
        <el-table-column prop="email" :label="t('user.email')" min-width="180" />
        <el-table-column prop="role" :label="t('user.role')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
              {{ row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('common.status')" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="row.role === 'ADMIN'"
              @change="(val: boolean) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('common.createTime')" min-width="160" />
        <el-table-column :label="t('common.actions')" width="140" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              v-if="row.role !== 'ADMIN'"
              :title="row.role === 'AUTHOR' ? t('user.promoteConfirm') : t('user.demoteConfirm')"
              @confirm="handleToggleRole(row)"
            >
              <template #reference>
                <el-button type="warning" size="small">
                  {{ row.role === 'AUTHOR' ? t('user.promote') : t('user.demote') }}
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '@/composables/useI18n'
import { getUsers, updateUserStatus, updateUserRole } from '@/api/users'

const { t } = useI18n()
const loading = ref(false)
const users = ref<any[]>([])

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getUsers()
    users.value = res.data || []
  } catch {
    users.value = []
  } finally {
    loading.value = false
  }
}

async function handleToggleStatus(row: any, val: boolean) {
  try {
    await updateUserStatus(row.id, val ? 1 : 0)
    row.status = val ? 1 : 0
    ElMessage.success(t('common.operationSuccess'))
  } catch {
    // error shown by interceptor
  }
}

async function handleToggleRole(row: any) {
  const newRole = row.role === 'AUTHOR' ? 'ADMIN' : 'AUTHOR'
  try {
    await updateUserRole(row.id, newRole)
    row.role = newRole
    ElMessage.success(t('common.operationSuccess'))
  } catch {
    // error shown by interceptor
  }
}

onMounted(fetchUsers)
</script>
