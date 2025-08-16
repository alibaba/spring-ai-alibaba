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
  <div class="loading-spinner" :class="sizeClass">
    <div class="spinner" :style="spinnerStyle"></div>
    <div v-if="text" class="loading-text">{{ text }}</div>
  </div>
</template>

<script>
export default {
  name: 'LoadingSpinner',
  props: {
    size: {
      type: String,
      default: 'medium',
      validator: (value) => ['small', 'medium', 'large'].includes(value)
    },
    color: {
      type: String,
      default: '#1890ff'
    },
    text: {
      type: String,
      default: ''
    }
  },
  computed: {
    sizeClass() {
      return `spinner-${this.size}`
    },
    spinnerStyle() {
      return {
        borderTopColor: this.color
      }
    }
  }
}
</script>

<style scoped>
.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.spinner {
  border: 2px solid #f0f0f0;
  border-top-color: #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.spinner-small .spinner {
  width: 16px;
  height: 16px;
  border-width: 2px;
}

.spinner-medium .spinner {
  width: 24px;
  height: 24px;
  border-width: 2px;
}

.spinner-large .spinner {
  width: 32px;
  height: 32px;
  border-width: 3px;
}

.loading-text {
  font-size: 0.9rem;
  color: #8c8c8c;
  text-align: center;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 减少动画模式支持 */
@media (prefers-reduced-motion: reduce) {
  .spinner {
    animation: none;
    border-top-color: transparent;
    border-right-color: #1890ff;
  }
}
</style>
