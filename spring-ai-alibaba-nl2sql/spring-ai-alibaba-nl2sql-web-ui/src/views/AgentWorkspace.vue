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
import { ref, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { agentApi } from '../utils/api.js';

export default {
  name: 'AgentWorkspace',
  setup() {
    const router = useRouter();
    
    const publishedAgents = ref([]);
    const selectedAgent = ref(null);
    const chatMessages = ref([]);
    const currentMessage = ref('');
    const isTyping = ref(false);
    const chatContainer = ref(null);
    const chatInput = ref(null);

    const exampleQueries = ref([
      '查询销售额最高的5个产品',
      '分析最近一个月的销售趋势',
      '统计各个分类的商品数量',
      '查询用户购买行为分析'
    ]);

    const loadPublishedAgents = async () => {
      try {
        const response = await agentApi.getList({ status: 'published' });
        publishedAgents.value = response || [];
      } catch (error) {
        console.error('获取智能体列表失败:', error);
        publishedAgents.value = [];
      }
    };

    const selectAgent = (agent) => {
      selectedAgent.value = agent;
      chatMessages.value = [];
      currentMessage.value = '';
      nextTick(() => {
        if (chatInput.value) {
          chatInput.value.focus();
        }
      });
    };

    const sendMessage = async (message = null) => {
      const messageText = message || currentMessage.value.trim();
      if (!messageText || isTyping.value || !selectedAgent.value) return;

      chatMessages.value.push({
        type: 'user',
        content: messageText,
        timestamp: new Date()
      });

      currentMessage.value = '';
      isTyping.value = true;
      scrollToBottom();

      try {
        const eventSource = new EventSource(`/nl2sql/stream/search?query=${encodeURIComponent(messageText)}`);
        
        const agentMessageIndex = chatMessages.value.length;
        chatMessages.value.push({ 
          type: 'agent', 
          content: '<div class="typing-indicator"><span></span><span></span><span></span></div>', 
          timestamp: new Date() 
        });

        const streamState = {
            contentByType: {},
            typeOrder: [],
        };

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
        };

        const updateDisplay = () => {
            let fullContent = '';
            for (const type of streamState.typeOrder) {
                const typeInfo = typeMapping[type] || { title: type, icon: 'bi bi-file-text' };
                const content = streamState.contentByType[type] || '';
                const formattedSubContent = formatContentByType(type, content);
                fullContent += `<div class="agent-response-block">
                                  <div class="agent-response-title">
                                    <i class="${typeInfo.icon}"></i> ${typeInfo.title}
                                  </div>
                                  <div class="agent-response-content">${formattedSubContent}</div>
                                </div>`;
            }
            chatMessages.value[agentMessageIndex].content = fullContent;
            scrollToBottom();
        };

        eventSource.onmessage = (event) => {
            let chunk;
            let actualType;
            let actualData;
            
            try {
                // 尝试解析JSON
                let parsedData = JSON.parse(event.data);
                
                // 如果第一次解析结果还是字符串，再解析一次
                if (typeof parsedData === 'string') {
                    chunk = JSON.parse(parsedData);
                } else {
                    chunk = parsedData;
                }

                // 直接提取type和data，使用方括号语法
                actualType = chunk['type'];
                actualData = chunk['data'];

                // 处理嵌套JSON的情况
                if (actualType === 'explanation' && typeof actualData === 'string') {
                    try {
                        const innerChunk = JSON.parse(actualData);
                        if (innerChunk.type && innerChunk.data !== undefined) {
                            actualType = innerChunk.type;
                            actualData = innerChunk.data;
                        }
                    } catch (e) {
                        // 如果内层解析失败，保持原来的值
                    }
                }

            } catch (e) {
                console.error('JSON解析失败:', e, event.data);
                return;
            }

            if (actualType && actualData !== undefined && actualData !== null) {
                // 对数据进行预处理
                let processedData = actualData;
                
                // 只对SQL类型进行Markdown代码块标记的预清理
                if (actualType === 'sql' && typeof actualData === 'string') {
                    processedData = actualData.replace(/^```\s*sql?\s*/i, '').replace(/```\s*$/, '').trim();
                }
                
                // 累积数据到对应的类型
                if (!streamState.contentByType.hasOwnProperty(actualType)) {
                    streamState.typeOrder.push(actualType);
                    streamState.contentByType[actualType] = '';
                }
                
                if (processedData) {
                    streamState.contentByType[actualType] += processedData;
                }
                
                updateDisplay();
            } else {
                console.warn('Missing type or data:', {
                    type: actualType,
                    data: actualData,
                    originalChunk: chunk
                });
            }
        };

        eventSource.addEventListener('complete', () => {
          console.log('流式输出完成');
          isTyping.value = false;
          eventSource.close();
        });

        eventSource.onerror = (error) => {
          console.error('流式连接错误:', error);
          isTyping.value = false;
          
          // 检查连接状态，如果是正常结束（readyState = 2），不显示错误
          if (eventSource.readyState === EventSource.CLOSED) {
            console.log('EventSource 连接已正常关闭');
          } else {
            // 只有在真正的错误情况下才显示错误信息
            if (chatMessages.value[agentMessageIndex]) {
              chatMessages.value[agentMessageIndex].content = '抱歉，处理您的请求时出现了错误，请稍后重试。';
            }
          }
          
          eventSource.close();
        };

      } catch (error) {
        console.error('发送消息失败:', error);
        isTyping.value = false;
        chatMessages.value.push({
          type: 'agent',
          content: '抱歉，处理您的请求时出现了错误，请稍后重试。',
          timestamp: new Date()
        });
      }
    };

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
        html = html.replace(/(\|[^|\n]*\|[^|\n]*\|[^\n]*\n\|[-:\s|]*\|[^\n]*\n(?:\|[^|\n]*\|[^\n]*\n?)*)/gm, (match) => {
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

    const clearChat = () => {
      chatMessages.value = [];
    };

    const scrollToBottom = () => {
      nextTick(() => {
        if (chatContainer.value) {
          chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
        }
      });
    };

    const goToAgentList = () => {
      router.push('/agents');
    };

    const getRandomColor = (id) => {
      const colors = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96'];
      return colors[id % colors.length];
    };

    const getRandomIcon = (id) => {
      const icons = ['bi-robot', 'bi-cpu', 'bi-gear', 'bi-lightning', 'bi-star', 'bi-heart', 'bi-diamond'];
      return icons[id % icons.length];
    };

    onMounted(() => {
      loadPublishedAgents();
    });

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
    };
  }
};
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
  padding: 1rem;
}

