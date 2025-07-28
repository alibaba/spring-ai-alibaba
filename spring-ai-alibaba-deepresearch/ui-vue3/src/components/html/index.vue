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
  <div ref="containerRef" class="__container_html_renderer" :class="{ 'fullscreen': isFullscreen }">
    <!-- Tab 切换器 -->
    <div class="tab-header">
      <div class="tab-buttons">
        <button 
          class="tab-button" 
          :class="{ active: activeTab === 'code' }"
          @click="activeTab = 'code'"
        >
          代码
        </button>
        <button 
          class="tab-button" 
          :class="{ active: activeTab === 'preview' }"
          @click="activeTab = 'preview'"
        >
          预览
        </button>
      </div>
      <div class="header-right">
        <div v-if="isLoading" class="loading-indicator">
          <span class="loading-text">正在渲染页面...</span>
        </div>
        <!-- 全屏按钮 -->
        <button 
          class="fullscreen-button"
          @click="toggleFullscreen"
          :title="isFullscreen ? '退出全屏' : '全屏显示'"
        >
          <svg v-if="!isFullscreen" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3"/>
          </svg>
        </button>
      </div>
    </div>
    
    <!-- Tab 内容区域 -->
    <div class="tab-content">
      <!-- 代码视图 -->
      <div v-if="activeTab === 'code'" class="code-view">
        <pre class="code-content"><code>{{ displayHtml }}</code></pre>
      </div>
      
      <!-- 预览视图 -->
      <div 
        v-if="activeTab === 'preview'" 
        class="preview-view"
        :class="{ 'loading': isLoading }"
      >
        <iframe 
          ref="htmlIframe"
          class="html-content"
          :srcdoc="iframeContent"
          sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
        ></iframe>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

interface Props {
  // 增量HTML字符串数组
  htmlChunks?: string[]
  // 单个HTML字符串（用于一次性传入完整HTML）
  htmlContent?: string
  // 渲染延迟（毫秒）
  renderDelay?: number
}

const props = withDefaults(defineProps<Props>(), {
  htmlChunks: () => [],
  htmlContent: '',
  renderDelay: 100
})

const emit = defineEmits<{
  // 当HTML完全渲染完成时触发
  renderComplete: [html: string]
}>()

// 当前累积的HTML内容
const accumulatedHtml = ref('')
// 是否正在加载
const isLoading = ref(false)
// 渲染定时器
let renderTimer: NodeJS.Timeout | null = null
// 当前激活的tab
const activeTab = ref<'code' | 'preview'>('code')
// iframe引用
const htmlIframe = ref<HTMLIFrameElement | null>(null)
// 全屏状态
const isFullscreen = ref(false)
// 容器引用
const containerRef = ref<HTMLElement | null>(null)

// 显示的HTML内容（用于代码视图）
const displayHtml = computed(() => {
  return accumulatedHtml.value || '<!-- 暂无HTML内容 -->'
})

// iframe内容（用于预览视图）
const iframeContent = computed(() => {
  if (!accumulatedHtml.value) return ''
  
  // 确保HTML内容包含完整的文档结构
  let htmlContent = accumulatedHtml.value.trim()  
  return htmlContent
})

// 渲染增量HTML chunks
const renderChunks = async () => {
  if (!props.htmlChunks.length) return
  
  isLoading.value = true
  accumulatedHtml.value = ''
  
  for (let i = 0; i < props.htmlChunks.length; i++) {
    accumulatedHtml.value += props.htmlChunks[i]
    // 如果是最后一个chunk，标记渲染完成
    if (i === props.htmlChunks.length - 1) {
      isLoading.value = false
      emit('renderComplete', accumulatedHtml.value)
    }
  }
}

// 渲染单个HTML内容
const renderSingleHtml = () => {
  if (!props.htmlContent) return
  
  isLoading.value = true
  
  renderTimer = setTimeout(() => {
    accumulatedHtml.value = props.htmlContent
    isLoading.value = false
    emit('renderComplete', accumulatedHtml.value)
  }, props.renderDelay)
}

// 清空内容
const clearContent = () => {
  accumulatedHtml.value = ''
  isLoading.value = false
  
  if (renderTimer) {
    clearTimeout(renderTimer)
    renderTimer = null
  }
}

