<template>
  <div class="agent-debug-panel">
    <!-- 调试头部 -->
    <div class="debug-header">
      <h2>智能体调试</h2>
      <p class="debug-subtitle">测试智能体的响应能力和配置正确性</p>
    </div>

    <!-- 调试结果区域 -->
    <div class="debug-result-section">
      <div class="result-header">
        <div class="result-title">
          <i class="bi bi-terminal"></i>
          调试结果
        </div>
        <div class="result-status" v-if="debugStatus">
          <span class="badge" :class="getStatusClass()">{{ debugStatus }}</span>
        </div>
      </div>
      <div class="result-content" ref="resultContainer">
        <!-- 空状态 -->
        <div v-if="!hasResults" class="empty-state">
          <div class="empty-icon">
            <i class="bi bi-chat-square-text"></i>
          </div>
          <div class="empty-text">
            输入测试问题，查看智能体的响应结果
          </div>
          <div class="example-queries" v-if="exampleQueries.length > 0">
            <div 
              class="example-query" 
              v-for="example in exampleQueries" 
              :key="example"
              @click="useExampleQuery(example)"
            >
              {{ example }}
            </div>
          </div>
        </div>
        
        <!-- 结果展示 -->
        <div v-else class="debug-results-container">
          <!-- 流式结果区块 - 使用与 AgentWorkspace.vue 相同的结构 -->
          <div v-for="section in streamingSections" :key="section.id" class="agent-response-block">
            <div class="agent-response-title">
              <i :class="section.icon"></i> {{ section.title }}
              <button 
                v-if="section.type === 'sql'" 
                class="copy-button" 
                @click="copyToClipboard(section.rawContent)"
                title="复制SQL"
              >
                <i class="bi bi-clipboard"></i>
              </button>
            </div>
            <div class="agent-response-content" v-html="section.content"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 调试输入区域 -->
    <div class="debug-input-section">
      <div class="input-container">
        <input 
          type="text" 
          v-model="debugQuery" 
          class="debug-input" 
          placeholder="请输入测试问题..."
          :disabled="isDebugging || isInitializing"
          @keyup.enter="startDebug"
          ref="debugInput"
        >
        <button 
          class="debug-button" 
          :disabled="isDebugging"
          @click="handleDebugClick"
        >
          <i class="bi bi-play-circle" v-if="!isDebugging"></i>
          <div class="spinner" v-else></div>
          {{ isDebugging ? '调试中...' : '开始调试' }}
        </button>
        <button 
          class="schema-init-button" 
          :disabled="isDebugging || isInitializing"
          @click="openSchemaInitModal"
        >
          <i class="bi bi-database-gear"></i>
          初始化信息源
        </button>
        <button 
          class="init-button" 
          :disabled="isInitializing || isDebugging"
          :class="{ loading: isInitializing }"
          @click="initializeDataSource"
        >
          <i class="bi bi-database-add" v-if="!isInitializing && !isInitialized"></i>
          <i class="bi bi-check-circle" v-if="!isInitializing && isInitialized"></i>
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
    // 响应式数据
    const debugQuery = ref('查询用户总数')
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
        debugQuery.value = '查询用户总数'
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
        const eventSource = new EventSource(`/nl2sql/stream/search?query=${encodeURIComponent(debugQuery.value)}&agentId=${props.agentId}`)
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
                    timestamp: new Date().toLocaleTimeString()
                })
            }
            
            console.log('更新显示，当前section数量:', streamingSections.value.length)
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
              content: '<div class="error-content">连接失败，请检查后端服务是否正在运行</div>'
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
        const response = await fetch(`/api/agent/${props.agentId}/schema/tables`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            datasourceId: schemaInitForm.selectedDatasource.id
          })
        })
        
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
        
        const response = await fetch(`/api/agent/${props.agentId}/schema/initialize`, {
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
        const response = await fetch(`/api/agent/${props.agentId}/schema/statistics`)
        
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
        const response = await fetch(`/api/agent/${props.agentId}/schema/clear`, {
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
<style scoped>
.agent-debug-panel {
  padding: 0;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.debug-header {
  margin-bottom: 1rem;
  flex-shrink: 0;
}

.debug-header h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.5rem;
}

.debug-subtitle {
  color: #666;
  font-size: 0.95rem;
  margin: 0;
}

.debug-result-section {
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.result-header {
  padding: 1rem 1.5rem;
  background-color: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title {
  font-size: 1.1rem;
  font-weight: 500;
  color: #333;
  display: flex;
  align-items: center;
  gap: 0.5rem;
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
  padding: 1.5rem;
  overflow-y: auto;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: #999;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
  color: #ddd;
}

.empty-text {
  font-size: 1.1rem;
  margin-bottom: 2rem;
}

.example-queries {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
  max-width: 600px;
  margin: 0 auto;
}

.example-query {
  padding: 0.5rem 1rem;
  background: #f0f8ff;
  border: 1px solid #d6e4ff;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 0.9rem;
  color: #1890ff;
}

.example-query:hover {
  background: #1890ff;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.debug-input-section {
  margin-top: 1.5rem;
  margin-bottom: 0;
}

.input-container {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.debug-input {
  flex: 1;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  transition: all 0.3s;
  outline: none;
}

.debug-input:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.debug-input:disabled {
  background-color: #f5f5f5;
  color: #999;
  cursor: not-allowed;
  border-color: #ddd;
}

.debug-button {
  padding: 0.75rem 1.5rem;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 120px;
  justify-content: center;
}

.debug-button:hover:not(:disabled) {
  background-color: #40a9ff;
}

.debug-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
}

.schema-init-button {
  padding: 0.75rem 1.5rem;
  background-color: #722ed1;
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

.schema-init-button:hover:not(:disabled) {
  background-color: #9254de;
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

/* 流式数据展示样式 - 与 AgentWorkspace.vue 保持一致 */
.agent-response-block {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  margin-bottom: 1rem;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.agent-response-title {
  background: #f8f9fa;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.agent-response-title i {
  margin-right: 0.5rem;
  color: #1890ff;
}

.agent-response-content {
  padding: 1rem;
  line-height: 1.6;
}

.agent-response-content pre {
  background: #f6f8fa;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  padding: 1rem;
  margin: 0.5rem 0;
  overflow-x: auto;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.9rem;
}

.agent-response-content code {
  background: #f6f8fa;
  padding: 0.2rem 0.4rem;
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.9rem;
}

.agent-response-content pre code {
  background: transparent;
  padding: 0;
  border-radius: 0;
}

.debug-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
  margin: 0.5rem 0;
}

.debug-table th,
.debug-table td {
  border: 1px solid #e8e8e8;
  padding: 0.5rem 0.75rem;
  text-align: left;
}

.debug-table th {
  background: #f8f9fa;
  font-weight: 600;
  color: #333;
}

.debug-table tr:nth-child(even) {
  background: #fafafa;
}

.debug-table tr:hover {
  background: #f0f7ff;
}

.error-content {
  color: #ff4d4f;
  padding: 1rem;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  margin: 0.5rem 0;
}

/* dynamic-table 样式 - 与 AgentWorkspace.vue 保持一致 */
:deep(.dynamic-table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1rem 0;
  font-size: 0.9rem;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

:deep(.dynamic-table th),
:deep(.dynamic-table td) {
  border: 1px solid #e8e8e8;
  padding: 0.75rem;
  text-align: left;
  word-wrap: break-word;
}

:deep(.dynamic-table th) {
  background: #f8f9fa;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #e8e8e8;
}

:deep(.dynamic-table tr:nth-child(even)) {
  background: #fafafa;
}

:deep(.dynamic-table tr:hover) {
  background: #f0f7ff;
}

:deep(.dynamic-table tbody tr:last-child td) {
  border-bottom: none;
}

/* Markdown 内容样式 */
:deep(.markdown-content) {
  line-height: 1.7;
  color: #333;
}

:deep(.markdown-content h1),
:deep(.markdown-content h2),
:deep(.markdown-content h3) {
  margin: 1.5rem 0 1rem 0;
  color: #2c3e50;
  font-weight: 600;
  line-height: 1.4;
}

:deep(.markdown-content h1) {
  font-size: 1.8rem;
  border-bottom: 2px solid #e8e8e8;
  padding-bottom: 0.5rem;
}

:deep(.markdown-content h2) {
  font-size: 1.5rem;
  border-bottom: 1px solid #e8e8e8;
  padding-bottom: 0.3rem;
}

:deep(.markdown-content h3) {
  font-size: 1.3rem;
}

:deep(.markdown-content p) {
  margin: 0.8rem 0;
}

:deep(.markdown-content ul),
:deep(.markdown-content ol) {
  margin: 1rem 0;
  padding-left: 1.5rem;
}

:deep(.markdown-content li) {
  margin: 0.3rem 0;
}

:deep(.markdown-content code) {
  background: #f6f8fa;
  padding: 0.2rem 0.4rem;
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.85rem;
  color: #e83e8c;
}

:deep(.markdown-content pre) {
  background: #f6f8fa;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  padding: 1rem;
  margin: 1rem 0;
  overflow-x: auto;
}

:deep(.markdown-content pre code) {
  background: transparent;
  padding: 0;
  border-radius: 0;
  color: inherit;
  font-size: 0.9rem;
}

:deep(.markdown-content a) {
  color: #1890ff;
  text-decoration: none;
}

:deep(.markdown-content a:hover) {
  text-decoration: underline;
}

:deep(.markdown-content strong) {
  font-weight: 600;
  color: #2c3e50;
}

:deep(.markdown-content em) {
  font-style: italic;
  color: #666;
}

:deep(.markdown-content table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1rem 0;
  font-size: 0.9rem;
}

:deep(.markdown-content table th),
:deep(.markdown-content table td) {
  border: 1px solid #e8e8e8;
  padding: 0.75rem;
  text-align: left;
}

:deep(.markdown-content table th) {
  background: #f8f9fa;
  font-weight: 600;
}
</style>
