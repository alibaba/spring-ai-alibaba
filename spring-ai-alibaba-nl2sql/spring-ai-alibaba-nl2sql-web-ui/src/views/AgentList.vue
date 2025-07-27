<template>
  <div class="agent-list-page">
    <!-- 头部导航 -->
    <div class="top-nav">
      <div class="nav-items">
        <span class="nav-item">数据智能体</span>
        <span class="nav-item active">智能体</span>
      </div>
      <div class="nav-right">
      </div>
    </div>

    <!-- 智能体列表 -->
    <div class="agent-container">
      <div class="page-header">
        <h1 class="page-title">智能体列表</h1>
      </div>

      <!-- 过滤标签 -->
      <div class="filter-tabs">
        <button class="filter-tab" :class="{ active: activeFilter === 'all' }" @click="setFilter('all')">
          全部智能体
        </button>
        <button class="filter-tab" :class="{ active: activeFilter === 'published' }" @click="setFilter('published')">
          已发布
        </button>
        <button class="filter-tab" :class="{ active: activeFilter === 'draft' }" @click="setFilter('draft')">
          待发布
        </button>
        <button class="filter-tab" :class="{ active: activeFilter === 'offline' }" @click="setFilter('offline')">
          已下线
        </button>
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <div class="search-section">
          <div class="search-box">
            <i class="bi bi-search"></i>
            <input type="text" v-model="searchKeyword" placeholder="请输入智能体名称、ID" @input="searchAgents">
          </div>
        </div>
        <div class="action-buttons">
          <button class="query-btn" @click="refreshAgentList">
            <i class="bi bi-arrow-clockwise"></i>
            查询
          </button>
          <button class="create-agent-btn" @click="createNewAgent">
            <i class="bi bi-plus"></i>
            创建智能体
          </button>
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
    </div>

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
        if (useMockData.value) {
          agents.value = mockAgents
        } else {
          alert('加载失败，请重试')
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
      refreshAgentList
    }
  }
}
</script>

<style scoped>
.agent-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.top-nav {
  background: white;
  border-bottom: 1px solid #e5e5e5;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}

.nav-items {
  display: flex;
  gap: 32px;
}

.nav-item {
  padding: 8px 0;
  cursor: pointer;
  color: #666;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.nav-item.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.project-select {
  padding: 4px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: white;
  cursor: pointer;
}

.agent-container {
  width: 100%;
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  margin: 0;
}

.filter-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  border-bottom: 1px solid #e5e5e5;
}

.filter-tab {
  padding: 12px 24px;
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.filter-tab.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.filter-tab:hover {
  color: #1890ff;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.search-section {
  display: flex;
  gap: 16px;
  align-items: center;
}

.search-box {
  position: relative;
  width: 300px;
}

.search-box i {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
}

.search-box input {
  width: 100%;
  padding: 8px 12px 8px 36px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
}

.create-agent-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 24px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.3s;
}

.create-agent-btn:hover {
  background: #40a9ff;
}

.action-buttons {
  display: flex;
  gap: 12px;
  align-items: center;
}

.query-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f5f5f5;
  color: #666;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.query-btn:hover {
  background: #e6f7ff;
  border-color: #1890ff;
  color: #1890ff;
}

.agents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.agent-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: all 0.3s;
  cursor: pointer;
  position: relative;
}

.agent-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.agent-avatar {
  width: 60px;
  height: 60px;
  margin-bottom: 16px;
}

.avatar-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: bold;
}

.agent-name {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 8px 0;
}

.agent-description {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.agent-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

.agent-status {
  position: absolute;
  top: 16px;
  right: 16px;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.published {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.status-badge.draft {
  background: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.status-badge.offline {
  background: #f5f5f5;
  color: #999;
  border: 1px solid #d9d9d9;
}

.agent-actions {
  position: absolute;
  top: 16px;
  left: 16px;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.agent-card:hover .agent-actions {
  opacity: 1;
}

.action-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(255,255,255,0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
  color: #666;
  transition: all 0.3s;
}

.action-btn:hover {
  background: white;
  color: #1890ff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #666;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #666;
  text-align: center;
}

.empty-state i {
  font-size: 64px;
  color: #d9d9d9;
  margin-bottom: 16px;
}

.empty-state h3 {
  margin: 0 0 8px 0;
  color: #262626;
}

.empty-state p {
  margin: 0 0 24px 0;
}

.create-first-btn {
  padding: 8px 24px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 500px;
  max-width: 90vw;
  max-height: 90vh;
  overflow: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e5e5;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #262626;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
}

.form-group textarea {
  resize: vertical;
  min-height: 80px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e5e5e5;
}

.btn {
  padding: 8px 24px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
}

.btn-secondary:hover {
  background: #e6e6e6;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:hover {
  background: #40a9ff;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .agent-container {
    padding: 16px;
  }
  
  .action-bar {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .search-section {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-box {
    width: 100%;
  }
  
  .agents-grid {
    grid-template-columns: 1fr;
  }
}
</style>
