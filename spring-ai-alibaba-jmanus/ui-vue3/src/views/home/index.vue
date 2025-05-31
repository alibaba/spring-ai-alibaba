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
    <Sidebar />
    <div class="conversation">
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
            <img src="/Java-AI.svg" alt="JManus" class="java-logo" />
            <h1>JManus</h1>
          </div>
          <span class="tagline">Java AI 智能体</span>
        </div>
      </header>

      <!-- Main content -->
      <main class="main-content">
        <div class="conversation-container">
          <!-- Welcome section -->
          <div class="welcome-section">
            <h2 class="welcome-title">今天我能帮你构建什么？</h2>
            <p class="welcome-subtitle">描述您的任务或项目，我将帮助您逐步规划和执行。</p>
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
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import Sidebar from '@/components/sidebar/index.vue'
import BlurCard from '@/components/blurCard/index.vue'

const router = useRouter()
const userInput = ref('')
const textareaRef = ref<HTMLTextAreaElement>()

const examples = [
  {
    title: '查询股价',
    description: '获取今天阿里巴巴的最新股价',
    icon: 'carbon:chart-line-data',
    prompt: '查询今天阿里巴巴的股价',
  },
  {
    title: '预订机票',
    description: '帮我查找并预订从上海到北京的机票',
    icon: 'carbon:plane',
    prompt: '帮忙预定一下从上海到北京的机票',
  },
  {
    title: '查询天气',
    description: '获取北京今天的天气情况',
    icon: 'carbon:partly-cloudy',
    prompt: '查询北京今天的天气',
  },
  {
    title: '设置提醒',
    description: '提醒我明天下午三点开会',
    icon: 'carbon:alarm',
    prompt: '提醒我明天下午三点开会',
  },
]

const adjustTextareaHeight = () => {
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
      textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 200) + 'px'
    }
  })
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

const handleSend = () => {
  if (!userInput.value.trim()) return

  // Navigate to plan page with the user input
  const planId = Date.now().toString()
  router.push({
    name: 'plan',
    params: { id: planId },
    query: { prompt: userInput.value },
  })
}

const selectExample = (example: any) => {
  userInput.value = example.prompt
  adjustTextareaHeight()
}
</script>

<style lang="less" scoped>
.home-page {
  width: 100%;
  display: flex;
  position: relative;
}
.conversation {
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
</style>
