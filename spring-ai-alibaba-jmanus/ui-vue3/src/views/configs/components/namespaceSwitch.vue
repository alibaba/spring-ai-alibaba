<!--
  Copyright 2025 the original author or authors.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

<template>
  <div>
    <CustomSelect
      v-model="namespace"
      :options="namespaces"
      :dropdown-title="t('config.namespaceConfig.namespace.namespace')"
      @change="handleNamespaceChange"
      :placeholder="t('config.namespaceConfig.namespace.selectNamespace')"
      direction="right"
      :dropStyles="{
        minWidth: '220px',
      }"
    />
    <span
      onclick="event.stopPropagation()"
      class="setting-icon-wrapper"
      @click="handleChangeNamespace"
    >
      <Icon icon="carbon:settings-edit" class="setting-icon" width="20" />
    </span>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import CustomSelect from '@/components/select/index.vue'
import { storeToRefs } from 'pinia'
import { usenameSpaceStore } from '@/stores/namespace'
import { Icon } from '@iconify/vue'
import { useRoute, useRouter } from 'vue-router'
import { notEmpty } from '@/utils'
import { NamespaceApiService, type Namespace } from '@/api/namespace-api-service'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const namespaceStore = usenameSpaceStore()
const { namespace, namespaces } = storeToRefs(namespaceStore)
const { setCurrentNs, setNamespaces } = namespaceStore

const handleNamespaceChange = (value: string, option: { host?: string }) => {
  if (notEmpty(option.host)) {
    const host = option.host as string
    const normalizedHost = host.endsWith('/') ? host.slice(0, -1) : host

    const newUrl = new URL(normalizedHost)
    // If it's a complete URL with index.html, use it directly

    if (newUrl.pathname.includes('index.html')) {
      window.open(newUrl.toString(), '_blank')
      return
    }

    try {
      const currentUrl = new URL(window.location.href)
      const newUrl = new URL(
        currentUrl.pathname + currentUrl.search + currentUrl.hash,
        normalizedHost
      )
      window.open(newUrl.toString(), '_blank' + '?namespace=' + value)
    } catch (e) {
      console.error('Failed to construct new URL:', e)
      const currentPath = window.location.pathname + window.location.search
      // Fallback to original behavior if URL construction fails
      const newUrl = normalizedHost + currentPath
      window.open(newUrl, '_blank' + '?namespace=' + value)
    }
  } else {
    setCurrentNs(value)
  }
}
const getAllNamespaces = async () => {
  const loadedNamespaces = (await NamespaceApiService.getAllNamespaces()) as Namespace[]

   // Check if the current URL's host is contained in the host of each item in loadedNamespaces, 
  // if so, select this as the defaultNamespaceCode
  const currentHost = window.location.host
  let defaultNamespaceCode = ''

  const matchedNamespace = loadedNamespaces.find(
    namespace => namespace.host?.includes(currentHost)
  )

  if (matchedNamespace) {
    defaultNamespaceCode = matchedNamespace.code
  } else {
    defaultNamespaceCode = loadedNamespaces[0]?.code || ''
  }

  setCurrentNs(defaultNamespaceCode)
  setNamespaces(
    loadedNamespaces.map(namespace => ({
      id: namespace.code,
      name: namespace.name,
      host: namespace.host,
    }))
  )
}
const handleChangeNamespace = () => {
  router.push({
    name: route.name,
    params: {
      ...route.params,
      category: 'namespace',
    },
    query: {
      namespace: namespace.value,
    },
  })
}

onMounted(async () => {
  getAllNamespaces()
})
</script>

<style scoped>
.setting-icon {
  color: #667eea;
  margin-left: 4px;
  cursor: pointer;
}
</style>
