
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
  <div class="home-page">
    <!-- 三栏布局 -->
    <div class="layout-container">
      <!-- 左侧导航栏 -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <div class="logo">
            <i class="bi bi-database-gear"></i>
            <span>NL2SQL</span>
          </div>
        </div>
        <nav class="sidebar-nav">
          <div class="nav-item active">
            <i class="bi bi-house"></i>
            <span>主页</span>
          </div>
          <div class="nav-item">
            <i class="bi bi-robot"></i>
            <span>智能体</span>
          </div>
          <div class="nav-item">
            <i class="bi bi-database"></i>
            <span>数据源</span>
          </div>
          <div class="nav-item">
            <i class="bi bi-graph-up"></i>
            <span>分析报告</span>
          </div>
          <div class="nav-item">
            <i class="bi bi-gear"></i>
            <span>设置</span>
          </div>
        </nav>
      </aside>

      <!-- 中间主操作面板 -->
      <main class="main-panel">
        <div class="panel-header">
          <h1 class="panel-title">自然语言转SQL</h1>
          <p class="panel-subtitle">输入自然语言问题，系统将自动转换为SQL查询语句</p>
        </div>

        <!-- 搜索输入区域 -->
        <div class="search-section">
          <div class="search-container">
            <div class="search-box">
              <i class="search-icon bi bi-search"></i>
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
                v-if="query"
                class="clear-btn"
                @click="clearQuery"
              >
                <i class="bi bi-x"></i>
              </button>
            </div>
            <div class="search-actions">
              <button 
                class="btn btn-primary"
                :disabled="isInitializing || !query.trim()"
                @click="performSearch"
              >
                <i class="bi bi-play-fill"></i>
                生成 SQL
              </button>
              <button 
                class="btn btn-outline"
                :class="{ loading: isInitializing }"
                :disabled="isInitializing"
                @click="initializeDataSource"
              >
                <div v-if="isInitializing" class="spinner spinner-sm"></div>
                <i v-else :class="isInitialized ? 'bi bi-check-circle' : 'bi bi-database-add'"></i>
                {{ isInitialized ? '重新初始化' : '初始化数据源' }}
              </button>
            </div>
          </div>

          <!-- 示例查询 -->
          <div v-if="showEmptyState" class="example-section">
            <h3 class="example-title">试试这些查询</h3>
            <div class="example-queries">
              <div 
                v-for="example in exampleQueries" 
                :key="example"
                class="example-query"
                @click="useExampleQuery(example)"
              >
                <i class="bi bi-lightbulb"></i>
                {{ example }}
              </div>
            </div>
          </div>

          <!-- 初始化提示 -->
          <div v-if="initTip.show" class="init-tip" :class="initTip.type">
            <div class="tip-header">
              <i :class="initTip.icon"></i>
              <span class="tip-title">{{ initTip.title }}</span>
              <span class="tip-badge" :class="{ error: initTip.type === 'error' }">
                {{ initTip.type === 'error' ? 'ERROR' : 'SUCCESS' }}
              </span>
            </div>
            <div class="tip-content">
              <i :class="initTip.guideIcon"></i>
              {{ initTip.message }}
            </div>
          </div>
        </div>
      </main>

      <!-- 右侧结果展示区 -->
      <aside class="result-panel">
        <div class="result-header">
          <div class="result-title">
            <i class="bi bi-file-earmark-code"></i>
            <span>执行结果</span>
          </div>
          <div v-if="status" class="result-status" v-html="status"></div>
        </div>

        <div class="result-content" ref="resultsDiv">
          <!-- 空状态 -->
          <div v-if="showEmptyState && !resultSections.length" class="empty-state">
            <div class="empty-icon">
              <i class="bi bi-chat-square-text"></i>
            </div>
            <div class="empty-text">
              <h4>开始你的查询</h4>
              <p>在左侧输入自然语言问题，查看系统如何将其转换为SQL查询语句</p>
            </div>
          </div>

          <!-- 查询结果 -->
          <div v-if="resultSections.length > 0" class="result-sections">
            <div 
              v-for="section in resultSections" 
              :key="section.id"
              class="result-section"
            >
              <div class="section-header">
                <div class="section-title">
                  <i :class="section.icon"></i>
                  <span>{{ section.title }}</span>
                </div>
                <button 
                  v-if="section.type === 'sql'"
                  class="copy-btn"
                  @click="copyToClipboard(section.data)"
                  data-tooltip="复制SQL"
                >
                  <i class="bi bi-clipboard"></i>
                </button>
              </div>
              <div class="section-content" v-html="section.formattedContent"></div>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/default.css'

