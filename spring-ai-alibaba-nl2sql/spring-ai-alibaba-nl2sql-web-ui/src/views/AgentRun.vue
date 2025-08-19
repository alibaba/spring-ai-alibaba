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
  <div class="agent-run-page" :class="{ 'with-preview': showReportPreview }">


    <!-- 主要聊天区域 -->
    <div class="chat-container" :class="{ 'with-preview': showReportPreview }">
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
                    <!-- 报告格式选择按钮 - 嵌入到报告内容中 -->
                    <div v-if="isReportMessage(message)" class="report-controls-inline">
                      <div class="format-section">
                        <span class="format-label">查看格式：</span>
                        <div class="format-selector">
                          <button
                            class="format-btn"
                            :class="{ active: getMessageFormat(message.id) === 'markdown' }"
                            @click="setMessageFormat(message.id, 'markdown')"
                            title="切换到Markdown格式查看"
                          >
                            <i class="bi bi-markdown"></i>
                            Markdown
                          </button>
                          <button
                            class="format-btn"
                            :class="{ active: getMessageFormat(message.id) === 'html' }"
                            @click="setMessageFormat(message.id, 'html')"
                            title="切换到HTML格式查看"
                          >
                            <i class="bi bi-code-slash"></i>
                            HTML
                          </button>
                        </div>
                      </div>
                      <div class="export-actions">
                        <button
                          class="export-btn"
                          @click="exportMessageReport(message)"
                          title="导出当前格式的报告文件"
                        >
                          <i class="bi bi-download"></i>
                          导出报告
                        </button>
                      </div>
                    </div>
                    <div v-html="message.type === 'streaming' ? message.content : formatMessageWithFormat(message)"></div>

                    <!-- 报告预览按钮 - 在报告内容后显示，只在流式输出完成后显示 -->
                    <div v-if="isReportMessage(message) && hasHtmlContent(message) && message.type !== 'streaming'" class="report-preview-section">
                      <button
                        class="preview-report-btn"
                        @click="openReportPreview(message)"
                        title="在右侧面板中预览完整报告"
                      >
                        <i class="bi bi-eye"></i>
                        预览完整报告
                      </button>
                    </div>
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

    <!-- 报告预览面板 -->
    <div v-if="showReportPreview" class="report-preview-panel" :class="{ 'show': showReportPreview }">
      <div class="report-preview-header">
        <div class="report-preview-title">
          <i class="bi bi-file-earmark-text"></i>
          <span>报告预览</span>
        </div>
        <div class="report-preview-actions">
          <button class="preview-action-btn" @click="refreshReportPreview" title="刷新">
            <i class="bi bi-arrow-clockwise"></i>
          </button>
          <button class="preview-action-btn" @click="exportCurrentPreviewReport" title="导出">
            <i class="bi bi-download"></i>
          </button>
          <button class="preview-action-btn" @click="closeReportPreview" title="关闭">
            <i class="bi bi-x"></i>
          </button>
        </div>
      </div>
      <div class="report-preview-content">
        <div class="report-preview-iframe-container">
          <iframe
            ref="reportPreviewFrame"
            class="report-preview-iframe"
            :srcdoc="previewReportContent"
            frameborder="0"
            sandbox="allow-same-origin"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 移动端遮罩层 -->
    <div v-if="showReportPreview" class="mobile-preview-overlay" @click="closeReportPreview"></div>
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
    
    // 消息格式管理
    const messageFormats = ref({}) // 存储每个消息的显示格式，默认为html

    // 报告预览相关状态
    const showReportPreview = ref(false)
    const previewReportContent = ref('')
    const currentPreviewMessage = ref(null)
    const reportPreviewFrame = ref(null)
    
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
            
            // 使用 nextTick 确保 DOM 更新后再滚动
            nextTick(() => {
                scrollToBottom()
            })
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

          // 更新消息类型为完成状态
          const assistantMessage = currentMessages.value[agentMessageIndex]
          if (assistantMessage) {
            assistantMessage.type = 'completed'
            console.log('消息更新为完成状态，内容长度:', assistantMessage.content?.length)

            // 触发响应式更新
            currentMessages.value[agentMessageIndex] = { ...assistantMessage }

            // 保存AI回复消息到数据库
            if (assistantMessage.content) {
              await saveMessage({
                sessionId: currentSessionId.value,
                role: 'assistant',
                content: assistantMessage.content,
                messageType: 'completed'
              })
            }
          }

          // 确保DOM更新后滚动到底部
          await nextTick()
          scrollToBottom()
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
        console.log('滚动到底部 - 当前scrollTop:', messagesContainer.value.scrollTop, '目标scrollHeight:', messagesContainer.value.scrollHeight)
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      } else {
        console.log('messagesContainer 未找到')
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
      
      // 关闭导出下拉菜单
      if (!event.target.closest('.export-dropdown-menu') && !event.target.closest('.export-btn')) {
        activeExportDropdown.value = null
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
            // 检查是否包含HTML代码块
            const htmlCodeBlockRegex = /```\s*html?\s*([\s\S]*?)```/gi;
            const htmlMatches = processedData.match(htmlCodeBlockRegex);

            if (htmlMatches && htmlMatches.length > 0) {
                let htmlContent = processedData;

                // 处理HTML代码块 - 直接渲染HTML内容
                htmlContent = htmlContent.replace(htmlCodeBlockRegex, (match, htmlCode) => {
                    let cleanedHTML = htmlCode.trim();
                    // 基本的HTML清理，移除潜在的脚本标签
                    cleanedHTML = cleanedHTML.replace(/<script[\s\S]*?<\/script>/gi, '');
                    cleanedHTML = cleanedHTML.replace(/on\w+\s*=\s*["'][^"']*["']/gi, '');
                    // 直接返回HTML内容，让浏览器渲染
                    return `<div class="html-rendered-content">${cleanedHTML}</div>`;
                });

                // 处理其他文本内容，保持换行
                htmlContent = htmlContent.replace(/\n/g, '<br>');
                return htmlContent;
            }

            // 检查是否包含SQL代码块
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
    
    // 报告格式管理方法
    const isReportMessage = (message) => {
      if (!message.content) return false
      
      // 检查消息是否包含报告内容
      const reportKeywords = [
        '数据分析报告',
        '输出报告', 
        'output_report',
        '执行摘要',
        '关键发现',
        '业务洞察',
        '建议和行动计划',
        '数据分析过程',
        '详细分析结果'
      ]
      
      const hasKeyword = reportKeywords.some(keyword => message.content.includes(keyword))
      const hasStructuredContent = message.content.includes('1.') && message.content.includes('2.') && 
                                  (message.content.includes('用户') || message.content.includes('分析') || message.content.includes('数据'))
      
      const isReport = hasKeyword || hasStructuredContent
      
      // 调试输出
      if (message.content.includes('数据分析报告') || message.content.includes('执行摘要')) {
        console.log('检测报告消息:', {
          messageId: message.id,
          hasKeyword,
          hasStructuredContent,
          isReport,
          contentPreview: message.content.substring(0, 200)
        })
      }
      
      return isReport
    }
    
    const getMessageFormat = (messageId) => {
      return messageFormats.value[messageId] || 'html'
    }
    
    const setMessageFormat = (messageId, format) => {
      messageFormats.value[messageId] = format
      // 重新渲染该消息
      const messageIndex = currentMessages.value.findIndex(m => m.id === messageId)
      if (messageIndex !== -1) {
        // 触发响应式更新
        currentMessages.value[messageIndex] = { ...currentMessages.value[messageIndex] }
        nextTick(() => {
          // 确保DOM更新后的处理
        })
      }
    }
    
    // 新增：根据格式显示消息内容
    const formatMessageWithFormat = (message) => {
      const format = getMessageFormat(message.id)
      const originalContent = formatMessage(message.content)
      
      if (!isReportMessage(message)) {
        return originalContent
      }
      
      if (format === 'markdown') {
        // 将HTML内容转换为Markdown显示
        const markdownContent = convertHtmlToMarkdown(originalContent)
        return `
          <div class="markdown-container">
            <div class="format-indicator">
              <i class="bi bi-markdown"></i>
              <span>Markdown 格式</span>
            </div>
            <pre class="markdown-content">${escapeHtml(markdownContent)}</pre>
          </div>
        `
      } else {
        // HTML格式，添加格式指示器
        return `
          <div class="html-container">
            <div class="format-indicator">
              <i class="bi bi-code-slash"></i>
              <span>HTML 格式</span>
            </div>
            <div class="html-content">${originalContent}</div>
          </div>
        `
      }
    }
    
    const exportMessageReport = (message) => {
      const format = getMessageFormat(message.id)
      const content = extractReportContent(message.content, format)
      
      let filename = ''
      let blob = null
      
      if (format === 'html') {
        const htmlContent = generateHTMLReportFromMessage(content)
        filename = `report_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.html`
        blob = new Blob([htmlContent], { type: 'text/html' })
      } else {
        const markdownContent = extractMarkdownFromMessage(content)
        filename = `report_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.md`
        blob = new Blob([markdownContent], { type: 'text/markdown' })
      }
      
      // 创建下载链接
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    }
    
    const extractReportContent = (messageContent, format) => {
      // 从消息内容中提取报告部分
      
      // 方法1: 尝试提取"输出报告"部分的内容
      const reportSectionMatch = messageContent.match(/<div class="agent-response-block"[^>]*>\s*<i class="bi bi-file-earmark-text"><\/i>\s*输出报告[\s\S]*?<\/div>\s*<div class="agent-response-content">([\s\S]*?)<\/div>/i)
      
      if (reportSectionMatch) {
        const reportContent = reportSectionMatch[1]
        if (format === 'markdown') {
          return convertHtmlToMarkdown(reportContent)
        } else {
          return reportContent
        }
      }
      
      // 方法2: 查找最后一个包含"数据分析报告"的agent-response-content部分
      const allResponseBlocks = messageContent.match(/<div class="agent-response-content">([\s\S]*?)<\/div>/g)
      if (allResponseBlocks) {
        // 从后往前查找包含"数据分析报告"的部分
        for (let i = allResponseBlocks.length - 1; i >= 0; i--) {
          const blockMatch = allResponseBlocks[i].match(/<div class="agent-response-content">([\s\S]*?)<\/div>/)
          if (blockMatch && blockMatch[1].includes('数据分析报告')) {
            const reportContent = blockMatch[1]
            if (format === 'markdown') {
              return convertHtmlToMarkdown(reportContent)
            } else {
              return reportContent
            }
          }
        }
      }
      
      // 方法3: 简单的文本匹配（后备方案）
      if (format === 'markdown') {
        const markdownMatch = messageContent.match(/数据分析报告[\s\S]*/i)
        return markdownMatch ? markdownMatch[0] : messageContent
      } else {
        const htmlReportMatch = messageContent.match(/(.*?数据分析报告[\s\S]*)/i)
        if (htmlReportMatch) {
          return htmlReportMatch[1]
        }
        return messageContent
      }
    }
    
    // 添加HTML转Markdown的辅助函数
    const convertHtmlToMarkdown = (htmlContent) => {
      return htmlContent
        .replace(/<h([1-6])>/g, (match, level) => '#'.repeat(parseInt(level)) + ' ')
        .replace(/<\/h[1-6]>/g, '\n')
        .replace(/<p>/g, '\n')
        .replace(/<\/p>/g, '\n')
        .replace(/<br\s*\/?>/g, '\n')
        .replace(/<strong>(.*?)<\/strong>/g, '**$1**')
        .replace(/<em>(.*?)<\/em>/g, '*$1*')
        .replace(/<code>(.*?)<\/code>/g, '`$1`')
        .replace(/<pre><code[^>]*>([\s\S]*?)<\/code><\/pre>/g, '```\n$1\n```')
        .replace(/<li>/g, '- ')
        .replace(/<\/li>/g, '\n')
        .replace(/<ul>/g, '\n')
        .replace(/<\/ul>/g, '\n')
        .replace(/<ol>/g, '\n')
        .replace(/<\/ol>/g, '\n')
        .replace(/<[^>]+>/g, '') // 移除剩余的HTML标签
        .replace(/\n\s*\n\s*\n/g, '\n\n') // 清理多余的空行
        .trim()
    }
    
    const generateHTMLReportFromMessage = (content) => {
      return `
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>数据分析报告</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; margin: 40px; }
        .report-header { border-bottom: 2px solid #e9ecef; padding-bottom: 20px; margin-bottom: 30px; }
        .report-title { font-size: 2em; font-weight: bold; color: #2c3e50; }
        .report-meta { color: #6c757d; margin-top: 10px; }
        table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        th, td { border: 1px solid #dee2e6; padding: 8px 12px; text-align: left; }
        th { background-color: #e9ecef; font-weight: 600; }
        pre { background: #f8f9fa; padding: 15px; border-radius: 6px; overflow-x: auto; }
        code { background: #e9ecef; padding: 2px 4px; border-radius: 3px; }
        h1, h2, h3 { color: #2c3e50; }
        .agent-response-block { margin: 20px 0; padding: 15px; border: 1px solid #e9ecef; border-radius: 8px; }
    </style>
</head>
<body>
    <div class="report-header">
        <div class="report-title">数据分析报告</div>
        <div class="report-meta">导出时间: ${new Date().toLocaleString('zh-CN')}</div>
    </div>
    <div class="report-content">
        ${content}
    </div>
</body>
</html>`
    }
    
    const extractMarkdownFromMessage = (content) => {
      // 直接返回提取的内容，因为extractReportContent已经处理了格式转换
      return `# 数据分析报告\n\n> 导出时间: ${new Date().toLocaleString('zh-CN')}\n\n---\n\n${content}`
    }

    // 报告预览相关方法
    const hasHtmlContent = (message) => {
      if (!message.content) return false

      console.log('检查HTML内容，消息ID:', message.id)

      // 方法1: 检查是否包含HTML代码块
      const htmlCodeBlockRegex = /```\s*html?\s*([\s\S]*?)```/gi
      if (htmlCodeBlockRegex.test(message.content)) {
        console.log('发现HTML代码块')
        return true
      }

      // 方法2: 检查是否包含language-html代码块
      if (message.content.includes('language-html')) {
        console.log('发现language-html代码块')
        return true
      }

      // 方法3: 检查是否包含已渲染的HTML内容
      if (message.content.includes('html-rendered-content')) {
        console.log('发现html-rendered-content')
        return true
      }

      // 方法4: 检查是否是报告消息且包含表格等HTML元素
      if (isReportMessage(message)) {
        const hasTableElements = /<table[\s\S]*?<\/table>/i.test(message.content) ||
                                /<thead[\s\S]*?<\/thead>/i.test(message.content) ||
                                /<tbody[\s\S]*?<\/tbody>/i.test(message.content) ||
                                message.content.includes('<!DOCTYPE html')
        if (hasTableElements) {
          console.log('发现表格或HTML文档元素')
          return true
        }
      }

      console.log('未发现HTML内容')
      return false
    }

    const openReportPreview = (message) => {
      currentPreviewMessage.value = message
      previewReportContent.value = generatePreviewReportContent(message)
      showReportPreview.value = true

      // 确保DOM更新后再处理iframe
      nextTick(() => {
        // 可以在这里添加额外的iframe处理逻辑
      })
    }

    const closeReportPreview = () => {
      showReportPreview.value = false
      previewReportContent.value = ''
      currentPreviewMessage.value = null
    }

    const refreshReportPreview = () => {
      if (currentPreviewMessage.value) {
        previewReportContent.value = generatePreviewReportContent(currentPreviewMessage.value)
      }
    }

    const exportCurrentPreviewReport = () => {
      if (currentPreviewMessage.value) {
        exportMessageReport(currentPreviewMessage.value)
      }
    }

    const generatePreviewReportContent = (message) => {
      // 提取HTML内容
      const htmlContent = extractHtmlContentFromMessage(message.content)

      // 生成完整的HTML页面
      return `
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>数据分析报告</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            line-height: 1.6;
            margin: 0;
            padding: 20px;
            background: #ffffff;
            color: #333333;
        }
        .report-container {
            max-width: 100%;
            margin: 0 auto;
        }
        .report-header {
            border-bottom: 2px solid #e9ecef;
            padding-bottom: 20px;
            margin-bottom: 30px;
            text-align: center;
        }
        .report-title {
            font-size: 2em;
            font-weight: bold;
            color: #2c3e50;
            margin: 0;
        }
        .report-meta {
            color: #6c757d;
            margin-top: 10px;
            font-size: 0.9em;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            border-radius: 8px;
            overflow: hidden;
        }
        th, td {
            border: 1px solid #dee2e6;
            padding: 12px 16px;
            text-align: left;
        }
        th {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            font-weight: 600;
            text-transform: uppercase;
            font-size: 0.85em;
            letter-spacing: 0.5px;
        }
        tr:nth-child(even) {
            background: #f8f9fa;
        }
        tr:hover {
            background: #e3f2fd;
            transition: background-color 0.3s ease;
        }
        h1, h2, h3, h4, h5, h6 {
            color: #2c3e50;
            margin: 24px 0 12px 0;
        }
        h1 {
            font-size: 2.2em;
            border-bottom: 3px solid #3498db;
            padding-bottom: 10px;
            color: #2c3e50;
        }
        h2 {
            font-size: 1.8em;
            color: #34495e;
        }
        h3 {
            font-size: 1.4em;
            color: #34495e;
        }
        p {
            margin: 12px 0;
            line-height: 1.7;
            color: #333333;
        }
        ul, ol {
            margin: 16px 0;
            padding-left: 24px;
            color: #333333;
        }
        li {
            margin: 8px 0;
            line-height: 1.6;
            color: #333333;
        }
        strong {
            color: #2c3e50;
            font-weight: 600;
        }
        em {
            color: #6c757d;
            font-style: italic;
        }
        .highlight {
            background: linear-gradient(120deg, #e3f2fd 0%, #f3e5f5 100%);
            padding: 2px 6px;
            border-radius: 4px;
            color: #333333;
        }
        .metric-card {
            background: #ffffff;
            border: 1px solid #e9ecef;
            border-radius: 8px;
            padding: 16px;
            margin: 12px 0;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            color: #333333;
        }
        .trend-up {
            color: #27ae60;
            font-weight: bold;
        }
        .trend-down {
            color: #e74c3c;
            font-weight: bold;
        }
        .trend-stable {
            color: #f39c12;
            font-weight: bold;
        }
        /* 确保打印时也是明亮主题 */
        @media print {
            body {
                margin: 0;
                padding: 15px;
                background: #ffffff !important;
                color: #333333 !important;
            }
            .report-container {
                max-width: none;
                background: #ffffff !important;
            }
            * {
                background: #ffffff !important;
                color: #333333 !important;
            }
        }
    </style>
</head>
<body>
    <div class="report-container">
        <div class="report-header">
            <h1 class="report-title">数据分析报告</h1>
            <div class="report-meta">生成时间: ${new Date().toLocaleString('zh-CN')}</div>
        </div>
        <div class="report-content">
            ${htmlContent}
        </div>
    </div>
</body>
</html>`
    }

    const extractHtmlContentFromMessage = (messageContent) => {
      console.log('提取HTML内容，原始消息内容:', messageContent.substring(0, 500) + '...')

      let htmlContent = ''
      let match

      // 方法1: 直接从原始消息内容中提取HTML代码块
      const htmlCodeBlockRegex = /```\s*html?\s*([\s\S]*?)```/gi
      htmlCodeBlockRegex.lastIndex = 0

      while ((match = htmlCodeBlockRegex.exec(messageContent)) !== null) {
        const extractedHtml = match[1].trim()
        console.log('找到HTML代码块:', extractedHtml.substring(0, 200) + '...')
        htmlContent += extractedHtml + '\n'
      }

      // 方法2: 从<code class="language-html">标签中提取（处理markdown渲染后的情况）
      if (!htmlContent) {
        console.log('尝试从language-html代码块中提取...')
        const codeHtmlRegex = /<code class="language-html">([\s\S]*?)<\/code>/gi
        codeHtmlRegex.lastIndex = 0

        while ((match = codeHtmlRegex.exec(messageContent)) !== null) {
          let extractedHtml = match[1].trim()
          // 处理HTML实体编码和br标签
          extractedHtml = extractedHtml
            .replace(/<br\s*\/?>/gi, '\n')
            .replace(/&lt;/g, '<')
            .replace(/&gt;/g, '>')
            .replace(/&amp;/g, '&')
            .replace(/&quot;/g, '"')
            .replace(/&#x27;/g, "'")
          console.log('从language-html中提取HTML:', extractedHtml.substring(0, 200) + '...')
          htmlContent += extractedHtml + '\n'
        }
      }

      // 方法3: 从已渲染的html-rendered-content中提取
      if (!htmlContent) {
        console.log('尝试从html-rendered-content中提取...')
        const htmlRenderedRegex = /<div class="html-rendered-content">([\s\S]*?)<\/div>/g
        htmlRenderedRegex.lastIndex = 0

        while ((match = htmlRenderedRegex.exec(messageContent)) !== null) {
          const extractedHtml = match[1].trim()
          console.log('从渲染内容中提取HTML:', extractedHtml.substring(0, 200) + '...')
          htmlContent += extractedHtml + '\n'
        }
      }

      // 方法4: 从markdown-content中的pre code标签提取
      if (!htmlContent) {
        console.log('尝试从markdown-content中提取...')
        const markdownCodeRegex = /<div class="markdown-content">[\s\S]*?<pre><code class="language-html">([\s\S]*?)<\/code><\/pre>[\s\S]*?<\/div>/gi
        markdownCodeRegex.lastIndex = 0

        while ((match = markdownCodeRegex.exec(messageContent)) !== null) {
          let extractedHtml = match[1].trim()
          // 处理HTML实体编码和br标签
          extractedHtml = extractedHtml
            .replace(/<br\s*\/?>/gi, '\n')
            .replace(/&lt;/g, '<')
            .replace(/&gt;/g, '>')
            .replace(/&amp;/g, '&')
            .replace(/&quot;/g, '"')
            .replace(/&#x27;/g, "'")
          console.log('从markdown-content中提取HTML:', extractedHtml.substring(0, 200) + '...')
          htmlContent += extractedHtml + '\n'
        }
      }

      // 方法5: 查找包含"输出报告"的response block
      if (!htmlContent) {
        console.log('尝试从agent-response-content中提取...')
        const reportBlockRegex = /<div class="agent-response-block"[^>]*>[\s\S]*?<i class="bi bi-file-earmark-text"><\/i>\s*输出报告[\s\S]*?<div class="agent-response-content">([\s\S]*?)<\/div>/gi
        reportBlockRegex.lastIndex = 0

        while ((match = reportBlockRegex.exec(messageContent)) !== null) {
          const blockContent = match[1]
          console.log('找到报告块内容:', blockContent.substring(0, 200) + '...')

          // 尝试从块内容中提取各种格式的HTML
          const patterns = [
            /<div class="html-rendered-content">([\s\S]*?)<\/div>/g,
            /<code class="language-html">([\s\S]*?)<\/code>/g,
            /```\s*html?\s*([\s\S]*?)```/gi
          ]

          for (const pattern of patterns) {
            pattern.lastIndex = 0
            while ((match = pattern.exec(blockContent)) !== null) {
              let extractedHtml = match[1].trim()
              if (pattern.source.includes('language-html')) {
                extractedHtml = extractedHtml
                  .replace(/<br\s*\/?>/gi, '\n')
                  .replace(/&lt;/g, '<')
                  .replace(/&gt;/g, '>')
                  .replace(/&amp;/g, '&')
                  .replace(/&quot;/g, '"')
                  .replace(/&#x27;/g, "'")
              }
              htmlContent += extractedHtml + '\n'
            }
          }
        }
      }

      // 清理和格式化HTML内容
      if (htmlContent) {
        htmlContent = htmlContent
          .replace(/\\n/g, '\n')  // 处理转义的换行符
          .replace(/class="dark"/gi, '')  // 移除暗色模式class
          .replace(/class='dark'/gi, '')  // 移除暗色模式class（单引号）
          .replace(/<html[^>]*class="[^"]*dark[^"]*"[^>]*>/gi, '<html lang="zh-CN">')  // 移除html标签上的dark class
          .replace(/<html[^>]*class='[^']*dark[^']*'[^>]*>/gi, '<html lang="zh-CN">')  // 移除html标签上的dark class（单引号）
          .trim()

        console.log('最终提取的HTML内容长度:', htmlContent.length)
        console.log('最终提取的HTML内容预览:', htmlContent.substring(0, 300) + '...')
        return htmlContent
      }

      console.log('未找到任何HTML内容')
      return '<div style="text-align: center; padding: 40px; color: #666;"><h2>未找到HTML报告内容</h2><p>请确保报告包含HTML格式的内容</p></div>'
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
      messageFormats,
      showReportPreview,
      previewReportContent,
      currentPreviewMessage,
      reportPreviewFrame,

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
      escapeHtml,
      // 报告格式管理方法
      isReportMessage,
      getMessageFormat,
      setMessageFormat,
      formatMessageWithFormat,
      exportMessageReport,
      // 报告预览方法
      hasHtmlContent,
      openReportPreview,
      closeReportPreview,
      refreshReportPreview,
      exportCurrentPreviewReport
    }
  }
}
</script>

<style scoped>
.agent-run-page {
  height: 100vh;
  background: #f0f2f5;
  font-family: var(--font-family);
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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

/* 报告控制按钮样式 */
.report-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

/* 内联报告控制按钮样式 */
.report-controls-inline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin: 0 0 16px 0;
  background: linear-gradient(135deg, #f0f7ff 0%, #e6f3ff 100%);
  border-radius: 8px;
  border: 1px solid #d6e4ff;
  position: relative;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.report-controls-inline:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.format-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.format-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
  white-space: nowrap;
}

.format-selector {
  display: flex;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  overflow: hidden;
  background: white;
}

.format-btn {
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  border-right: 1px solid #dee2e6;
  position: relative;
  overflow: hidden;
}

.format-btn:last-child {
  border-right: none;
}

.format-btn:hover {
  background-color: #f0f7ff;
  color: #1890ff;
  transform: translateY(-1px);
}

.format-btn.active {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(24, 144, 255, 0.3);
  transform: translateY(-1px);
}

.format-btn.active::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0) 100%);
  pointer-events: none;
}

.export-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(82, 196, 26, 0.3);
  position: relative;
  overflow: hidden;
}

