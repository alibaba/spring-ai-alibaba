<template>
  <div class="basic-config">
    <h1>{{ $t('configs.basicConfig.title') }}</h1>
    <p class="description">{{ $t('configs.basicConfig.description') }}</p>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'BasicConfigPage'
})
</script>

<style scoped>
.basic-config {
  padding: 20px;
}
.description {
  color: #666;
  margin-top: 10px;
}
</style>