export default {
  name: 'HomeOptimized',
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
      '统计每个分类下销量最高的商品'
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

        let html = '<table class="data-table"><thead><tr>'
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
          const lines = data.trim().split('\n')
          if (lines.length >= 2 && lines[1].includes('---')) {
            const headers = lines[0].split('|').map(h => h.trim()).filter(Boolean)
            let html = '<table class="data-table"><thead><tr>'
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
        console.log('内容已复制到剪贴板')
      }).catch(err => {
        console.error('复制失败:', err)
      })
    }

    const clearQuery = () => {
      query.value = ''
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
      initTip.type = success ? 'success' : 'error'
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
        status.value = '<div class="status-loading"><div class="spinner spinner-sm"></div> 正在初始化数据源...</div>'

        const response = await fetch('/nl2sql/init', {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          }
        })

        if (response.ok) {
          const result = await response.text()
          isInitialized.value = true
          status.value = '<span class="status-badge success">数据源初始化完成</span>'

          if (showEmptyState.value) {
            showInitTip(true, '现在您可以使用下面的推荐问题开始查询，或输入自己的问题')
          } else {
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
        status.value = '<span class="status-badge error">初始化失败</span>'

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
      status.value = '<div class="status-loading"><div class="spinner spinner-sm"></div> 正在处理...</div>'

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
        status.value = '<span class="status-badge error">连接错误</span>'
        eventSource.close()
      }

      eventSource.addEventListener('complete', function (e) {
        status.value = '<span class="status-badge success">查询完成</span>'
        eventSource.close()
        setTimeout(() => {
          status.value = ''
        }, 3000)
      })
    }

    onMounted(() => {
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
      clearQuery,
      useExampleQuery,
      copyToClipboard,
      initializeDataSource,
      performSearch
    }
  }
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--bg-layout);
  font-family: var(--font-family);
}

/* 三栏布局容器 */
.layout-container {
  display: flex;
  min-height: 100vh;
  max-width: 1600px;
  margin: 0 auto;
}

/* 左侧导航栏 */
.sidebar {
  width: 240px;
  background: var(--bg-primary);
  border-right: 1px solid var(--border-secondary);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}

.sidebar-header {
  padding: var(--space-xl);
  border-bottom: 1px solid var(--border-secondary);
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--accent-color);
}

.logo i {
  font-size: var(--font-size-xl);
}

.sidebar-nav {
  flex: 1;
  padding: var(--space-lg) 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-base);
  padding: var(--space-base) var(--space-xl);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border-left: 3px solid transparent;
}

.nav-item:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--primary-lighter);
  color: var(--primary-color);
  border-left-color: var(--primary-color);
}

.nav-item i {
  font-size: var(--font-size-lg);
  width: 20px;
  text-align: center;
}

/* 中间主操作面板 */
.main-panel {
  flex: 1;
  padding: var(--space-2xl);
  background: var(--bg-layout);
  overflow-y: auto;
}

.panel-header {
  margin-bottom: var(--space-2xl);
  text-align: center;
}

.panel-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-sm);
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.panel-subtitle {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  margin: 0;
}

/* 搜索区域 */
.search-section {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-2xl);
  box-shadow: var(--shadow-base);
  border: 1px solid var(--border-secondary);
}

.search-container {
  margin-bottom: var(--space-xl);
}

.search-box {
  position: relative;
  margin-bottom: var(--space-lg);
}

.search-input {
  width: 100%;
  padding: var(--space-md) var(--space-xl) var(--space-md) var(--space-3xl);
  border: 2px solid var(--border-primary);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  font-family: var(--font-family);
  color: var(--text-primary);
  background: var(--bg-primary);
  transition: all var(--transition-base);
  box-sizing: border-box;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.search-input:disabled {
  background: var(--bg-secondary);
  color: var(--text-disabled);
  cursor: not-allowed;
}

.search-icon {
  position: absolute;
  left: var(--space-md);
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-tertiary);
  font-size: var(--font-size-lg);
  pointer-events: none;
}