.export-btn:hover {
  background: linear-gradient(135deg, #73d13d 0%, #95de64 100%);
  box-shadow: 0 4px 8px rgba(82, 196, 26, 0.4);
  transform: translateY(-2px);
}

.export-btn:active {
  transform: translateY(0px);
  box-shadow: 0 2px 4px rgba(82, 196, 26, 0.3);
}

.export-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0) 100%);
  pointer-events: none;
}

/* 格式容器样式 */
.markdown-container,
.html-container {
  margin: 12px 0;
  border: 1px solid #e1e4e8;
  border-radius: 8px;
  overflow: hidden;
  background: #ffffff;
  transition: all 0.4s ease;
  opacity: 0;
  animation: fadeInUp 0.5s ease forwards;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.format-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #f6f8fa;
  border-bottom: 1px solid #e1e4e8;
  font-size: 12px;
  color: #586069;
  font-weight: 500;
}

.format-indicator i {
  font-size: 14px;
}

.markdown-content {
  margin: 0;
  padding: 16px;
  background: #f8f9fa;
  font-family: 'SF Mono', Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  overflow-x: auto;
  border: none;
  color: #24292e;
}

.html-content {
  padding: 16px;
  background: #ffffff;
}

/* Markdown格式特殊样式 */
.markdown-container .format-indicator {
  background: #e3f2fd;
  color: #1565c0;
  border-bottom-color: #bbdefb;
}

