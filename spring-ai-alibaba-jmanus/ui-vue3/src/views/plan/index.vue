<!-- /*
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
      <div class="right-panel">
        <div class="preview-header">
          <div class="preview-tabs">
            <button
              v-for="tab in previewTabs"
              :key="tab.id"
              class="tab-button"
              :class="{ active: activeTab === tab.id }"
              @click="activeTab = tab.id"
            >
              <Icon :icon="tab.icon" />
              {{ tab.name }}
            </button>
          </div>
          <div class="preview-actions">
            <button class="action-button" @click="copyCode" v-if="activeTab === 'code'">
              <Icon icon="carbon:copy" />
            </button>
            <button class="action-button" @click="downloadCode" v-if="activeTab === 'code'">
              <Icon icon="carbon:download" />
            </button>
          </div>
        </div>

        <div class="preview-content">
          <!-- Code Preview -->
          <div v-if="activeTab === 'code'" class="code-preview">
            <MonacoEditor
              v-model="codeContent"
              :language="codeLanguage"
              :theme="'vs-dark'"
              :height="'100%'"
              :readonly="true"
              :editor-options="{
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                wordWrap: 'on',
              }"
            />
          </div>

          <!-- Chat Preview -->
          <div v-else-if="activeTab === 'chat'" class="chat-preview">
            <div class="chat-bubbles">
              <div
                v-for="bubble in chatBubbles"
                :key="bubble.id"
                class="chat-bubble"
                :class="bubble.type"
              >
                <div class="bubble-header">
                  <Icon :icon="bubble.icon" />
                  <span>{{ bubble.title }}</span>
                  <span class="timestamp">{{ bubble.timestamp }}</span>
                </div>
                <div class="bubble-content">
                  {{ bubble.content }}
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-else class="empty-preview">
            <Icon icon="carbon:document" class="empty-icon" />
            <h3>No preview available</h3>
            <p>Start a conversation to see the generated content here.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import MonacoEditor from '@/components/editor/index.vue'
import Sidebar from '@/components/sidebar/index.vue'

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

const previewTabs = [
  { id: 'chat', name: 'Chat', icon: 'carbon:chat' },
  { id: 'code', name: 'Code', icon: 'carbon:code' },
]

const codeContent = ref(`// Generated Spring Boot REST API
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        user.setId(id);
        User updatedUser = userService.save(user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}`)

const codeLanguage = ref('java')

const chatBubbles = ref([
  {
    id: '1',
    type: 'thinking',
    icon: 'carbon:thinking',
    title: '分析需求',
    content:
      '将您的请求分解为可操作的步骤：1) 创建用户实体，2) 实现用户服务，3) 构建 REST 端点，4) 添加验证和错误处理。',
    timestamp: '2 分钟前',
  },
  {
    id: '2',
    type: 'progress',
    icon: 'carbon:in-progress',
    title: '生成代码',
    content:
      '创建具有用户管理 CRUD 操作的 Spring Boot REST API。包括正确的 HTTP 状态代码和错误处理。',
    timestamp: '1 分钟前',
  },
  {
    id: '3',
    type: 'success',
    icon: 'carbon:checkmark',
    title: '代码已生成',
    content:
      '成功生成具有所有 CRUD 操作的 UserController。代码包含正确的 REST 约定、错误处理，并遵循 Spring Boot 最佳实践。',
    timestamp: '刚刚',
  },
])

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

const copyCode = () => {
  navigator.clipboard.writeText(codeContent.value)
}

const downloadCode = () => {
  const blob = new Blob([codeContent.value], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'UserController.java'
  a.click()
  URL.revokeObjectURL(url)
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

.right-panel {
  width: 50%;

  display: flex;
  flex-direction: column;
}

.preview-header {
  padding: 20px 24px;
  border-bottom: 1px solid #1a1a1a;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.02);
}

.preview-tabs {
  display: flex;
  gap: 8px;
}

.tab-button {
  padding: 8px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #888888;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #ffffff;
  }

  &.active {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
    border-color: #667eea;
    color: #667eea;
  }
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.action-button {
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #888888;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #ffffff;
  }
}

.preview-content {
  flex: 1;
  overflow: hidden;
}

.code-preview {
  height: 100%;
}

.chat-preview {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

.chat-bubbles {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-bubble {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;

  &.thinking {
    border-left: 4px solid #f39c12;
  }

  &.progress {
    border-left: 4px solid #3498db;
  }

  &.success {
    border-left: 4px solid #27ae60;
  }
}

.bubble-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;

  .timestamp {
    margin-left: auto;
    font-size: 12px;
    color: #666666;
    font-weight: 400;
  }
}

.bubble-content {
  color: #cccccc;
  line-height: 1.5;
  font-size: 14px;
}

.empty-preview {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666666;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  h3 {
    margin: 0 0 8px 0;
    font-size: 18px;
    color: #888888;
  }

  p {
    margin: 0;
    font-size: 14px;
    text-align: center;
  }
}
</style>
