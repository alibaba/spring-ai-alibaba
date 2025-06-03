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
  <div class="sidebar-wrapper" :class="{ 'sidebar-wrapper-collapsed': isCollapsed }">
    <div class="sidebar-content">
      <div class="sidebar-content-header">
        <div class="sidebar-content-title">历史记录</div>

        <div class="config-button" @click="handleConfig">
          <Icon icon="carbon:settings-adjust" width="20" />
        </div>
      </div>
      <div class="sidebar-content-list">
        <BlurCard
          class="sidebar-content-list-item"
          :class="{ 'sidebar-content-list-item-active': item.id === activeId }"
          v-for="item in mockHistory"
          :key="item.id"
          :content="{ title: item.title, description: item.content, icon: 'carbon:send-alt' }"
          @click="navigateTo(item.id)"
        />
      </div>
    </div>

    <div class="sidebar-switch" @click="toggleSidebar">
      <div class="tb-line-wrapper" :class="{ 'tb-line-wrapper-expanded': !isCollapsed }">
        <div class="tb-line" :class="{ 'tb-line-expanded': !isCollapsed }"></div>
        <div class="bt-line" :class="{ 'bt-line-expanded': !isCollapsed }"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Icon } from '@iconify/vue'

import BlurCard from '@/components/blurCard/index.vue'
import router from '@/router'

const isCollapsed = ref(true)
const activeId = ref<number | null>(null)

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const navigateTo = (id: number) => {
  activeId.value = id
  router.push(`/plan/${id}`)
}

const handleConfig = () => {
  router.push('/configs')
}

// TODO: 替换为真实接口
const mockData = Array(10)
  .fill(0)
  .map((_, index) => {
    return {
      id: index,
      title: '已完成',
      content: '查询杭州到成都的机票...',
    }
  })
const mockHistory = reactive(mockData)
</script>

<style scoped>
.sidebar-wrapper {
  position: relative;
  width: 280px;
  height: 100vh;
  background: rgba(255, 255, 255, 0.05);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  transition: width 0.3s ease-in-out;
  cursor: pointer;

  .sidebar-switch {
    position: absolute;
    top: 50%;
    right: -40px;
    transform: translateY(-50%);
    height: 80px;
    z-index: 10;

    .tb-line-wrapper {
      display: flex;
      flex-direction: column;
      gap: 6px;
      transition: all 0.3s ease-in-out;

      .tb-line,
      .bt-line {
        background-color: #667eea;
        width: 22px;
        height: 6px;
        border-radius: 10px;
        transform: rotate(-45deg);
        transition: all 0.3s ease-in-out;
      }
      .tb-line {
        background-color: #6646a2;
        transition: all 0.5s ease-in-out;
        transform: rotate(45deg);
      }

      .tb-line-expanded,
      .bt-line-expanded {
        width: 36px;
      }

      .tb-line-expanded {
        transform: rotate(45deg) translateY(9px);
      }

      .bt-line-expanded {
        transform: rotate(-45deg) translateY(-9px);
      }
    }

    .tb-line-wrapper-expanded {
      transform: rotate(180deg);
    }
  }
}
.sidebar-wrapper-collapsed {
  width: 1px;
}

.sidebar-content {
  height: 100%;
  width: 100%;
  padding: 12px 0 12px 12px;

  .sidebar-content-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    overflow: hidden;

    .sidebar-content-title {
      font-size: 20px;
      font-weight: 600;

      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      text-fill-color: transparent;
      /* margin-bottom: 12px; */
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .config-button {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      margin-right: 16px;
      /* border: 1px solid rgba(255, 255, 255, 0.1); */
      /* border-radius: 6px; */
      /* background: rgba(255, 255, 255, 0.05); */
      cursor: pointer;
      display: flex;
      align-items: center;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        border-color: rgba(255, 255, 255, 0.2);
      }
    }
  }

  .sidebar-content-list {
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow-y: auto;

    .sidebar-content-list-item {
      width: calc(100% - 12px);
      margin-top: 12px;
      min-height: 96px;
      text-wrap: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .sidebar-content-list-item-active {
      border: 2px solid #667eea;
    }
  }
}
</style>
