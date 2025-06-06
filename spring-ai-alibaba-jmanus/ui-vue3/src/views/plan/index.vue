<!-- /*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the const messagesRef = ref<HTMLElement>()
const inputRef = ref<HTMLTextAreaElement>()
const currentInput = ref('')
const isLoading = ref(false)nse");
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
  <div class="plan-page">
    <Sidebar />
    <div class="plan">
      <!-- Left Panel - Chat -->
      <div class="left-panel">
        <div class="chat-header">
          <button class="back-button" @click="goBack">
            <Icon icon="carbon:arrow-left" />
          </button>
          <h2>Task Planning</h2>
          <!-- <button class="new-chat-button" @click="newChat">
            <Icon icon="carbon:add" />
            New
          </button> -->
        </div>

        <div class="chat-container">
          <div class="messages" ref="messagesRef">
            <div
              v-for="message in messages"
              :key="message.id"
              class="message"
              :class="{ user: message.type === 'user', assistant: message.type === 'assistant' }"
            >
              <div class="message-content">
                <div v-if="message.type === 'user'" class="user-message">
                  {{ message.content }}
                </div>
                <div v-else class="assistant-message">
                  <div class="thinking" v-if="message.thinking">
                    <Icon icon="carbon:thinking" class="thinking-icon" />
                    <span>{{ message.thinking }}</span>
                  </div>
                  <div class="response" v-if="message.content">
                    {{ message.content }}
                  </div>
                  <div class="progress" v-if="message.progress">
                    <div class="progress-bar">
                      <div class="progress-fill" :style="{ width: message.progress + '%' }"></div>
                    </div>
                    <span class="progress-text">{{ message.progressText }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="isLoading" class="message assistant">
              <div class="message-content">
                <div class="assistant-message">
                  <div class="thinking">
                    <div class="thinking-dots">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                    <span>Analyzing your request...</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="input-area">
            <div class="input-container">
              <textarea
                v-model="currentInput"
                ref="inputRef"
                class="chat-input"
                placeholder="Ask a follow-up question or provide more details..."
                @keydown="handleKeydown"
                @input="adjustInputHeight"
              ></textarea>
              <button
                class="send-button"
                :disabled="!currentInput.trim() || isLoading"
                @click="sendMessage"
              >
                <Icon icon="carbon:send-alt" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Panel - Preview -->
      <RightPanel />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import Sidebar from '@/components/sidebar/index.vue'
import RightPanel from '@/components/right-panel/index.vue'

const route = useRoute()
const router = useRouter()

const messagesRef = ref<HTMLElement>()
const inputRef = ref<HTMLTextAreaElement>()
const currentInput = ref('')
const isLoading = ref(false)
const activeTab = ref('chat')

interface Message {
  id: string
  type: 'user' | 'assistant'
  content: string
  thinking?: string
  progress?: number
  progressText?: string
  timestamp: Date
}

const messages = ref<Message[]>([])

const prompt = ref<string>('')

onMounted(() => {
  // Initialize with prompt from conversation page
  prompt.value = (route.query.prompt as string) || ''
  if (prompt.value) {
    addMessage('user', prompt.value)
    simulateAssistantResponse()
  }
})

const addMessage = (type: 'user' | 'assistant', content: string, options?: Partial<Message>) => {
  const message: Message = {
    id: Date.now().toString(),
    type,
    content,
    timestamp: new Date(),
    ...options,
  }
  messages.value.push(message)
  scrollToBottom()
}

const simulateAssistantResponse = async () => {
  isLoading.value = true

  // Simulate thinking
  await new Promise(resolve => setTimeout(resolve, 1500))

  addMessage('assistant', '', {
    thinking: '正在分析您的需求并规划实施方案...',
  })

  await new Promise(resolve => setTimeout(resolve, 2000))

  // Update with progress
  const lastMessage = messages.value[messages.value.length - 1]
  lastMessage.thinking = undefined
  lastMessage.content =
    '我将帮助您构建一个具有用户管理功能的 Spring Boot REST API。让我将其分解为以下步骤：'
  lastMessage.progress = 25
  lastMessage.progressText = '创建项目结构...'

  await new Promise(resolve => setTimeout(resolve, 1500))

  lastMessage.progress = 75
  lastMessage.progressText = '生成 CRUD 操作...'

  await new Promise(resolve => setTimeout(resolve, 1500))

  lastMessage.progress = 100
  lastMessage.progressText = '完成!'
  lastMessage.content +=
    '\n\n✅ 已创建具有 CRUD 操作的 UserController\n✅ 已添加正确的 HTTP 状态代码\n✅ 已实现错误处理\n✅ 遵循 REST 最佳实践\n\n您可以在"代码"选项卡中查看生成的代码。您是否希望我向此 API 添加身份验证或验证？'

  isLoading.value = false
  scrollToBottom()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const adjustInputHeight = () => {
  nextTick(() => {
    if (inputRef.value) {
      inputRef.value.style.height = 'auto'
      inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 120) + 'px'
    }
  })
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

const sendMessage = () => {
  if (!currentInput.value.trim() || isLoading.value) return

  addMessage('user', currentInput.value)
  currentInput.value = ''
  adjustInputHeight()

  // Simulate assistant response
  setTimeout(() => {
    simulateAssistantResponse()
  }, 500)
}

const goBack = () => {
  router.push('/home')
}

const newChat = () => {
  router.push('/home')
}
</script>

<style lang="less" scoped>
.plan-page {
  width: 100%;
  display: flex;
  position: relative;
}

.plan {
  height: 100vh;
  width: 100%;
  background: #0a0a0a;
  display: flex;
}

.left-panel {
  position: relative;
  width: 50%;
  border-right: 1px solid #1a1a1a;
  display: flex;
  flex-direction: column;
  max-height: 100vh;
}

.chat-header {
  padding: 20px 24px;
  border-bottom: 1px solid #1a1a1a;
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.02);

  h2 {
    flex: 1;
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #ffffff;
  }
}

.back-button {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
  }
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.messages {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: scroll;
}

.message {
  display: flex;

  &.user {
    justify-content: flex-end;

    .message-content {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #ffffff;
      max-width: 80%;
    }
  }

  &.assistant {
    justify-content: flex-start;

    .message-content {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      color: #ffffff;
      max-width: 85%;
    }
  }
}

.message-content {
  padding: 16px 20px;
  border-radius: 16px;
  backdrop-filter: blur(20px);
}

.user-message {
  line-height: 1.5;
}

.assistant-message {
  .thinking {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #888888;
    font-size: 14px;
    margin-bottom: 12px;

    .thinking-icon {
      animation: pulse 2s infinite;
    }
  }

  .response {
    line-height: 1.5;
    white-space: pre-line;
  }

  .progress {
    margin-top: 12px;

    .progress-bar {
      width: 100%;
      height: 4px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 2px;
      overflow: hidden;
      margin-bottom: 8px;

      .progress-fill {
        height: 100%;
        background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
        transition: width 0.3s ease;
      }
    }

    .progress-text {
      font-size: 12px;
      color: #888888;
    }
  }
}

.thinking-dots {
  display: flex;
  gap: 4px;

  span {
    width: 4px;
    height: 4px;
    background: #667eea;
    border-radius: 50%;
    animation: thinking 1.4s infinite ease-in-out;

    &:nth-child(1) {
      animation-delay: -0.32s;
    }
    &:nth-child(2) {
      animation-delay: -0.16s;
    }
    &:nth-child(3) {
      animation-delay: 0s;
    }
  }
}

@keyframes thinking {
  0%,
  80%,
  100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.input-area {
  min-height: 112px;
  padding: 20px 24px;
  border-top: 1px solid #1a1a1a;
  background: rgba(255, 255, 255, 0.02);
}

.input-container {
  position: relative;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 12px 50px 12px 16px;

  &:focus-within {
    border-color: #667eea;
  }
}

.chat-input {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  color: #ffffff;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  min-height: 20px;
  max-height: 120px;

  &::placeholder {
    color: #666666;
  }
}

.send-button {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}
</style>
