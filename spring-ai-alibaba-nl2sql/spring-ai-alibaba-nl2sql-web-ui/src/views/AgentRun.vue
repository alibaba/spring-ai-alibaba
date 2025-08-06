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
  <div class="agent-run-page">


    <!-- 主要聊天区域 -->
    <div class="chat-container">
      <!-- 左侧智能体信息 -->
      <div class="chat-sidebar">
        <!-- 智能体信息头部 -->
        <div class="agent-header">
          <div class="agent-avatar-small">
            <div class="avatar-icon-small" :style="{ backgroundColor: getRandomColor(agent.id) }">
              <i :class="getRandomIcon(agent.id)"></i>
            </div>
          </div>
          <div class="agent-info-text">
            <div class="agent-name">{{ agent.name }}</div>
          </div>
          <div class="agent-actions">
            <button class="action-btn" @click="clearHistory" title="清空历史">
              <i class="bi bi-trash"></i>
            </button>
          </div>
        </div>
        
        <!-- 开启新对话按钮 -->
        <div class="new-chat-section">
          <button class="new-chat-btn" @click="startNewChat">
            <i class="bi bi-plus"></i>
            开启新对话
          </button>
        </div>
        
        <!-- 历史对话列表 -->
        <div class="chat-history">
          <div class="history-header">
            <span class="history-title">历史对话</span>
          </div>
          <div class="history-list">
            <div 
              v-for="session in chatSessions" 
              :key="session.id"
              class="history-item"
              :class="{ active: currentSessionId === session.id }"
              @click="switchSession(session.id)"
            >
              <div class="history-title">{{ session.title || '新的对话' }}</div>
              <div class="history-preview">{{ getSessionPreview(session) }}</div>
            </div>
            <div v-if="chatSessions.length === 0" class="empty-history">
              <div class="history-item">
                <div class="history-title">新的对话</div>
                <div class="history-preview">帮我查询最近一个月的6月份...</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="chat-main">
        <!-- 消息列表 -->
        <div class="messages-container" ref="messagesContainer">
          <div v-if="currentMessages.length === 0" class="welcome-message">
            <div class="welcome-content">
              <div class="welcome-icon">
                <i class="bi bi-robot"></i>
              </div>
              <h3>欢迎使用 {{ agent.name }}</h3>
              <p>{{ agent.description }}</p>
              <div class="quick-actions">
                <button 
                  v-for="action in quickActions" 
                  :key="action.id"
                  class="quick-action-btn"
                  @click="sendQuickMessage(action.message)"
                >
                  <i :class="action.icon"></i>
                  {{ action.label }}
                </button>
              </div>
            </div>
          </div>

          <div v-else class="messages-list">
            <div 
              v-for="message in currentMessages" 
              :key="message.id"
              class="message-item"
              :class="{ 'user-message': message.role === 'user', 'assistant-message': message.role === 'assistant' }"
            >
              <div class="message-avatar">
                <div v-if="message.role === 'user'" class="user-avatar">
                  <i class="bi bi-person-fill"></i>
                </div>
                <div v-else class="assistant-avatar" :style="{ backgroundColor: getRandomColor(agent.id) }">
                  <i :class="getRandomIcon(agent.id)"></i>
                </div>
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-role">{{ message.role === 'user' ? '用户' : agent.name }}</span>
                  <span class="message-time">{{ formatTime(message.timestamp) }}</span>
                </div>
                <div class="message-body">
                  <div class="text-message">
                    <div v-html="formatMessage(message.content)"></div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 加载中消息 -->
            <div v-if="isLoading" class="message-item assistant-message loading">
              <div class="message-avatar">
                <div class="assistant-avatar" :style="{ backgroundColor: getRandomColor(agent.id) }">
                  <i :class="getRandomIcon(agent.id)"></i>
                </div>
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-role">{{ agent.name }}</span>
                </div>
                <div class="message-body">
                  <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-container">
          <div class="input-wrapper">
            <div class="input-field">
              <textarea
                v-model="inputMessage"
                ref="messageInput"
                placeholder="输入您的问题..."
                @keydown="handleKeyDown"
                @input="adjustTextareaHeight"
                rows="1"
              ></textarea>
            </div>
            <div class="send-actions">
              <button 
                class="btn btn-primary send-btn"
                :disabled="!inputMessage.trim() || isLoading"
                @click="sendMessage"
              >
                <i class="bi bi-send"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

