<template>
  <div class="not-found-page">
    <el-result icon="warning" title="404" sub-title="Page Not Found">
      <template #extra>
        <el-button type="primary" @click="goHome">Back to Dashboard</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/dashboard')
}
</script>

<style scoped>
.not-found-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
</style>
