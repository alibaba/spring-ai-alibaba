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
  <div class="agent-list-page">
    <!-- 现代化头部导航 -->
    <header class="page-header">
      <div class="header-content">
        <div class="brand-section">
          <div class="brand-logo" @click="goToHome">
            <i class="bi bi-robot"></i>
            <span class="brand-text">数据智能体</span>
          </div>
          <nav class="header-nav">
            <div class="nav-item active">
              <i class="bi bi-grid-3x3-gap"></i>
              <span>智能体列表</span>
            </div>
            <div class="nav-item" @click="goToWorkspace">
              <i class="bi bi-chat-square-dots"></i>
              <span>智能体工作台</span>
            </div>
          </nav>
        </div>
        <div class="header-actions">
          <button class="btn btn-outline" @click="openHelp">
            <i class="bi bi-question-circle"></i>
            帮助
          </button>
          <button class="btn btn-primary" @click="createNewAgent">
            <i class="bi bi-plus-lg"></i>
            创建智能体
          </button>
        </div>
      </div>
    </header>

    <!-- 主内容区域 -->
    <main class="main-content">
      <div class="content-header">
        <div class="header-info">
          <h1 class="content-title">智能体管理中心</h1>
          <p class="content-subtitle">创建和管理您的AI智能体，让数据分析更智能</p>
        </div>
        <div class="header-stats">
          <div class="stat-item">
            <div class="stat-number">{{ agents.length }}</div>
            <div class="stat-label">总数量</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ filteredAgents.filter(a => a.status === 'published').length }}</div>
            <div class="stat-label">已发布</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ filteredAgents.filter(a => a.status === 'draft').length }}</div>
            <div class="stat-label">草稿</div>
          </div>
        </div>
      </div>

      <!-- 过滤和搜索区域 -->
      <div class="filter-section">
        <div class="filter-tabs-row">
          <div class="filter-tabs">
            <button class="filter-tab" :class="{ active: activeFilter === 'all' }" @click="setFilter('all')">
              <i class="bi bi-grid-3x3-gap"></i>
              <span>全部智能体</span>
              <span class="tab-count">{{ agents.length }}</span>
            </button>
            <button class="filter-tab" :class="{ active: activeFilter === 'published' }" @click="setFilter('published')">
              <i class="bi bi-check-circle"></i>
              <span>已发布</span>
              <span class="tab-count">{{ agents.filter(a => a.status === 'published').length }}</span>
            </button>
            <button class="filter-tab" :class="{ active: activeFilter === 'draft' }" @click="setFilter('draft')">
              <i class="bi bi-pencil-square"></i>
              <span>草稿</span>
              <span class="tab-count">{{ agents.filter(a => a.status === 'draft').length }}</span>
            </button>
            <button class="filter-tab" :class="{ active: activeFilter === 'offline' }" @click="setFilter('offline')">
              <i class="bi bi-pause-circle"></i>
              <span>已下线</span>
              <span class="tab-count">{{ agents.filter(a => a.status === 'offline').length }}</span>
            </button>
          </div>

          <div class="search-and-actions">
            <div class="search-box">
              <i class="search-icon bi bi-search"></i>
              <input 
                type="text" 
                v-model="searchKeyword" 
                class="form-control"
                placeholder="搜索智能体名称、ID或描述..." 
                @input="searchAgents"
                @keyup.enter="refreshAgentList"
              >
            </div>
            <div class="action-buttons">
              <button class="btn btn-outline" @click="refreshAgentList">
                <i class="bi bi-search"></i>
                搜索
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 智能体网格 -->
      <div class="agents-grid" v-if="!loading">
        <div 
          v-for="agent in filteredAgents" 
          :key="agent.id" 
          class="agent-card"
          @click="enterAgent(agent.id)"
        >
          <div class="agent-avatar">
            <div class="avatar-icon" :style="{ backgroundColor: getRandomColor(agent.id) }">
              <i :class="getRandomIcon(agent.id)"></i>
            </div>
          </div>
          <div class="agent-info">
            <h3 class="agent-name">{{ agent.name }}</h3>
            <p class="agent-description">{{ agent.description }}</p>
            <div class="agent-meta">
              <span class="agent-id">ID: {{ agent.id }}</span>
              <span class="agent-time">{{ formatTime(agent.updateTime) }}</span>
            </div>
          </div>
          <div class="agent-status">
            <span class="status-badge" :class="agent.status">{{ getStatusText(agent.status) }}</span>
          </div>
          <div class="agent-actions" @click.stop>
            <button class="action-btn" @click="editAgent(agent)">
              <i class="bi bi-pencil"></i>
            </button>
            <button class="action-btn" @click="deleteAgent(agent.id)">
              <i class="bi bi-trash"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && filteredAgents.length === 0" class="empty-state">
        <i class="bi bi-robot"></i>
        <h3>暂无智能体</h3>
        <p>点击"创建智能体"开始创建您的第一个智能体</p>
        <button class="create-first-btn" @click="createNewAgent">创建智能体</button>
      </div>
    </main>

    <!-- 创建智能体模态框 -->
    <div v-if="showCreateModal" class="modal-overlay" @click="closeCreateModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>创建智能体</h3>
          <button class="close-btn" @click="closeCreateModal">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="createAgent">
            <div class="form-group">
              <label for="agentName">智能体名称 *</label>
              <input 
                type="text" 
                id="agentName"
                v-model="newAgent.name" 
                placeholder="请输入智能体名称"
                required
              >
            </div>
            <div class="form-group">
              <label for="agentDescription">智能体描述</label>
              <textarea 
                id="agentDescription"
                v-model="newAgent.description" 
                placeholder="请输入智能体描述"
                rows="3"
              ></textarea>
            </div>
            <div class="form-group">
              <label for="agentAvatar">头像URL</label>
              <input 
                type="url" 
                id="agentAvatar"
                v-model="newAgent.avatar" 
                placeholder="请输入头像URL（可选）"
              >
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeCreateModal">取消</button>
          <button type="button" class="btn btn-primary" @click="createAgent">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { agentApi } from '../utils/api.js'

