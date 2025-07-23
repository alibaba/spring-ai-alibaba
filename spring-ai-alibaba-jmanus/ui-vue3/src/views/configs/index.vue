<!-- 
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<template>
  <div class="config-container">
    <!-- Top title bar -->
    <div class="config-header">
      <!-- <h1>Configuration Center</h1> -->
      <div class="header-actions">
        <div class="header-actions-left">
          <button class="action-btn" @click="$router.push('/')">
            <Icon icon="carbon:arrow-left" />
            {{ $t('backHome') }}
          </button>
          <LanguageSwitcher />
        </div>
        <div class="header-actions-right">
          <NamespaceSwitch />
        </div>
      </div>
    </div>

    <!-- Main content area -->
    <div class="config-content">
      <!-- Left navigation -->
      <nav class="config-nav">
        <template v-for="(item, index) in categories">
          <div
            v-if="!item.disabled"
            :key="index"
            class="nav-item"
            :class="{ active: activeCategory === item.key }"
            @click="handleNavClick(item.key)"
          >
            <Icon :icon="item.icon" width="20" />
            <span>{{ item.label }}</span>
          </div>
        </template>
      </nav>

      <!-- Right configuration details -->
      <div class="config-details">
        <component :is="activeComponent" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import BasicConfig from './basicConfig.vue'
import AgentConfig from './agentConfig.vue'
import ModelConfig from './modelConfig.vue'
import McpConfig from './mcpConfig.vue'
import DynamicPromptConfig from './dynamicPromptConfig.vue'
import LanguageSwitcher from '@/components/language-switcher/index.vue'
import NamespaceConfig from './namespaceConfig.vue'
import NamespaceSwitch from './components/namespaceSwitch.vue'

interface CategoryMap {
  [key: string]: any
  basic: typeof BasicConfig
  agent: typeof AgentConfig
  model: typeof ModelConfig
  mcp: typeof McpConfig
  prompt: typeof DynamicPromptConfig
  namespace: typeof NamespaceConfig
}

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const activeCategory = ref(route.params.category || 'basic')

const categoryMap: CategoryMap = {
  basic: BasicConfig,
  agent: AgentConfig,
  model: ModelConfig,
  mcp: McpConfig,
  prompt: DynamicPromptConfig,
  namespace: NamespaceConfig,
}

const activeComponent = computed(() => {
  const categoryKey = activeCategory.value as string
  return categoryMap[categoryKey] || BasicConfig
})

const categories = computed(() => [
  { key: 'basic', label: t('config.categories.basic'), icon: 'carbon:settings' },
  { key: 'agent', label: t('config.categories.agent'), icon: 'carbon:bot' },
  { key: 'model', label: t('config.categories.model'), icon: 'carbon:build-image' },
  { key: 'mcp', label: t('config.categories.mcp'), icon: 'carbon:tool-box' },
  { key: 'prompt', label: t('config.categories.prompt'), icon: 'carbon:repo-artifact' },
  { key: 'namespace', disabled: true, icon: 'carbon:repo-artifact' },
])
const handleNavClick = (categoryKey: string) => {
  activeCategory.value = categoryKey

  router.push({
    name: route.name as string,
    params: {
      ...route.params,
      category: categoryKey,
    },
    query: route.query,
  })
}
</script>

<style scoped>
.config-container {
  height: 100vh;
  background: rgba(255, 255, 255, 0.02);
  color: #fff;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.header-actions-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.config-header h1 {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-size: 24px;
  font-weight: 600;
}

.config-content {
  display: flex;
  height: calc(100vh - 80px);
}

.config-nav {
  width: 242px;
  padding: 20px;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.nav-item.active {
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.2);
}

.config-details {
  flex: 1;
  padding: 24px 30px;
  overflow-y: auto;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}
</style>
