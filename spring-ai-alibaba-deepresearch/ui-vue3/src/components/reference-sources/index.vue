<template>
  <div class="reference-sources">
    <div class="sources-header">
      <span class="sources-count">{{ sources.length }} 个来源</span>
    </div>
    <div class="sources-list">
      <div 
        v-for="(source, index) in sources" 
        :key="index"
        class="source-item"
        @click="openSource(source.url)"
      >
        <div class="source-icon">
          <img 
            :src="source.icon" 
            :alt="getHostname(source.url)"
            @error="handleImageError"
          />
        </div>
        <div class="source-content">
          <div class="source-title">{{ source.title }}</div>
          <div class="source-host">{{ getHostname(source.url) }}</div>
        </div>
        <div class="source-weight" v-if="source.weight">
          <span class="weight-label">权重</span>
          <span class="weight-value">{{ source.weight }}</span>
        </div>
        <div class="source-arrow">
          <ExportOutlined />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ExportOutlined } from '@ant-design/icons-vue'

interface SourceItem {
  title: string
  url: string
  content?: string
  icon: string
  weight?: number
}

interface Props {
  sources: SourceItem[]
}

withDefaults(defineProps<Props>(), {
  sources: () => []
})

// 获取域名
const getHostname = (url: string): string => {
  try {
    const urlObj = new URL(url)
    return urlObj.hostname
  } catch {
    return url
  }
}

// 打开链接
const openSource = (url: string) => {
  window.open(url, '_blank')
}

// 图片加载失败处理
const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTYiIGhlaWdodD0iMTYiIHZpZXdCb3g9IjAgMCAxNiAxNiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjE2IiBoZWlnaHQ9IjE2IiByeD0iMiIgZmlsbD0iI0Y1RjVGNSIvPgo8cGF0aCBkPSJNNSA2SDExVjEwSDVWNloiIGZpbGw9IiNEOUQ5RDkiLz4KPC9zdmc+'
}
</script>

<style lang="less" scoped>
.reference-sources {
  margin-top: 24px;
  
  .sources-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: #1890ff;
    }
    
    .sources-count {
      font-size: 12px;
      color: #999;
      background: #f5f5f5;
      padding: 2px 8px;
      border-radius: 10px;
    }
  }
  
  .sources-list {
    .source-item {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      margin-bottom: 8px;
      background: linear-gradient(135deg, #fafbff 0%, #f6f8ff 100%);
      border: 1px solid #e8f0ff;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s ease;
      
      &:hover {
        background: linear-gradient(135deg, #f0f4ff 0%, #e8f0ff 100%);
        border-color: #d0e0ff;
        transform: translateY(-1px);
        box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
      }
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .source-icon {
        width: 20px;
        height: 20px;
        margin-right: 12px;
        flex-shrink: 0;
        
        img {
          width: 100%;
          height: 100%;
          border-radius: 3px;
          object-fit: cover;
        }
      }
      
      .source-content {
        flex: 1;
        min-width: 0;
        
        .source-title {
          font-size: 14px;
          font-weight: 500;
          color: #262626;
          margin-bottom: 4px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        
        .source-host {
          font-size: 12px;
          color: #8c8c8c;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
      
      .source-weight {
        display: flex;
        flex-direction: column;
        align-items: center;
        margin-right: 12px;
        
        .weight-label {
          font-size: 10px;
          color: #999;
          margin-bottom: 2px;
        }
        
        .weight-value {
          font-size: 12px;
          font-weight: 600;
          color: #1890ff;
          background: #e6f7ff;
          padding: 2px 6px;
          border-radius: 4px;
        }
      }
      
      .source-arrow {
        color: #bfbfbf;
        font-size: 12px;
        transition: all 0.3s ease;
      }
      
      &:hover .source-arrow {
        color: #1890ff;
        transform: translateX(2px);
      }
    }
  }
}
</style>