export default {
  name: 'AgentList',
  setup() {
    const router = useRouter()
    const loading = ref(true)
    const activeFilter = ref('all')
    const searchKeyword = ref('')
    const showCreateModal = ref(false)
    const agents = ref([])

    // 模拟数据配置
    const useMockData = ref(false) // 设置为 true 使用模拟数据，false 使用真实 API

    // 模拟数据
    const mockAgents = [
      {
        id: 'agent_001',
        name: '销售数据分析师',
        description: '专门分析销售数据，提供销售趋势和业绩洞察',
        avatar: '/api/placeholder/80/80',
        status: 'published',
        createTime: '2024-01-15 10:30:00',
        updateTime: '2024-01-20 14:20:00'
      },
      {
        id: 'agent_002', 
        name: '客户行为分析师',
        description: '分析客户行为数据，提供客户画像和预测模型',
        avatar: '/api/placeholder/80/80',
        status: 'draft',
        createTime: '2024-01-16 09:15:00',
        updateTime: '2024-01-18 16:45:00'
      },
      {
        id: 'agent_003',
        name: '财务报表分析师', 
        description: '专业的财务数据分析，生成财务报告和预算建议',
        avatar: '/api/placeholder/80/80',
        status: 'published',
        createTime: '2024-01-10 15:20:00',
        updateTime: '2024-01-22 11:30:00'
      }
    ]

    const newAgent = reactive({
      name: '',
      description: '',
      avatar: ''
    })

    // 计算属性
    const filteredAgents = computed(() => {
      let filtered = agents.value

      // 按状态过滤
      if (activeFilter.value !== 'all') {
        filtered = filtered.filter(agent => agent.status === activeFilter.value)
      }

      // 按关键词搜索
      if (searchKeyword.value.trim()) {
        const keyword = searchKeyword.value.toLowerCase()
        filtered = filtered.filter(agent => 
          agent.name.toLowerCase().includes(keyword) ||
          agent.id.toLowerCase().includes(keyword) ||
          (agent.description && agent.description.toLowerCase().includes(keyword))
        )
      }

      return filtered
    })

    // 方法
    const loadAgents = async () => {
      try {
        loading.value = true
        
        if (useMockData.value) {
          // 使用模拟数据
          await new Promise(resolve => setTimeout(resolve, 1000))
          agents.value = mockAgents
        } else {
          // 使用真实 API
          const params = {}
          if (activeFilter.value !== 'all') {
            params.status = activeFilter.value
          }
          if (searchKeyword.value.trim()) {
            params.keyword = searchKeyword.value.trim()
          }
          const data = await agentApi.getList(params)
          agents.value = data
        }
      } catch (error) {
        console.error('加载智能体列表失败:', error)
        let errorMessage = '加载失败，请重试'
        
        // 提供更具体的错误信息
        if (error.response) {
          if (error.response.status === 500) {
            errorMessage = '服务器内部错误，请稍后重试或联系管理员'
          } else if (error.response.status === 404) {
            errorMessage = '请求的资源未找到'
          } else if (error.response.data && error.response.data.message) {
            errorMessage = error.response.data.message
          }
        } else if (error.message) {
          errorMessage = error.message
        }
        
        if (useMockData.value) {
          agents.value = mockAgents
        } else {
          alert(errorMessage)
        }
      } finally {
        loading.value = false
      }
    }

    const refreshAgentList = async () => {
      console.log('手动刷新智能体列表')
      await loadAgents()
    }

    const setFilter = (filter) => {
      activeFilter.value = filter
    }

    const searchAgents = () => {
      // 搜索逻辑已通过计算属性实现
    }

    const enterAgent = (agentId) => {
      router.push(`/agent/${agentId}`)
    }

    const createNewAgent = () => {
      router.push('/agent/create')
    }

    const editAgent = (agent) => {
      // 编辑智能体逻辑
      console.log('编辑智能体:', agent)
    }

    const deleteAgent = async (agentId) => {
      if (!confirm('确定要删除这个智能体吗？')) {
        return
      }

      try {
        if (useMockData.value) {
          // 使用模拟数据
          const index = agents.value.findIndex(agent => agent.id === agentId)
          if (index !== -1) {
            agents.value.splice(index, 1)
          }
        } else {
          // 使用真实 API
          await agentApi.delete(agentId)
          await loadAgents()
        }
        alert('删除成功')
      } catch (error) {
        console.error('删除失败:', error)
        alert('删除失败，请重试')
      }
    }

    const createAgent = async () => {
      if (!newAgent.name.trim()) {
        alert('请填写智能体名称')
        return
      }

      try {
        const agentData = {
          name: newAgent.name.trim(),
          description: newAgent.description.trim(),
          avatar: newAgent.avatar.trim() || '/default-avatar.png',
          status: 'draft'
        }

        if (useMockData.value) {
          // 使用模拟数据
          const newId = `agent_${Date.now()}`
          const newAgentItem = {
            ...agentData,
            id: newId,
            createTime: new Date().toLocaleString(),
            updateTime: new Date().toLocaleString()
          }
          agents.value.unshift(newAgentItem)
          
          // 创建成功后跳转到智能体详情页
          router.push(`/agent/${newId}`)
        } else {
          // 使用真实 API
          const result = await agentApi.create(agentData)
          router.push(`/agent/${result.id}`)
        }

        closeCreateModal()
      } catch (error) {
        console.error('创建智能体失败:', error)
        alert('创建失败，请重试')
      }
    }

    const closeCreateModal = () => {
      showCreateModal.value = false
      Object.assign(newAgent, { name: '', description: '', avatar: '' })
    }

    const getStatusText = (status) => {
      const statusMap = {
        published: '已发布',
        draft: '待发布', 
        offline: '已下线'
      }
      return statusMap[status] || status
    }

    const formatTime = (timeStr) => {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      return date.toLocaleDateString()
    }

    // 生成随机颜色和图标的方法
    const getRandomColor = (id) => {
      const colors = [
        '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
        '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9',
        '#F8C471', '#82E0AA', '#F1948A', '#85C1E9', '#D7BDE2'
      ]
      // 将ID转换为数字哈希值
      let hash = 0
      const str = String(id)
      for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash)
        hash = hash & hash // 转换为32位整数
      }
      const index = Math.abs(hash) % colors.length
      return colors[index]
    }
    
    const getRandomIcon = (id) => {
      const icons = [
        'bi bi-robot', 'bi bi-cpu', 'bi bi-gear', 'bi bi-lightbulb',
        'bi bi-graph-up', 'bi bi-pie-chart', 'bi bi-bar-chart',
        'bi bi-diagram-3', 'bi bi-puzzle', 'bi bi-lightning',
        'bi bi-star', 'bi bi-heart', 'bi bi-trophy', 'bi bi-gem',
        'bi bi-brain'
      ]
      // 将ID转换为数字哈希值
      let hash = 0
      const str = String(id)
      for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash)
        hash = hash & hash // 转换为32位整数
      }
      const index = Math.abs(hash) % icons.length
      return icons[index]
    }

    const goToWorkspace = () => {
      router.push('/workspace')
    }

    const openHelp = () => {
      window.open('https://github.com/alibaba/spring-ai-alibaba/blob/main/spring-ai-alibaba-nl2sql/README.md', '_blank')
    }

    const goToHome = () => {
      router.push('/')
    }

    // 生命周期
    onMounted(() => {
      loadAgents()
    })

    return {
      loading,
      activeFilter,
      searchKeyword,
      showCreateModal,
      agents,
      newAgent,
      filteredAgents,
      setFilter,
      searchAgents,
      enterAgent,
      createNewAgent,
      editAgent,
      deleteAgent,
      createAgent,
      closeCreateModal,
      getStatusText,
      formatTime,
      getRandomColor,
      getRandomIcon,
      refreshAgentList,
      goToWorkspace,
      openHelp,
      goToHome
    }
  }
}
</script>

