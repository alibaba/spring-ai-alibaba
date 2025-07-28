<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
-->
<template>
  <div class="agent-workspace">
    <!-- 头部导航 -->
    <div class="top-nav">
      <div class="nav-items">
        <span class="nav-item logo-item">
          <i class="bi bi-robot"></i>
          智能体工作台
        </span>
        <span class="nav-item clickable active">对话交互</span>
      </div>
      <div class="nav-right">
        <button class="btn btn-outline" @click="goToAgentList">
          <i class="bi bi-gear"></i>
          管理智能体
        </button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <div class="workspace-layout">
        <!-- 左侧智能体列表 -->
        <div class="agent-sidebar">
          <div class="sidebar-header">
            <h3>已发布智能体</h3>
            <span class="agent-count">{{ publishedAgents.length }} 个</span>
          </div>
          <div class="agent-list">
            <div 
              v-for="agent in publishedAgents" 
              :key="agent.id"
              class="agent-item"
              :class="{ active: selectedAgent?.id === agent.id }"
              @click="selectAgent(agent)"
            >
              <div class="agent-avatar">
                <div class="avatar-icon" :style="{ backgroundColor: getRandomColor(agent.id) }">
                  <i :class="getRandomIcon(agent.id)"></i>
                </div>
              </div>
              <div class="agent-info">
                <div class="agent-name">{{ agent.name }}</div>
                <div class="agent-description">{{ agent.description || '暂无描述' }}</div>
              </div>
            </div>
            <div v-if="publishedAgents.length === 0" class="empty-agents">
              <i class="bi bi-robot"></i>
              <p>暂无已发布的智能体</p>
            </div>
          </div>
        </div>
        
        <!-- 右侧对话区域 -->
        <div class="chat-area">
          <div v-if="!selectedAgent" class="no-agent-selected">
            <div class="empty-icon">
              <i class="bi bi-chat-dots"></i>
            </div>
            <div class="empty-text">
              <h3>选择智能体开始对话</h3>
              <p>从左侧选择一个已发布的智能体，开始您的智能对话体验</p>
            </div>
          </div>
          
          <div v-else class="chat-container">
            <!-- 智能体信息头部 -->
            <div class="chat-header">
              <div class="agent-info">
                <div class="agent-avatar">
                  <div class="avatar-icon" :style="{ backgroundColor: getRandomColor(selectedAgent.id) }">
                    <i :class="getRandomIcon(selectedAgent.id)"></i>
                  </div>
                </div>
                <div class="agent-meta">
                  <h2>{{ selectedAgent.name }}</h2>
                  <p>{{ selectedAgent.description || '智能助手，随时为您服务' }}</p>
                </div>
              </div>
              <div class="chat-actions">
                <button class="btn btn-outline btn-sm" @click="clearChat">
                  <i class="bi bi-trash"></i>
                  清空对话
                </button>
              </div>
            </div>

            <!-- 对话内容区域 -->
            <div class="chat-content" ref="chatContainer">
              <div v-if="chatMessages.length === 0" class="welcome-message">
                <div class="welcome-avatar">
                  <div class="avatar-icon" :style="{ backgroundColor: getRandomColor(selectedAgent.id) }">
                    <i :class="getRandomIcon(selectedAgent.id)"></i>
                  </div>
                </div>
                <div class="welcome-text">
                  <h4>Hi~ 我是{{ selectedAgent.name }}</h4>
                  <p>{{ selectedAgent.description || '我是您的智能助手，有什么可以帮助您的吗？' }}</p>
                  <div class="example-queries">
                    <div 
                      class="example-query" 
                      v-for="example in exampleQueries" 
                      :key="example"
                      @click="sendMessage(example)"
                    >
                      {{ example }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- 聊天消息 -->
              <div v-for="(message, index) in chatMessages" :key="index" class="message-group">
                <div v-if="message.type === 'user'" class="message user-message">
                  <div class="message-content">{{ message.content }}</div>
                  <div class="message-avatar">
                    <i class="bi bi-person-circle"></i>
                  </div>
                </div>
                
                <div v-else class="message agent-message">
                  <div class="message-avatar">
                    <div class="avatar-icon" :style="{ backgroundColor: getRandomColor(selectedAgent.id) }">
                      <i :class="getRandomIcon(selectedAgent.id)"></i>
                    </div>
                  </div>
                  <div class="message-content" v-html="message.content"></div>
                </div>
              </div>

              <!-- 正在输入指示器 -->
              <div v-if="isTyping" class="message agent-message typing-message">
                <div class="message-avatar">
                  <div class="avatar-icon" :style="{ backgroundColor: getRandomColor(selectedAgent.id) }">
                    <i :class="getRandomIcon(selectedAgent.id)"></i>
                  </div>
                </div>
                <div class="message-content">
                  <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 输入区域 -->
            <div class="chat-input-area">
              <div class="input-container">
                <input 
                  type="text" 
                  v-model="currentMessage" 
                  class="chat-input" 
                  placeholder="输入您的问题..."
                  :disabled="isTyping"
                  @keyup.enter="sendMessage()"
                  ref="chatInput"
                >
                <button 
                  class="send-button" 
                  :disabled="isTyping || !currentMessage.trim()"
                  @click="sendMessage()"
                >
                  <i class="bi bi-send" v-if="!isTyping"></i>
                  <div class="spinner" v-else></div>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { agentApi } from '../utils/api.js'

export default {
  name: 'AgentWorkspace',
  setup() {
    const router = useRouter()
    
    // 响应式数据
    const publishedAgents = ref([])
    const selectedAgent = ref(null)
    const chatMessages = ref([])
    const currentMessage = ref('')
    const isTyping = ref(false)
    const chatContainer = ref(null)
    const chatInput = ref(null)

    // 示例问题
    const exampleQueries = ref([
      '查询销售额最高的5个产品',
      '分析最近一个月的销售趋势',
      '统计各个分类的商品数量',
      '查询用户购买行为分析'
    ])

    // 获取已发布的智能体列表
    const loadPublishedAgents = async () => {
      try {
        const response = await agentApi.getList({ status: 'published' })
        publishedAgents.value = response || []
      } catch (error) {
        console.error('获取智能体列表失败:', error)
        publishedAgents.value = []
      }
    }

    // 选择智能体
    const selectAgent = (agent) => {
      selectedAgent.value = agent
      chatMessages.value = []
      currentMessage.value = ''
      
      nextTick(() => {
        if (chatInput.value) {
          chatInput.value.focus()
        }
      })
    }

    // 发送消息
    const sendMessage = async (message = null) => {
      const messageText = message || currentMessage.value.trim()
      if (!messageText || isTyping.value || !selectedAgent.value) return

      // 添加用户消息
      chatMessages.value.push({
        type: 'user',
        content: messageText,
        timestamp: new Date()
      })

      // 清空输入框
      currentMessage.value = ''
      isTyping.value = true

      // 滚动到底部
      scrollToBottom()

      try {
        // 创建流式响应
        const eventSource = new EventSource(`/nl2sql/stream/search?query=${encodeURIComponent(messageText)}`)
        
        // 创建智能体回复消息
        const agentMessageIndex = chatMessages.value.length
        chatMessages.value.push({
          type: 'agent',
          content: '',
          timestamp: new Date()
        })

        let accumulatedContent = ''

        eventSource.onmessage = (event) => {
          try {
            const chunk = JSON.parse(event.data)
            const { type, data } = chunk

            if (type && data !== undefined && data !== null) {
              // 处理不同类型的数据
              let processedData = processStreamData(type, data)
              if (processedData) {
                accumulatedContent += processedData + '\n\n'
                chatMessages.value[agentMessageIndex].content = formatMessageContent(accumulatedContent)
                scrollToBottom()
              }
            }
          } catch (error) {
            console.error('解析流式数据失败:', error)
          }
        }

        eventSource.addEventListener('complete', () => {
          isTyping.value = false
          eventSource.close()
        })

        eventSource.onerror = (error) => {
          console.error('流式连接错误:', error)
          isTyping.value = false
          eventSource.close()
          
          // 显示错误消息
          if (chatMessages.value[agentMessageIndex]) {
            chatMessages.value[agentMessageIndex].content = '抱歉，处理您的请求时出现了错误，请稍后重试。'
          }
        }

      } catch (error) {
        console.error('发送消息失败:', error)
        isTyping.value = false
        
        // 显示错误消息
        chatMessages.value.push({
          type: 'agent',
          content: '抱歉，处理您的请求时出现了错误，请稍后重试。',
          timestamp: new Date()
        })
      }
    }

    // 处理流式数据
    const processStreamData = (type, data) => {
      // 数据预处理
      let processedData = data
      
      if (typeof data === 'string') {
        try {
          const jsonData = JSON.parse(data)
          if (jsonData && typeof jsonData === 'object' && jsonData.data) {
            processedData = jsonData.data
          }
        } catch (e) {
          // 不是JSON，保持原始数据
        }
      }

      // 根据类型添加标题
      const typeMapping = {
        'rewrite': '💭 需求理解',
        'keyword_extract': '🔍 关键词提取', 
        'schema_recall': '📊 数据召回',
        'sql': '⚡ SQL查询',
        'result': '📋 查询结果',
        'explanation': '💡 结果解释'
      }

      const title = typeMapping[type] || type
      return `**${title}**\n\n${processedData}`
    }

    // 格式化消息内容
    const formatMessageContent = (content) => {
      // 简单的Markdown渲染
      return content
        .replace(/\*\*(.*?)\*\*/g, function(match, p1) { return '<strong>' + p1 + '</strong>' })
        .replace(/\*(.*?)\*/g, function(match, p1) { return '<em>' + p1 + '</em>' })
        .replace(/`([^`]+)`/g, function(match, p1) { return '<code>' + p1 + '</code>' })
        .replace(/```(\w+)?\s*([\s\S]*?)```/g, function(match, p1, p2) { return '<pre><code>' + p2 + '</code></pre>' })
        .replace(/\n/g, '<br>')
    }

    // 清空对话
    const clearChat = () => {
      chatMessages.value = []
    }

    // 滚动到底部
    const scrollToBottom = () => {
      nextTick(() => {
        if (chatContainer.value) {
          chatContainer.value.scrollTop = chatContainer.value.scrollHeight
        }
      })
    }

    // 跳转到智能体管理页面
    const goToAgentList = () => {
      router.push('/agents')
    }

    // 获取随机颜色
    const getRandomColor = (id) => {
      const colors = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96']
      return colors[id % colors.length]
    }

    // 获取随机图标
    const getRandomIcon = (id) => {
      const icons = ['bi-robot', 'bi-cpu', 'bi-gear', 'bi-lightning', 'bi-star', 'bi-heart', 'bi-diamond']
      return icons[id % icons.length]
    }

    // 组件挂载时加载数据
    onMounted(() => {
      loadPublishedAgents()
    })

    return {
      publishedAgents,
      selectedAgent,
      chatMessages,
      currentMessage,
      isTyping,
      chatContainer,
      chatInput,
      exampleQueries,
      selectAgent,
      sendMessage,
      clearChat,
      goToAgentList,
      getRandomColor,
      getRandomIcon
    }
  }
}
</script>

<style scoped>
.agent-workspace {
  min-height: 100vh;
  background-color: #f5f8fa;
}

/* 头部导航样式 */
.top-nav {
  background-color: #ffffff;
  padding: 1rem 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-items {
  display: flex;
  align-items: center;
  gap: 2rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  color: #333;
}

.logo-item {
  font-size: 1.2rem;
  font-weight: 600;
  color: #1890ff;
}

.nav-item.clickable {
  cursor: pointer;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  transition: all 0.2s;
}

.nav-item.clickable:hover {
  background-color: #f0f5ff;
}

.nav-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.btn-outline {
  background-color: transparent;
  border: 1px solid #d9d9d9;
  color: #666;
}

.btn-outline:hover {
  border-color: #1890ff;
  color: #1890ff;
}

/* 主要内容区域 */
.main-content {
  padding: 2rem;
}

.workspace-layout {
  display: flex;
  gap: 2rem;
  height: calc(100vh - 140px);
  max-width: 1400px;
  margin: 0 auto;
}

/* 左侧智能体列表 */
.agent-sidebar {
  width: 320px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 1.5rem;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

.agent-count {
  font-size: 0.9rem;
  color: #666;
  background-color: #f5f5f5;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
}

.agent-list {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.agent-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 0.5rem;
}

.agent-item:hover {
  background-color: #f8f9fa;
}

.agent-item.active {
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
}

.agent-avatar .avatar-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.2rem;
}

.agent-info {
  flex: 1;
  min-width: 0;
}

.agent-name {
  font-weight: 500;
  color: #333;
  margin-bottom: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.agent-description {
  font-size: 0.85rem;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-agents {
  text-align: center;
  padding: 3rem 1rem;
  color: #999;
}

.empty-agents i {
  font-size: 3rem;
  margin-bottom: 1rem;
  color: #ccc;
}

/* 右侧对话区域 */
.chat-area {
  flex: 1;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.no-agent-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  text-align: center;
}

.no-agent-selected .empty-icon {
  font-size: 4rem;
  color: #ccc;
  margin-bottom: 2rem;
}

.no-agent-selected h3 {
  margin-bottom: 1rem;
  color: #333;
}

.no-agent-selected p {
  color: #666;
  font-size: 1rem;
}

/* 对话容器 */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 1.5rem;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header .agent-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.chat-header .agent-avatar .avatar-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.4rem;
}

.chat-header .agent-meta h2 {
  margin: 0 0 0.25rem 0;
  font-size: 1.3rem;
  color: #333;
}

.chat-header .agent-meta p {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.btn-sm {
  padding: 0.4rem 0.8rem;
  font-size: 0.85rem;
}

/* 对话内容区域 */
.chat-content {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.welcome-message {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 2rem;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 12px;
  border: 1px solid #bae6fd;
}

.welcome-avatar .avatar-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.4rem;
}

.welcome-text h4 {
  margin: 0 0 0.5rem 0;
  color: #333;
  font-size: 1.2rem;
}

.welcome-text p {
  margin: 0 0 1.5rem 0;
  color: #666;
  line-height: 1.5;
}

.example-queries {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.example-query {
  padding: 0.5rem 1rem;
  background-color: rgba(255, 255, 255, 0.8);
  border: 1px solid #d1ecf1;
  border-radius: 20px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  color: #0c5460;
}

.example-query:hover {
  background-color: white;
  border-color: #1890ff;
  color: #1890ff;
}

/* 消息样式 */
.message {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.user-message {
  flex-direction: row-reverse;
}

.user-message .message-content {
  background-color: #1890ff;
  color: white;
  padding: 1rem 1.25rem;
  border-radius: 18px 18px 4px 18px;
  max-width: 70%;
  word-wrap: break-word;
}

.user-message .message-avatar {
  font-size: 2rem;
  color: #1890ff;
}

.agent-message .message-avatar .avatar-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1rem;
}

.agent-message .message-content {
  background-color: #f8f9fa;
  color: #333;
  padding: 1rem 1.25rem;
  border-radius: 4px 18px 18px 18px;
  max-width: 80%;
  word-wrap: break-word;
  line-height: 1.6;
}

/* 正在输入指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 0.5rem 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ccc;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.8); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* 输入区域 */
.chat-input-area {
  padding: 1.5rem;
  border-top: 1px solid #f0f0f0;
  background-color: #fafafa;
}

.input-container {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.chat-input {
  flex: 1;
  padding: 1rem 1.25rem;
  font-size: 1rem;
  border: 1px solid #d9d9d9;
  border-radius: 24px;
  outline: none;
  transition: all 0.2s;
}

.chat-input:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.chat-input:disabled {
  background-color: #f5f5f5;
  color: #999;
  cursor: not-allowed;
}

.send-button {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #1890ff;
  color: white;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
}

.send-button:hover:not(:disabled) {
  background-color: #40a9ff;
  transform: scale(1.05);
}

.send-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  transform: none;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .workspace-layout {
    flex-direction: column;
    height: auto;
  }
  
  .agent-sidebar {
    width: 100%;
    max-height: 300px;
  }
  
  .chat-area {
    min-height: 500px;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 1rem;
  }
  
  .top-nav {
    padding: 1rem;
  }
  
  .nav-items {
    gap: 1rem;
  }
  
  .example-queries {
    flex-direction: column;
  }
  
  .example-query {
    text-align: center;
  }
}
</style>