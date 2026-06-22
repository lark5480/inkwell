<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ t('report.listTitle') }}</h2>
    </div>

    <div class="search-bar">
      <el-select v-model="query.status" :placeholder="t('report.status')" clearable @change="fetchReports">
        <el-option :label="t('report.all')" value="" />
        <el-option label="PENDING" value="PENDING" />
        <el-option label="RESOLVED" value="RESOLVED" />
        <el-option label="DISMISSED" value="DISMISSED" />
      </el-select>
      <el-button @click="fetchReports">{{ t('report.refresh') }}</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="reports"
      stripe
      border
      style="width: 100%"
      :empty-text="t('report.noData')"
    >
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="reason" :label="t('report.reason')" width="120">
        <template #default="{ row }">
          <el-tag :type="reasonType(row.reason)" size="small">{{ row.reason || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="commentId" :label="t('report.commentId')" width="100" />
      <el-table-column prop="reporterId" :label="t('report.reporterId')" width="100" />
      <el-table-column prop="status" :label="t('report.status')" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" :label="t('report.created')" width="160">
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('report.actions')" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            type="success"
            :disabled="row.status !== 'PENDING'"
            @click="handleResolve(row.id)"
          >
            {{ t('report.resolve') }}
          </el-button>
          <el-button
            size="small"
            type="info"
            :disabled="row.status !== 'PENDING'"
            @click="handleDismiss(row.id)"
          >
            {{ t('report.dismiss') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 16px; display: flex; justify-content: flex-end">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchReports"
        @size-change="fetchReports"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getReports, updateReportStatus, type CommentReport } from '@/api/reports'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from '@/composables/useI18n'

const { t } = useI18n()

const loading = ref(false)
const reports = ref<CommentReport[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  pageSize: 10,
  status: '',
})

async function fetchReports() {
  loading.value = true
  try {
    const res = await getReports({
      page: query.page,
      pageSize: query.pageSize,
      status: query.status || undefined,
    })
    const data = res.data as {
      records: CommentReport[]
      total: number
      page: number
      pageSize: number
    }
    reports.value = data.records
    total.value = data.total
  } catch {
    reports.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleResolve(id: number) {
  try {
    await ElMessageBox.confirm(t('report.resolveConfirm'), t('report.resolveTitle'), {
      confirmButtonText: t('report.resolve'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
    await updateReportStatus(id, 'RESOLVED')
    ElMessage.success(t('report.resolved'))
    fetchReports()
  } catch {
    // cancelled or error
  }
}

async function handleDismiss(id: number) {
  try {
    await updateReportStatus(id, 'DISMISSED')
    ElMessage.success(t('report.dismissed'))
    fetchReports()
  } catch {
    // error handled by interceptor
  }
}

function statusType(status: string): string {
  switch (status) {
    case 'PENDING': return 'warning'
    case 'RESOLVED': return 'success'
    case 'DISMISSED': return 'info'
    default: return ''
  }
}

function reasonType(reason: string): string {
  switch (reason) {
    case 'SPAM': return 'danger'
    case 'ABUSE': return 'warning'
    default: return 'info'
  }
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

onMounted(() => {
  fetchReports()
})
</script>