<style scoped>
.agent-list-page {
  min-height: 100vh;
  background: var(--bg-layout);
  font-family: var(--font-family);
}

/* 现代化头部导航 */
.page-header {
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

.brand-section {
  display: flex;
  align-items: center;
  gap: var(--space-2xl);
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--primary-color);
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.brand-logo i {
  font-size: var(--font-size-xl);
  color: var(--accent-color);
}

.brand-text {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-nav {
  display: flex;
  gap: var(--space-lg);
}

.header-nav .nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border-radius: var(--radius-base);
  border: 1px solid transparent;
}

.header-nav .nav-item:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
  border-color: var(--border-primary);
}

.header-nav .nav-item.active {
  background: var(--primary-light);
  color: var(--primary-color);
  border-color: var(--primary-color);
}

.header-nav .nav-item i {
  font-size: var(--font-size-base);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

/* 主内容区域 */
.main-content {
  max-width: 100%;
  margin: 0 auto;
  padding: var(--space-lg);
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-lg);
  padding: var(--space-md);
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-secondary);
}

.header-info {
  flex: 1;
}

.content-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-sm);
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.content-subtitle {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}

.header-stats {
  display: flex;
  gap: var(--space-md);
}

.stat-item {
  text-align: center;
  padding: var(--space-md);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-tertiary);
  min-width: 80px;
}

