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
    <!-- Simplified Hello World Home Page -->
    <div class="welcome-container">
      <!-- Background effects -->
      <div class="background-effects">
        <div class="gradient-orb orb-1"></div>
        <div class="gradient-orb orb-2"></div>
        <div class="gradient-orb orb-3"></div>
      </div>
      
      <!-- Header -->
      <header class="header">
        <div class="header-top">
          <LanguageSwitcher />
        </div>
        <div class="logo-container">
          <div class="logo">
            <img src="/Java-AI.svg" alt="JTaskPoilot" class="java-logo" />
            <h1>JTaskPoilot</h1>
          </div>
                      <span class="tagline">{{ $t('home.tagline') }}</span>
        </div>
      </header>

      <!-- Main content -->
      <main class="main-content">
        <div class="conversation-container">
          <!-- Welcome section -->
          <div class="welcome-section">
            <h2 class="welcome-title">{{ $t('home.welcomeTitle') }}</h2>
            <p class="welcome-subtitle">{{ $t('home.welcomeSubtitle') }}</p>
            <button class="direct-button" @click="goToDirectPage">{{ $t('home.directButton') }}</button>
          </div>

          <!-- Input section -->
          <div class="input-section">
            <div class="input-container">
              <textarea
                v-model="userInput"
                ref="textareaRef"
                class="main-input"
                :placeholder="$t('home.inputPlaceholder')"
                @keydown="handleKeydown"
                @input="adjustTextareaHeight"
              ></textarea>
              <button class="send-button" :disabled="!userInput.trim()" @click="handleSend">
                <Icon icon="carbon:send-alt" />
              </button>
            </div>
          </div>
          <!-- All examples and plans -->
          <div class="examples-section">
            <div class="examples-grid">
              <div v-for="item in allCards" :key="item.title" class="card-with-type">
                <BlurCard
                  :content="item"
                  @clickCard="handleCardClick(item)"
                />
                <span class="card-type">{{ item.type }}</span>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import BlurCard from '@/components/blurCard/index.vue'
import LanguageSwitcher from '@/components/language-switcher/index.vue'
import { useTaskStore } from '@/stores/task'

const router = useRouter()
const taskStore = useTaskStore()
const userInput = ref('')
const textareaRef = ref<HTMLTextAreaElement>()

const { t } = useI18n()

const goToDirectPage = () => {
  const chatId = Date.now().toString()
  router.push({
    name: 'direct',
    params: { id: chatId },
  }).then(() => {
    console.log('[Home] jump to direct page' + t('common.success'))
  }).catch((error) => {
    console.error('[Home] jump to direct page' + t('common.error'), error)
  })
}

const examples = computed(() => [
  { title: t('home.examples.stockPrice.title'), type: 'message', description: t('home.examples.stockPrice.description'), icon: 'carbon:chart-line-data', prompt: t('home.examples.stockPrice.prompt') },
  { title: t('home.examples.novel.title'), type: 'message', description: t('home.examples.novel.description'), icon: 'carbon:book', prompt: t('home.examples.novel.prompt') },
  { title: t('home.examples.weather.title'), type: 'message', description: t('home.examples.weather.description'), icon: 'carbon:partly-cloudy', prompt: t('home.examples.weather.prompt') }
])
const plans = computed(() => [
  { title: t('home.examples.queryplan.title'), type: 'plan', description: t('home.examples.queryplan.description'), icon: 'carbon:plan', prompt: t('home.examples.queryplan.prompt'), planJson: { planType: 'simple', title: '查询沈询 阿里的所有信息并优化终止结构列', steps: [{ stepRequirement: '[BROWSER_AGENT] 通过 百度 查询 沈询 阿里 ， 获取第一页的html 百度数据，合并聚拢 到 html_data 的目录里', terminateColumns: '存放的目录路径' }, { stepRequirement: '[BROWSER_AGENT] 从 html_data 目录中找到所有的有效关于沈询 阿里 的网页链接，输出到 link.md里面', terminateColumns: 'url地址，说明' }], planId: 'planTemplate-1749200517403' } }
])
const allCards = computed(() => [...examples.value, ...plans.value])

const handleCardClick = (item: any) => {
  if (item.type === 'message') {
    selectExample(item)
  } else if (item.type === 'plan') {
    selectPlan(item)
  }
}