export default {
  name: 'AgentRun',
  setup() {
    const route = useRoute()
    const router = useRouter()
    
    // 响应式数据
    const agent = ref({
      id: route.params.id,
      name: 'NL2SQL 智能助手',
      description: '自然语言转SQL查询助手，帮助您快速生成和执行数据库查询',
      type: 'nl2sql'
    })
    
    const chatSessions = ref([])
    const currentSessionId = ref(null)
    const currentMessages = ref([])
    const inputMessage = ref('')
    const isLoading = ref(false)
    
    const messagesContainer = ref(null)
    const messageInput = ref(null)
    
    // 快捷操作
    const quickActions = ref([
      {
        id: 1,
        label: '查询销售数据',
        message: '帮我查询最近一个月的销售数据',
        icon: 'bi bi-graph-up'
      },
      {
        id: 2,
        label: '用户统计',
        message: '统计用户注册情况',
        icon: 'bi bi-people'
      },
      {
        id: 3,
        label: '产品分析',
        message: '分析产品销售情况',
        icon: 'bi bi-bar-chart'
      },
      {
        id: 4,
        label: '数据导出',
        message: '导出数据报表',
        icon: 'bi bi-download'
      }
    ])
    
    // 方法
    const goBack = () => {
      router.push(`/agent/${agent.value.id}`)
    }
    
    const startNewChat = async () => {
      try {
        const newSession = {
          id: Date.now(),
          title: '新对话',
          agentId: agent.value.id,
          createdAt: new Date()
        }
        
        chatSessions.value.unshift(newSession)
        currentSessionId.value = newSession.id
        currentMessages.value = []
      } catch (error) {
        console.error('创建新对话失败:', error)
        alert('创建新对话失败')
      }
    }
    
    const clearHistory = async () => {
      try {
        if (!confirm('确定要清空所有历史对话吗？')) {
          return
        }
        
        chatSessions.value = []
        currentSessionId.value = null
        currentMessages.value = []
        console.log('历史记录已清空')
      } catch (error) {
        console.error('清空历史失败:', error)
        alert('清空历史失败')
      }
    }
    
    const sendMessage = async () => {
      if (!inputMessage.value.trim() || isLoading.value) return
      
      const message = inputMessage.value.trim()
      inputMessage.value = ''
      
      // 如果没有当前会话，创建新会话
      if (!currentSessionId.value) {
        await startNewChat()
      }
      
      // 添加用户消息
      const userMessage = {
        id: Date.now(),
        role: 'user',
        type: 'text',
        content: message,
        timestamp: new Date()
      }
      
      currentMessages.value.push(userMessage)
      await nextTick()
      scrollToBottom()
      
      // 模拟AI回复
      isLoading.value = true
      
      setTimeout(() => {
        const assistantMessage = {
          id: Date.now() + 1,
          role: 'assistant',
          type: 'text',
          content: `我收到了您的问题："${message}"。这是一个模拟回复，实际应用中会调用后端API进行处理。`,
          timestamp: new Date()
        }
        
        currentMessages.value.push(assistantMessage)
        isLoading.value = false
        
        nextTick(() => {
          scrollToBottom()
        })
      }, 1000)
    }
    
    const sendQuickMessage = (message) => {
      inputMessage.value = message
      sendMessage()
    }
    
    const handleKeyDown = (event) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        sendMessage()
      }
    }
    
    const adjustTextareaHeight = () => {
      const textarea = messageInput.value
      if (textarea) {
        textarea.style.height = 'auto'
        textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px'
      }
    }
    
    const scrollToBottom = () => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    }
    
    const formatMessage = (content) => {
      // 简单的markdown格式化
      return content
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/`(.*?)`/g, '<code>$1</code>')
        .replace(/\n/g, '<br>')
    }
    
    const formatTime = (timestamp) => {
      const date = new Date(timestamp)
      const now = new Date()
      const diff = now - date
      
      if (diff < 60000) { // 1分钟内
        return '刚刚'
      } else if (diff < 3600000) { // 1小时内
        return `${Math.floor(diff / 60000)}分钟前`
      } else if (diff < 86400000) { // 1天内
        return `${Math.floor(diff / 3600000)}小时前`
      } else {
        return date.toLocaleDateString()
      }
    }
    
    const getRandomColor = (id) => {
      const colors = [
        '#1890ff', '#52c41a', '#faad14', '#f5222d',
        '#722ed1', '#13c2c2', '#eb2f96', '#fa541c'
      ]
      return colors[id % colors.length]
    }
    
    const getRandomIcon = (id) => {
      const icons = [
        'bi bi-robot', 'bi bi-cpu', 'bi bi-gear',
        'bi bi-lightning', 'bi bi-star', 'bi bi-heart',
        'bi bi-diamond', 'bi bi-gem'
      ]
      return icons[id % icons.length]
    }
    
    const switchSession = async (sessionId) => {
      if (sessionId === currentSessionId.value) return
      
      currentSessionId.value = sessionId
      // 这里可以加载对应会话的消息
      currentMessages.value = []
    }
    
    const getSessionPreview = (session) => {
      // 返回会话的预览文本
      return '帮我查询最近一个月的6月份...'
    }
    
    // 生命周期
    onMounted(async () => {
      // 初始化时创建一个默认会话
      await startNewChat()
    })
    
    return {
      // 数据
      agent,
      chatSessions,
      currentSessionId,
      currentMessages,
      inputMessage,
      isLoading,
      quickActions,
      messagesContainer,
      messageInput,
      
      // 方法
      goBack,
      startNewChat,
      clearHistory,
      sendMessage,
      sendQuickMessage,
      handleKeyDown,
      adjustTextareaHeight,
      formatMessage,
      formatTime,
      getRandomColor,
      getRandomIcon
    }
  }
}
</script>

