<template>
  <div class="agent-config">
    <h1>{{ $t('configs.agentConfig.title') }}</h1>
    <p class="description">{{ $t('configs.agentConfig.description') }}</p>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'AgentConfigPage'
})
</script>

<style scoped>
.agent-config {
  padding: 20px;
}
.description {
  color: #666;
  margin-top: 10px;
}
</style>