onMounted(() => {
  console.log('[Home] onMounted called')
  console.log('[Home] taskStore:', taskStore)
  console.log('[Home] examples:', examples)
  
  // Mark that the home page has been visited
  taskStore.markHomeVisited()
  console.log('[Home] Home visited marked')
})

import { sidebarStore } from '@/stores/sidebar'

const saveJsonPlanToTemplate = async (jsonPlan: any) => {
  try {
    sidebarStore.createNewTemplate();
    sidebarStore.jsonContent = JSON.stringify(jsonPlan);
    const saveResult = await sidebarStore.saveTemplate();
    if (saveResult?.duplicate) {
      console.log('[Sidebar] ' + t('sidebar.saveCompleted', { message: saveResult.message, versionCount: saveResult.versionCount }));
    } else if (saveResult?.saved) {
      console.log('[Sidebar] ' + t('sidebar.saveSuccess', { message: saveResult.message, versionCount: saveResult.versionCount }));
    } else if (saveResult?.message) {
      console.log('[Sidebar] ' + t('sidebar.saveStatus', { message: saveResult.message }));
    }
  } catch (error: any) {
    console.error('[Sidebar] Failed to save the plan to the template library:', error);
    alert(error.message || t('sidebar.saveFailed'));
  }
}

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
  
  // Use the store to pass task data
  taskStore.setTask(taskContent)
  console.log('[Home] Task set to store, current task:', taskStore.currentTask)
  
  // Navigate to direct page
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
  
  // Send the task directly using the example's prompt
  taskStore.setTask(example.prompt)
  console.log('[Home] Task set to store from example, current task:', taskStore.currentTask)
  
  // Navigate to direct page
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

const selectPlan = async (plan: any) => {
  console.log('[Home] selectPlan called with plan:', plan)
  
  try {
    // 1. First, save the plan to the template library
    await saveJsonPlanToTemplate(plan.planJson)
    console.log('[Home] Plan saved to templates')
    
    // 2. Navigate to the direct page
    const chatId = Date.now().toString()
    await router.push({
      name: 'direct',
      params: { id: chatId },
    })
    
    // 3. Navigate to the direct page after loading
    nextTick(async () => {
      // Ensure the page is fully loaded
      await new Promise(resolve => setTimeout(resolve, 300))
      
      // Toggle the sidebar
      if (sidebarStore.isCollapsed) {
        await sidebarStore.toggleSidebar()
        console.log('[Sidebar] Sidebar toggled')
      } else {
        console.log('[Sidebar] Sidebar is already open')
      }
      
      // Load the template list
      await sidebarStore.loadPlanTemplateList()
      console.log('[Sidebar] Template list loaded')
      
      // Find and select the template 
      const template = sidebarStore.planTemplateList.find(t => t.id === plan.planJson.planId)
      if (!template) {
        console.error('[Sidebar] Template not found')
        return
      }
      
      await sidebarStore.selectTemplate(template)
      console.log('[Sidebar] Template selected:', template.title)
      
      // Call the execute logic directly
      const executeBtn = document.querySelector('.execute-btn') as HTMLButtonElement
      if (!executeBtn.disabled) {
        console.log('[Sidebar] Triggering execute button click')
        executeBtn.click()
      } else {
        console.error('[Sidebar] Execute button not found or disabled')
      }
    })
  } catch (error) {
    console.error('[Home] Error in selectPlan:', error)
  }
}


</script>

<style lang="less" scoped>
.home-page {
  width: 100%;
  height: 100vh;
  position: relative;
  overflow-y: auto;
}

.welcome-container {
  flex: 1;
  height: 100vh;
  background: #0a0a0a;
  position: relative;
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
  z-index: 1000;
  padding: 32px 32px 0;
}

.header-top {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
  position: relative;
  z-index: 1001;
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
  margin-bottom: 48px;

  .examples-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); 
    gap: 16px;
    
    .card-with-type {
      width: 100%;
      min-width: 300px; 

      &:hover {
        .card-type {
          transform: translateY(-1px); 
          box-shadow: 0 8px 25px rgba(130, 151, 246, 0.4); 
        }
      }

    }
  }
}

.card-with-type {
  position: relative;
}

.card-type {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  z-index: 1;
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

.direct-button {
  margin-top: 20px;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.direct-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}
</style>