.stat-number {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--primary-color);
  margin-bottom: var(--space-xs);
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  font-weight: var(--font-weight-medium);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* 过滤和搜索区域 */
.filter-section {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  margin-bottom: var(--space-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-secondary);
}

.filter-tabs-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-lg);
}

.filter-tabs {
  display: flex;
  gap: var(--space-xs);
  background: var(--bg-secondary);
  padding: var(--space-xs);
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all var(--transition-base);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border-radius: var(--radius-sm);
  position: relative;
}

.filter-tab:hover {
  background: var(--bg-primary);
  color: var(--text-primary);
}

.filter-tab.active {
  background: var(--primary-color);
  color: var(--bg-primary);
  box-shadow: var(--shadow-sm);
}

.filter-tab i {
  font-size: var(--font-size-sm);
}

.tab-count {
  background: rgba(255, 255, 255, 0.2);
  color: inherit;
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  min-width: 20px;
  text-align: center;
}

.filter-tab.active .tab-count {
  background: rgba(255, 255, 255, 0.3);
}

.search-and-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-shrink: 0;
}

.search-box {
  position: relative;
  width: 300px;
}

.action-buttons {
  display: flex;
  gap: var(--space-md);
  flex-shrink: 0;
}

/* 智能体网格 */
.agents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}

.agent-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-secondary);
  transition: all var(--transition-base);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.agent-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--primary-color), var(--accent-color));
  opacity: 0;
  transition: opacity var(--transition-base);
}

.agent-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-color);
}

.agent-card:hover::before {
  opacity: 1;
}

.agent-avatar {
  width: 64px;
  height: 64px;
  margin-bottom: var(--space-lg);
}