.clear-btn {
  position: absolute;
  right: var(--space-md);
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: var(--space-xs);
  border-radius: var(--radius-sm);
  transition: all var(--transition-base);
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-btn:hover {
  color: var(--text-secondary);
  background: var(--bg-secondary);
}

.search-actions {
  display: flex;
  gap: var(--space-md);
  justify-content: center;
}

/* 示例查询区域 */
.example-section {
  margin-top: var(--space-xl);
  text-align: center;
}

.example-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  margin-bottom: var(--space-lg);
}

.example-queries {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
  justify-content: center;
}

.example-query {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  background: var(--primary-light);
  color: var(--primary-color);
  border-radius: var(--radius-2xl);
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border: 1px solid rgba(95, 112, 225, 0.2);
}

.example-query:hover {
  background: var(--primary-color);
  color: var(--bg-primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.example-query i {
  font-size: var(--font-size-sm);
}

/* 初始化提示 */
.init-tip {
  margin-top: var(--space-xl);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  border: 1px solid transparent;
  animation: fadeIn 0.5s ease-out;
}

.init-tip.success {
  background: var(--success-light);
  border-color: rgba(82, 196, 26, 0.2);
}

.init-tip.error {
  background: var(--error-light);
  border-color: rgba(255, 77, 79, 0.2);
}

.tip-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
}

.tip-title {
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-base);
  flex: 1;
}

.init-tip.success .tip-title {
  color: var(--success-color);
}

.init-tip.error .tip-title {
  color: var(--error-color);
}

.tip-badge {
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  background: var(--success-color);
  color: var(--bg-primary);
  letter-spacing: 0.5px;
}

.tip-badge.error {
  background: var(--error-color);
}

.tip-content {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.init-tip.success .tip-content i {
  color: var(--success-color);
}

.init-tip.error .tip-content i {
  color: var(--error-color);
}

/* 右侧结果展示区 */
.result-panel {
  width: 480px;
  background: var(--bg-primary);
  border-left: 1px solid var(--border-secondary);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
}

.result-header {
  padding: var(--space-xl);
  border-bottom: 1px solid var(--border-secondary);
  background: var(--bg-secondary);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.result-title i {
  color: var(--accent-color);
}

.result-status {
  display: flex;
  align-items: center;
}

.result-content {
  flex: 1;
  padding: var(--space-xl);
  overflow-y: auto;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: var(--space-2xl);
}

.empty-icon {
  font-size: 4rem;
  color: var(--text-quaternary);
  margin-bottom: var(--space-lg);
}

.empty-text h4 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-sm);
}

