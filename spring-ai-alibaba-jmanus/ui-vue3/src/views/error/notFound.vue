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
  <div class="not-found-page">
    <div class="error-container">
      <div class="error-icon">
        <img src="/Java-AI.svg" alt="Java-AI" width="96" height="96" class="java-logo" />
      </div>
      <h1 class="error-code">404</h1>
      <p class="error-message">页面未找到</p>
      <button class="back-button" @click="backHome">
        <Icon icon="carbon:arrow-left" />
        返回首页
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'

const router = useRouter()

const backHome = () => {
  router.push('/home')
}
</script>

<style lang="less" scoped>
.not-found-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #0a0a0a;
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 48px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
}

.error-icon {
  width: 120px;
  height: 120px;
  border-radius: 60px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
  display: flex;
  align-items: center;
  justify-content: center;

  .java-logo {
    color: #667eea;
    filter: drop-shadow(0 0 12px rgba(102, 126, 234, 0.5));
  }
}

.error-code {
  font-size: 64px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 30px rgba(102, 126, 234, 0.3);
}

.error-message {
  font-size: 18px;
  color: #888888;
  margin: 0;
  text-align: center;
}

.back-button {
  padding: 12px 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  }
}
</style>
