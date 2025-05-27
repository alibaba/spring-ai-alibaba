<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
-->
<template>
  <div class="layout-container">
    <!-- Fullscreen pages (conversation, plan) -->
    <div v-if="isFullscreenPage" class="fullscreen-page">
      <router-view />
    </div>
    
    <!-- Regular layout with sidebar -->
    <div v-else class="regular-layout">
      <div class="sidebar">
        <div class="logo">
          <h2>JTaskPilot</h2>
        </div>
        <nav class="navigation">
          <router-link 
            v-for="route in navigationRoutes" 
            :key="route.name"
            :to="route.path"
            class="nav-item"
            :class="{ active: $route.name === route.name }"
          >
            <Icon v-if="route.meta?.icon" :icon="route.meta.icon" class="nav-icon" />
            {{ route.name }}
          </router-link>
        </nav>
      </div>
      
      <div class="main-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import { routes } from '@/router/defaultRoutes'

const route = useRoute()

const isFullscreenPage = computed(() => {
  return route.meta?.fullscreen === true
})

const navigationRoutes = computed(() => {
  const rootRoute = routes.find(r => r.name === 'Root')
  return rootRoute?.children?.filter(child => !child.meta?.skip && !child.meta?.fullscreen) || []
})
</script>

<style lang="less" scoped>
.layout-container {
  height: 100vh;
  background: #0a0a0a;
  color: #ffffff;
}

.fullscreen-page {
  height: 100vh;
  width: 100vw;
}

.regular-layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 280px;
  background: linear-gradient(180deg, #111111 0%, #0a0a0a 100%);
  border-right: 1px solid #1a1a1a;
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.logo {
  margin-bottom: 32px;
  
  h2 {
    color: #ffffff;
    font-size: 20px;
    font-weight: 600;
    margin: 0;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.navigation {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  color: #888888;
  text-decoration: none;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
  
  &:hover {
    background: rgba(255, 255, 255, 0.05);
    color: #ffffff;
  }
  
  &.active {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
    color: #667eea;
    box-shadow: 0 0 20px rgba(102, 126, 234, 0.2);
  }
}

.nav-icon {
  font-size: 18px;
}

.main-content {
  flex: 1;
  overflow: hidden;
}
</style>