.workspace-layout {
  display: flex;
  gap: 1rem;
  height: calc(100vh - 100px);
  max-width: 100%;
  margin: 0;
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
  min-height: 0; /* 关键修复：确保flex子元素可以正确收缩和滚动 */
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

/* Styles for dynamically generated content */
:deep(.agent-response-block) {
  margin-bottom: 1rem;
}
:deep(.agent-response-block:last-child) {
  margin-bottom: 0;
}

:deep(.agent-response-title) {
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #333;
}

:deep(.agent-response-content) {
  border-radius: 8px;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  background-color: #f9f9f9;
  padding: 1rem;
  border: 1px solid #e8e8e8;
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

:deep(code) {
    white-space: pre-wrap !important;
    word-wrap: break-word !important;
}

:deep(.dynamic-table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0;
  background-color: #fff;
}

:deep(.dynamic-table th),
:deep(.dynamic-table td) {
  padding: 0.75rem;
  border: 1px solid #e8e8e8;
  text-align: left;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 200px;
}

:deep(.dynamic-table th) {
  background-color: #fafafa;
  font-weight: 500;
}

:deep(.dynamic-table tr:nth-child(even)) {
  background-color: #f9f9f9;
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

:deep(.markdown-content strong) {
  font-weight: 600;
}

:deep(.markdown-content em) {
  font-style: italic;
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