// 切换全屏
const toggleFullscreen = async () => {
  if (!containerRef.value) return
  
  try {
    if (!isFullscreen.value) {
      // 进入全屏
      if (containerRef.value.requestFullscreen) {
        await containerRef.value.requestFullscreen()
      } else if ((containerRef.value as any).webkitRequestFullscreen) {
        await (containerRef.value as any).webkitRequestFullscreen()
      } else if ((containerRef.value as any).msRequestFullscreen) {
        await (containerRef.value as any).msRequestFullscreen()
      }
    } else {
      // 退出全屏
      if (document.exitFullscreen) {
        await document.exitFullscreen()
      } else if ((document as any).webkitExitFullscreen) {
        await (document as any).webkitExitFullscreen()
      } else if ((document as any).msExitFullscreen) {
        await (document as any).msExitFullscreen()
      }
    }
  } catch (error) {
    console.warn('全屏操作失败:', error)
  }
}

// 监听全屏状态变化
const handleFullscreenChange = () => {
  const fullscreenElement = document.fullscreenElement || 
    (document as any).webkitFullscreenElement || 
    (document as any).msFullscreenElement
  
  isFullscreen.value = fullscreenElement === containerRef.value
}

// 监听htmlChunks变化
watch(
  () => props.htmlChunks,
  (newChunks) => {
    if (newChunks && newChunks.length > 0) {
      // clearContent()
      renderChunks()
    }
  },
  { deep: true, immediate: true }
)

// 监听htmlContent变化
watch(
  () => props.htmlContent,
  (newContent) => {
    if (newContent) {
      clearContent()
      renderSingleHtml()
    }
  },
  { immediate: true }
)

// 暴露方法给父组件
defineExpose({
  clearContent,
  renderChunks,
  renderSingleHtml,
  getCurrentHtml: () => accumulatedHtml.value,
  isLoading: () => isLoading.value,
  setActiveTab: (tab: 'code' | 'preview') => { activeTab.value = tab },
  getActiveTab: () => activeTab.value,
  getIframe: () => htmlIframe.value
})

onMounted(() => {
  // 组件挂载时根据props初始化内容
  if (props.htmlChunks.length > 0) {
    renderChunks()
  } else if (props.htmlContent) {
    renderSingleHtml()
  }
  
  // 添加全屏事件监听器
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.addEventListener('msfullscreenchange', handleFullscreenChange)
})

onUnmounted(() => {
  if (renderTimer) {
    clearTimeout(renderTimer)
  }
  
  // 移除全屏事件监听器
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.removeEventListener('msfullscreenchange', handleFullscreenChange)
})
</script>

<style lang="less" scoped>
.__container_html_renderer {
  position: relative;
  width: 100%;
  min-height: 400px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  
  &.fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    border: none;
    border-radius: 0;
    z-index: 9999;
  }
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
  padding: 0;
}

.tab-buttons {
  display: flex;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-button {
  padding: 12px 20px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.3s ease;
  border-bottom: 2px solid transparent;
  
  &:hover {
    background: rgba(0, 123, 255, 0.1);
    color: #007bff;
  }
  
  &.active {
    color: #007bff;
    background: #fff;
    border-bottom-color: #007bff;
    font-weight: 500;
  }
}

.loading-indicator {
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.fullscreen-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    background: #f8f9fa;
    color: #007bff;
    border-color: #007bff;
  }
  
  &:active {
    transform: scale(0.95);
  }
  
  svg {
    flex-shrink: 0;
  }
}

.loading-text {
  display: inline-block;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
  100% {
    opacity: 1;
  }
}

.tab-content {
  height: calc(100% - 49px);
  overflow: auto;
}

.code-view {
  height: 100%;
  background: #f8f9fa;
}

.code-content {
  margin: 0;
  padding: 16px;
  height: 100%;
  overflow: auto;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #333;
  background: #f8f9fa;
  border: none;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.preview-view {
  height: 100%;
  background: #fff;
  transition: opacity 0.3s ease;
  
  &.loading {
    opacity: 0.7;
  }
}

.html-content {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}
</style>