.empty-text p {
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

/* 结果区域 */
.result-sections {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.result-section {
  border: 1px solid var(--border-secondary);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-primary);
  animation: fadeIn 0.3s ease-out;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md) var(--space-lg);
  background: var(--bg-tertiary);
  border-bottom: 1px solid var(--border-tertiary);
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.section-title i {
  color: var(--primary-color);
  font-size: var(--font-size-base);
}

.copy-btn {
  padding: var(--space-xs) var(--space-sm);
  background: var(--primary-light);
  color: var(--primary-color);
  border: 1px solid rgba(95, 112, 225, 0.2);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: var(--font-size-xs);
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.copy-btn:hover {
  background: var(--primary-color);
  color: var(--bg-primary);
}

.section-content {
  padding: var(--space-lg);
  font-size: var(--font-size-sm);
  line-height: 1.6;
  color: var(--text-secondary);
}

/* 状态徽章 */
.status-loading {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--primary-color);
  font-size: var(--font-size-sm);
}

.status-badge {
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-badge.success {
  background: var(--success-light);
  color: var(--success-color);
  border: 1px solid rgba(82, 196, 26, 0.2);
}

.status-badge.error {
  background: var(--error-light);
  color: var(--error-color);
  border: 1px solid rgba(255, 77, 79, 0.2);
}

/* 数据表格 */
.data-table {
  width: 100%;
  border-collapse: collapse;
  margin: var(--space-md) 0;
  font-size: var(--font-size-xs);
  background: var(--bg-primary);
  border-radius: var(--radius-sm);
  overflow: hidden;
  box-shadow: var(--shadow-xs);
}

.data-table th {
  background: var(--bg-secondary);
  padding: var(--space-sm) var(--space-base);
  text-align: left;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-secondary);
}

.data-table td {
  padding: var(--space-sm) var(--space-base);
  border-bottom: 1px solid var(--border-tertiary);
  color: var(--text-secondary);
}

.data-table tbody tr:hover {
  background: var(--bg-tertiary);
}

.data-table tbody tr:last-child td {
  border-bottom: none;
}

/* 代码块样式 */
.section-content pre {
  background: var(--bg-secondary) !important;
  border: 1px solid var(--border-secondary);
  border-radius: var(--radius-sm);
  padding: var(--space-md) !important;
  margin: var(--space-sm) 0;
  overflow-x: auto;
  font-family: var(--font-family-mono);
  font-size: var(--font-size-xs);
  line-height: 1.5;
}

.section-content pre code {
  background: none !important;
  padding: 0 !important;
  color: var(--text-primary);
}

/* Markdown 内容样式 */
:deep(.markdown-content) {
  line-height: 1.6;
  color: var(--text-secondary);
}

:deep(.markdown-content h1),
:deep(.markdown-content h2),
:deep(.markdown-content h3),
:deep(.markdown-content h4),
:deep(.markdown-content h5),
:deep(.markdown-content h6) {
  margin-top: var(--space-lg);
  margin-bottom: var(--space-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

:deep(.markdown-content p) {
  margin-bottom: var(--space-md);
}

:deep(.markdown-content ul),
:deep(.markdown-content ol) {
  margin-bottom: var(--space-md);
  padding-left: var(--space-xl);
}

:deep(.markdown-content li) {
  margin-bottom: var(--space-xs);
}

:deep(.markdown-content code) {
  background: var(--bg-secondary);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-xs);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-xs);
  color: var(--primary-color);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .layout-container {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
    height: auto;
    position: static;
    border-right: none;
    border-bottom: 1px solid var(--border-secondary);
  }
  
  .sidebar-nav {
    display: flex;
    overflow-x: auto;
    padding: var(--space-md) var(--space-lg);
  }
  
  .nav-item {
    white-space: nowrap;
    border-left: none;
    border-bottom: 3px solid transparent;
  }
  
  .nav-item.active {
    border-left: none;
    border-bottom-color: var(--primary-color);
  }
  
  .result-panel {
    width: 100%;
    height: auto;
    position: static;
    border-left: none;
    border-top: 1px solid var(--border-secondary);
  }
  
  .main-panel {
    padding: var(--space-lg);
  }
}

@media (max-width: 768px) {
  .panel-title {
    font-size: var(--font-size-2xl);
  }
  
  .search-section {
    padding: var(--space-lg);
  }
  
  .search-actions {
    flex-direction: column;
  }
  
  .example-queries {
    flex-direction: column;
    align-items: center;
  }
  
  .example-query {
    width: 100%;
    max-width: 300px;
    justify-content: center;
  }
  
  .result-content {
    padding: var(--space-md);
  }
  
  .section-content {
    padding: var(--space-md);
  }
}

@media (max-width: 480px) {
  .main-panel {
    padding: var(--space-md);
  }
  
  .search-section {
    padding: var(--space-md);
  }
  
  .search-input {
    padding: var(--space-sm) var(--space-lg) var(--space-sm) var(--space-2xl);
  }
  
  .panel-header {
    margin-bottom: var(--space-lg);
  }
}

/* 搜索区域 */
.search-section {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
}

.search-actions {
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-success {
  background: #52c41a;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #73d13d;
  transform: translateY(-2px);
}

.btn-success:disabled {
  background: #d9d9d9;
  color: #8c8c8c;
  cursor: not-allowed;
  transform: none;
}

.btn-success.loading {
  background: #1890ff;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:hover {
  background: #40a9ff;
}

.btn-outline {
  background: transparent;
  color: #8c8c8c;
  border: 1px solid #d9d9d9;
}

.btn-outline:hover {
  background: #f5f5f5;
  color: #262626;
}

/* 结果区域 */
.result-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
  overflow: hidden;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem 2rem;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.result-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.1rem;
  font-weight: 600;
  color: #262626;
}

.result-status {
  display: flex;
  align-items: center;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 500;
  text-transform: uppercase;
}

.status-badge.success {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.status-badge.error {
  background: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

.status-badge.loading {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}

.status-badge.info {
  background: #f0f5ff;
  color: #1890ff;
  border: 1px solid #adc6ff;
}

.result-content {
  padding: 2rem;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 3rem 2rem;
}

.empty-icon {
  font-size: 4rem;
  color: #d9d9d9;
  margin-bottom: 1rem;
}

.empty-text {
  font-size: 1.1rem;
  color: #8c8c8c;
  margin-bottom: 2rem;
  line-height: 1.6;
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
  background: #f0f5ff;
  color: #1890ff;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.9rem;
  border: 1px solid #d6e4ff;
}

.example-query:hover {
  background: #1890ff;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

/* 初始化提示 */
.init-success-tip,
.init-error-tip {
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.init-success-tip {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
}

.init-error-tip {
  background: #fff2f0;
  border: 1px solid #ffccc7;
}

.init-tip-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.init-tip-title {
  font-weight: 600;
  font-size: 1rem;
}

.init-success-tip .init-tip-title {
  color: #52c41a;
}

.init-error-tip .init-tip-title {
  color: #ff4d4f;
}

.init-tip-badge {
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 600;
  background: #52c41a;
  color: white;
  margin-left: auto;
}

.init-tip-badge.error {
  background: #ff4d4f;
}

.init-tip-content {
  color: #595959;
  line-height: 1.5;
}

.init-tip-guide {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
}

/* 查询结果 */
.query-result {
  space-y: 1.5rem;
}

.sql-section,
.data-section,
.execution-info {
  margin-bottom: 2rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #f0f0f0;
}

.section-header span {
  font-size: 1.1rem;
  font-weight: 600;
  color: #262626;
  margin-left: 0.5rem;
}

.copy-btn,
.export-btn {
  padding: 0.4rem 0.8rem;
  background: #f0f5ff;
  color: #1890ff;
  border: 1px solid #d6e4ff;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.copy-btn:hover,
.export-btn:hover {
  background: #1890ff;
  color: white;
}

.sql-content {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 1rem;
  overflow-x: auto;
}

.sql-content pre {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  color: #262626;
}

.sql-content code {
  background: none;
  padding: 0;
  color: inherit;
}

.info-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: #fafafa;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}

.info-label {
  font-weight: 500;
  color: #595959;
}

.info-value {
  font-weight: 600;
  color: #262626;
}

/* 错误信息 */
.error-message {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 8px;
  padding: 1.5rem;
}

.error-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
  color: #ff4d4f;
  font-weight: 600;
}

.error-content {
  color: #8c8c8c;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.error-actions {
  display: flex;
  gap: 1rem;
}

/* 历史记录 */
.history-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.history-section .section-header {
  padding: 1.5rem 2rem;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 0;
}

.clear-btn {
  padding: 0.4rem 0.8rem;
  background: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.clear-btn:hover {
  background: #ff4d4f;
  color: white;
}

.empty-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 3rem 2rem;
  color: #8c8c8c;
  gap: 0.5rem;
}

.empty-history i {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.history-list {
  padding: 1rem 2rem 2rem;
}

.history-item {
  padding: 1rem;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.history-item:hover {
  background: #fafafa;
  border-color: #d9d9d9;
  transform: translateY(-1px);
}

.history-item:last-child {
  margin-bottom: 0;
}

.history-query {
  font-size: 0.95rem;
  color: #262626;
  margin-bottom: 0.5rem;
  line-height: 1.4;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
}

.history-time {
  color: #8c8c8c;
}

.history-status {
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-weight: 500;
}

.history-status.success {
  background: #f6ffed;
  color: #52c41a;
}

.history-status.error {
  background: #fff2f0;
  color: #ff4d4f;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .container {
    padding: 1rem;
  }
  
  .search-section,
  .result-content {
    padding: 1.5rem;
  }
  
  .result-header {
    padding: 1rem 1.5rem;
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
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
  
  .info-content {
    grid-template-columns: 1fr;
  }
  
  .error-actions {
    flex-direction: column;
  }
  
  .history-list {
    padding: 1rem;
  }
  
  .history-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
  }
}

@media (max-width: 480px) {
  .container {
    padding: 0.5rem;
  }
  
  .search-section {
    padding: 1rem;
  }
  
  .result-content {
    padding: 1rem;
  }
  
  .sql-content {
    padding: 0.75rem;
  }
  
  .sql-content pre {
    font-size: 0.8rem;
  }
}

/* 加载动画 */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.loading {
  animation: pulse 1.5s ease-in-out infinite;
}

/* 滚动条样式 */
.sql-content::-webkit-scrollbar {
  height: 6px;
}

.sql-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.sql-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.sql-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .search-section,
  .result-section,
  .history-section {
    border: 2px solid #000;
  }
  
  .btn {
    border: 2px solid currentColor;
  }
  
  .example-query {
    border: 2px solid #1890ff;
  }
}

/* 减少动画模式支持 */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
