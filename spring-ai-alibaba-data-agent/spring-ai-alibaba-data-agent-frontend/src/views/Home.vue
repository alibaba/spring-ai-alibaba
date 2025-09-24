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
  <div>
    <!-- 现代化头部导航 -->
    <header class="page-header">
      <div class="header-content">
        <div class="brand-section">
          <div class="brand-logo">
            <i class="bi bi-robot"></i>
            <span class="brand-text">智能体管理</span>
          </div>
          <nav class="header-nav">
            <div class="nav-item" @click="goToAgentList">
              <i class="bi bi-grid-3x3-gap"></i>
              <span>智能体列表</span>
            </div>
            <div class="nav-item" @click="goToWorkspace">
              <i class="bi bi-chat-square-dots"></i>
              <span>工作台</span>
            </div>
            <div class="nav-item active">
              <i class="bi bi-graph-up-arrow"></i>
              <span>分析报告</span>
            </div>
          </nav>
        </div>
        <div class="header-actions">
          <button class="btn btn-outline btn-sm">
            <i class="bi bi-question-circle"></i>
            帮助
          </button>
          <button class="btn btn-primary" @click="goToAgentList">
            <i class="bi bi-plus-lg"></i>
            创建智能体
          </button>
        </div>
      </div>
    </header>

    <div class="container">
      <div class="search-container">
        <input 
          type="text" 
          v-model="query"
          class="search-input" 
          placeholder="例如：查询销售额前10的产品..." 
          :disabled="isInitializing"
          @keyup.enter="performSearch"
          autofocus
        >
        <button 
          class="search-button"
          :disabled="isInitializing"
          @click="performSearch"
        >
          <i class="bi bi-search"></i> 查询
        </button>
        <button 
          class="init-button"
          :class="{ loading: isInitializing }"
          :disabled="isInitializing"
          @click="initializeDataSource"
        >
          <i v-if="!isInitializing" :class="isInitialized ? 'bi bi-check-circle' : 'bi bi-database-add'"></i>
          <span v-if="isInitializing" style="opacity: 0;">初始化数据源</span>
          <span v-else>{{ isInitialized ? '重新初始化' : '初始化数据源' }}</span>
        </button>
      </div>

      <div class="result-container">
        <div class="result-header">
          <div class="result-title">
            <i class="bi bi-file-earmark-text"></i> 查询结果
          </div>
          <div v-if="status" v-html="status"></div>
        </div>
        <div class="result-content" ref="resultsDiv">
          <!-- 空状态 -->
          <div v-if="showEmptyState" class="empty-state">
            <div class="empty-icon">
              <i class="bi bi-chat-square-text"></i>
            </div>
            <div class="empty-text">
              输入自然语言问题，查看系统如何将其转换为SQL查询语句
            </div>
            <div class="example-queries">
              <div 
                v-for="example in exampleQueries" 
                :key="example"
                class="example-query"
                @click="useExampleQuery(example)"
              >
                {{ example }}
              </div>
            </div>
          </div>

          <!-- 初始化提示 -->
          <div v-if="initTip.show" :class="initTip.type">
            <div class="init-tip-header">
              <i :class="initTip.icon"></i>
              <span class="init-tip-title">{{ initTip.title }}</span>
              <span class="init-tip-badge" :class="{ error: initTip.type === 'init-error-tip' }">
                {{ initTip.type === 'init-error-tip' ? 'ERROR' : 'SUCCESS' }}
              </span>
            </div>
            <div class="init-tip-content">
              <div class="init-tip-guide">
                <i :class="initTip.guideIcon"></i>
                {{ initTip.message }}
              </div>
            </div>
          </div>

          <!-- 搜索结果 -->
          <div v-if="resultSections.length > 0" id="result-sections-container">
            <div 
              v-for="section in resultSections" 
              :key="section.id"
              class="result-section"
            >
              <div class="section-title">
                <i :class="section.icon"></i> {{ section.title }}
                <button 
                  v-if="section.type === 'sql'"
                  class="copy-button"
                  @click="copyToClipboard(section.data)"
                >
                  <i class="bi bi-clipboard"></i>
                </button>
              </div>
              <div class="section-content" v-html="section.formattedContent"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import HeaderComponent from '../components/HeaderComponent.vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/default.css'

