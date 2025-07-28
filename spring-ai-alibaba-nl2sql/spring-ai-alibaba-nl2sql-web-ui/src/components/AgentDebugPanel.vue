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
          <div class="example-queries">
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
        <div v-else id="debug-results-container">
          <!-- 动态生成的结果区块将在这里显示 -->
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
          :disabled="isDebugging || isInitializing || !debugQuery.trim()"
          @click="startDebug"
        >
          <i class="bi bi-play-circle" v-if="!isDebugging"></i>
          <div class="spinner" v-else></div>
          {{ isDebugging ? '调试中...' : '开始调试' }}
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
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { agentDebugApi } from '../utils/api.js'

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
    const debugQuery = ref('')
    const isDebugging = ref(false)
    const isInitializing = ref(false)
    const isInitialized = ref(false)
    const debugStatus = ref('')
    const hasResults = ref(false)
    const debugInput = ref(null)
    const resultContainer = ref(null)

    // 流式响应状态
    const streamState = reactive({
      eventSource: null,
      streamData: {},
      currentType: null,
      typeCounters: {},
      sectionCounter: 0
    })

    // 示例问题
    const exampleQueries = ref([
      '查询销售额最高的5个产品',
      '分析2024年的销售趋势',
      '统计各个分类的商品数量',
      '查询最近一个月的订单情况'
    ])

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

    // 重置调试状态
    const resetDebugState = () => {
      hasResults.value = false
      debugStatus.value = ''
      streamState.streamData = {}
      streamState.currentType = null
      streamState.typeCounters = {}
      streamState.sectionCounter = 0
      
      console.log('重置调试状态，清空所有计数器和状态') // 调试日志
      console.log('当前状态:', {
        currentType: streamState.currentType,
        typeCounters: streamState.typeCounters,
        sectionCounter: streamState.sectionCounter
      })
      
      // 清空结果容器
      const container = document.getElementById('debug-results-container')
      if (container) {
        container.innerHTML = ''
        console.log('已清空结果容器')
      }
    }

    // 开始调试
    const startDebug = async () => {
      if (!debugQuery.value.trim() || isDebugging.value) return

      try {
        isDebugging.value = true
        debugStatus.value = '正在处理中...'
        
        // 重置状态
        resetDebugState()
        hasResults.value = true

        // 关闭之前的连接
        if (streamState.eventSource) {
          streamState.eventSource.close()
        }

        // 创建新的EventSource连接 - 直接调用nl2sql接口
        streamState.eventSource = new EventSource(`/nl2sql/stream/search?query=${encodeURIComponent(debugQuery.value)}`)

        // 处理连接打开
        streamState.eventSource.onopen = () => {
          console.log('调试流连接已建立')
        }

        // 处理流式消息
        streamState.eventSource.onmessage = handleStreamMessage

        // 处理连接错误
        streamState.eventSource.onerror = handleStreamError

        // 处理完成事件
        streamState.eventSource.addEventListener('complete', handleStreamComplete)

      } catch (error) {
        console.error('启动调试失败:', error)
        handleDebugError('启动调试失败: ' + error.message)
      }
    }

    // 处理流式消息
    const handleStreamMessage = (event) => {
      try {
        debugStatus.value = ''
        
        console.log('收到流式数据:', event.data) // 调试日志
        
        let chunk
        let actualType
        let actualData
        
        try {
          // 尝试解析JSON
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
          return
        }

        console.log('解析后的数据:', { type: actualType, data: actualData }) // 调试日志

        // 修改过滤条件，允许空字符串通过
        if (actualType && actualData !== undefined && actualData !== null) {
          processStreamData(actualType, actualData)
        } else {
          console.log('数据被过滤:', { type: actualType, data: actualData })
        }

      } catch (error) {
        console.error('解析流式数据失败:', error, '原始数据:', event.data)
      }
    }

    // 处理流式数据 - 参考nl2sql.html的实现
    const processStreamData = (type, data) => {
      console.log(`Processing stream data - Type: ${type}, Current Type: ${streamState.currentType}`) // 调试日志
      
      // 对数据进行预处理
      let processedData = data
      
      // 只对SQL类型进行Markdown代码块标记的预清理
      if (type === 'sql' && typeof data === 'string') {
        processedData = data.replace(/^```\s*sql?\s*/i, '').replace(/```\s*$/, '').trim()
      }
      
      // 检查是否需要创建新的section
      if (streamState.currentType !== type) {
        // type切换了，创建新的section
        console.log(`Type changed from ${streamState.currentType} to ${type}, creating new section`)
        streamState.currentType = type

        // 先增加类型计数器
        if (!streamState.typeCounters[type]) {
          streamState.typeCounters[type] = 0
        }
        streamState.typeCounters[type]++

        // 创建新的section
        createResultSection(type, processedData)
        
        // 同时将第一条数据添加到streamState中
        const currentCount = streamState.typeCounters[type]
        const currentSectionKey = `${type}_${currentCount}`
        streamState.streamData[currentSectionKey] = processedData
        console.log(`Created new section for ${type}, count: ${currentCount}`)
      } else {
        // 同一个type，继续累积数据到当前section
        console.log(`Same type ${type}, appending data`)
        const currentCount = streamState.typeCounters[type]
        const currentSectionKey = `${type}_${currentCount}`

        // 累积数据
        if (!streamState.streamData[currentSectionKey]) {
          streamState.streamData[currentSectionKey] = ''
        }
        streamState.streamData[currentSectionKey] += processedData
        
        console.log(`Appending data to ${currentSectionKey}:`, processedData)
        console.log(`Total accumulated data for ${currentSectionKey}:`, streamState.streamData[currentSectionKey])

        // 更新当前section的内容
        updateResultSection(type, currentCount, streamState.streamData[currentSectionKey])

        console.log(`Updated section ${type}-${currentCount}-section with accumulated data`)
      }

      // 自动滚动到底部
      nextTick(() => {
        if (resultContainer.value) {
          resultContainer.value.scrollTop = resultContainer.value.scrollHeight
        }
      })
    }

    // 创建结果区块
    const createResultSection = (type, data) => {
      streamState.sectionCounter++

      // 注意：类型计数器已经在processStreamData中增加了，这里不需要再增加
      const currentCount = streamState.typeCounters[type]
      const sectionId = `${type}-${currentCount}-section`
      const contentId = `${type}-${currentCount}-content`

      // 获取类型信息
      const typeInfo = getTypeInfo(type)

      // 创建section HTML
      const sectionHTML = `
        <div id="${sectionId}" class="result-section">
          <div class="section-title">
            <i class="${typeInfo.icon}"></i> ${typeInfo.title} (${currentCount})
            ${type === 'sql' ? `<button class="copy-button" id="copy-${sectionId}-button" style="display: none;"><i class="bi bi-clipboard"></i></button>` : ''}
          </div>
          <div class="section-content" id="${contentId}"></div>
        </div>
      `

      // 添加到容器
      const container = document.getElementById('debug-results-container')
      if (container) {
        container.insertAdjacentHTML('beforeend', sectionHTML)
        
        // 立即更新内容
        updateResultSection(type, currentCount, data)
      }
    }

    // 更新结果区块
    const updateResultSection = (type, count, data) => {
      const sectionId = `${type}-${count}-section`
      const contentId = `${type}-${count}-content`
      
      const section = document.getElementById(sectionId)
      const content = document.getElementById(contentId)

      if (!section || !content) {
        console.error(`Element not found: section=${sectionId}, content=${contentId}`)
        return
      }

      section.style.display = 'block'

      // 根据类型格式化内容
      let formattedContent = formatContentByType(type, data, sectionId)
      content.innerHTML = formattedContent
    }

    // 根据类型格式化内容
    const formatContentByType = (type, data, sectionId) => {
      if (type === 'sql') {
        // 处理SQL内容
        const copyButton = document.getElementById(`copy-${sectionId}-button`)
        if (copyButton) {
          copyButton.style.display = 'inline-block'
          copyButton.setAttribute('data-content', data)
          copyButton.onclick = () => copyToClipboard(copyButton)
        }
        
        // 使用简单的SQL高亮
        let highlightedSQL = data
        if (window.hljs) {
          try {
            highlightedSQL = hljs.highlight(data, { language: 'sql' }).value
          } catch (e) {
            highlightedSQL = data
          }
        }
        
        return `<pre><code class="hljs language-sql">${highlightedSQL}</code></pre>`
      } else if (type === 'result') {
        // 处理结果数据
        return convertJsonToHTMLTable(data) || `<pre>${data}</pre>`
      } else {
        // 处理其他类型内容 - 参考nl2sql.html的逻辑
        let processedData = data
        
        if (typeof data === 'string') {
          // 先尝试按JSON对象分割
          const jsonPattern = /\{"[^"]+":"[^"]*"[^}]*\}/g
          const jsonMatches = data.match(jsonPattern)
          
          if (jsonMatches && jsonMatches.length > 1) {
            // 多个JSON对象，分别解析并提取data字段
            let extractedContent = []
            jsonMatches.forEach(jsonStr => {
              try {
                const jsonObj = JSON.parse(jsonStr)
                if (jsonObj.data) {
                  extractedContent.push(jsonObj.data.replace(/\\n/g, '\n'))
                }
              } catch (e) {
                extractedContent.push(jsonStr)
              }
            })
            processedData = extractedContent.join('')
          } else {
            // 单个JSON对象或普通文本
            try {
              const jsonData = JSON.parse(data)
              if (jsonData && typeof jsonData === 'object') {
                if (jsonData.data) {
                  processedData = jsonData.data
                } else {
                  processedData = JSON.stringify(jsonData, null, 2)
                }
              }
            } catch (e) {
              // 不是JSON，保持原始数据
              processedData = data
            }
          }
        }
        
        // 首先检查是否是Markdown格式（包含SQL代码块的也可能是Markdown）
        if (isMarkdown(processedData)) {
          // 使用markdown渲染器，它会自动处理SQL代码块和其他markdown语法
          return renderMarkdown(processedData)
        } else {
          // 检查内容是否包含SQL代码块（用于非markdown格式的SQL）
          const sqlCodeBlockRegex = /```\s*sql?\s*([\s\S]*?)```/gi
          const sqlMatches = processedData.match(sqlCodeBlockRegex)
          
          if (sqlMatches && sqlMatches.length > 0) {
            // 包含SQL代码块，进行特殊处理
            let htmlContent = processedData
            
            // 替换每个SQL代码块为高亮显示
            htmlContent = htmlContent.replace(sqlCodeBlockRegex, (match, sqlContent) => {
              let cleanedSQL = sqlContent.trim()
              let highlightedSQL = cleanedSQL
              
              if (window.hljs) {
                try {
                  highlightedSQL = hljs.highlight(cleanedSQL, { language: 'sql' }).value
                } catch (e) {
                  try {
                    highlightedSQL = hljs.highlightAuto(cleanedSQL).value
                  } catch (e2) {
                    highlightedSQL = cleanedSQL
                  }
                }
              }
              
              return `<pre><code class="hljs language-sql">${highlightedSQL}</code></pre>`
            })
            
            // 处理剩余的文本（将换行转换为<br>）
            return htmlContent.replace(/\n/g, '<br>')
          } else {
            return processedData.toString().replace(/\n/g, '<br>')
          }
        }
      }
    }

    // 获取类型信息
    const getTypeInfo = (type) => {
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
        'result': { title: '查询结果', icon: 'bi bi-table' }
      }

      return typeMapping[type] || { title: type, icon: 'bi bi-file-text' }
    }

    // 处理流式错误
    const handleStreamError = (error) => {
      console.error('调试流连接错误:', error)
      handleDebugError('连接错误，请重试')
    }

    // 处理流式完成
    const handleStreamComplete = () => {
      debugStatus.value = '调试完成'
      isDebugging.value = false
      
      if (streamState.eventSource) {
        streamState.eventSource.close()
        streamState.eventSource = null
      }
    }

    // 处理调试错误
    const handleDebugError = (message) => {
      debugStatus.value = message
      isDebugging.value = false
      
      if (streamState.eventSource) {
        streamState.eventSource.close()
        streamState.eventSource = null
      }
    }

    // 复制到剪贴板
    const copyToClipboard = (button) => {
      const content = button.getAttribute('data-content')
      if (navigator.clipboard) {
        navigator.clipboard.writeText(content).then(() => {
          const originalIcon = button.innerHTML
          button.innerHTML = '<i class="bi bi-check2"></i>'
          setTimeout(() => { button.innerHTML = originalIcon }, 2000)
        })
      }
    }

    // 检查是否是Markdown格式
    const isMarkdown = (text) => {
      if (!text || typeof text !== 'string') return false
      
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
      ]
      
      return markdownPatterns.some(pattern => pattern.test(text))
    }

    // 渲染Markdown
    const renderMarkdown = (text) => {
      if (!text || typeof text !== 'string') return ''
      
      let html = text
      
      // 首先处理代码块（三个反引号），避免被行内代码处理干扰
      html = html.replace(/```(\w+)?\s*([\s\S]*?)```/g, (match, lang, code) => {
        const language = lang || 'text'
        let highlightedCode = code.trim()
        
        // 如果是SQL代码，进行语法高亮
        if (language.toLowerCase() === 'sql' && window.hljs) {
          try {
            highlightedCode = hljs.highlight(code.trim(), { language: 'sql' }).value
          } catch (e) {
            highlightedCode = code.trim()
          }
        }
        
        return `<pre><code class="hljs language-${language}">${highlightedCode}</code></pre>`
      })
      
      // 处理标题
      html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>')
      html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>')
      html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>')
      
      // 处理粗体和斜体
      html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      html = html.replace(/\*(.*?)\*/g, '<em>$1</em>')
      
      // 处理行内代码（单个反引号）- 在代码块处理之后
      html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
      
      // 处理无序列表
      html = html.replace(/^\* (.*$)/gim, '<li>$1</li>')
      html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
      
      // 处理有序列表
      html = html.replace(/^\d+\. (.*$)/gim, '<li>$1</li>')
      
      // 处理Markdown表格
      html = html.replace(/(\|[^|\n]*\|[^|\n]*\|[^\n]*\n\|[-:\s|]*\|[^\n]*\n(?:\|[^|\n]*\|[^\n]*\n?)*)/gm, (match) => {
        return convertMarkdownTableToHTML(match)
      })
      
      // 处理链接
      html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
      
      // 处理换行
      html = html.replace(/\n/g, '<br>')
      
      return `<div class="markdown-content">${html}</div>`
    }

    // 转换JSON为HTML表格
    const convertJsonToHTMLTable = (jsonString) => {
      try {
        const data = JSON.parse(jsonString)
        if (!data || !data.columns || !data.data) {
          return `<pre>${jsonString}</pre>`
        }

        let html = '<table class="table"><thead><tr>'
        data.columns.forEach(header => {
          html += `<th>${header}</th>`
        })
        html += '</tr></thead><tbody>'

        data.data.forEach(row => {
          html += '<tr>'
          for (let i = 0; i < data.columns.length; i++) {
            html += `<td>${row[i] || ''}</td>`
          }
          html += '</tr>'
        })

        html += '</tbody></table>'
        return html
      } catch (e) {
        return `<pre>${jsonString}</pre>`
      }
    }

    // 转换Markdown表格为HTML表格
    const convertMarkdownTableToHTML = (markdownTable) => {
      if (!markdownTable) return ''
      const lines = markdownTable.trim().split('\n')
      if (lines.length < 2 || !lines[1].includes('---')) return markdownTable

      const headers = lines[0].split('|').map(h => h.trim()).filter(Boolean)
      let html = '<table class="table"><thead><tr>'
      headers.forEach(header => { 
        html += `<th>${header}</th>` 
      })
      html += '</tr></thead><tbody>'

      for (let i = 2; i < lines.length; i++) {
        const rowCells = lines[i].split('|').map(c => c.trim()).filter(Boolean)
        if (rowCells.length > 0) {
          html += '<tr>'
          for (let j = 0; j < headers.length; j++) {
            html += `<td>${rowCells[j] || ''}</td>`
          }
          html += '</tr>'
        }
      }
      html += '</tbody></table>'
      return html
    }

    // 获取初始化按钮文本
    const getInitButtonText = () => {
      if (isInitializing.value) return '初始化中...'
      if (isInitialized.value) return '重新初始化'
      return '初始化数据源'
    }

    // 初始化数据源
    const initializeDataSource = async () => {
      if (isInitializing.value || isDebugging.value) return

      try {
        isInitializing.value = true
        debugStatus.value = '正在初始化数据源...'

        const response = await fetch('/nl2sql/init', {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          }
        })

        if (response.ok) {
          const result = await response.text()
          isInitialized.value = true
          debugStatus.value = '数据源初始化完成'

          // 显示初始化成功提示
          showInitializationResult(true, result)

          setTimeout(() => {
            debugStatus.value = ''
          }, 3000)
        } else {
          throw new Error(`初始化失败: ${response.status}`)
        }
      } catch (error) {
        console.error('初始化错误:', error)
        debugStatus.value = '初始化失败'
        
        // 显示初始化失败提示
        showInitializationResult(false, error.message)
      } finally {
        isInitializing.value = false
      }
    }

    // 显示初始化结果
    const showInitializationResult = (success, message) => {
      const hasEmptyState = !hasResults.value
      
      if (hasEmptyState) {
        // 如果有空状态，在空状态上方添加初始化提示
        const resultContent = resultContainer.value
        if (resultContent) {
          const tipClass = success ? 'init-success-tip' : 'init-error-tip'
          const iconClass = success ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill'
          const badgeClass = success ? '' : 'error'
          const badgeText = success ? 'SUCCESS' : 'ERROR'
          const guideIcon = success ? 'bi-lightbulb' : 'bi-info-circle'
          const guideText = success 
            ? '现在您可以使用下面的推荐问题开始查询，或输入自己的问题'
            : '请检查网络连接或联系管理员，然后重试初始化'

          const tipHTML = `
            <div class="${tipClass}">
              <div class="init-tip-header">
                <i class="${iconClass}"></i>
                <span class="init-tip-title">${success ? '数据源初始化完成' : '数据源初始化失败'}</span>
                <span class="init-tip-badge ${badgeClass}">${badgeText}</span>
              </div>
              <div class="init-tip-content">
                <div class="init-tip-result">${message.replace(/\n/g, '<br>')}</div>
                <div class="init-tip-guide">
                  <i class="${guideIcon}"></i>
                  ${guideText}
                </div>
              </div>
            </div>
          `

          // 在空状态前插入提示
          const emptyState = resultContent.querySelector('.empty-state')
          if (emptyState) {
            emptyState.insertAdjacentHTML('beforebegin', tipHTML)
          }

          // 自动隐藏提示
          setTimeout(() => {
            const tip = resultContent.querySelector(`.${tipClass}`)
            if (tip) {
              tip.style.opacity = '0'
              tip.style.transform = 'translateY(-10px)'
              tip.style.transition = 'all 0.5s ease-out'
              setTimeout(() => {
                if (tip.parentNode) {
                  tip.parentNode.removeChild(tip)
                }
              }, 500)
            }
          }, success ? 5000 : 8000)
        }
      }
    }

    // 组件卸载时清理资源
    onUnmounted(() => {
      if (streamState.eventSource) {
        streamState.eventSource.close()
      }
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
      getStatusClass,
      getInitButtonText,
      useExampleQuery,
      startDebug,
      initializeDataSource
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

.debug-button:active:not(:disabled) {
  background-color: #096dd9;
}

.debug-button:disabled {
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

.init-button:active:not(:disabled) {
  background-color: #389e0d;
}

.init-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
}

.init-button.loading {
  position: relative;
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
  padding: 1rem;
  flex: 1;
  overflow-y: auto;
  line-height: 1.7;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  color: #999;
  text-align: center;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  color: #ccc;
}

.empty-text {
  font-size: 1rem;
  max-width: 400px;
  margin-bottom: 1.5rem;
}

.example-queries {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
}

.example-query {
  padding: 0.5rem 1rem;
  background-color: #f0f5ff;
  border-radius: 20px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #d6e4ff;
}

.example-query:hover {
  background-color: #d6e4ff;
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

/* 结果区块样式 */
:deep(.result-section) {
  margin-bottom: 1.5rem;
  animation: fadeIn 0.5s ease-in-out;
}

:deep(.section-title) {
  font-size: 1rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #555;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

:deep(.section-content) {
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 1rem;
  border: 1px solid #e8e8e8;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

:deep(.copy-button) {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 4px;
  transition: all 0.2s;
  margin-left: auto;
}

:deep(.copy-button:hover) {
  color: #1890ff;
  background-color: rgba(0, 0, 0, 0.05);
}

:deep(pre) {
  background-color: #282c34;
  color: #abb2bf;
  padding: 1rem;
  border-radius: 6px;
  overflow-x: auto;
  margin: 0;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

:deep(.table) {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1rem;
}

:deep(.table th),
:deep(.table td) {
  padding: 0.75rem;
  border: 1px solid #e8e8e8;
  text-align: left;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 200px;
}

:deep(.table th) {
  background-color: #fafafa;
  font-weight: 500;
}

:deep(.table tr:nth-child(even)) {
  background-color: #f9f9f9;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Markdown 内容样式 */
:deep(.markdown-content) {
  line-height: 1.6;
  color: #333;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

:deep(.markdown-content h1),
:deep(.markdown-content h2),
:deep(.markdown-content h3),
:deep(.markdown-content h4),
:deep(.markdown-content h5),
:deep(.markdown-content h6) {
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
  line-height: 1.25;
  color: #2c3e50;
}

:deep(.markdown-content h1) {
  font-size: 1.8rem;
  border-bottom: 2px solid #eee;
  padding-bottom: 0.5rem;
}

:deep(.markdown-content h2) {
  font-size: 1.5rem;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.3rem;
}

:deep(.markdown-content h3) {
  font-size: 1.3rem;
}

:deep(.markdown-content h4) {
  font-size: 1.1rem;
}

:deep(.markdown-content p) {
  margin-bottom: 1rem;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

:deep(.markdown-content ul),
:deep(.markdown-content ol) {
  margin-bottom: 1rem;
  padding-left: 2rem;
}

:deep(.markdown-content li) {
  margin-bottom: 0.25rem;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

:deep(.markdown-content blockquote) {
  margin: 1rem 0;
  padding: 0.5rem 1rem;
  border-left: 4px solid #1890ff;
  background-color: #f8f9fa;
  font-style: italic;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

:deep(.markdown-content code) {
  background-color: #f1f3f4;
  border-radius: 3px;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 0.9em;
  padding: 0.2em 0.4em;
}

:deep(.markdown-content pre) {
  background-color: #282c34;
  color: #abb2bf;
  border-radius: 6px;
  overflow-x: auto;
  padding: 1rem;
  margin: 1rem 0;
  border: 1px solid #444;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  white-space: pre-wrap;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

:deep(.markdown-content pre code) {
  background-color: transparent;
  color: inherit;
  padding: 0;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 0.9rem;
  line-height: 1.5;
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
}

:deep(.markdown-content em) {
  font-style: italic;
}

:deep(.markdown-content hr) {
  border: none;
  border-top: 1px solid #eee;
  margin: 2rem 0;
}

/* 初始化提示样式 */
:deep(.init-success-tip), :deep(.init-error-tip) {
  margin-bottom: 1.5rem;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  animation: slideInDown 0.5s ease-out;
  transition: all 0.5s ease-out;
}

:deep(.init-success-tip) {
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%);
}

:deep(.init-error-tip) {
  background: linear-gradient(135deg, #fff2f0 0%, #fff1f0 100%);
}

:deep(.init-tip-header) {
  display: flex;
  align-items: center;
  padding: 1rem 1.5rem 0.5rem;
  gap: 0.75rem;
}

:deep(.init-success-tip .init-tip-header i) {
  font-size: 1.25rem;
  color: #52c41a;
}

:deep(.init-error-tip .init-tip-header i) {
  font-size: 1.25rem;
  color: #ff4d4f;
}

:deep(.init-tip-title) {
  font-size: 1.1rem;
  font-weight: 600;
  color: #2c3e50;
  flex: 1;
}

:deep(.init-tip-badge) {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.5px;
  background-color: #52c41a;
  color: white;
}

:deep(.init-tip-badge.error) {
  background-color: #ff4d4f;
}

:deep(.init-tip-content) {
  padding: 0.5rem 1.5rem 1.5rem;
}

:deep(.init-tip-result) {
  background-color: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 0.9rem;
  line-height: 1.5;
}

:deep(.init-tip-result hr) {
  display: none;
}

:deep(.init-tip-guide) {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background-color: rgba(255, 255, 255, 0.4);
  border-radius: 8px;
  font-size: 0.9rem;
  color: #666;
}

:deep(.init-tip-guide i) {
  color: #1890ff;
  font-size: 1rem;
}

:deep(.init-error-tip .init-tip-guide i) {
  color: #ff4d4f;
}

@keyframes slideInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .input-container {
    flex-direction: column;
  }

  .debug-button, .init-button {
    width: 100%;
  }

  .example-queries {
    flex-direction: column;
    align-items: center;
  }

  .example-query {
    width: 100%;
    max-width: 300px;
    text-align: center;
  }
}
</style>