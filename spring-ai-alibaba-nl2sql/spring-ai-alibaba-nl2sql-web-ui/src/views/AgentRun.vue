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
              :class="{ active: currentSessionId === session.id, pinned: session.isPinned }"
            >
              <div class="history-content" @click="switchSession(session.id)">
                <div class="history-title-row">
                  <i v-if="session.isPinned" class="bi bi-pin-fill pin-icon"></i>
                  <span class="history-title">{{ session.title || '新的对话' }}</span>
                </div>
                <div class="history-preview">{{ getSessionPreview(session) }}</div>
              </div>
              <div class="history-actions">
                <div class="dropdown" :class="{ active: activeDropdown === session.id }">
                  <button class="dropdown-toggle" @click.stop="toggleDropdown(session.id)">
                    <i class="bi bi-three-dots"></i>
                  </button>
                  <div class="dropdown-menu" v-if="activeDropdown === session.id">
                    <button class="dropdown-item" @click.stop="togglePin(session)">
                      <i :class="session.isPinned ? 'bi bi-pin-fill' : 'bi bi-pin'"></i>
                      {{ session.isPinned ? '取消置顶' : '置顶' }}
                    </button>
                    <button class="dropdown-item" @click.stop="showRenameDialog(session)">
                      <i class="bi bi-pencil"></i>
                      重命名
                    </button>
                    <button class="dropdown-item delete" @click.stop="deleteSession(session)">
                      <i class="bi bi-trash"></i>
                      删除
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="chatSessions.length === 0" class="empty-history">
              <div class="history-item">
                <div class="history-content">
                  <div class="history-title-row">
                    <span class="history-title">新的对话</span>
                  </div>
                  <div class="history-preview">帮我查询最近一个月的6月份...</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 重命名对话框 -->
        <div v-if="showRenameModal" class="modal-overlay" @click="closeRenameDialog">
          <div class="modal-content" @click.stop>
            <div class="modal-header">
              <h3>重命名对话</h3>
              <button class="modal-close" @click="closeRenameDialog">
                <i class="bi bi-x"></i>
              </button>
            </div>
            <div class="modal-body">
              <input 
                v-model="renameTitle" 
                type="text" 
                class="rename-input" 
                placeholder="请输入新的对话标题"
                @keydown.enter="confirmRename"
                ref="renameInput"
              />
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="closeRenameDialog">取消</button>
              <button class="btn btn-primary" @click="confirmRename" :disabled="!renameTitle.trim()">确认</button>
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
              <!-- 用户消息保持原有布局 -->
              <template v-if="message.role === 'user'">
                <div class="message-avatar">
                  <div class="user-avatar">
                    <i class="bi bi-person-fill"></i>
                  </div>
                </div>
                <div class="message-content">
                  <div class="message-header">
                    <span class="message-role">用户</span>
                    <span class="message-time">{{ formatTime(message.timestamp) }}</span>
                  </div>
                  <div class="message-body">
                    <div class="text-message">
                      <div v-html="formatMessage(message.content)"></div>
                    </div>
                  </div>
                </div>
              </template>
              
              <!-- 智能体消息使用垂直布局 -->
              <template v-else>
                <div class="assistant-message-header">
                  <div class="assistant-avatar" :style="{ backgroundColor: getRandomColor(agent.id) }">
                    <i :class="getRandomIcon(agent.id)"></i>
                  </div>
                  <div class="assistant-info">
                    <span class="message-role">{{ agent.name }}</span>
                    <span class="message-time">{{ formatTime(message.timestamp) }}</span>
                  </div>
                </div>
                <div class="assistant-message-body">
                  <div class="text-message">
                    <div v-html="message.type === 'streaming' ? message.content : formatMessage(message.content)"></div>
                  </div>
                </div>
              </template>
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
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { presetQuestionApi } from '../utils/api.js'