export default {
  name: 'Home',
  components: {
    HeaderComponent
  },
  setup() {
    const query = ref('')
    const isInitializing = ref(false)
    const isInitialized = ref(false)
    const status = ref('')
    const showEmptyState = ref(true)
    const resultsDiv = ref(null)
    const resultSections = ref([])
    
    const exampleQueries = [
      '查询销售额最高的5个产品',
      '分析2025年6月的销售情况',
      '查询每个分类下已成交商品中销量最高的商品及其销售总量（每个分类仅返回销量最高的商品），同时统计商品的总数量及分类的总数量。'
    ]

    const initTip = reactive({
      show: false,
      type: '',
      title: '',
      icon: '',
      message: '',
      guideIcon: ''
    })

    let eventSource = null
    let streamState = {}
    let currentType = null
    let typeCounters = {}
    let sectionCounter = 0

    // 工具函数
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

    const renderMarkdown = (text) => {
      try {
        // 配置 marked 选项
        marked.setOptions({
          breaks: true,
          gfm: true,
          tables: true,
          sanitize: false,
          highlight: function(code, lang) {
            if (lang && hljs.getLanguage(lang)) {
              try {
                return hljs.highlight(code, { language: lang }).value
              } catch (e) {
                return hljs.highlightAuto(code).value
              }
            } else {
              return hljs.highlightAuto(code).value
            }
          }
        })
        
        const html = marked.parse(text)
        return `<div class="markdown-content">${html}</div>`
      } catch (e) {
        console.error('Error rendering markdown:', e)
        return text.replace(/\n/g, '<br>')
      }
    }

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
        'python_execute': { title: 'Python执行', icon: 'bi bi-play-circle-fill' },
        'python_generate': { title: 'Python代码生成', icon: 'bi bi-code-square-fill' },
        'python_analysis': { title: 'Python分析执行', icon: 'bi bi-code-slash' },
        'validation': { title: '校验', icon: 'bi bi-check-circle' },
        'output_report': { title: '输出报告', icon: 'bi bi-file-earmark-text' },
        'explanation': { title: '解释说明', icon: 'bi bi-info-circle' },
        'result': { title: '查询结果', icon: 'bi bi-table' }
      }

      return typeMapping[type] || { title: type, icon: 'bi bi-file-text' }
    }

    const formatContent = (type, data) => {
      let processedData = data

      if (type === 'sql') {
        processedData = data.replace(/^```\s*sql?\s*/i, '').replace(/```\s*$/, '').trim()
        
        let highlightedSQL = processedData
        // 使用正确导入的 hljs
        if (hljs) {
          try {
            highlightedSQL = hljs.highlight(processedData, { language: 'sql' }).value
          } catch (e) {
            try {
              highlightedSQL = hljs.highlightAuto(processedData).value
            } catch (e2) {
              highlightedSQL = processedData
            }
          }
        }
        
        return `<pre><code class="hljs language-sql">${highlightedSQL}</code></pre>`
      } else if (type === 'result') {
        let tableHtml = convertJsonToHTMLTable(data)
        if (tableHtml.startsWith('<pre>')) {
          // 尝试转换Markdown表格
          const lines = data.trim().split('\n')
          if (lines.length >= 2 && lines[1].includes('---')) {
            const headers = lines[0].split('|').map(h => h.trim()).filter(Boolean)
            let html = '<table class="table"><thead><tr>'
            headers.forEach(header => { html += `<th>${header}</th>` })
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
          return `<pre>${data}</pre>`
        }
        return tableHtml
      } else {
        // 处理其他类型的数据
        if (typeof data === 'string') {
          const jsonPattern = /\{"[^"]+":"[^"]*"[^}]*\}/g
          const jsonMatches = data.match(jsonPattern)
          
          if (jsonMatches && jsonMatches.length > 1) {
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
              processedData = data
            }
          }
        }
        
        if (isMarkdown(processedData)) {
          return renderMarkdown(processedData)
        } else {
          const sqlCodeBlockRegex = /```\s*sql?\s*([\s\S]*?)```/gi
          const sqlMatches = processedData.match(sqlCodeBlockRegex)
          
          if (sqlMatches && sqlMatches.length > 0) {
            let htmlContent = processedData
            
            htmlContent = htmlContent.replace(sqlCodeBlockRegex, (match, sqlContent) => {
              let cleanedSQL = sqlContent.trim()
              let highlightedSQL = cleanedSQL
              
              // 使用正确导入的 hljs
              if (hljs) {
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
            
            return htmlContent.replace(/\n/g, '<br>')
          } else {
            return processedData.toString().replace(/\n/g, '<br>')
          }
        }
      }
    }

    const copyToClipboard = (content) => {
      navigator.clipboard.writeText(content).then(() => {
        // 可以添加复制成功的提示
        console.log('内容已复制到剪贴板')
      }).catch(err => {
        console.error('复制失败:', err)
      })
    }



    const initUI = () => {
      showEmptyState.value = false
      resultSections.value = []
      currentType = null
      typeCounters = {}
      sectionCounter = 0
      streamState = {}
    }

    const createNewSection = (type, data) => {
      sectionCounter++

      if (!typeCounters[type]) {
        typeCounters[type] = 0
      }
      typeCounters[type]++

      const typeInfo = getTypeInfo(type)
      const formattedContent = formatContent(type, data)

      const section = {
        id: `${type}-${typeCounters[type]}-section`,
        type: type,
        title: `${typeInfo.title} (${typeCounters[type]})`,
        icon: typeInfo.icon,
        data: data,
        formattedContent: formattedContent
      }

      resultSections.value.push(section)

      nextTick(() => {
        if (resultsDiv.value) {
          resultsDiv.value.scrollTop = resultsDiv.value.scrollHeight
        }
      })

      return section
    }

    const updateSection = (sectionIndex, data) => {
      if (sectionIndex >= 0 && sectionIndex < resultSections.value.length) {
        const section = resultSections.value[sectionIndex]
        section.data = data
        section.formattedContent = formatContent(section.type, data)
      }
    }

    const useExampleQuery = (example) => {
      query.value = example
      performSearch()
    }

    const showInitTip = (success, message) => {
      initTip.show = true
      initTip.type = success ? 'init-success-tip' : 'init-error-tip'
      initTip.title = success ? '数据源初始化完成' : '数据源初始化失败'
      initTip.icon = success ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-triangle-fill'
      initTip.message = message
      initTip.guideIcon = success ? 'bi bi-lightbulb' : 'bi bi-info-circle'

      setTimeout(() => {
        initTip.show = false
      }, success ? 5000 : 8000)
    }

    const initializeDataSource = async () => {
      if (isInitializing.value) return

      try {
        isInitializing.value = true
        status.value = '<div class="loading"><div class="spinner"></div> 正在初始化数据源...</div>'

        const response = await fetch('/nl2sql/init', {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          }
        })

        if (response.ok) {
          const result = await response.text()
          isInitialized.value = true
          status.value = '<span class="badge badge-success">数据源初始化完成</span>'

          if (showEmptyState.value) {
            showInitTip(true, '现在您可以使用下面的推荐问题开始查询，或输入自己的问题')
          } else {
            // 显示初始化结果
            resultSections.value = [{
              id: 'init-result',
              type: 'init',
              title: '初始化完成',
              icon: 'bi bi-check-circle',
              data: result,
              formattedContent: result.replace(/\n/g, '<br>')
            }]
          }

          setTimeout(() => {
            status.value = ''
          }, 3000)
        } else {
          throw new Error(`初始化失败: ${response.status}`)
        }
      } catch (error) {
        console.error('初始化错误:', error)
        status.value = '<span class="badge badge-error">初始化失败</span>'

        if (showEmptyState.value) {
          showInitTip(false, '请检查网络连接或联系管理员，然后重试初始化')
        } else {
          resultSections.value = [{
            id: 'init-error',
            type: 'error',
            title: '初始化失败',
            icon: 'bi bi-exclamation-circle',
            data: error.message,
            formattedContent: error.message
          }]
        }
      } finally {
        isInitializing.value = false
      }
    }

    const performSearch = () => {
      if (isInitializing.value) {
        alert('正在初始化数据源，请等待完成后再进行查询')
        return
      }

      if (!query.value) {
        alert('请输入查询内容')
        return
      }

      if (!isInitialized.value) {
        console.warn('数据源未初始化，使用默认配置进行查询')
      }

      if (eventSource) {
        eventSource.close()
      }

      initUI()
      streamState = {}
      status.value = '<div class="loading"><div class="spinner"></div> 正在处理...</div>'

      eventSource = new EventSource(`/nl2sql/stream/search?query=${encodeURIComponent(query.value)}`)

      eventSource.onopen = () => {
        // Stream connection established
      }

      eventSource.onmessage = (event) => {
        status.value = ''

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
              // 保持原来的值
            }
          }

        } catch (e) {
          return
        }

        if (actualType && actualData !== undefined && actualData !== null) {
          let processedData = actualData
          
          if (actualType === 'sql' && typeof actualData === 'string') {
            processedData = actualData.replace(/^```\s*sql?\s*/i, '').replace(/```\s*$/, '').trim()
          }
          
          if (currentType !== actualType) {
            currentType = actualType
            createNewSection(actualType, processedData)
            
            const currentCount = typeCounters[actualType]
            const currentSectionKey = `${actualType}_${currentCount}`
            streamState[currentSectionKey] = processedData
          } else {
            const currentCount = typeCounters[actualType]
            const currentSectionKey = `${actualType}_${currentCount}`

            if (!streamState[currentSectionKey]) {
              streamState[currentSectionKey] = ''
            }
            streamState[currentSectionKey] += processedData

            const sectionIndex = resultSections.value.length - 1
            updateSection(sectionIndex, streamState[currentSectionKey])
          }

          nextTick(() => {
            if (resultsDiv.value) {
              resultsDiv.value.scrollTop = resultsDiv.value.scrollHeight
            }
          })
        }
      }

      eventSource.onerror = (err) => {
        console.error('EventSource error:', err)
        status.value = '<span class="badge badge-error">连接错误</span>'
        eventSource.close()
      }

      eventSource.addEventListener('complete', function (e) {
        status.value = '<span class="badge badge-success">查询完成</span>'
        eventSource.close()
        setTimeout(() => {
          status.value = ''
        }, 3000)
      })
    }

    onMounted(() => {
      // 初始化语法高亮
      if (hljs) {
        hljs.highlightAll()
      }
    })

    onUnmounted(() => {
      if (eventSource) {
        eventSource.close()
      }
    })

    return {
      query,
      isInitializing,
      isInitialized,
      status,
      showEmptyState,
      resultsDiv,
      resultSections,
      exampleQueries,
      initTip,
      useExampleQuery,
      copyToClipboard,
      initializeDataSource,
      performSearch
    }
  }
}
</script>