.markdown-container .format-indicator i {
  color: #1976d2;
}

/* HTML格式特殊样式 */
.html-container .format-indicator {
  background: #fff3e0;
  color: #e65100;
  border-bottom-color: #ffcc02;
}

.html-container .format-indicator i {
  color: #ff9800;
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

/* HTML渲染内容样式 */
.html-rendered-content {
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 16px;
  margin: 8px 0;
  max-width: 100%;
  overflow-x: auto;
  box-sizing: border-box;
}

.html-rendered-content * {
  max-width: 100%;
  box-sizing: border-box;
}

.html-rendered-content table {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
}

.html-rendered-content th,
.html-rendered-content td {
  border: 1px solid #dee2e6;
  padding: 8px 12px;
  text-align: left;
}

.html-rendered-content th {
  background-color: #f8f9fa;
  font-weight: 600;
}

.html-rendered-content h1,
.html-rendered-content h2,
.html-rendered-content h3,
.html-rendered-content h4,
.html-rendered-content h5,
.html-rendered-content h6 {
  margin: 16px 0 8px 0;
  color: #2c3e50;
}

.html-rendered-content p {
  margin: 8px 0;
  line-height: 1.6;
}

.html-rendered-content ul,
.html-rendered-content ol {
  margin: 8px 0;
  padding-left: 20px;
}

.html-rendered-content li {
  margin: 4px 0;
}

.html-rendered-content pre {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 8px 0;
}

.html-rendered-content code {
  background: #f8f9fa;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
}

/* 报告预览按钮样式 */
.report-preview-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e9ecef;
  text-align: center;
}

