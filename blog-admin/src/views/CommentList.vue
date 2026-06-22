<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ t('comment.listTitle') }}</h2>
    </div>

    <div class="search-bar">
      <el-select v-model="query.status" :placeholder="t('comment.status')" clearable @change="fetchComments">
        <el-option :label="t('comment.all')" value="" />
        <el-option :label="t('comment.pending')" value="PENDING" />
        <el-option :label="t('comment.approved')" value="APPROVED" />
        <el-option :label="t('comment.spam')" value="SPAM" />
      </el-select>
      <el-button @click="fetchComments">{{ t('comment.refresh') }}</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="comments"
      stripe
      border
      style="width: 100%"
      @expand-change="onExpandChange"
      :empty-text="t('comment.noData')"
    >
      <el-table-column type="expand" width="40">
        <template #default="{ row }">
          <div class="expanded-content">
            <p><strong>{{ t('comment.fullContent') }}</strong></p>
            <p>{{ row.content }}</p>
            <p v-if="row.articleTitle"><strong>{{ t('comment.articleLabel') }}</strong> {{ row.articleTitle }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="articleTitle" :label="t('comment.article')" min-width="180" show-overflow-tooltip />
      <el-table-column prop="content" :label="t('comment.content')" min-width="250" show-overflow-tooltip>
        <template #default="{ row }">
          {{ truncate(row.content, 80) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('comment.commenter')" width="140">
        <template #default="{ row }">
          <span :title="row.authorEmail || ''">{{ row.userNickname || row.authorName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="articleAuthorName" :label="t('comment.articleAuthor')" width="120" />
      <el-table-column prop="status" :label="t('comment.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" :label="t('comment.created')" width="160">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('comment.actions')" width="240" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            type="success"
            :disabled="row.status === 'APPROVED'"
            @click="changeStatus(row.id, 'APPROVED')"
          >
            {{ t('comment.approve') }}
          </el-button>
          <el-button
            size="small"
            type="warning"
            :disabled="row.status === 'SPAM'"
            @click="changeStatus(row.id, 'SPAM')"
          >
            {{ t('comment.spamBtn') }}
          </el-button>
          <el-button size="small" type="danger" @click="confirmDelete(row.id)">{{ t('comment.delete') }}</el-button>
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
        @current-change="fetchComments"
        @size-change="fetchComments"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from '@/composables/useI18n'
import { getComments, updateCommentStatus, deleteComment } from '@/api/comments'

const { t } = useI18n()

const loading = ref(false)
const comments = ref<any[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  pageSize: 10,
  status: ''
})

function statusType(status: string) {
  switch (status) {
    case 'APPROVED': return 'success'
    case 'PENDING': return 'warning'
    case 'SPAM': return 'danger'
    default: return 'info'
  }
}

function truncate(text: string, len: number) {
  if (!text) return ''
  return text.length > len ? text.substring(0, len) + '...' : text
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function onExpandChange() {
  // handled by template
}

async function fetchComments() {
  loading.value = true
  try {
    const res = await getComments(query)
    comments.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    comments.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function changeStatus(id: number, status: string) {
  try {
    await updateCommentStatus(id, status)
    ElMessage.success(t('comment.markedAs', { status }))
    fetchComments()
  } catch {
    // message shown by interceptor
  }
}

async function confirmDelete(id: number) {
  try {
    await ElMessageBox.confirm(t('comment.deleteConfirmText'), t('comment.deleteConfirmTitle'), {
      confirmButtonText: t('comment.delete'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
    await deleteComment(id)
    ElMessage.success(t('comment.deleted'))
    fetchComments()
  } catch {
    // cancelled
  }
}

onMounted(fetchComments)
</script>

<style scoped>
.expanded-content {
  padding: 12px;
  background-color: #fafafa;
  border-radius: 4px;
  line-height: 1.6;
}

.expanded-content p {
  margin: 4px 0;
}
</style>