<style scoped>
.container {
  max-width: 100%;
  margin: 0 auto;
  padding: 1rem 2rem;
}

.search-container {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.search-input {
  flex: 1;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  transition: all 0.3s;
  outline: none;
}

.search-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.search-input:disabled {
  background-color: #f5f5f5;
  color: #999;
  cursor: not-allowed;
  border-color: #ddd;
}

.search-button {
  padding: 0.75rem 1.5rem;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--radius);
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.search-button:hover:not(:disabled) {
  background-color: #40a9ff;
}

.search-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
}

.init-button {
  padding: 0.75rem 1.5rem;
  background-color: var(--secondary-color);
  color: white;
  border: none;
  border-radius: var(--radius);
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.init-button:hover:not(:disabled) {
  background-color: #73d13d;
}

.init-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
}

.init-button.loading {
  position: relative;
}

.init-button.loading::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  margin-left: -8px; /* 使用 margin 代替 transform 进行居中定位 */
  margin-top: -8px;  /* 使用 margin 代替 transform 进行居中定位 */
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.result-container {
  background-color: var(--card-bg);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
  margin-bottom: 1.5rem;
}

.result-header {
  padding: 1rem 1.5rem;
  background-color: #fafafa;
  border-bottom: 1px solid var(--border-color);
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

.result-content {
  padding: 1.5rem;
  min-height: 200px;
  max-height: 600px;
  overflow-y: auto;
  line-height: 1.7;
}

.result-section {
  margin-bottom: 1.5rem;
  animation: fadeIn 0.5s ease-in-out;
}

.section-title {
  font-size: 1rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #555;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.section-content {
  background-color: #f9f9f9;
  border-radius: var(--radius);
  padding: 1rem;
  border: 1px solid var(--border-color);
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

.copy-button {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 4px;
  transition: all 0.2s;
  margin-left: auto;
}

.copy-button:hover {
  color: var(--primary-color);
  background-color: rgba(0, 0, 0, 0.05);
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
}

.example-queries {
  margin-top: 1.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
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

.loading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #666;
  font-style: italic;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  border-radius: 20px;
}

.badge-success {
  background-color: #f6ffed;
  color: var(--secondary-color);
}

.badge-error {
  background-color: #fff2f0;
  color: #ff4d4f;
}

/* 初始化提示样式 */
.init-success-tip, .init-error-tip {
  margin-bottom: 1.5rem;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  animation: slideInDown 0.5s ease-out;
  transition: all 0.5s ease-out;
}

.init-success-tip {
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%);
}

.init-error-tip {
  background: linear-gradient(135deg, #fff2f0 0%, #fff1f0 100%);
}

.init-tip-header {
  display: flex;
  align-items: center;
  padding: 1rem 1.5rem 0.5rem;
  gap: 0.75rem;
}

.init-success-tip .init-tip-header i {
  font-size: 1.25rem;
  color: #52c41a;
}

.init-error-tip .init-tip-header i {
  font-size: 1.25rem;
  color: #ff4d4f;
}

.init-tip-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #2c3e50;
  flex: 1;
}

.init-tip-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.5px;
  background-color: #52c41a;
  color: white;
}

