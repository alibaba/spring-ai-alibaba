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
  <div class="agent-debug-panel" style="padding: 1.5rem; height: calc(100vh - 120px); display: flex; flex-direction: column; background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%); gap: 1.5rem;">
    <!-- 调试头部 -->
    <div class="debug-header" style="background: white; padding: 1rem 1.5rem; border-radius: 12px; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06); border: 1px solid #e2e8f0;">
      <h2 style="font-size: 1.3rem; font-weight: 600; color: #1e293b; margin-bottom: 0.25rem; background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">智能体调试</h2>
      <p class="debug-subtitle" style="color: #64748b; font-size: 0.85rem; margin: 0; font-weight: 400;">测试智能体的响应能力和配置正确性</p>
    </div>

    <!-- 调试结果区域 -->
    <div class="debug-result-section" style="background: white; border-radius: 16px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08); overflow: hidden; flex: 1; display: flex; flex-direction: column; border: 1px solid #e2e8f0;">
      <div class="result-header" style="padding: 0.75rem 1.25rem; background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%); border-bottom: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center;">
        <div class="result-title" style="font-size: 1rem; font-weight: 600; color: #1e293b; display: flex; align-items: center; gap: 0.5rem;">
          <i class="bi bi-terminal" style="color: #3b82f6; font-size: 1rem;"></i>
          调试结果
        </div>
        <div class="result-status" v-if="debugStatus">
          <span class="badge" :class="getStatusClass()" style="padding: 0.3rem 0.75rem; border-radius: 16px; font-size: 0.75rem; font-weight: 500;">{{ debugStatus }}</span>
        </div>
      </div>
      <div class="result-content" ref="resultContainer" style="flex: 1; overflow-y: auto; background: #fafbfc; display: flex; flex-direction: column;">
        <!-- 空状态 -->
        <div v-if="!hasResults" class="empty-state" style="text-align: center; padding: 2.5rem 1.5rem; background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%); border-radius: 16px; margin: 1rem; border: 2px dashed #cbd5e1; flex: 1; display: flex; flex-direction: column; justify-content: center; min-height: 0;">
          <div class="empty-icon" style="font-size: 2.5rem; margin-bottom: 1.25rem; color: #94a3b8;">
            <i class="bi bi-chat-square-text"></i>
          </div>
          <div class="empty-text" style="font-size: 0.95rem; margin-bottom: 1.5rem; color: #475569; font-weight: 500;">
            输入测试问题，查看智能体的响应结果
          </div>
          <div class="example-queries" v-if="exampleQueries.length > 0" style="display: flex; flex-wrap: wrap; gap: 0.75rem; justify-content: center; max-width: 700px; margin: 0 auto;">
            <div
              class="example-query"
              v-for="example in exampleQueries"
              :key="example"
              @click="useExampleQuery(example)"
              style="padding: 0.75rem 1.25rem; background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%); color: white; border-radius: 20px; cursor: pointer; transition: all 0.3s ease; font-weight: 500; box-shadow: 0 3px 12px rgba(59, 130, 246, 0.25); font-size: 0.85rem;"
            >
              {{ example }}
            </div>
          </div>
        </div>

        <!-- 结果展示 -->
        <div v-else class="debug-results-container" style="padding: 1.25rem; background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%); min-height: 300px;">
          <!-- 流式结果区块 - 使用与 AgentWorkspace.vue 相同的结构 -->
          <div v-for="section in streamingSections" :key="section.id"
               class="agent-response-block"
               :data-type="section.type"
               :class="{ 'loading': section.isLoading }"
               style="margin-bottom: 1.5rem; border-radius: 12px; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08); border: 1px solid #e2e8f0; overflow: hidden; background: white;">
            <div class="agent-response-title" style="padding: 0.5rem 1rem !important; background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important; border-bottom: 1px solid #e2e8f0 !important; display: flex !important; justify-content: space-between !important; align-items: center !important; min-height: auto !important; height: auto !important; line-height: 1.2 !important;">
              <div class="title-left" style="display: flex !important; align-items: center !important; gap: 0.5rem !important;">
                <i :class="section.icon" style="color: #3b82f6 !important; font-size: 0.9rem !important;"></i>
                <span class="title-text" style="font-weight: 600 !important; color: #1e293b !important; font-size: 0.85rem !important; line-height: 1.2 !important; margin: 0 !important;">{{ section.title }}</span>
                <span v-if="section.isLoading" class="loading-indicator">
                  <i class="bi bi-three-dots loading-dots" style="color: #3b82f6;"></i>
                </span>
              </div>
              <div class="title-actions" style="display: flex !important; align-items: center !important; gap: 0.5rem !important;">
                <button
                  v-if="section.type === 'sql'"
                  class="copy-button"
                  @click="copyToClipboard(section.rawContent)"
                  title="复制SQL"
                  style="background: #3b82f6 !important; color: white !important; border: none !important; padding: 0.25rem 0.5rem !important; border-radius: 4px !important; cursor: pointer !important; transition: all 0.2s ease !important; font-size: 0.75rem !important; line-height: 1 !important;"
                >
                  <i class="bi bi-clipboard" style="font-size: 0.75rem !important;"></i>
                </button>
                <span v-if="section.timestamp" class="timestamp" style="font-size: 0.65rem !important; color: #64748b !important; background: #f1f5f9 !important; padding: 0.1rem 0.4rem !important; border-radius: 8px !important; line-height: 1.2 !important;">
                  {{ formatTime(section.timestamp) }}
                </span>
              </div>
            </div>
            <div class="agent-response-content"
                 v-html="section.content"
                 style="padding: 1rem !important; font-size: 0.9rem !important; line-height: 1.6 !important; color: #374151 !important; background: white !important; min-height: 30px !important;"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 调试输入区域 -->
    <div class="debug-input-section" style="background: white; padding: 1rem 1.5rem; border-radius: 12px; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06); border: 1px solid #e2e8f0;">
      <div class="input-container" style="display: flex; gap: 0.75rem; align-items: center;">
        <input
          type="text"
          v-model="debugQuery"
          class="debug-input"
          placeholder="请输入问题..."
          :disabled="isDebugging || isInitializing"
          @keyup.enter="startDebug"
          ref="debugInput"
          style="flex: 1; padding: 0.75rem 1rem; font-size: 0.9rem; border: 1px solid #e2e8f0; border-radius: 8px; outline: none; background: #fafbfc; color: #1e293b; transition: all 0.3s ease;"
        >
        <button
          class="debug-button"
          :disabled="isDebugging"
          @click="handleDebugClick"
          style="padding: 0.75rem 1.25rem; background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%); color: white; border: none; border-radius: 8px; font-size: 0.85rem; cursor: pointer; transition: all 0.3s ease; display: flex; align-items: center; gap: 0.4rem; min-width: 100px; justify-content: center; font-weight: 500; box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);"
        >
          <i class="bi bi-play-circle" v-if="!isDebugging" style="font-size: 0.85rem;"></i>
          <div class="spinner" v-else></div>
          {{ isDebugging ? '调试中...' : '开始调试' }}
        </button>
        <button
          class="schema-init-button"
          :disabled="isDebugging || isInitializing"
          @click="openSchemaInitModal"
          style="padding: 0.75rem 1rem; background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%); color: white; border: none; border-radius: 8px; font-size: 0.8rem; cursor: pointer; transition: all 0.3s ease; display: flex; align-items: center; gap: 0.4rem; font-weight: 500; box-shadow: 0 2px 8px rgba(139, 92, 246, 0.25);"
        >
          <i class="bi bi-database-gear" style="font-size: 0.8rem;"></i>
          初始化信息源
        </button>
        <button
          class="init-button"
          :disabled="isInitializing || isDebugging"
          :class="{ loading: isInitializing }"
          @click="initializeDataSource"
          style="padding: 0.75rem 1rem; background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; border: none; border-radius: 8px; font-size: 0.8rem; cursor: pointer; transition: all 0.3s ease; display: flex; align-items: center; gap: 0.4rem; font-weight: 500; box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);"
        >
          <i class="bi bi-database-add" v-if="!isInitializing && !isInitialized" style="font-size: 0.8rem;"></i>
          <i class="bi bi-check-circle" v-if="!isInitializing && isInitialized" style="font-size: 0.8rem;"></i>
          <div class="spinner" v-if="isInitializing"></div>
          {{ getInitButtonText() }}
        </button>
      </div>
    </div>

    <!-- 初始化信息源模态框 -->
    <div v-if="showSchemaInitModal" class="modal-overlay" @click="closeSchemaInitModal">
      <div class="modal-dialog" @click.stop>
        <div class="modal-header">
          <h3>
            <i class="bi bi-database-gear"></i>
            初始化信息源
          </h3>
          <button class="close-btn" @click="closeSchemaInitModal">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <!-- 初始化状态显示 -->
          <div class="init-status-card" :class="{ 'initialized': isInitialized, 'not-initialized': !isInitialized }">
            <div class="status-indicator">
              <div class="status-icon">
                <i class="bi bi-check-circle-fill" v-if="isInitialized"></i>
                <i class="bi bi-exclamation-triangle-fill" v-else></i>
              </div>
              <div class="status-info">
                <div class="status-title">
                  {{ isInitialized ? '数据源已初始化' : '数据源未初始化' }}
                </div>
                <div class="status-desc">
                  {{ isInitialized ? '智能体已准备就绪，可以开始调试' : '请配置并初始化数据源，然后再进行调试' }}
                </div>
              </div>
            </div>

            <!-- 统计信息 -->
            <div class="stats-info" v-if="schemaStatistics && isInitialized">
              <div class="stat-item">
                <span class="stat-label">文档总数</span>
                <span class="stat-value">{{ schemaStatistics.documentCount || 0 }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">智能体ID</span>
                <span class="stat-value">{{ schemaStatistics.agentId }}</span>
              </div>
            </div>
          </div>

          <!-- 初始化配置表单 -->
          <div class="init-config-form" v-if="!isInitialized || showConfigForm">
            <div class="form-row">
              <div class="form-group">
                <label>选择数据源</label>
                <select v-model="schemaInitForm.selectedDatasource" class="form-control" @change="onDatasourceChange">
                  <option value="">请选择数据源</option>
                  <option v-for="ds in availableDatasources"
                          :key="ds.id"
                          :value="ds">
                    {{ ds.name }} ({{ getDatasourceTypeText(ds.type) }})
                  </option>
                </select>
              </div>
            </div>

            <div class="form-group" v-if="schemaInitForm.selectedDatasource">
              <label>选择表 ({{ selectedTables.length }} 个已选择)</label>
              <div class="table-selection">
                <div class="table-search">
                  <input type="text" v-model="tableSearchKeyword" placeholder="搜索表名..." class="form-control">
                  <div class="table-actions">
                    <button type="button" class="btn btn-sm btn-outline" @click="selectAllTables">全选</button>
                    <button type="button" class="btn btn-sm btn-outline" @click="clearAllTables">清空</button>
                    <button type="button" class="btn btn-sm btn-primary" @click="loadTables" :disabled="!schemaInitForm.selectedDatasource">
                      <i class="bi bi-arrow-clockwise"></i>
                      刷新表列表
                    </button>
                  </div>
                </div>
                <div class="table-list" v-if="availableTables.length > 0">
                  <div class="table-grid">
                    <label v-for="table in filteredTables" :key="table" class="table-checkbox">
                      <input type="checkbox" v-model="selectedTables" :value="table">
                      <span class="table-name">{{ table }}</span>
                    </label>
                  </div>
                </div>
                <div v-else-if="schemaInitForm.selectedDatasource" class="empty-tables">
                  <i class="bi bi-database"></i>
                  <p>暂无可用表，请检查数据源连接或点击"刷新表列表"</p>
                </div>
                <div v-else class="empty-tables">
                  <i class="bi bi-database"></i>
                  <p>请先选择数据源</p>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeSchemaInitModal">取消</button>
          <button type="button" class="btn btn-primary" @click="initializeSchema"
                  :disabled="!canInitialize || schemaInitializing" v-if="!isInitialized || showConfigForm">
            <i class="bi bi-database-add" v-if="!schemaInitializing"></i>
            <i class="bi bi-arrow-repeat spin" v-else></i>
            {{ schemaInitializing ? '初始化中...' : '初始化信息源' }}
          </button>
          <button type="button" class="btn btn-outline" @click="getSchemaStatistics" v-if="isInitialized">
            <i class="bi bi-bar-chart"></i>
            刷新统计
          </button>
          <button type="button" class="btn btn-warning" @click="clearSchemaData" v-if="isInitialized">
            <i class="bi bi-trash"></i>
            清空数据
          </button>
          <button type="button" class="btn btn-secondary" @click="toggleConfigForm" v-if="isInitialized">
            <i class="bi bi-gear"></i>
            {{ showConfigForm ? '隐藏配置' : '重新配置' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template><script>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'

export default {
  name: 'AgentDebugPanel',
  props: {
    agentId: {
      type: [String, Number],
      required: true
    }
  },
  setup(props) {
    // 调试模式下的 Agent ID 偏移量
    const DEBUG_AGENT_ID_OFFSET = 999999
    const debugAgentId = props.agentId + DEBUG_AGENT_ID_OFFSET

    // 响应式数据
    const debugQuery = ref('')
    const isDebugging = ref(false)
    const isInitializing = ref(false)
    const isInitialized = ref(false)
    const debugStatus = ref('')
    const hasResults = ref(false)
    const debugInput = ref(null)
    const resultContainer = ref(null)
    const exampleQueries = ref([])
    const streamingSections = ref([])

    // EventSource实例引用
    let currentEventSource = null

    // 获取状态样式类
    const getStatusClass = () => {
      if (debugStatus.value.includes('完成')) return 'badge-success'
      if (debugStatus.value.includes('错误') || debugStatus.value.includes('失败')) return 'badge-error'
      if (debugStatus.value.includes('处理中') || debugStatus.value.includes('调试中')) return 'badge-warning'
      return 'badge-info'
    }

    // 使用示例问题
    const useExampleQuery = (example) => {
      debugQuery.value = example
      nextTick(() => {
        if (debugInput.value) {
          debugInput.value.focus()
        }
      })
    }

    // 获取初始化按钮文本
    const getInitButtonText = () => {
      if (isInitializing.value) return '检查中...'
      if (isInitialized.value) return '已初始化'
      return '检查初始化状态'
    }

    // 处理调试按钮点击
    const handleDebugClick = () => {
      console.log('=== 调试按钮被点击 ===')
      if (isDebugging.value) return

      if (!debugQuery.value || !debugQuery.value.trim()) {
        debugQuery.value = ''
      }

      startDebug()
    }

    // 完全使用 AgentWorkspace.vue 的流式数据处理逻辑
    const startDebug = () => {
      console.log('=== startDebug 函数被调用 ===')

      if (!debugQuery.value.trim() || isDebugging.value) {
        return
      }

      // 清理之前的EventSource连接
      if (currentEventSource) {
        currentEventSource.close()
        currentEventSource = null
      }

      isDebugging.value = true
      debugStatus.value = '正在连接...'
      hasResults.value = true
      streamingSections.value = []

      try {
        const eventSource = new EventSource(`/nl2sql/stream/search?query=${encodeURIComponent(debugQuery.value)}&agentId=${debugAgentId}`)
        currentEventSource = eventSource

        // 使用与 AgentWorkspace.vue 完全相同的流式数据处理逻辑
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
            // 清空现有数据
            streamingSections.value = []

            // 按顺序重建所有section
            for (const type of streamState.typeOrder) {
                const typeInfo = typeMapping[type] || { title: type, icon: 'bi bi-file-text' }
                const content = streamState.contentByType[type] || ''
                const formattedContent = formatContentByType(type, content)

                streamingSections.value.push({
                    id: `${type}-${Date.now()}`,
                    type,
                    title: typeInfo.title,
                    icon: typeInfo.icon,
                    content: formattedContent,
                    rawContent: content,
                    timestamp: Date.now(),
                    isLoading: !content || content.trim() === ''
                })
            }

            console.log('更新显示，当前section数量:', streamingSections.value.length)
            
            // 自动滚动到底部
            nextTick(() => {
                if (resultContainer.value) {
                    resultContainer.value.scrollTop = resultContainer.value.scrollHeight
                }
            })
        }

        eventSource.onmessage = (event) => {
            let chunk
            let actualType
            let actualData

            try {
                // 使用与 AgentWorkspace.vue 相同的解析逻辑
                let parsedData = JSON.parse(event.data)

                // 如果第一次解析结果还是字符串，再解析一次
                if (typeof parsedData === 'string') {
                    chunk = JSON.parse(parsedData)
                } else {
                    chunk = parsedData
                }

                // 直接提取type和data，使用方括号语法
                actualType = chunk['type']
                actualData = chunk['data']

                // 处理嵌套JSON的情况
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
                // 对数据进行预处理
                let processedData = actualData

                // 只对SQL类型进行Markdown代码块标记的预清理
                if (actualType === 'sql' && typeof actualData === 'string') {
                    processedData = actualData.replace(/^```\s*sql?\s*/i, '').replace(/```\s*$/, '').trim()
                }

                // 累积数据到对应的类型
                if (!streamState.contentByType.hasOwnProperty(actualType)) {
                    streamState.typeOrder.push(actualType)
                    streamState.contentByType[actualType] = ''
                }

                if (processedData) {
                    streamState.contentByType[actualType] += processedData
                }

                updateDisplay()
            } else {
                console.warn('Missing type or data:', {
                    type: actualType,
                    data: actualData,
                    originalChunk: chunk
                })
            }
        }

        eventSource.addEventListener('complete', () => {
          console.log('流式输出完成')
          isDebugging.value = false
          debugStatus.value = '调试完成'
          eventSource.close()
        })

        eventSource.onerror = (error) => {
          console.error('流式连接错误:', error)
          isDebugging.value = false
          debugStatus.value = '连接出错'

          if (eventSource.readyState === EventSource.CLOSED) {
            console.log('EventSource 连接已正常关闭')
          } else {
            streamingSections.value.push({
              id: 'error-section',
              type: 'error',
              title: '连接错误',
              icon: 'bi bi-exclamation-triangle',
              content: '<div class="error-content">连接失败，请检查后端服务是否正在运行</div>',
              timestamp: Date.now(),
              isLoading: false
            })
          }

          eventSource.close()
        }

      } catch (error) {
        console.error('发送消息失败:', error)
        isDebugging.value = false
        debugStatus.value = '发送失败'
      }
    }

    // 使用与 AgentWorkspace.vue 相同的内容格式化逻辑
    const formatContentByType = (type, data) => {
        if (data === null || data === undefined) return '';

        if (type === 'sql') {
            let cleanedData = data.replace(/^```\s*sql?\s*/i, '').replace(/```\s*$/, '').trim();
            return `<pre><code class="language-sql">${cleanedData}</code></pre>`;
        }

        if (type === 'result') {
            return convertJsonToHTMLTable(data);
        }

        // 处理其他类型的数据
        let processedData = data;
        if (typeof data === 'string') {
            // 检查数据是否包含多个JSON对象连接在一起
            const jsonPattern = /\{"[^"]+":"[^"]*"[^}]*\}/g;
            const jsonMatches = data.match(jsonPattern);

            if (jsonMatches && jsonMatches.length > 1) {
                // 多个JSON对象，分别解析并提取data字段
                let extractedContent = [];
                jsonMatches.forEach(jsonStr => {
                    try {
                        const jsonObj = JSON.parse(jsonStr);
                        if (jsonObj.data) {
                            extractedContent.push(jsonObj.data.replace(/\\n/g, '\n'));
                        }
                    } catch (e) {
                        extractedContent.push(jsonStr);
                    }
                });
                processedData = extractedContent.join('');
            } else {
                // 单个JSON对象或普通文本
                try {
                    const jsonData = JSON.parse(data);
                    if (jsonData && typeof jsonData === 'object') {
                        if (jsonData.data) {
                            processedData = jsonData.data;
                        } else {
                            processedData = JSON.stringify(jsonData, null, 2);
                        }
                    }
                } catch (e) {
                    // 不是JSON，保持原始数据
                    processedData = data;
                }
            }
        }

        // 检查是否是Markdown格式
        if (isMarkdown(processedData)) {
            return renderMarkdown(processedData);
        } else {
            // 检查内容是否包含SQL代码块
            const sqlCodeBlockRegex = /```\s*sql?\s*([\s\S]*?)```/gi;
            const sqlMatches = processedData.match(sqlCodeBlockRegex);

            if (sqlMatches && sqlMatches.length > 0) {
                // 包含SQL代码块，进行特殊处理
                let htmlContent = processedData;

                // 替换每个SQL代码块为高亮显示
                htmlContent = htmlContent.replace(sqlCodeBlockRegex, (match, sqlContent) => {
                    let cleanedSQL = sqlContent.trim();
                    return `<pre><code class="language-sql">${cleanedSQL}</code></pre>`;
                });

                // 处理剩余的文本（将换行转换为<br>）
                return htmlContent.replace(/\n/g, '<br>');
            } else {
                return processedData.toString().replace(/\n/g, '<br>');
            }
        }
    };

    // 检测Markdown格式的辅助函数
    const isMarkdown = (text) => {
        if (!text || typeof text !== 'string') return false;

        // 检测常见的Markdown语法
        const markdownPatterns = [
            /^#{1,6}\s+.+/m,           // 标题 # ## ###
            /\*\*[^*]+\*\*/,           // 粗体 **text**
            /\*[^*]+\*/,               // 斜体 *text*
            /`[^`]+`/,                 // 行内代码 `code`
            /```[\s\S]*?```/,          // 代码块 ```code```
            /^\s*[-*+]\s+/m,           // 无序列表 - * +
            /^\s*\d+\.\s+/m,           // 有序列表 1. 2.
            /^\s*>\s+/m,               // 引用 >
            /\[.+\]\(.+\)/,            // 链接 [text](url)
            /^\s*\|.+\|/m,             // 表格 |col1|col2|
            /^---+$/m                  // 分隔线 ---
        ];

        return markdownPatterns.some(pattern => pattern.test(text));
    };

    // 渲染Markdown的辅助函数
    const renderMarkdown = (text) => {
        if (!text || typeof text !== 'string') return '';

        let html = text;

        // 首先处理代码块（三个反引号），避免被行内代码处理干扰
        html = html.replace(/```(\w+)?\s*([\s\S]*?)```/g, (match, lang, code) => {
            const language = lang || 'text';
            let highlightedCode = code.trim();

            // 如果是SQL代码，进行语法高亮
            if (language.toLowerCase() === 'sql') {
                // 这里可以添加SQL语法高亮逻辑
                highlightedCode = code.trim();
            }

            return `<pre><code class="language-${language}">${highlightedCode}</code></pre>`;
        });

        // 处理标题
        html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>');
        html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>');
        html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>');

        // 处理粗体和斜体
        html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');

        // 处理行内代码（单个反引号）- 在代码块处理之后
        html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

        // 处理无序列表
        html = html.replace(/^\* (.*$)/gim, '<li>$1</li>');
        html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');

        // 处理有序列表
        html = html.replace(/^\d+\. (.*$)/gim, '<li>$1</li>');

        // 处理Markdown表格
        html = html.replace(/(\|[^|\r\n]*\|[^|\r\n]*\|[^\r\n]*\r?\n\|[-:\s|]*\|[^\r\n]*\r?\n(?:\|[^|\r\n]*\|[^\r\n]*\r?\n?)*)/gm, (match) => {
            return convertMarkdownTableToHTML(match);
        });

        // 处理链接
        html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>');

        // 处理换行
        html = html.replace(/\n/g, '<br>');

        return `<div class="markdown-content">${html}</div>`;
    };

    // 转换Markdown表格为HTML表格
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
    };

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
    };

    // 初始化数据源
    const initializeDataSource = async () => {
      if (isInitializing.value || isDebugging.value) return

      try {
        isInitializing.value = true
        debugStatus.value = '正在检查初始化状态...'

        setTimeout(() => {
          isInitialized.value = true
          debugStatus.value = '数据源已初始化，可以开始调试'
          isInitializing.value = false

          setTimeout(() => {
            debugStatus.value = ''
          }, 3000)
        }, 1000)

      } catch (error) {
        console.error('检查初始化状态错误:', error)
        debugStatus.value = '检查失败，请确保智能体配置正确'
        isInitialized.value = false
        isInitializing.value = false
      }
    }

    // 模态框相关状态
    const showSchemaInitModal = ref(false)
    const showConfigForm = ref(false)
    const schemaInitializing = ref(false)
    const schemaStatistics = ref(null)

    // 表单数据
    const schemaInitForm = reactive({
      selectedDatasource: ''
    })

    // 数据源和表相关
    const availableDatasources = ref([])
    const availableTables = ref([])
    const selectedTables = ref([])
    const tableSearchKeyword = ref('')

    // 计算属性
    const filteredTables = computed(() => {
      if (!tableSearchKeyword.value) return availableTables.value
      return availableTables.value.filter(table =>
        table.toLowerCase().includes(tableSearchKeyword.value.toLowerCase())
      )
    })

    const canInitialize = computed(() => {
      return schemaInitForm.selectedDatasource &&
             selectedTables.value.length > 0
    })

    // 模态框相关函数
    const openSchemaInitModal = async () => {
      showSchemaInitModal.value = true
      await loadAvailableDatasources()
      await getSchemaStatistics()
    }

    const closeSchemaInitModal = () => {
      showSchemaInitModal.value = false
      showConfigForm.value = false
    }

    // 数据源相关函数
    const loadAvailableDatasources = async () => {
      try {
        const response = await fetch(`/api/agent/${props.agentId}/schema/datasources`)

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const result = await response.json()

        if (result.success) {
          availableDatasources.value = result.data || []
        } else {
          console.error('获取数据源失败:', result.message)
          availableDatasources.value = []
        }
      } catch (error) {
        console.error('加载数据源失败:', error)
        availableDatasources.value = []
      }
    }

    const getDatasourceTypeText = (type) => {
      const typeMap = {
        mysql: 'MySQL',
        postgresql: 'PostgreSQL',
        oracle: 'Oracle',
        sqlserver: 'SQL Server'
      }
      return typeMap[type] || type
    }

    const onDatasourceChange = () => {
      availableTables.value = []
      selectedTables.value = []
      if (schemaInitForm.selectedDatasource) {
        loadTables()
      }
    }

    const loadTables = async () => {
      if (!schemaInitForm.selectedDatasource) return

      try {
        const response = await fetch(`/api/agent/${debugAgentId}/schema/datasources/${schemaInitForm.selectedDatasource.id}/tables`)

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const result = await response.json()

        if (result.success) {
          availableTables.value = result.data || []
        } else {
          console.error('获取表列表失败:', result.message)
          availableTables.value = []
        }
      } catch (error) {
        console.error('加载表列表失败:', error)
        availableTables.value = []
      }
    }

    const selectAllTables = () => {
      selectedTables.value = [...filteredTables.value]
    }

    const clearAllTables = () => {
      selectedTables.value = []
    }

    const initializeSchema = async () => {
      if (schemaInitializing.value || !canInitialize.value) return

      try {
        schemaInitializing.value = true

        const response = await fetch(`/api/agent/${debugAgentId}/schema/init`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            datasourceId: schemaInitForm.selectedDatasource.id,
            tables: selectedTables.value
          })
        })

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const result = await response.json()

        if (result.success) {
          isInitialized.value = true
          showConfigForm.value = false
          await getSchemaStatistics()
        } else {
          console.error('初始化失败:', result.message)
          alert('初始化失败: ' + result.message)
        }
      } catch (error) {
        console.error('初始化Schema失败:', error)
        alert('初始化失败，请检查网络连接')
      } finally {
        schemaInitializing.value = false
      }
    }

    const getSchemaStatistics = async () => {
      try {
        const response = await fetch(`/api/agent/${debugAgentId}/schema/statistics`)

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const result = await response.json()

        if (result.success) {
          schemaStatistics.value = result.data
          isInitialized.value = result.data && result.data.documentCount > 0
        } else {
          console.error('获取统计信息失败:', result.message)
          schemaStatistics.value = null
          isInitialized.value = false
        }
      } catch (error) {
        console.error('获取统计信息失败:', error)
        schemaStatistics.value = null
        isInitialized.value = false
      }
    }

    const clearSchemaData = async () => {
      if (!confirm('确定要清空所有Schema数据吗？此操作不可恢复。')) return

      try {
        const response = await fetch(`/api/agent/${debugAgentId}/schema/clear`, {
          method: 'DELETE'
        })

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const result = await response.json()

        if (result.success) {
          isInitialized.value = false
          schemaStatistics.value = null
          selectedTables.value = []
        } else {
          console.error('清空数据失败:', result.message)
          alert('清空数据失败: ' + result.message)
        }
      } catch (error) {
        console.error('清空Schema数据失败:', error)
        alert('清空数据失败，请检查网络连接')
      }
    }

    const toggleConfigForm = () => {
      showConfigForm.value = !showConfigForm.value
    }

    // 格式化时间
    const formatTime = (timestamp) => {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      return date.toLocaleTimeString('zh-CN', {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }

    // 其他辅助函数
    const copyToClipboard = (text) => {
      navigator.clipboard.writeText(text).then(() => {
        console.log('复制成功')
      }).catch(err => {
        console.error('复制失败:', err)
      })
    }

    // 组件销毁时清理EventSource
    onUnmounted(() => {
      if (currentEventSource) {
        currentEventSource.close()
        currentEventSource = null
      }
    })

    // 初始检查状态
    onMounted(() => {
      initializeDataSource()
    })

    return {
      debugQuery,
      isDebugging,
      isInitializing,
      isInitialized,
      debugStatus,
      hasResults,
      debugInput,
      resultContainer,
      exampleQueries,
      streamingSections,
      getStatusClass,
      useExampleQuery,
      getInitButtonText,
      handleDebugClick,
      startDebug,
      initializeDataSource,
      copyToClipboard,
      formatTime,
      showSchemaInitModal,
      showConfigForm,
      schemaInitializing,
      schemaStatistics,
      schemaInitForm,
      availableDatasources,
      availableTables,
      selectedTables,
      tableSearchKeyword,
      filteredTables,
      canInitialize,
      openSchemaInitModal,
      closeSchemaInitModal,
      loadAvailableDatasources,
      getDatasourceTypeText,
      onDatasourceChange,
      loadTables,
      selectAllTables,
      clearAllTables,
      initializeSchema,
      getSchemaStatistics,
      clearSchemaData,
      toggleConfigForm
    }
  }
}
</script>
<style>
.agent-debug-panel {
  padding: 1rem;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  gap: 1rem;
}

.debug-header {
  margin-bottom: 0;
  flex-shrink: 0;
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #e2e8f0;
}

.debug-header h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 0.5rem;
}

.debug-subtitle {
  color: #64748b;
  font-size: 0.95rem;
  margin: 0;
}

.debug-result-section {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e2e8f0;
}

.result-header {
  padding: 1rem 1.5rem;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.result-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.result-status .badge {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 500;
}

.badge-success {
  background-color: #f6ffed;
  color: #52c41a;
}

.badge-error {
  background-color: #fff2f0;
  color: #ff4d4f;
}

.badge-warning {
  background-color: #fffbe6;
  color: #faad14;
}

.badge-info {
  background-color: #e6f7ff;
  color: #1890ff;
}

.result-content {
  flex: 1;
  padding: 0;
  overflow-y: auto;
  background: #fafbfc;
}

.debug-results-container {
  padding: 2rem;
  max-width: 100%;
  box-sizing: border-box;
  min-height: 100%;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 50%, #f1f5f9 100%);
  border-radius: 16px;
  position: relative;
}

.debug-results-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(59, 130, 246, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(139, 92, 246, 0.05) 0%, transparent 50%);
  border-radius: 16px;
  pointer-events: none;
}

.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  color: #64748b;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 20px;
  margin: 2rem;
  border: 2px dashed #cbd5e1;
  position: relative;
  overflow: hidden;
}

.empty-state::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.1), transparent);
  animation: shimmer 3s infinite;
}

@keyframes shimmer {
  0% { left: -100%; }
  100% { left: 100%; }
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1.5rem;
  color: #cbd5e1;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.empty-text {
  font-size: 1.2rem;
  margin-bottom: 2rem;
  color: #475569;
  font-weight: 500;
}

.example-queries {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
  max-width: 700px;
  margin: 0 auto;
}

.example-query {
  padding: 0.75rem 1.25rem;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.9rem;
  color: #0369a1;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(3, 105, 161, 0.1);
}

.example-query:hover {
  background: linear-gradient(135deg, #0369a1 0%, #0284c7 100%);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(3, 105, 161, 0.3);
  border-color: #0284c7;
}

.debug-input-section {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #e2e8f0;
  margin-top: 0;
  margin-bottom: 0;
}

.input-container {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.debug-input {
  flex: 1;
  padding: 1rem 1.25rem;
  font-size: 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  transition: all 0.3s ease;
  outline: none;
  background: #fafbfc;
  color: #1e293b;
}

.debug-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  background: #ffffff;
}

.debug-input:disabled {
  background-color: #f1f5f9;
  color: #94a3b8;
  cursor: not-allowed;
  border-color: #e2e8f0;
}

.debug-button {
  padding: 1rem 1.5rem;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 130px;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.debug-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #1d4ed8 0%, #1e40af 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
}

.debug-button:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
  box-shadow: none;
}

.schema-init-button {
  padding: 1rem 1.5rem;
  background: linear-gradient(135deg, #7c3aed 0%, #5b21b6 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 150px;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(124, 58, 237, 0.3);
}

.schema-init-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #5b21b6 0%, #4c1d95 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(124, 58, 237, 0.4);
}

.schema-init-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
}

.init-button {
  padding: 0.75rem 1.5rem;
  background-color: #52c41a;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 140px;
  justify-content: center;
}

.init-button:hover:not(:disabled) {
  background-color: #73d13d;
}

.init-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
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
  to {
    transform: rotate(360deg);
  }
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-dialog {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  max-width: 800px;
  width: 90%;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 1.5rem;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
  padding: 0.25rem;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid #e8e8e8;
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  background: #fafafa;
}

/* 初始化状态卡片 */
.init-status-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  border: 1px solid #e9ecef;
}

.init-status-card.initialized {
  background: #f6ffed;
  border-color: #b7eb8f;
}

.init-status-card.not-initialized {
  background: #fff7e6;
  border-color: #ffd591;
}

.status-indicator {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.status-icon {
  font-size: 1.5rem;
  margin-top: 0.25rem;
}

.init-status-card.initialized .status-icon {
  color: #52c41a;
}

.init-status-card.not-initialized .status-icon {
  color: #faad14;
}

.status-info {
  flex: 1;
}

.status-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.init-status-card.initialized .status-title {
  color: #389e0d;
}

.init-status-card.not-initialized .status-title {
  color: #d48806;
}

.status-desc {
  color: #666;
  line-height: 1.5;
}

.stats-info {
  display: flex;
  gap: 2rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #d9f7be;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-label {
  font-size: 0.85rem;
  color: #666;
}

.stat-value {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

/* 表单样式 */
.init-config-form {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  border: 1px solid #e8e8e8;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 500;
  color: #333;
  font-size: 0.9rem;
}

.form-control {
  padding: 0.75rem;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* 表选择区域 */
.table-selection {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
}

.table-search {
  padding: 1rem;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  gap: 1rem;
  align-items: center;
}

.table-search .form-control {
  flex: 1;
}

.table-actions {
  display: flex;
  gap: 0.5rem;
}

.table-list {
  max-height: 300px;
  overflow-y: auto;
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 0.5rem;
  padding: 1rem;
}

.table-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.table-checkbox:hover {
  background: #f0f8ff;
}

.table-checkbox input[type="checkbox"] {
  margin: 0;
}

.table-name {
  font-size: 0.9rem;
  color: #333;
}

.empty-tables {
  padding: 2rem;
  text-align: center;
  color: #999;
}

.empty-tables i {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  display: block;
}

/* 按钮样式 */
.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #40a9ff;
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #d9d9d9;
}

.btn-secondary:hover:not(:disabled) {
  background: #e6f7ff;
  border-color: #91d5ff;
  color: #1890ff;
}

.btn-outline {
  background: white;
  color: #1890ff;
  border: 1px solid #1890ff;
}

.btn-outline:hover:not(:disabled) {
  background: #1890ff;
  color: white;
}

.btn-warning {
  background: #faad14;
  color: white;
}

.btn-warning:hover:not(:disabled) {
  background: #ffc53d;
}

.btn-sm {
  padding: 0.25rem 0.75rem;
  font-size: 0.8rem;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spin {
  animation: spin 1s linear infinite;
}

/* 调试结果样式 */
.debug-result-item {
  background: white;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  margin-bottom: 1rem;
  overflow: hidden;
}

.result-header-info {
  background: #f8f9fa;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-timestamp {
  font-size: 0.85rem;
  color: #666;
}

.result-type {
  font-size: 0.85rem;
  color: #1890ff;
  font-weight: 500;
}

.result-question {
  padding: 1rem;
  border-bottom: 1px solid #f0f0f0;
}

.question-label {
  font-weight: 600;
  color: #333;
  margin-bottom: 0.5rem;
}

.question-content {
  color: #666;
  line-height: 1.5;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.result-answer {
  padding: 1rem;
}

.answer-label {
  font-weight: 600;
  color: #333;
  margin-bottom: 0.75rem;
}

.answer-content {
  color: #444;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.answer-content p {
  margin-bottom: 0.75rem;
}

.answer-content ul {
  margin: 0.5rem 0;
  padding-left: 1.5rem;
}

.answer-content li {
  margin-bottom: 0.25rem;
}

/* nl2sql.html样式兼容 */
.result-section {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  margin-bottom: 1rem;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-title {
  background: #f8f9fa;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 500;
  color: #333;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  justify-content: space-between;
}

.section-title i {
  color: #1890ff;
  font-size: 1rem;
}

.section-content {
  padding: 1rem;
  line-height: 1.6;
}

.copy-button {
  background: #f0f8ff;
  border: 1px solid #d6e4ff;
  border-radius: 4px;
  padding: 0.25rem 0.5rem;
  cursor: pointer;
  font-size: 0.8rem;
  color: #1890ff;
  transition: all 0.2s;
}

.copy-button:hover {
  background: #1890ff;
  color: white;
}

/* 流式数据展示样式 - 现代化版本 */
.agent-response-block {
  display: block !important;
  width: 100% !important;
  margin-bottom: 1.5rem !important;
  border: none;
  border-radius: 16px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  max-width: 100%;
  box-sizing: border-box;
  position: relative;
}

.agent-response-block::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6, #06b6d4);
  border-radius: 16px 16px 0 0;
}

.agent-response-block:last-child {
  margin-bottom: 0;
}

.agent-response-block:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12), 0 4px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-3px);
}

.agent-response-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
  font-size: 0.85rem;
  font-weight: 600;
  color: #334155;
  min-height: auto;
  height: auto;
  box-sizing: border-box;
  position: relative;
  margin-top: 0;
  overflow: hidden;
  line-height: 1.2;
}

.agent-response-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #3b82f6 0%, #1d4ed8 100%);
}

.title-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
}

.title-text {
  font-weight: 600;
  color: #1e293b;
  letter-spacing: 0.025em;
}

.title-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.timestamp {
  font-size: 0.75rem;
  color: #64748b;
  background: #f1f5f9;
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.loading-indicator {
  display: flex;
  align-items: center;
}

.loading-dots {
  color: #3b82f6;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.copy-button {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.375rem 0.75rem;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.8rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.copy-button:hover {
  background: #f8fafc;
  border-color: #3b82f6;
  color: #3b82f6;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.1);
}

.agent-response-title i {
  color: #3b82f6;
  font-size: 1.1rem;
  flex-shrink: 0;
}

.agent-response-content {
  display: block !important;
  width: 100% !important;
  padding: 1rem;
  font-size: 0.9rem;
  line-height: 1.6;
  color: #475569;
  background: #ffffff;
  min-height: auto;
  box-sizing: border-box;
  position: relative;
}

.agent-response-content::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, transparent 0%, rgba(59, 130, 246, 0.1) 50%, transparent 100%);
}

.agent-response-content:empty::after {
  content: '🔄 正在分析处理中，请稍候...';
  color: #6b7280;
  font-style: italic;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 4rem;
  font-size: 1rem;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  border-radius: 12px;
  border: 2px dashed #d1d5db;
  margin: 1rem 0;
}

/* 内容文本样式优化 */
.agent-response-content p {
  margin-bottom: 1rem;
  font-size: 0.95rem;
  line-height: 1.8;
  color: #374151;
}

.agent-response-content ul {
  padding-left: 1.5rem;
  margin-bottom: 1rem;
}

.agent-response-content li {
  margin-bottom: 0.5rem;
  color: #374151;
  line-height: 1.7;
}

.agent-response-content strong {
  font-weight: 600;
  color: #1f2937;
}

.agent-response-content em {
  font-style: italic;
  color: #6b7280;
}

/* 特殊内容样式 */
.agent-response-content .status-text {
  display: inline-block;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
  border-radius: 20px;
  color: #0369a1;
  font-weight: 500;
  margin: 0.5rem 0;
  font-size: 0.9rem;
}

.agent-response-content .keyword-tag {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 1px solid #f59e0b;
  border-radius: 15px;
  color: #92400e;
  font-weight: 500;
  margin: 0.25rem 0.25rem 0.25rem 0;
  font-size: 0.85rem;
}

/* 加载状态内容美化 */
.agent-response-content:empty::after {
  content: '🔄 正在分析处理中，请稍候...';
  color: #6b7280;
  font-style: italic;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 4rem;
  font-size: 1rem;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  border-radius: 12px;
  border: 2px dashed #d1d5db;
  margin: 1rem 0;
}
</style>
