<template>
  <div class="__container_layout_bread">
    <a-breadcrumb>
      <a-breadcrumb-item v-for="r in routes" :key="r.name">{{ $t(r.name) }}</a-breadcrumb-item>
      <a-breadcrumb-item v-if="pathId">{{ pathId }}</a-breadcrumb-item>
    </a-breadcrumb>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed } from 'vue'
import type { RouteRecordRaw } from 'vue-router'

interface BreadcrumbRoute {
  name: string
  path?: string
}

const route = useRoute()

const pathId = computed(() => route.params?.pathId?.toString() || '')

const routes = computed<BreadcrumbRoute[]>(() => {
  return route.matched
    .slice(1)
    .filter((route) => !route.meta?.hidden)
    .map((route) => ({
      name: route.name as string,
      path: route.path
    }))
})
</script>

<style lang="less" scoped>
.__container_layout_bread {
  padding-left: 20px;
  padding-top: 10px;
}
</style>
