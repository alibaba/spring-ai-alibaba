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
    <!-- 顶部标题栏 -->
    <div class="config-header">
      <!-- <h1>配置中心</h1> -->
      <div class="header-actions">
        <LanguageSwitcher />
        <button class="action-btn" @click="$router.push('/')">
          <Icon icon="carbon:arrow-left" />
          {{ $t('backHome') }}
        </button>
      </div>
    </div>

    <!-- 主体内容区域 -->
    <div class="config-content">
      <!-- 左侧导航 -->
      <nav class="config-nav">
        <div
          v-for="(item, index) in categories"
          :key="index"
          class="nav-item"
          :class="{ active: activeCategory === item.key }"
          @click="activeCategory = item.key"
        >
          <Icon :icon="item.icon" width="20" />
          <span>{{ item.label }}</span>
        </div>
      </nav>

      <!-- 右侧配置详情 -->
      <div class="config-details">
        <BasicConfig v-if="activeCategory === 'basic'" />
        <AgentConfig v-if="activeCategory === 'agent'" />
        <ModelConfig v-if="activeCategory === 'model'" />
        <McpConfig v-if="activeCategory === 'mcp'" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import BasicConfig from './basicConfig.vue'
import AgentConfig from './agentConfig.vue'
import ModelConfig from './modelConfig.vue'
import McpConfig from './mcpConfig.vue'
import LanguageSwitcher from '@/components/language-switcher/index.vue'

const { t } = useI18n()

const activeCategory = ref('basic')
const categories = computed(() => [
  { key: 'basic', label: t('config.categories.basic'), icon: 'carbon:settings' },
  { key: 'agent', label: t('config.categories.agent'), icon: 'carbon:bot' },
  { key: 'model', label: t('config.categories.model'), icon: 'carbon:build-image' },
  { key: 'mcp', label: t('config.categories.mcp'), icon: 'carbon:tool-box' },
])
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
  align-items: center;
  gap: 12px;
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
  width: 240px;
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
  padding: 30px;
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