<style scoped>
.agent-run-page {
  min-height: 100vh;
  background: #f0f2f5;
  font-family: var(--font-family);
  width: 100%;
  display: flex;
  flex-direction: column;
}

/* 头部样式 */
.run-header {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-secondary);
  box-shadow: var(--shadow-xs);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.header-content {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 var(--space-xl);
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.page-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  background: var(--bg-secondary);
  border-radius: var(--radius-base);
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--text-secondary);
}

.back-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.agent-info {
  display: flex;
  align-items: center;
  gap: var(--space-base);
}

.agent-avatar .avatar-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
}

.agent-meta h1 {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.agent-meta p {
  margin: var(--space-xs) 0 0 0;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

/* 聊天容器样式 */
.chat-container {
  flex: 1;
  display: flex;
  width: 100%;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

/* 左侧边栏样式 */
.chat-sidebar {
  width: 280px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-secondary);
  display: flex;
  flex-direction: column;
  padding: var(--space-xl);
}

.agent-sidebar-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: var(--space-2xl);
  width: 100%;
}

.agent-sidebar-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-lg);
  line-height: 1.4;
  word-break: break-word;
}

.agent-sidebar-avatar {
  align-self: center;
  margin-bottom: var(--space-md);
}

.sidebar-avatar-icon {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 32px;
  box-shadow: var(--shadow-md);
}

.agent-sidebar-description {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: var(--space-xl);
  text-align: left;
}

.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  width: 100%;
  margin-top: auto;
}

.sidebar-btn {
  width: 100%;
  height: 48px;
  border: 1px solid var(--border-primary);
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: var(--space-sm);
  padding: 0 var(--space-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  box-shadow: var(--shadow-xs);
}

.sidebar-btn:hover {
  background: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.sidebar-btn i {
  font-size: 16px;
}

/* 新的左侧边栏样式 - 根据图片设计 */
.agent-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--border-tertiary);
}

.agent-avatar-small {
  flex-shrink: 0;
}

.avatar-icon-small {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  box-shadow: var(--shadow-xs);
}

.agent-info-text {
  flex: 1;
  min-width: 0;
}

.agent-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0;
  line-height: 1.4;
  word-break: break-word;
}

.agent-actions {
  flex-shrink: 0;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.action-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.new-chat-section {
  margin-bottom: var(--space-xl);
}

.new-chat-btn {
  width: 100%;
  height: 44px;
  border: 1px solid var(--primary-color);
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  box-shadow: var(--shadow-xs);
}

.new-chat-btn:hover {
  background: var(--primary-color);
  color: white;
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.new-chat-btn i {
  font-size: 16px;
}

.chat-history {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-header {
  margin-bottom: var(--space-md);
}

.history-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
}

.history-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.history-item {
  padding: var(--space-md);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  background: var(--bg-primary);
  border: 1px solid transparent;
}

.history-item:hover {
  background: var(--bg-tertiary);
  border-color: var(--border-primary);
}

.history-item.active {
  background: var(--primary-light);
  border-color: var(--primary-color);
}

.history-item .history-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
  line-height: 1.4;
}

.history-preview {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-history .history-item {
  cursor: default;
  opacity: 0.6;
}

.empty-history .history-item:hover {
  background: var(--bg-primary);
  border-color: transparent;
}

/* 历史列表滚动条 */
.history-list::-webkit-scrollbar {
  width: 4px;
}

.history-list::-webkit-scrollbar-track {
  background: transparent;
}

.history-list::-webkit-scrollbar-thumb {
  background: var(--border-primary);
  border-radius: 2px;
}

.history-list::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary);
}