.init-tip-badge.error {
  background-color: #ff4d4f;
}

.init-tip-content {
  padding: 0.5rem 1.5rem 1.5rem;
}

.init-tip-guide {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background-color: rgba(255, 255, 255, 0.4);
  border-radius: 8px;
  font-size: 0.9rem;
  color: #666;
}

.init-tip-guide i {
  color: var(--primary-color);
  font-size: 1rem;
}

.init-error-tip .init-tip-guide i {
  color: #ff4d4f;
}

/* 表格样式 */
.table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1rem;
  table-layout: auto;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

.table th,
.table td {
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  text-align: left;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 200px;
}

.table th {
  background-color: #fafafa;
  font-weight: 500;
}

.table tr:nth-child(even) {
  background-color: #f9f9f9;
}

/* 代码块样式 */
pre {
  background-color: var(--code-bg);
  color: var(--code-color);
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

code {
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
}

.section-content pre {
  background-color: var(--code-bg) !important;
  margin: 0;
  border-radius: 6px;
}

.section-content pre code.hljs {
  background-color: transparent !important;
  padding: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
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
  .container {
    padding: 1rem;
  }

  .search-container {
    flex-direction: column;
  }

  .search-button,
  .init-button {
    width: 100%;
    justify-content: center;
  }
}

/* Markdown 样式 */
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
  border-left: 4px solid var(--primary-color);
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
  background-color: var(--code-bg);
  color: var(--code-color);
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

:deep(.markdown-content table) {
  border-collapse: collapse;
  margin: 1rem 0;
  width: 100%;
  table-layout: auto;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

:deep(.markdown-content table th),
:deep(.markdown-content table td) {
  border: 1px solid #ddd;
  padding: 0.5rem 0.75rem;
  text-align: left;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 200px;
}

:deep(.markdown-content table th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

:deep(.markdown-content table tr:nth-child(even)) {
  background-color: #f9f9f9;
}

:deep(.markdown-content a) {
  color: var(--primary-color);
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
</style>
