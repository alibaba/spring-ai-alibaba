<template>
  <div>
    <MD :content="markdownContent" />
    <button
      v-if="needCollapse"
      @click="toggle"
      style="margin-top: 8px; background: none; border: none; color: #1890ff; cursor: pointer"
    >
      {{ expanded ? '收起 ▲' : `展开更多 (${queries.length - 5} 条) ▼` }}
    </button>
  </div>
</template>
<script setup lang="ts">
import { ref, computed, watch } from 'vue'

import MD from '@/components/md/index.vue'

const props = defineProps<{
  queries: string[]
}>()

const expanded = ref(false)
const needCollapse = computed(() => props.queries.length > 5)
const displayedQueries = computed(() => {
  return !needCollapse.value || expanded.value ? props.queries : props.queries.slice(0, 5)
})

const markdownContent = computed(() => {
  return displayedQueries.value.map((q, i) => `(${i + 1}) ${q}`).join('\n')
})

function toggle() {
  expanded.value = !expanded.value
}
</script>

<style scoped>
button:hover {
  text-decoration: underline;
}
</style>
