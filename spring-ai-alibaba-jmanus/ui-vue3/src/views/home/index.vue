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
  <div class="home-page">
    <!-- 简化的 Hello World 主页 -->
    <div class="welcome-container">
      <!-- Background effects -->
      <div class="background-effects">
        <div class="gradient-orb orb-1"></div>
        <div class="gradient-orb orb-2"></div>
        <div class="gradient-orb orb-3"></div>
      </div>
      
      <!-- Header -->
      <header class="header">
        <div class="logo-container">
          <div class="logo">
            <img src="/Java-AI.svg" alt="JTaskPoilot" class="java-logo" />
            <h1>JTaskPoilot</h1>
          </div>
          <span class="tagline">Java AI 智能体</span>
        </div>
      </header>

      <!-- Main content -->
      <main class="main-content">
        <div class="conversation-container">
          <!-- Welcome section -->
          <div class="welcome-section">
            <h2 class="welcome-title">欢迎使用 JTaskPoilot！</h2>
            <p class="welcome-subtitle">您的 Java AI 智能助手，帮助您构建和完成各种任务。</p>
          </div>

          <!-- Input section -->
          <div class="input-section">
            <div class="input-container">
              <textarea
                v-model="userInput"
                ref="textareaRef"
                class="main-input"
                placeholder="描述您想构建或完成的内容..."
                @keydown="handleKeydown"
                @input="adjustTextareaHeight"
              ></textarea>
              <button class="send-button" :disabled="!userInput.trim()" @click="handleSend">
                <Icon icon="carbon:send-alt" />
              </button>
            </div>
          </div>

          <!-- Example prompts -->
          <div class="examples-section">
            <div class="examples-grid">
              <BlurCard
                v-for="example in examples"
                :key="example.title"
                :content="example"
                @clickCard="selectExample"
              />
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import BlurCard from '@/components/blurCard/index.vue'
import { useTaskStore } from '@/stores/task'

const router = useRouter()
const taskStore = useTaskStore()
const userInput = ref('')
const textareaRef = ref<HTMLTextAreaElement>()

const examples = [
  {
    title: '查询股价',
    description: '获取今天阿里巴巴的最新股价（Agent可以使用浏览器工具）',
    icon: 'carbon:chart-line-data',
    prompt: '用浏览器基于百度，查询今天阿里巴巴的股价，并返回最新股价',
  },
  {
    title: '生成一个中篇小说',
    description: '帮我生成一个中篇小说（Agent可以生成更长的内容）',
    icon: 'carbon:book',
    prompt: '请帮我写一个关于机器人取代人类的小说。20000字。 使用TEXT_FILE_AGENT ，先生成提纲，然后，完善和丰满整个提纲的内容为一篇通顺的小说，最后再全局通顺一下语法',
  },
  {
    title: '查询天气',
    description: '获取北京今天的天气情况（Agent可以使用MCP工具服务）',
    icon: 'carbon:partly-cloudy',
    prompt: '用浏览器，基于百度，查询北京今天的天气',
  },
]

onMounted(() => {
  console.log('[Home] onMounted called')
  console.log('[Home] taskStore:', taskStore)
  console.log('[Home] examples:', examples)
  
  // 标记已访问过 home 页面
  taskStore.markHomeVisited()
  console.log('[Home] Home visited marked')
})

const adjustTextareaHeight = () => {
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
      textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 200) + 'px'
    }
  })
}

const handleKeydown = (event: KeyboardEvent) => {
  console.log('[Home] handleKeydown called, key:', event.key)
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    console.log('[Home] Enter key pressed, calling handleSend')
    handleSend()
  }
}

const handleSend = () => {
  console.log('[Home] handleSend called, userInput:', userInput.value)
  if (!userInput.value.trim()) {
    console.log('[Home] handleSend aborted - empty input')
    return
  }

  const taskContent = userInput.value.trim()
  console.log('[Home] Setting task to store:', taskContent)
  
  // 使用 store 传递任务数据
  taskStore.setTask(taskContent)
  console.log('[Home] Task set to store, current task:', taskStore.currentTask)
  
  // 导航到 direct 页面
  const chatId = Date.now().toString()
  console.log('[Home] Navigating to direct page with chatId:', chatId)
  
  router.push({
    name: 'direct',
    params: { id: chatId },
  }).then(() => {
    console.log('[Home] Navigation to direct page completed')
  }).catch((error) => {
    console.error('[Home] Navigation error:', error)
  })
}

