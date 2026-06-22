<template>
  <div class="error-page">
    <div class="container">
      <div class="error-card clay-card">
        <div class="error-code">{{ error?.statusCode || 500 }}</div>
        <h1>{{ error?.statusCode === 404 ? 'Page Not Found' : 'Something Went Wrong' }}</h1>
        <p>{{ error?.message || 'An unexpected error occurred.' }}</p>
        <div class="error-actions">
          <NuxtLink to="/" class="btn btn-primary">Back to Home</NuxtLink>
          <button v-if="error?.statusCode !== 404" class="btn btn-outline" @click="handleError">Try Again</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps({
  error: {
    type: Object,
    default: null,
  },
})

const handleError = () => clearError({ redirect: '/' })
</script>

<style scoped>
.error-page {
  min-height: 70vh;
  display: flex;
  align-items: center;
  padding: var(--space-10) 0;
}

.container {
  max-width: 520px;
  margin: 0 auto;
  width: 100%;
  padding: 0 var(--space-4);
}

.error-card {
  padding: var(--space-10);
  text-align: center;
}

.error-code {
  font-family: var(--font-heading);
  font-size: clamp(4rem, 10vw, 7rem);
  font-weight: 900;
  line-height: 1;
  color: var(--primary);
  opacity: 0.5;
  margin-bottom: var(--space-4);
}

.error-card h1 {
  font-family: var(--font-heading);
  font-size: var(--font-size-2xl);
  font-weight: 800;
  color: var(--text);
  margin: 0 0 var(--space-3);
}

.error-card p {
  color: var(--text-secondary);
  font-size: var(--font-size-base);
  margin-bottom: var(--space-8);
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-3);
}

.btn {
  display: inline-flex;
  align-items: center;
  padding: var(--space-3) var(--space-6);
  border-radius: var(--clay-border-radius);
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  text-decoration: none;
  border: var(--clay-border);
}

.btn-primary {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.btn-primary:hover {
  background: var(--primary-dark);
  transform: translateY(-1px);
}

.btn-outline {
  background: var(--bg-card);
  color: var(--text-secondary);
}

.btn-outline:hover {
  background: var(--bg-tertiary);
  color: var(--text);
  transform: translateY(-1px);
}
</style>