.preview-report-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.preview-report-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.preview-report-btn:active {
  transform: translateY(0);
}

.preview-report-btn i {
  font-size: 16px;
}

/* 当显示预览时，主页面缩小 */
.agent-run-page.with-preview {
  width: 50%;
  transition: width 0.3s ease-in-out;
}

.chat-container.with-preview {
  width: 100%;
  transition: width 0.3s ease-in-out;
}

/* 报告预览面板样式 */
.report-preview-panel {
  position: fixed;
  top: 0;
  right: -50%;
  width: 50%;
  height: 100vh;
  background: white;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  transition: right 0.3s ease-in-out;
}

.report-preview-panel.show {
  right: 0;
}

.report-preview-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.report-preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.report-preview-title i {
  font-size: 18px;
}

.report-preview-actions {
  display: flex;
  gap: 8px;
}

.preview-action-btn {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  border-radius: 6px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.preview-action-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.preview-action-btn i {
  font-size: 14px;
}

.report-preview-content {
  flex: 1;
  overflow: hidden;
  background: #f8f9fa;
}

.report-preview-iframe-container {
  width: 100%;
  height: 100%;
  padding: 0;
  margin: 0;
}

.report-preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: white;
}

/* 移动端遮罩层 */
.mobile-preview-overlay {
  display: none;
}

@media (max-width: 768px) {
  .mobile-preview-overlay {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.3);
    z-index: 1000;
    backdrop-filter: blur(2px);
  }
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .agent-run-page.with-preview {
    width: 40%;
  }

  .report-preview-panel {
    width: 60%;
    right: -60%;
  }
}

@media (max-width: 768px) {
  .agent-run-page.with-preview {
    width: 100%;
    position: relative;
  }

  .report-preview-panel {
    width: 100%;
    right: -100%;
    position: fixed;
    z-index: 1001;
  }

  .preview-report-btn {
    padding: 10px 16px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .report-preview-header {
    padding: 12px 16px;
  }

  .report-preview-title {
    font-size: 14px;
  }

  .preview-action-btn {
    width: 32px;
    height: 32px;
  }
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
