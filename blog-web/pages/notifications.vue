<script setup lang="ts">
definePageMeta({ layout: 'default', middleware: 'auth' })
await navigateTo('/messages', { replace: true })
</script>
<template><div /></template>
