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
  <div class="error-boundary">
    <div v-if="hasError" class="error-fallback">
      <slot name="fallback" :error="error" :retry="retry">
        <div class="error-content">
          <div class="error-icon">
            <i class="bi bi-exclamation-triangle"></i>
          </div>
          <div class="error-info">
            <h3 class="error-title">{{ title }}</h3>
            <p class="error-message">{{ message }}</p>
            <div v-if="showDetails && error" class="error-details">
              <details>
                <summary>错误详情</summary>
                <pre class="error-stack">{{ error.stack || error.message }}</pre>
              </details>
            </div>
            <div class="error-actions">
              <button class="btn btn-primary" @click="retry">
                <i class="bi bi-arrow-clockwise"></i>
                重试
              </button>
              <button v-if="showReload" class="btn btn-outline" @click="reload">
                <i class="bi bi-arrow-repeat"></i>
                刷新页面
              </button>
            </div>
          </div>
        </div>
      </slot>
    </div>
    <div v-else>
      <slot></slot>
    </div>
  </div>
</template>

<script>
import { ref, onErrorCaptured, nextTick } from 'vue'

export default {
  name: 'ErrorBoundary',
  props: {
    title: {
      type: String,
      default: '出现了一些问题'
    },
    message: {
      type: String,
      default: '抱歉，页面遇到了错误。请尝试刷新页面或联系技术支持。'
    },
    showDetails: {
      type: Boolean,
      default: false
    },
    showReload: {
      type: Boolean,
      default: true
    },
    onError: {
      type: Function,
      default: null
    }
  },
  emits: ['error', 'retry'],
  setup(props, { emit }) {
    const hasError = ref(false)
    const error = ref(null)

    const retry = async () => {
      hasError.value = false
      error.value = null
      emit('retry')
      
      // 等待下一个tick，让组件重新渲染
      await nextTick()
    }

    const reload = () => {
      window.location.reload()
    }

    onErrorCaptured((err, instance, info) => {
      console.error('ErrorBoundary caught an error:', err)
      console.error('Component instance:', instance)
      console.error('Error info:', info)

      hasError.value = true
      error.value = err

      // 调用自定义错误处理函数
      if (props.onError) {
        props.onError(err, instance, info)
      }

      // 发出错误事件
      emit('error', {
        error: err,
        instance,
        info
      })

      // 阻止错误继续向上传播
      return false
    })

    return {
      hasError,
      error,
      retry,
      reload
    }
  }
}
</script>

<style scoped>
.error-boundary {
  width: 100%;
  height: 100%;
}

.error-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: var(--space-2xl);
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
}

.error-content {
  text-align: center;
  max-width: 500px;
}

.error-icon {
  margin-bottom: var(--space-lg);
}

.error-icon i {
  font-size: 4rem;
  color: var(--error-color);
}

.error-info {
  margin-bottom: var(--space-xl);
}

.error-title {
  margin: 0 0 var(--space-md) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.error-message {
  margin: 0 0 var(--space-lg) 0;
  color: var(--text-secondary);
  line-height: var(--line-height-relaxed);
}

.error-details {
  margin: var(--space-lg) 0;
  text-align: left;
}

.error-details summary {
  cursor: pointer;
  color: var(--text-tertiary);
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-sm);
}

.error-details summary:hover {
  color: var(--text-secondary);
}

.error-stack {
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  padding: var(--space-md);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 200px;
  overflow-y: auto;
}

.error-actions {
  display: flex;
  gap: var(--space-md);
  justify-content: center;
  flex-wrap: wrap;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  border: 1px solid transparent;
  border-radius: var(--radius);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--transition-base);
  text-decoration: none;
  white-space: nowrap;
}

.btn-primary {
  background: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.btn-primary:hover {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.btn-outline {
  background: transparent;
  color: var(--text-secondary);
  border-color: var(--border-color);
}

.btn-outline:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
  border-color: var(--text-tertiary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .error-fallback {
    padding: var(--space-lg);
    min-height: 300px;
  }
  
  .error-icon i {
    font-size: 3rem;
  }
  
  .error-title {
    font-size: var(--font-size-lg);
  }
  
  .error-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .btn {
    width: 100%;
    max-width: 200px;
    justify-content: center;
  }
}
</style>
