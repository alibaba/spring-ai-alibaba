<template>
  <div class="mcp-config">
    <h1>{{ $t('configs.mcpConfig.title') }}</h1>
    <p class="description">{{ $t('configs.mcpConfig.description') }}</p>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'McpConfigPage'
})
</script>

<style scoped>
.mcp-config {
  padding: 20px;
}
.description {
  color: #666;
  margin-top: 10px;
}
</style>