const selectExample = (example: any) => {
  console.log('[Home] selectExample called with example:', example)
  console.log('[Home] Example prompt:', example.prompt)
  
  // 直接使用示例的 prompt 发送任务
  taskStore.setTask(example.prompt)
  console.log('[Home] Task set to store from example, current task:', taskStore.currentTask)
  
  // 导航到 direct 页面
  const chatId = Date.now().toString()
  console.log('[Home] Navigating to direct page with chatId:', chatId)
  
  router.push({
    name: 'direct',
    params: { id: chatId },
  }).then(() => {
    console.log('[Home] Navigation to direct page completed (from example)')
  }).catch((error) => {
    console.error('[Home] Navigation error (from example):', error)
  })
}
</script>

<style lang="less" scoped>
.home-page {
  width: 100%;
  height: 100vh;
  position: relative;
}

.welcome-container {
  flex: 1;
  height: 100vh;
  background: #0a0a0a;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.background-effects {
  position: fixed;
  width: 100vw;
  height: 100vh;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.3;
  animation: float 6s ease-in-out infinite;

  &.orb-1 {
    width: 400px;
    height: 400px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    top: -200px;
    right: -200px;
    animation-delay: 0s;
  }

  &.orb-2 {
    width: 300px;
    height: 300px;
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    bottom: -150px;
    left: -150px;
    animation-delay: 2s;
  }

  &.orb-3 {
    width: 250px;
    height: 250px;
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    animation-delay: 4s;
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0px) rotate(0deg);
  }
  33% {
    transform: translateY(-20px) rotate(120deg);
  }
  66% {
    transform: translateY(10px) rotate(240deg);
  }
}

.header {
  position: relative;
  z-index: 1;
  padding: 32px 32px 0;
}

.logo-container {
  text-align: center;

  .logo {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  img {
    height: 52px;
    margin-bottom: 12px;
  }

  h1 {
    font-size: 48px;
    font-weight: 700;
    margin: 0 0 8px 0;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .tagline {
    color: #888888;
    font-size: 16px;
    font-weight: 400;
  }
}

.main-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 32px 32px;
  position: relative;
  z-index: 1;
}

.conversation-container {
  width: 100%;
  max-width: 800px;
}

.welcome-section {
  text-align: center;
  margin-bottom: 48px;
}

.welcome-title {
  font-size: 32px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 16px 0;
}

.welcome-subtitle {
  font-size: 18px;
  color: #888888;
  margin: 0;
  line-height: 1.5;
}

.input-section {
  margin-bottom: 48px;
}

.input-container {
  position: relative;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;

  &:focus-within {
    border-color: #667eea;
    box-shadow: 0 8px 32px rgba(102, 126, 234, 0.2);
  }
}

.main-input {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  color: #ffffff;
  font-size: 16px;
  line-height: 1.5;
  resize: none;
  min-height: 24px;
  max-height: 200px;
  padding-right: 60px;

  &::placeholder {
    color: #666666;
  }
}

.send-button {
  position: absolute;
  right: 16px;
  bottom: 16px;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  svg {
    font-size: 18px;
  }
}

.examples-section {
  .examples-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 16px;
  }
}

// .example-card {
//   background: rgba(255, 255, 255, 0.03);
//   border: 1px solid rgba(255, 255, 255, 0.08);
//   border-radius: 12px;
//   padding: 20px;
//   cursor: pointer;
//   transition: all 0.3s ease;
//   text-align: left;
//   display: flex;
//   align-items: flex-start;
//   gap: 16px;

//   &:hover {
//     background: rgba(255, 255, 255, 0.05);
//     border-color: rgba(102, 126, 234, 0.3);
//     transform: translateY(-2px);
//     box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
//   }
// }

// .example-icon {
//   font-size: 24px;
//   color: #667eea;
//   margin-top: 4px;
//   flex-shrink: 0;
// }

// .example-content {
//   h3 {
//     font-size: 16px;
//     font-weight: 600;
//     color: #ffffff;
//     margin: 0 0 8px 0;
//   }

//   p {
//     font-size: 14px;
//     color: #888888;
//     margin: 0;
//     line-height: 1.4;
//   }
// }

/* Config View Styles */
.config-view {
  flex: 1;
  height: 100vh;
  background: #0a0a0a;
  display: flex;
  flex-direction: column;
  position: relative;
}

.config-header-bar {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  gap: 16px;

  .back-button {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 8px;
    color: #ffffff;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.15);
      border-color: rgba(255, 255, 255, 0.3);
      transform: translateY(-1px);
    }
  }

  .config-title {
    font-size: 20px;
    font-weight: 600;
    color: #ffffff;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}
</style>
