<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ t('dashboard.title') }}</h2>
      <span class="dashboard-subtitle">{{ t('dashboard.subtitle') }}</span>
    </div>

    <div v-loading="loading" style="min-height: 200px">
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="stat in stats" :key="stat.label">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && stats.length === 0" :description="t('dashboard.noData')" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from '@/composables/useI18n'
import { getOverview } from '@/api/stats'

const { t } = useI18n()

interface StatItem {
  label: string
  value: number | string
}

const loading = ref(true)
const stats = ref<StatItem[]>([])

async function fetchStats() {
  loading.value = true
  try {
    const res = await getOverview()
    const data = res.data
    stats.value = [
      { label: t('dashboard.articles'), value: data.articleCount ?? 0 },
      { label: t('dashboard.comments'), value: data.commentCount ?? 0 },
      { label: t('dashboard.totalViews'), value: data.viewCount ?? 0 },
      { label: t('dashboard.categories'), value: data.categoryCount ?? 0 },
      { label: t('dashboard.tags'), value: data.tagCount ?? 0 },
      { label: t('dashboard.links'), value: data.linkCount ?? 0 }
    ]
  } catch {
    stats.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.dashboard-subtitle {
  font-size: 14px;
  color: #818CF8;
  font-weight: 400;
}

.el-card {
  margin-bottom: 20px;
}
</style>
