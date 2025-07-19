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
import { NamespaceApiService, type Namespace } from '@/api/namespace-api-service'

import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const namespaceStore = usenameSpaceStore()
const { namespace, namespaces } = storeToRefs(namespaceStore)
const { setCurrentNs, setNamespaces } = namespaceStore

const handleNamespaceChange = (value: string) => {
  setCurrentNs(value)
}

const getAllNamespaces = async () => {
  const loadedNamespaces = (await NamespaceApiService.getAllNamespaces()) as Namespace[]
  const defaultNamespaceCode = loadedNamespaces[0]?.code || ''

  setCurrentNs(defaultNamespaceCode)
  setNamespaces(
    loadedNamespaces.map(namespace => ({
      id: namespace.code,
      name: namespace.name,
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
      "namespace": namespace.value,
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
