<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

<script setup lang="ts">
useSeoMeta({
  titleTemplate: '%s | Personal Blog',
  defaultTitle: 'Personal Blog',
})
</script>