/* 右侧聊天区域样式 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xl);
}

/* 欢迎消息样式 */
.welcome-message {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
}

.welcome-content {
  text-align: center;
  max-width: 500px;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-xl);
  background: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-xl);
  color: white;
  font-size: var(--font-size-3xl);
  box-shadow: var(--shadow-md);
}

.welcome-content h3 {
  margin: 0 0 var(--space-base) 0;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.welcome-content p {
  margin: 0 0 var(--space-2xl) 0;
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  line-height: 1.6;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-base);
}

.quick-action-btn {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  background: var(--bg-primary);
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  box-shadow: var(--shadow-xs);
  color: var(--text-primary);
}

.quick-action-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

/* 消息列表样式 */
.messages-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.message-item {
  display: flex;
  gap: var(--space-base);
  align-items: flex-start;
  margin-bottom: var(--space-lg);
}

.message-avatar {
  flex-shrink: 0;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.assistant-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-xs);
}

.message-role {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--text-tertiary);
}

.message-time {
  font-size: var(--font-size-xs);
  color: var(--text-quaternary);
}

.message-body {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: var(--space-md) var(--space-lg);
  max-width: 70%;
  box-shadow: var(--shadow-xs);
}

.user-message {
  flex-direction: row-reverse;
}

.user-message .message-body {
  background: var(--primary-color);
  color: white;
  max-width: 70%;
}

.user-message .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.user-message .message-header {
  align-self: flex-end;
}

.assistant-message .message-body {
  background: var(--bg-secondary);
  border: 1px solid var(--border-tertiary);
}

.text-message {
  font-size: var(--font-size-sm);
  line-height: 1.6;
  word-wrap: break-word;
}

/* 加载动画样式 */
.loading .message-body {
  background: var(--bg-secondary);
  padding: var(--space-md);
}

.typing-indicator {
  display: flex;
  gap: 4px;
  align-items: center;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-tertiary);
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 输入区域样式 */
.input-container {
  border-top: 1px solid var(--border-secondary);
  padding: var(--space-lg) var(--space-xl);
  background: var(--bg-primary);
  display: flex;
  justify-content: center;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-xl);
  padding: var(--space-xs) var(--space-lg);
  transition: all var(--transition-base);
  background: var(--bg-primary);
  box-shadow: var(--shadow-xs);
  width: 100%;
  max-width: 800px;
  min-height: 32px;
}

.input-wrapper:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px var(--primary-light);
}

.input-field {
  flex: 1;
}

.input-field textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: var(--font-size-sm);
  line-height: 1.4;
  font-family: inherit;
  background: transparent;
  min-height: 24px;
  display: flex;
  align-items: center;
  padding: 0;
  margin: 0;
}

.input-field textarea::placeholder {
  color: var(--text-quaternary);
}

.send-actions {
  display: flex;
  align-items: flex-end;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 按钮样式 */
.btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  border: 1px solid transparent;
  border-radius: var(--radius-base);
  background: var(--bg-primary);
  cursor: pointer;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: all var(--transition-base);
  text-decoration: none;
  box-shadow: var(--shadow-xs);
}

.btn:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.btn-primary {
  background: var(--primary-color);
  color: var(--bg-primary);
  border-color: var(--primary-color);
}

.btn-primary:hover {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-container {
    flex-direction: column;
  }
  
  .chat-sidebar {
    width: 100%;
    height: 80px;
    flex-direction: row;
    justify-content: space-between;
    padding: var(--space-md) var(--space-lg);
    border-right: none;
    border-bottom: 1px solid var(--border-secondary);
  }
  
  .agent-sidebar-info {
    flex-direction: row;
    margin-bottom: 0;
    gap: var(--space-md);
  }
  
  .agent-sidebar-avatar {
    margin-bottom: 0;
  }
  
  .sidebar-avatar-icon {
    width: 48px;
    height: 48px;
    font-size: 20px;
  }
  
  .sidebar-actions {
    flex-direction: row;
    width: auto;
  }
  
  .sidebar-btn {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }
  
  .quick-actions {
    grid-template-columns: 1fr;
  }
  
  .message-body {
    max-width: 85%;
  }
  
  .user-message .message-body {
    max-width: 85%;
  }
  
  .header-content {
    padding: 0 var(--space-md);
  }
  
  .agent-meta h1 {
    font-size: var(--font-size-lg);
  }
  
  .messages-container {
    padding: var(--space-md);
  }
  
  .input-container {
    padding: var(--space-md);
  }
}

/* 滚动条样式 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: var(--bg-secondary);
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb {
  background: var(--border-primary);
  border-radius: 3px;
  transition: background var(--transition-base);
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary);
}
</style>