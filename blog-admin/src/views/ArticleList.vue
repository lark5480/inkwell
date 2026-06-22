<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ t('article.listTitle') }}</h2>
      <el-button type="primary" @click="createArticle">{{ t('article.create') }}</el-button>
    </div>

    <div class="search-bar">
      <el-input
        v-model="query.keyword"
        :placeholder="t('article.searchPlaceholder')"
        clearable
        @clear="fetchArticles"
        @keyup.enter="fetchArticles"
      />
      <el-select
        v-model="query.categoryId"
        :placeholder="t('article.category')"
        clearable
        @change="fetchArticles"
      >
        <el-option
          v-for="cat in categories"
          :key="cat.id"
          :label="cat.name"
          :value="cat.id"
        />
      </el-select>
      <el-select
        v-model="query.status"
        :placeholder="t('article.status')"
        clearable
        @change="fetchArticles"
      >
        <el-option :label="t('article.publishedOpt')" value="PUBLISHED" />
        <el-option :label="t('article.draft')" value="DRAFT" />
        <el-option :label="t('article.hiddenOpt')" value="HIDDEN" />
      </el-select>
      <el-button @click="fetchArticles">{{ t('article.search') }}</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="articles"
      stripe
      border
      style="width: 100%"
      :empty-text="t('article.noData')"
    >
      <el-table-column prop="id" :label="t('common.id')" width="60" />
      <el-table-column prop="title" :label="t('article.title')" min-width="200" show-overflow-tooltip />
      <el-table-column prop="categoryName" :label="t('article.category')" width="120" />
      <el-table-column prop="status" :label="t('article.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('article.featured')" width="80" align="center">
        <template #default="{ row }">
          <el-icon v-if="row.isFeatured" color="#e6a23c" size="18">
            <StarFilled />
          </el-icon>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" :label="t('article.views')" width="80" align="center" />
      <el-table-column prop="publishedAt" :label="t('article.published')" width="160">
        <template #default="{ row }">
          {{ row.publishedAt ? formatDate(row.publishedAt) : '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="editArticle(row.id)">{{ t('article.edit') }}</el-button>
          <el-button
            size="small"
            :type="row.status === 'PUBLISHED' ? 'warning' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'PUBLISHED' ? t('article.hide') : t('article.publish') }}
          </el-button>
          <el-button size="small" type="danger" @click="confirmDelete(row.id)">{{ t('article.delete') }}</el-button>
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
        @current-change="fetchArticles"
        @size-change="fetchArticles"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElIcon } from 'element-plus'
import { StarFilled } from '@element-plus/icons-vue'
import { useI18n } from '@/composables/useI18n'
import { getArticles, deleteArticle, updateArticleStatus } from '@/api/articles'
import { getCategories } from '@/api/categories'

const router = useRouter()
const { t } = useI18n()

const loading = ref(false)
const articles = ref<any[]>([])
const categories = ref<any[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  categoryId: undefined as number | undefined,
  status: ''
})

function statusType(status: string) {
  switch (status) {
    case 'PUBLISHED': return 'success'
    case 'DRAFT': return 'info'
    case 'HIDDEN': return 'warning'
    default: return 'info'
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function fetchArticles() {
  loading.value = true
  try {
    const res = await getArticles(query)
    articles.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    articles.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch {
    categories.value = []
  }
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED'
  try {
    await updateArticleStatus(row.id, newStatus)
    ElMessage.success(t('article.statusUpdated'))
    fetchArticles()
  } catch {
    // message shown by interceptor
  }
}

function editArticle(id: number) {
  router.push(`/articles/edit/${id}`)
}

function createArticle() {
  router.push('/articles/edit')
}

async function confirmDelete(id: number) {
  try {
    await ElMessageBox.confirm(t('article.deleteConfirmText'), t('article.deleteConfirmTitle'), {
      confirmButtonText: t('article.delete'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
    await deleteArticle(id)
    ElMessage.success(t('article.deleted'))
    fetchArticles()
  } catch {
    // cancelled or error
  }
}

onMounted(() => {
  fetchArticles()
  fetchCategories()
})
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
</style>