.avatar-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--bg-primary);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.avatar-icon::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.1), rgba(255,255,255,0));
  pointer-events: none;
}

.agent-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-sm) 0;
  line-height: 1.4;
}

.agent-description {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.6;
  margin: 0 0 var(--space-lg) 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.agent-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  margin-top: auto;
}

.agent-id {
  font-family: var(--font-family-mono);
  background: var(--bg-secondary);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-tertiary);
}

.agent-time {
  font-weight: var(--font-weight-medium);
}

.agent-status {
  position: absolute;
  top: var(--space-lg);
  right: var(--space-lg);
}

.status-badge {
  display: inline-block;
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.agent-actions {
  position: absolute;
  top: var(--space-lg);
  left: var(--space-lg);
  display: flex;
  gap: var(--space-sm);
  opacity: 0;
  transition: opacity var(--transition-base);
}

.agent-card:hover .agent-actions {
  opacity: 1;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-base);
  border: none;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.action-btn:hover {
  background: var(--primary-color);
  color: var(--bg-primary);
  transform: scale(1.1);
}

/* 加载和空状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-4xl);
  color: var(--text-secondary);
}

.loading-state .spinner {
  margin-bottom: var(--space-lg);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-4xl);
  text-align: center;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 2px dashed var(--border-primary);
}

.empty-state i {
  font-size: 4rem;
  color: var(--text-quaternary);
  margin-bottom: var(--space-lg);
}

.empty-state h3 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-sm) 0;
}

.empty-state p {
  color: var(--text-secondary);
  margin: 0 0 var(--space-xl) 0;
  line-height: 1.6;
}

.create-first-btn {
  padding: var(--space-md) var(--space-xl);
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: var(--bg-primary);
  border: none;
  border-radius: var(--radius-base);
  cursor: pointer;
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-sm);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.create-first-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal);
  animation: fadeIn 0.3s ease-out;
}

.modal-content {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  width: 500px;
  max-width: 90vw;
  max-height: 90vh;
  overflow: auto;
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--border-secondary);
  animation: slideInUp 0.3s ease-out;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-xl);
  border-bottom: 1px solid var(--border-secondary);
  background: var(--bg-secondary);
}

.modal-header h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.close-btn {
  background: none;
  border: none;
  font-size: var(--font-size-xl);
  cursor: pointer;
  color: var(--text-tertiary);
  padding: var(--space-xs);
  border-radius: var(--radius-sm);
  transition: all var(--transition-base);
}

.close-btn:hover {
  background: var(--error-light);
  color: var(--error-color);
}

.modal-body {
  padding: var(--space-xl);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-md);
  padding: var(--space-lg) var(--space-xl);
  border-top: 1px solid var(--border-secondary);
  background: var(--bg-tertiary);
}

/* 动画定义 */
@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    padding: var(--space-lg);
  }
  
  .content-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-lg);
  }
  
  .header-stats {
    align-self: stretch;
    justify-content: space-around;
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 var(--space-md);
    flex-direction: column;
    height: auto;
    padding-top: var(--space-md);
    padding-bottom: var(--space-md);
    gap: var(--space-md);
  }
  
  .brand-section {
    flex-direction: column;
    gap: var(--space-md);
    align-items: flex-start;
  }
  
  .header-nav {
    flex-wrap: wrap;
  }
  
  .main-content {
    padding: var(--space-md);
  }
  
  .content-header {
    padding: var(--space-lg);
  }
  
  .content-title {
    font-size: var(--font-size-2xl);
  }
  
  .filter-tabs {
    flex-wrap: wrap;
  }
  
  .search-and-actions {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-md);
  }
  
  .search-box {
    max-width: none;
  }
  
  .agents-grid {
    grid-template-columns: 1fr;
    gap: var(--space-lg);
  }
  
  .modal-content {
    margin: var(--space-md);
    width: auto;
  }
}

@media (max-width: 480px) {
  .header-stats {
    flex-direction: column;
    gap: var(--space-sm);
  }
  
  .stat-item {
    padding: var(--space-sm);
  }
  
  .filter-section {
    padding: var(--space-md);
  }
  
  .agent-card {
    padding: var(--space-lg);
  }
  
  .modal-header,
  .modal-body,
  .modal-footer {
    padding: var(--space-md);
  }
}
</style>