export default {
  name: 'AgentRun',
  setup() {
    const route = useRoute()
    const router = useRouter()
    
    // 响应式数据
    const agent = ref({
      id: route.params.id,
      name: '',
      description: '',
      type: 'nl2sql'
    })
    
    const chatSessions = ref([])
    const currentSessionId = ref(null)
    const currentMessages = ref([])
    const inputMessage = ref('')
    const isLoading = ref(false)
    
    const messagesContainer = ref(null)
    const messageInput = ref(null)
    const renameInput = ref(null)
    
    // 预设问题（快捷操作）
    const quickActions = ref([])
    
    // 下拉菜单和重命名对话框状态
    const activeDropdown = ref(null)
    const showRenameModal = ref(false)
    const renameTitle = ref('')
    const currentRenameSession = ref(null)
    
    // API方法
    const loadAgentInfo = async () => {
      try {
        const response = await fetch(`/api/agent/${agent.value.id}`)
        if (response.ok) {
          const data = await response.json()
          agent.value.name = data.name || 'NL2SQL 智能助手'
          agent.value.description = data.description || '自然语言转SQL查询助手，帮助您快速生成和执行数据库查询'
        } else {
          // 使用默认值
          agent.value.name = 'NL2SQL 智能助手'
          agent.value.description = '自然语言转SQL查询助手，帮助您快速生成和执行数据库查询'
        }
      } catch (error) {
        console.error('加载智能体信息失败:', error)
        agent.value.name = 'NL2SQL 智能助手'
        agent.value.description = '自然语言转SQL查询助手，帮助您快速生成和执行数据库查询'
      }
    }

    const loadChatSessions = async () => {
      try {
        const response = await fetch(`/api/agent/${agent.value.id}/sessions`)
        if (response.ok) {
          const data = await response.json()
          chatSessions.value = data || []
        }
      } catch (error) {
        console.error('加载对话历史失败:', error)
        chatSessions.value = []
      }
    }

    const loadMessages = async (sessionId) => {
      try {
        const response = await fetch(`/api/sessions/${sessionId}/messages`)
        if (response.ok) {
          const data = await response.json()
          // 将数据库消息转换为前端格式
          currentMessages.value = data.map(dbMessage => ({
            id: dbMessage.id,
            role: dbMessage.role,
            type: dbMessage.messageType || 'text',
            content: dbMessage.content,
            timestamp: new Date(dbMessage.createTime)
          })) || []
          await nextTick()
          scrollToBottom()
        }
      } catch (error) {
        console.error('加载消息失败:', error)
        currentMessages.value = []
      }
    }

    const loadPresetQuestions = async () => {
      try {
        const questions = await presetQuestionApi.getByAgentId(agent.value.id)
        // 转换为快捷操作格式
        quickActions.value = questions.map((question, index) => ({
          id: question.id || index + 1,
          label: question.question.length > 20 ? question.question.substring(0, 20) + '...' : question.question,
          message: question.question,
          icon: getQuestionIcon(index)
        }))
      } catch (error) {
        console.error('加载预设问题失败:', error)
        // 使用默认的快捷操作
        quickActions.value = [
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
        ]
      }
    }

    // 方法
    const goBack = () => {
      router.push(`/agent/${agent.value.id}`)
    }
    
    const startNewChat = async () => {
      try {
        const response = await fetch(`/api/agent/${agent.value.id}/sessions`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            title: '新对话',
            agentId: agent.value.id
          })
        })
        
        if (response.ok) {
          const newSession = await response.json()
          chatSessions.value.unshift(newSession)
          currentSessionId.value = newSession.id
          currentMessages.value = []
        } else {
          throw new Error('创建会话失败')
        }
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
        
        const response = await fetch(`/api/agent/${agent.value.id}/sessions`, {
          method: 'DELETE'
        })
        
        if (response.ok) {
          chatSessions.value = []
          currentSessionId.value = null
          currentMessages.value = []
          console.log('历史记录已清空')
        } else {
          throw new Error('清空历史失败')
        }
      } catch (error) {
        console.error('清空历史失败:', error)
        alert('清空历史失败')
      }
    }
    
    const sendMessage = async (messageText = null) => {
      const message = messageText || inputMessage.value.trim()
      if (!message || isLoading.value) return
      
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
      
      // 保存用户消息到数据库
      await saveMessage({
        sessionId: currentSessionId.value,
        role: 'user',
        content: message,
        messageType: 'text'
      })
      
      await nextTick()
      scrollToBottom()
      
      // 开始流式处理
      isLoading.value = true
      
      try {
        const eventSource = new EventSource(`/nl2sql/stream/search?query=${encodeURIComponent(message)}&agentId=${agent.value.id}`)
        
        const agentMessageIndex = currentMessages.value.length
        currentMessages.value.push({ 
          id: Date.now() + 1,
          role: 'assistant',
          type: 'streaming',
          content: '<div class="typing-indicator"><span></span><span></span><span></span></div>', 
          timestamp: new Date() 
        })

        const streamState = {
            contentByType: {},
            typeOrder: [],
        }

        const typeMapping = {
          'status': { title: '当前状态', icon: 'bi bi-activity' },
          'rewrite': { title: '需求理解', icon: 'bi bi-pencil-square' },
          'keyword_extract': { title: '关键词提取', icon: 'bi bi-key' },
          'plan_generation': { title: '计划生成', icon: 'bi bi-diagram-3' },
          'schema_recall': { title: 'Schema初步召回', icon: 'bi bi-database-gear' },
          'schema_deep_recall': { title: 'Schema深度召回', icon: 'bi bi-database-fill-gear' },
          'sql': { title: '生成的SQL', icon: 'bi bi-code-square' },
          'execute_sql': { title: '执行SQL', icon: 'bi bi-play-circle' },
          'python_execute': { title: 'Python执行', icon: 'bi bi-play-circle-fill' },
          'python_generate': { title: 'Python代码生成', icon: 'bi bi-code-square-fill' },
          'python_analysis': { title: 'Python分析执行', icon: 'bi bi-code-slash' },
          'validation': { title: '校验', icon: 'bi bi-check-circle' },
          'output_report': { title: '输出报告', icon: 'bi bi-file-earmark-text' },
          'explanation': { title: '解释说明', icon: 'bi bi-info-circle' },
          'result': { title: '查询结果', icon: 'bi bi-table' },
          'error': { title: '解析错误', icon: 'bi bi-exclamation-triangle' }
        }

        const updateDisplay = () => {
            let fullContent = '<div class="agent-responses-container" style="display: flex; flex-direction: column; width: 100%; gap: 0.75rem;">'
            for (const type of streamState.typeOrder) {
                const typeInfo = typeMapping[type] || { title: type, icon: 'bi bi-file-text' }
                const content = streamState.contentByType[type] || ''
                const formattedSubContent = formatContentByType(type, content)
                fullContent += `
<div class="agent-response-block" style="display: block !important; width: 100% !important;">
  <div class="agent-response-title">
    <i class="${typeInfo.icon}"></i> ${typeInfo.title}
  </div>
  <div class="agent-response-content">${formattedSubContent}</div>
</div>
`
            }
            fullContent += '</div>'
            currentMessages.value[agentMessageIndex].content = fullContent
            scrollToBottom()
        }

        eventSource.onmessage = (event) => {
            let chunk
            let actualType
            let actualData
            
            try {
                let parsedData = JSON.parse(event.data)
                
                if (typeof parsedData === 'string') {
                    chunk = JSON.parse(parsedData)
                } else {
                    chunk = parsedData
                }

                actualType = chunk['type']
                actualData = chunk['data']

                if (actualType === 'explanation' && typeof actualData === 'string') {
                    try {
                        const innerChunk = JSON.parse(actualData)
                        if (innerChunk.type && innerChunk.data !== undefined) {
                            actualType = innerChunk.type
                            actualData = innerChunk.data
                        }
                    } catch (e) {
                        // 如果内层解析失败，保持原来的值
                    }
                }

            } catch (e) {
                console.error('JSON解析失败:', e, event.data)
                return
            }

            if (actualType && actualData !== undefined && actualData !== null) {
                let processedData = actualData
                
                if (typeof processedData === 'string') {
                    processedData = processedData.replace(/\\n/g, '\n')
                }
                
                if (actualType === 'sql' && typeof processedData === 'string') {
                    processedData = processedData.replace(/^```\s*sql?\s*/i, '').replace(/```\s*$/, '').trim()
                }
                
                if (!streamState.contentByType.hasOwnProperty(actualType)) {
                    streamState.typeOrder.push(actualType)
                    streamState.contentByType[actualType] = ''
                }
                
                if (processedData) {
                    streamState.contentByType[actualType] += processedData
                }
                
                updateDisplay()
            }
        }

        eventSource.addEventListener('complete', async () => {
          console.log('流式输出完成')
          isLoading.value = false
          eventSource.close()
          
          // 保存AI回复消息到数据库
          const assistantMessage = currentMessages.value[agentMessageIndex]
          if (assistantMessage && assistantMessage.content) {
            await saveMessage({
              sessionId: currentSessionId.value,
              role: 'assistant',
              content: assistantMessage.content,
              messageType: 'streaming'
            })
          }
        })

        eventSource.onerror = (error) => {
          console.error('流式连接错误:', error)
          isLoading.value = false
          
          if (eventSource.readyState === EventSource.CLOSED) {
            console.log('EventSource 连接已正常关闭')
          } else {
            const errorMessage = {
              id: Date.now() + 2,
              role: 'assistant',
              type: 'error',
              content: '抱歉，处理您的请求时出现了错误，请稍后重试。',
              timestamp: new Date()
            }
            currentMessages.value.push(errorMessage)
          }
          
          eventSource.close()
          scrollToBottom()
        }
        
      } catch (error) {
        console.error('发送消息失败:', error)
        isLoading.value = false
        
        const errorMessage = {
          id: Date.now() + 1,
          role: 'assistant',
          type: 'error',
          content: '抱歉，处理您的请求时出现了错误，请稍后重试。',
          timestamp: new Date()
        }
        
        currentMessages.value.push(errorMessage)
        await nextTick()
        scrollToBottom()
      }
    }

    const saveMessage = async (message) => {
      try {
        await fetch(`/api/sessions/${currentSessionId.value}/messages`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(message)
        })
      } catch (error) {
        console.error('保存消息失败:', error)
      }
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
      await loadMessages(sessionId)
    }
    
    const getSessionPreview = (session) => {
      // 如果是当前会话且有消息，显示最后一条用户消息的预览
      if (session.id === currentSessionId.value && currentMessages.value.length > 0) {
        const lastUserMessage = currentMessages.value.slice().reverse().find(msg => msg.role === 'user')
        if (lastUserMessage) {
          return lastUserMessage.content.length > 30 ? 
            lastUserMessage.content.substring(0, 30) + '...' : 
            lastUserMessage.content
        }
      }
      // 否则显示默认文本
      return '点击开始对话...'
    }
    
    const getQuestionIcon = (index) => {
      const icons = [
        'bi bi-graph-up',
        'bi bi-people',
        'bi bi-bar-chart',
        'bi bi-download',
        'bi bi-search',
        'bi bi-table',
        'bi bi-pie-chart',
        'bi bi-clipboard-data'
      ]
      return icons[index % icons.length]
    }

    // 下拉菜单和会话操作方法
    const toggleDropdown = (sessionId) => {
      if (activeDropdown.value === sessionId) {
        activeDropdown.value = null
      } else {
        activeDropdown.value = sessionId
      }
    }

    const togglePin = async (session) => {
      try {
        const response = await fetch(`/api/sessions/${session.id}/pin`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            isPinned: !session.isPinned
          })
        })

        if (response.ok) {
          // 更新本地状态
          session.isPinned = !session.isPinned
          // 重新排序会话列表
          await loadChatSessions()
          activeDropdown.value = null
        } else {
          const errorData = await response.json()
          alert(errorData.message || '操作失败')
        }
      } catch (error) {
        console.error('置顶操作失败:', error)
        alert('置顶操作失败')
      }
    }

    const showRenameDialog = (session) => {
      currentRenameSession.value = session
      renameTitle.value = session.title || '新的对话'
      showRenameModal.value = true
      activeDropdown.value = null
      
      // 等待DOM更新后聚焦输入框
      nextTick(() => {
        if (renameInput.value) {
          renameInput.value.focus()
          renameInput.value.select()
        }
      })
    }

    const closeRenameDialog = () => {
      showRenameModal.value = false
      currentRenameSession.value = null
      renameTitle.value = ''
    }

    const confirmRename = async () => {
      if (!renameTitle.value.trim()) return

      try {
        const response = await fetch(`/api/sessions/${currentRenameSession.value.id}/rename`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            title: renameTitle.value.trim()
          })
        })

        if (response.ok) {
          // 更新本地状态
          currentRenameSession.value.title = renameTitle.value.trim()
          closeRenameDialog()
        } else {
          const errorData = await response.json()
          alert(errorData.message || '重命名失败')
        }
      } catch (error) {
        console.error('重命名失败:', error)
        alert('重命名失败')
      }
    }

    const deleteSession = async (session) => {
      if (!confirm(`确定要删除对话"${session.title || '新的对话'}"吗？`)) {
        return
      }

      try {
        const response = await fetch(`/api/sessions/${session.id}`, {
          method: 'DELETE'
        })

        if (response.ok) {
          // 从本地列表中移除
          const index = chatSessions.value.findIndex(s => s.id === session.id)
          if (index > -1) {
            chatSessions.value.splice(index, 1)
          }

          // 如果删除的是当前会话，切换到其他会话或清空
          if (currentSessionId.value === session.id) {
            if (chatSessions.value.length > 0) {
              currentSessionId.value = chatSessions.value[0].id
              await loadMessages(chatSessions.value[0].id)
            } else {
              currentSessionId.value = null
              currentMessages.value = []
            }
          }

          activeDropdown.value = null
        } else {
          const errorData = await response.json()
          alert(errorData.message || '删除失败')
        }
      } catch (error) {
        console.error('删除会话失败:', error)
        alert('删除会话失败')
      }
    }

    // 点击外部关闭下拉菜单
    const handleClickOutside = (event) => {
      if (!event.target.closest('.dropdown')) {
        activeDropdown.value = null
      }
    }

    const formatContentByType = (type, data) => {
        if (data === null || data === undefined) return '';

        if (type === 'sql') {
            let cleanedData = data.replace(/^```\s*sql?\s*/i, '').replace(/```\s*$/, '').trim();
            cleanedData = cleanedData.replace(/\\n/g, '\n');
            return `<pre style="max-width: 100%; overflow-x: auto; word-wrap: break-word; white-space: pre-wrap;"><code class="language-sql">${cleanedData}</code></pre>`;
        } 
        
        if (type === 'result') {
            return convertJsonToHTMLTable(data);
        }

        let processedData = data;
        
        if (typeof processedData === 'string') {
            processedData = processedData.replace(/\\n/g, '\n');
        }

        // 检查是否是JSON格式的字符串，如果是，进行格式化显示
        if (typeof processedData === 'string' && (processedData.trim().startsWith('{') || processedData.trim().startsWith('['))) {
            try {
                const parsed = JSON.parse(processedData);
                const formattedJson = JSON.stringify(parsed, null, 2);
                return `<pre><code class="language-json">${escapeHtml(formattedJson)}</code></pre>`;
            } catch (e) {
                // 如果不是有效的JSON，继续原来的处理逻辑
            }
        }

        if (isMarkdown(processedData)) {
            return renderMarkdown(processedData);
        } else {
            const sqlCodeBlockRegex = /```\s*sql?\s*([\s\S]*?)```/gi;
            const sqlMatches = processedData.match(sqlCodeBlockRegex);
            
            if (sqlMatches && sqlMatches.length > 0) {
                let htmlContent = processedData;
                
                htmlContent = htmlContent.replace(sqlCodeBlockRegex, (match, sqlContent) => {
                    let cleanedSQL = sqlContent.trim();
                    return `<pre><code class="language-sql">${cleanedSQL}</code></pre>`;
                });
                
                return htmlContent.replace(/\n/g, '<br>');
            } else {
                // 对于长文本，确保正确换行
                let result = processedData.toString()
                    .replace(/\n\s*\n\s*\n+/g, '\n\n')
                    .replace(/\n/g, '<br>');
                
                // 对所有文本都添加强制换行样式，确保不会溢出
                result = `<div style="word-break: break-all; overflow-wrap: break-word; white-space: pre-wrap; max-width: 100%; overflow-x: auto;">${result}</div>`;
                
                return result;
            }
        }
    }

    // 添加HTML转义函数
    const escapeHtml = (text) => {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    const isMarkdown = (text) => {
        if (!text || typeof text !== 'string') return false;
        
        const markdownPatterns = [
            /^#{1,6}\s+.+/m,
            /\*\*[^*]+\*\*/,
            /\*[^*]+\*/,
            /`[^`]+`/,
            /```[\s\S]*?```/,
            /^\s*[-*+]\s+/m,
            /^\s*\d+\.\s+/m,
            /^\s*>\s+/m,
            /\[.+\]\(.+\)/,
            /^\s*\|.+\|/m,
            /^---+$/m
        ];
        
        return markdownPatterns.some(pattern => pattern.test(text));
    }

    const renderMarkdown = (text) => {
        if (!text || typeof text !== 'string') return '';
        
        let html = text;
        
        html = html.replace(/```(\w+)?\s*([\s\S]*?)```/g, (match, lang, code) => {
            const language = lang || 'text';
            let highlightedCode = code.trim();
            return `<pre><code class="language-${language}">${highlightedCode}</code></pre>`;
        });
        
        html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>');
        html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>');
        html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>');
        
        html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
        
        html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
        
        html = html.replace(/^\* (.*$)/gim, '<li>$1</li>');
        html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');
        
        html = html.replace(/^\d+\. (.*$)/gim, '<li>$1</li>');
        
        html = html.replace(/(\|[^|\r\n]*\|[^|\r\n]*\|[^\r\n]*\r?\n\|[-:\s|]*\|[^\r\n]*\r?\n(?:\|[^|\r\n]*\|[^\r\n]*\r?\n?)*)/gm, (match) => {
            return convertMarkdownTableToHTML(match);
        });
        
        html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>');
        
        html = html.replace(/\n\s*\n\s*\n/g, '\n\n')
             .replace(/\n/g, '<br>');
        
        return `<div class="markdown-content">${html}</div>`;
    }

    const convertMarkdownTableToHTML = (markdownTable) => {
        if (!markdownTable) return '';
        const lines = markdownTable.trim().split('\n');
        if (lines.length < 2 || !lines[1].includes('---')) return markdownTable;

        const headers = lines[0].split('|').map(h => h.trim()).filter(Boolean);
        let html = '<table class="dynamic-table"><thead><tr>';
        headers.forEach(header => { 
            html += `<th>${header}</th>` 
        });
        html += '</tr></thead><tbody>';

        for (let i = 2; i < lines.length; i++) {
            const rowCells = lines[i].split('|').map(c => c.trim()).filter(Boolean);
            if (rowCells.length > 0) {
                html += '<tr>';
                for (let j = 0; j < headers.length; j++) {
                    html += `<td>${rowCells[j] || ''}</td>`;
                }
                html += '</tr>';
            }
        }
        html += '</tbody></table>';
        return html;
    }
    
    const convertJsonToHTMLTable = (jsonString) => {
      try {
        const data = JSON.parse(jsonString);
        if (!data || !Array.isArray(data.columns) || !Array.isArray(data.data)) {
          return `<pre><code>${JSON.stringify(data, null, 2)}</code></pre>`;
        }

        let html = '<table class="dynamic-table"><thead><tr>';
        data.columns.forEach(header => {
          html += `<th>${header}</th>`;
        });
        html += '</tr></thead><tbody>';

        data.data.forEach(row => {
          html += '<tr>';
          data.columns.forEach((col, i) => {
            html += `<td>${row[i] || ''}</td>`;
          });
          html += '</tr>';
        });

        html += '</tbody></table>';
        return html;
      } catch (e) {
        return `<pre><code>${jsonString}</code></pre>`;
      }
    }
    
    // 生命周期
    onMounted(async () => {
      // 加载智能体信息
      await loadAgentInfo()
      // 加载预设问题
      await loadPresetQuestions()
      // 加载历史对话
      await loadChatSessions()
      // 如果没有历史对话，创建一个新对话
      if (chatSessions.value.length === 0) {
        await startNewChat()
      } else {
        // 选择最新的对话
        currentSessionId.value = chatSessions.value[0].id
        await loadMessages(chatSessions.value[0].id)
      }
      
      // 添加全局点击事件监听器
      document.addEventListener('click', handleClickOutside)
    })
    
    // 组件卸载时移除事件监听器
    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside)
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
      renameInput,
      activeDropdown,
      showRenameModal,
      renameTitle,
      currentRenameSession,
      
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
      getRandomIcon,
      getSessionPreview,
      switchSession,
      toggleDropdown,
      togglePin,
      showRenameDialog,
      closeRenameDialog,
      confirmRename,
      deleteSession,
      escapeHtml
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
  max-width: 100%;
  overflow: hidden;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
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
  display: flex;
  align-items: center;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
  background: var(--bg-primary);
  border: 1px solid transparent;
  position: relative;
  min-height: 60px;
}

.history-item:hover {
  background: var(--bg-tertiary);
  border-color: var(--border-primary);
}

.history-item.active {
  background: var(--primary-light);
  border-color: var(--primary-color);
}

.history-item.pinned {
  border-left: 3px solid var(--primary-color);
}

.history-content {
  flex: 1;
  cursor: pointer;
  padding: var(--space-xs) 0;
  min-width: 0;
}

.history-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  margin-bottom: var(--space-xs);
}

.pin-icon {
  color: var(--primary-color);
  font-size: 12px;
  flex-shrink: 0;
}

.history-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.history-actions {
  flex-shrink: 0;
  margin-left: var(--space-xs);
}

.dropdown {
  position: relative;
}

.dropdown-toggle {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--text-quaternary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  opacity: 0;
  transition: opacity var(--transition-base);
}

.history-item:hover .dropdown-toggle {
  opacity: 1;
}

.dropdown-toggle:hover {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.dropdown.active .dropdown-toggle {
  opacity: 1;
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: var(--bg-primary);
  border: 1px solid var(--border-secondary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: 1000;
  min-width: 120px;
  padding: var(--space-xs) 0;
  margin-top: var(--space-xs);
}

.dropdown-item {
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  border: none;
  background: transparent;
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  text-align: left;
}

.dropdown-item:hover {
  background: var(--bg-tertiary);
}

.dropdown-item.delete {
  color: var(--danger-color);
}

.dropdown-item.delete:hover {
  background: var(--danger-light);
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

/* 重命名对话框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.modal-content {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  width: 90%;
  max-width: 400px;
  max-height: 90vh;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-lg) var(--space-xl);
  border-bottom: 1px solid var(--border-secondary);
}

.modal-header h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.modal-close {
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
  font-size: 18px;
}

.modal-close:hover {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.modal-body {
  padding: var(--space-xl);
}

.rename-input {
  width: 100%;
  padding: var(--space-md);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-family: inherit;
  background: var(--bg-primary);
  color: var(--text-primary);
  transition: all var(--transition-base);
  box-sizing: border-box;
}

.rename-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px var(--primary-light);
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-md);
  padding: var(--space-lg) var(--space-xl);
  border-top: 1px solid var(--border-secondary);
  background: var(--bg-secondary);
}

.btn-secondary {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  border-color: var(--border-primary);
}

.btn-secondary:hover {
  background: var(--bg-quaternary);
  color: var(--text-primary);
  border-color: var(--border-secondary);
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
  min-width: 0;
  max-width: calc(100% - 280px);
  overflow: hidden;
  box-sizing: border-box;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: var(--space-xl);
  box-sizing: border-box;
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
  max-width: 100%;
  box-sizing: border-box;
}

.message-item {
  display: flex;
  gap: var(--space-base);
  align-items: flex-start;
  margin-bottom: var(--space-lg);
  max-width: 100%;
  box-sizing: border-box;
}

/* 智能体消息使用垂直布局 */
.assistant-message {
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-sm);
  width: 100%;
}

/* 智能体消息头部区域（头像+名字+时间） */
.assistant-message-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-xs);
}

.assistant-message-header .assistant-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  flex-shrink: 0;
}

.assistant-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.assistant-info .message-role {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
}

.assistant-info .message-time {
  font-size: var(--font-size-xs);
  color: var(--text-quaternary);
}

/* 智能体消息内容区域 */
.assistant-message-body {
  width: 100%;
  max-width: 90%;
  background: var(--bg-secondary);
  border: 1px solid var(--border-tertiary);
  border-radius: var(--radius-lg);
  padding: var(--space-md) var(--space-lg);
  box-shadow: var(--shadow-xs);
  word-break: break-word;
  overflow-wrap: break-word;
  overflow-x: auto;
  box-sizing: border-box;
}

.assistant-message-body .text-message {
  font-size: var(--font-size-sm);
  line-height: 1.6;
  word-wrap: break-word;
  overflow-wrap: break-word;
  max-width: 100%;
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
  max-width: calc(100% - 60px);
  box-sizing: border-box;
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
  word-break: break-word;
  overflow-wrap: break-word;
  overflow-x: auto;
  box-sizing: border-box;
}

.user-message {
  flex-direction: row-reverse;
}

.user-message .message-body {
  background: var(--primary-color);
  color: white;
  max-width: 70%;
  word-break: break-word;
  overflow-wrap: break-word;
  overflow-x: auto;
  box-sizing: border-box;
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
  overflow-wrap: break-word;
  max-width: 100%;
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

/* 流式响应样式 */
.agent-responses-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  gap: 0.75rem;
  box-sizing: border-box;
}

.agent-response-block {
  display: block !important;
  width: 100% !important;
  max-width: 100% !important;
  border: 1px solid #e1e5e9;
  border-radius: 8px;
  overflow: hidden;
  background: #f8f9fa;
  box-sizing: border-box;
}

.agent-response-title {
  background: #e9ecef;
  padding: 8px 12px;
  font-weight: 600;
  font-size: 14px;
  color: #495057;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  align-items: center;
  gap: 6px;
}

.agent-response-title i {
  font-size: 14px;
  color: #6c757d;
}

.agent-response-content {
  padding: 12px;
  background: white;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
  max-width: 100%;
  overflow-x: auto;
  box-sizing: border-box;
  white-space: pre-wrap;
  word-wrap: break-word;
  overflow-wrap: break-word;
  word-break: break-all;
  hyphens: auto;
}

.agent-response-content pre {
  margin: 0;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 4px;
  overflow-x: auto;
  border: 1px solid #e9ecef;
  max-width: 100%;
  word-wrap: break-word;
  white-space: pre-wrap;
  box-sizing: border-box;
}

.agent-response-content code {
  background: #f8f9fa;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
}

.agent-response-content .language-sql {
  color: #0066cc;
}

.agent-response-content .language-json {
  color: #0066cc;
  white-space: pre-wrap !important;
  word-break: break-all;
  overflow-wrap: break-word;
}

.dynamic-table {
  width: 100%;
  max-width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
  font-size: 12px;
  display: block;
  overflow-x: auto;
  white-space: nowrap;
}

.dynamic-table th,
.dynamic-table td {
  border: 1px solid #dee2e6;
  padding: 6px 8px;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.dynamic-table th {
  background: #f8f9fa;
  font-weight: 600;
  color: #495057;
}

.dynamic-table tr:nth-child(even) {
  background: #f8f9fa;
}

.markdown-content {
  line-height: 1.6;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3 {
  margin: 16px 0 8px 0;
  color: #333;
}

.markdown-content ul,
.markdown-content ol {
  margin: 8px 0;
  padding-left: 20px;
}

.markdown-content li {
  margin: 4px 0;
}

/* 图片和媒体内容样式 */
.text-message img,
.assistant-message-body img,
.message-body img {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-md);
  display: block;
  margin: var(--space-sm) 0;
  box-shadow: var(--shadow-xs);
}

.text-message pre,
.assistant-message-body pre,
.message-body pre {
  max-width: 100%;
  overflow-x: auto;
  background: #f8f9fa;
  border-radius: var(--radius-sm);
  padding: var(--space-sm);
  margin: var(--space-sm) 0;
  font-size: 12px;
  line-height: 1.4;
  border: 1px solid #e9ecef;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.text-message code,
.assistant-message-body code,
.message-body code {
  max-width: 100%;
  word-break: break-all;
  overflow-wrap: break-word;
  white-space: pre-wrap;
}

.text-message table,
.assistant-message-body table,
.message-body table {
  max-width: 100%;
  overflow-x: auto;
  display: block;
  white-space: nowrap;
}

.text-message video,
.assistant-message-body video,
.message-body video {
  max-width: 100%;
  height: auto;
}

/* 处理长文本内容的换行 */
.text-message *,
.assistant-message-body *,
.message-body * {
  max-width: 100%;
  word-wrap: break-word;
  overflow-wrap: break-word;
  hyphens: auto;
}

/* 特殊处理JSON和代码块 */
.text-message .language-json,
.assistant-message-body .language-json,
.message-body .language-json {
  white-space: pre-wrap !important;
  word-break: break-all;
  overflow-wrap: break-word;
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

/* CSS变量定义 */
:root {
  --danger-color: #dc3545;
  --danger-light: #f8d7da;
  --bg-quaternary: #e9ecef;
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
    word-break: break-word;
    overflow-wrap: break-word;
    overflow-x: auto;
  }
  
  .user-message .message-body {
    max-width: 85%;
    word-break: break-word;
    overflow-wrap: break-word;
    overflow-x: auto;
  }

  .assistant-message-body {
    max-width: 95%;
    word-break: break-word;
    overflow-wrap: break-word;
    overflow-x: auto;
  }

  .chat-main {
    max-width: 100%;
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
