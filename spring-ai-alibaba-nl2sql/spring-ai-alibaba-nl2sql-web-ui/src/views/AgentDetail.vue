<template>
  <div class="agent-detail-page">
    <!-- 头部导航 -->
    <div class="top-nav">
      <div class="nav-items">
        <span class="nav-item">数据智能体</span>
        <span class="nav-item active">智能体</span>
      </div>
      <div class="nav-right">
      </div>
    </div>

    <!-- 智能体信息头部 -->
    <div class="agent-header">
      <div class="container">
        <div class="header-content">
          <button class="back-btn" @click="goBack">
            <i class="bi bi-arrow-left"></i>
          </button>
          <div class="agent-info">
            <div class="agent-avatar">
              <div class="avatar-icon" :style="{ backgroundColor: getRandomColor(agent.id) }">
                <i :class="getRandomIcon(agent.id)"></i>
              </div>
            </div>
            <div class="agent-meta">
              <h1 class="agent-name">{{ agent.name }}</h1>
              <p class="agent-description">{{ agent.description }}</p>
              <div class="agent-tags">
                <span class="tag">ID: {{ agent.id }}</span>
                <span class="tag status-tag" :class="agent.status">{{ getStatusText(agent.status) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <div class="container">
        <div class="content-layout">
          <!-- 左侧导航 -->
          <div class="sidebar">
            <nav class="sidebar-nav">
              <div class="nav-section">
                <div class="nav-section-title">基本信息</div>
                <a href="#" class="nav-link" :class="{ active: activeTab === 'basic' }" @click="setActiveTab('basic')">
                  <i class="bi bi-info-circle"></i>
                  基本信息
                </a>
              </div>

              <div class="nav-section">
                <div class="nav-section-title">数据源配置</div>
                <a href="#" class="nav-link" :class="{ active: activeTab === 'datasource' }" @click="setActiveTab('datasource')">
                  <i class="bi bi-database"></i>
                  数据源配置
                </a>
              </div>

              <div class="nav-section">
                <div class="nav-section-title">Prompt配置</div>
                <a href="#" class="nav-link" :class="{ active: activeTab === 'prompt' }" @click="setActiveTab('prompt')">
                  <i class="bi bi-chat-square-text"></i>
                  自定义Prompt配置
                </a>
              </div>

              <div class="nav-section">
                <div class="nav-section-title">知识配置</div>
                <a href="#" class="nav-link" :class="{ active: activeTab === 'business-knowledge' }" @click="setActiveTab('business-knowledge')">
                  <i class="bi bi-lightbulb"></i>
                  业务知识管理
                </a>
                <a href="#" class="nav-link" :class="{ active: activeTab === 'semantic-model' }" @click="setActiveTab('semantic-model')">
                  <i class="bi bi-diagram-3"></i>
                  语义模型配置
                </a>
                <a href="#" class="nav-link" :class="{ active: activeTab === 'knowledge' }" @click="setActiveTab('knowledge')">
                  <i class="bi bi-file-text"></i>
                  知识库配置
                </a>
              </div>
            </nav>
          </div>

          <!-- 右侧内容 -->
          <div class="main-panel">
            <!-- 基本信息 -->
            <div v-if="activeTab === 'basic'" class="tab-content">
              <div class="content-header">
                <h2>基本信息</h2>
                <p class="content-subtitle">智能体的基本配置信息</p>
              </div>
              <div class="basic-info-form">
                <div class="form-group">
                  <label>智能体名称</label>
                  <input type="text" v-model="agent.name" class="form-control">
                </div>
                <div class="form-group">
                  <label>描述</label>
                  <textarea v-model="agent.description" class="form-control" rows="3"></textarea>
                </div>
                <div class="form-group">
                  <label>状态</label>
                  <select v-model="agent.status" class="form-control">
                    <option value="active">启用</option>
                    <option value="inactive">禁用</option>
                  </select>
                </div>
                <div class="form-group">
                  <label>创建时间</label>
                  <input type="text" :value="agent.createdAt" class="form-control" readonly>
                </div>
                <div class="form-group">
                  <label>更新时间</label>
                  <input type="text" :value="agent.updatedAt" class="form-control" readonly>
                </div>
                <div class="form-actions">
                  <button class="btn btn-primary" @click="updateAgent">保存</button>
                </div>
              </div>
            </div>

            <!-- 业务知识管理 -->
            <div v-if="activeTab === 'business-knowledge'" class="tab-content">
              <div class="content-header">
                <h2>业务知识管理</h2>
                <p class="content-subtitle">管理智能体的业务知识库</p>
              </div>
              <div class="business-knowledge-section">
                <div class="section-header">
                  <h3>业务知识列表</h3>
                  <button class="btn btn-primary" @click="showCreateKnowledgeModal = true">
                    <i class="bi bi-plus"></i>
                    添加知识
                  </button>
                </div>
                <div class="knowledge-table">
                  <table class="table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>知识名称</th>
                        <th>知识内容</th>
                        <th>创建时间</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="knowledge in businessKnowledgeList" :key="knowledge.id">
                        <td>{{ knowledge.id }}</td>
                        <td>{{ knowledge.name }}</td>
                        <td>{{ knowledge.content.substring(0, 50) }}...</td>
                        <td>{{ knowledge.createdAt }}</td>
                        <td>
                          <div class="action-buttons">
                            <button class="btn btn-sm btn-outline" @click="editKnowledge(knowledge)">编辑</button>
                            <button class="btn btn-sm btn-danger" @click="deleteKnowledge(knowledge.id)">删除</button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- 语义模型配置 -->
            <div v-if="activeTab === 'semantic-model'" class="tab-content">
              <div class="content-header">
                <h2>语义模型配置</h2>
                <p class="content-subtitle">配置智能体的语义模型</p>
              </div>
              <div class="semantic-model-section">
                <div class="section-header">
                  <h3>语义模型列表</h3>
                  <button class="btn btn-primary" @click="showCreateModelModal = true">
                    <i class="bi bi-plus"></i>
                    添加模型
                  </button>
                </div>
                <div class="semantic-model-table">
                  <table class="table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>模型名称</th>
                        <th>模型类型</th>
                        <th>配置</th>
                        <th>状态</th>
                        <th>创建时间</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="model in semanticModelList" :key="model.id">
                        <td>{{ model.id }}</td>
                        <td>{{ model.name }}</td>
                        <td>{{ model.type }}</td>
                        <td>{{ model.config }}</td>
                        <td>
                          <span class="status-badge" :class="model.status">{{ model.status }}</span>
                        </td>
                        <td>{{ model.createdAt }}</td>
                        <td>
                          <div class="action-buttons">
                            <button class="btn btn-sm btn-outline" @click="editModel(model)">编辑</button>
                            <button class="btn btn-sm btn-danger" @click="deleteModel(model.id)">删除</button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- Prompt配置 -->
            <div v-if="activeTab === 'prompt'" class="tab-content">
              <div class="content-header">
                <h2>自定义Prompt配置</h2>
                <p class="content-subtitle">配置智能体的提示词模板</p>
              </div>
              <div class="prompt-config-section">
                <div class="form-group">
                  <label>系统提示词</label>
                  <textarea v-model="promptConfig.systemPrompt" class="form-control" rows="6" 
                    placeholder="请输入系统提示词，定义智能体的基本行为和角色"></textarea>
                </div>
                <div class="form-group">
                  <label>用户提示词模板</label>
                  <textarea v-model="promptConfig.userPrompt" class="form-control" rows="4"
                    placeholder="请输入用户提示词模板，支持变量替换"></textarea>
                </div>
                <div class="form-actions">
                  <button class="btn btn-primary" @click="savePromptConfig">保存配置</button>
                </div>
              </div>
            </div>

            <!-- 数据源配置 -->
            <div v-if="activeTab === 'datasource'" class="tab-content">
              <div class="content-header">
                <h2>数据源配置</h2>
                <p class="content-subtitle">配置智能体需要读取的数据源</p>
              </div>
              <div class="datasource-section">
                <div class="section-header">
                  <h3>数据源列表</h3>
                  <button class="btn btn-primary" @click="showAddDatasourceModal = true">
                    <i class="bi bi-plus"></i>
                    添加数据源
                  </button>
                </div>
                <div class="datasource-table">
                  <table class="table">
                    <thead>
                      <tr>
                        <th>数据源名称</th>
                        <th>数据源类型</th>
                        <th>连接地址</th>
                        <th>状态</th>
                        <th>创建时间</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="datasource in datasourceList" :key="datasource.id">
                        <td>{{ datasource.name }}</td>
                        <td>{{ datasource.type }}</td>
                        <td>{{ datasource.connectionUrl }}</td>
                        <td>
                          <span class="status-badge" :class="datasource.status">{{ getStatusText(datasource.status) }}</span>
                        </td>
                        <td>{{ datasource.createdAt }}</td>
                        <td>
                          <div class="action-buttons">
                            <button class="btn btn-sm btn-outline" @click="testConnection(datasource)">测试连接</button>
                            <button class="btn btn-sm btn-outline" @click="editDatasource(datasource)">编辑</button>
                            <button class="btn btn-sm btn-danger" @click="deleteDatasource(datasource.id)">删除</button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- 知识库配置 -->
            <div v-if="activeTab === 'knowledge'" class="tab-content">
              <div class="content-header">
                <h2>知识库配置</h2>
                <p class="content-subtitle">配置智能体的知识库和文档资源</p>
              </div>
              <div class="knowledge-config-section">
                <div class="section-header">
                  <h3>文档库</h3>
                  <button class="btn btn-primary" @click="showUploadModal = true">
                    <i class="bi bi-upload"></i>
                    上传文档
                  </button>
                </div>
                <div class="document-grid">
                  <div v-for="doc in knowledgeDocuments" :key="doc.id" class="document-card">
                    <div class="document-icon">
                      <i class="bi bi-file-text"></i>
                    </div>
                    <div class="document-info">
                      <h4>{{ doc.name }}</h4>
                      <p>{{ doc.description }}</p>
                      <span class="document-size">{{ doc.size }}</span>
                    </div>
                    <div class="document-actions">
                      <button class="action-btn" @click="viewDocument(doc)">查看</button>
                      <button class="action-btn text-danger" @click="deleteDocument(doc.id)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { agentApi, businessKnowledgeApi, semanticModelApi } from '../utils/api.js'

export default {
  name: 'AgentDetail',
  setup() {
    const router = useRouter()
    const route = useRoute()
    
    // 响应式数据
    const activeTab = ref('basic')
    const agent = reactive({
      id: '',
      name: '',
      description: '',
      status: 'active',
      createdAt: '',
      updatedAt: '',
      avatar: ''
    })
    
    const businessKnowledgeList = ref([])
    const semanticModelList = ref([])
    const knowledgeDocuments = ref([])
    const datasourceList = ref([])
    
    const promptConfig = reactive({
      systemPrompt: '',
      userPrompt: ''
    })
    
    const showCreateKnowledgeModal = ref(false)
    const showCreateModelModal = ref(false)
    const showUploadModal = ref(false)
    const showAddDatasourceModal = ref(false)
    
    // 方法
    const setActiveTab = (tab) => {
      activeTab.value = tab
      loadTabData(tab)
    }
    
    const goBack = () => {
      router.push('/agent')
    }
    
    const loadAgentDetail = async () => {
      try {
        const agentId = route.params.id
        const response = await agentApi.getDetail(agentId)
        Object.assign(agent, response.data)
      } catch (error) {
        console.error('加载智能体详情失败:', error)
      }
    }
    
    const loadTabData = async (tab) => {
      switch (tab) {
        case 'business-knowledge':
          await loadBusinessKnowledge()
          break
        case 'semantic-model':
          await loadSemanticModels()
          break
        case 'datasource':
          await loadDatasources()
          break
        case 'knowledge':
          await loadKnowledgeDocuments()
          break
      }
    }
    
    const loadBusinessKnowledge = async () => {
      try {
        const response = await businessKnowledgeApi.getList()
        businessKnowledgeList.value = response.data
      } catch (error) {
        console.error('加载业务知识失败:', error)
      }
    }
    
    const loadSemanticModels = async () => {
      try {
        const response = await semanticModelApi.getList()
        semanticModelList.value = response.data
      } catch (error) {
        console.error('加载语义模型失败:', error)
      }
    }
    
    const loadDatasources = async () => {
      try {
        // TODO: 实现数据源API
        datasourceList.value = [
          {
            id: 1,
            name: 'MySQL主库',
            type: 'MySQL',
            connectionUrl: 'mysql://localhost:3306/main_db',
            status: 'active',
            createdAt: '2024-01-15 10:30:00'
          },
          {
            id: 2,
            name: 'PostgreSQL数据仓库',
            type: 'PostgreSQL',
            connectionUrl: 'postgresql://localhost:5432/warehouse',
            status: 'active',
            createdAt: '2024-01-16 14:20:00'
          },
          {
            id: 3,
            name: 'Redis缓存',
            type: 'Redis',
            connectionUrl: 'redis://localhost:6379',
            status: 'inactive',
            createdAt: '2024-01-17 09:15:00'
          }
        ]
      } catch (error) {
        console.error('加载数据源失败:', error)
      }
    }
    
    const loadKnowledgeDocuments = async () => {
      try {
        // TODO: 实现知识文档API
        knowledgeDocuments.value = [
          {
            id: 1,
            name: '产品手册.pdf',
            description: '产品使用手册',
            size: '2.5MB'
          },
          {
            id: 2,
            name: '用户指南.docx',
            description: '用户操作指南',
            size: '1.8MB'
          }
        ]
      } catch (error) {
        console.error('加载知识文档失败:', error)
      }
    }
    
    const updateAgent = async () => {
      try {
        await agentApi.update(agent.id, agent)
        alert('更新成功')
      } catch (error) {
        console.error('更新智能体失败:', error)
        alert('更新失败')
      }
    }
    
    const editKnowledge = (knowledge) => {
      // TODO: 实现编辑业务知识功能
      console.log('编辑知识:', knowledge)
    }
    
    const deleteKnowledge = async (id) => {
      if (confirm('确定要删除这条知识吗？')) {
        try {
          await businessKnowledgeApi.delete(id)
          await loadBusinessKnowledge()
        } catch (error) {
          console.error('删除知识失败:', error)
        }
      }
    }
    
    const editModel = (model) => {
      // TODO: 实现编辑语义模型功能
      console.log('编辑模型:', model)
    }
    
    const deleteModel = async (id) => {
      if (confirm('确定要删除这个模型吗？')) {
        try {
          await semanticModelApi.delete(id)
          await loadSemanticModels()
        } catch (error) {
          console.error('删除模型失败:', error)
        }
      }
    }
    
    const savePromptConfig = () => {
      // TODO: 实现保存Prompt配置功能
      console.log('保存Prompt配置:', promptConfig)
      alert('配置已保存')
    }
    
    const viewDocument = (doc) => {
      // TODO: 实现查看文档功能
      console.log('查看文档:', doc)
    }
    
    const deleteDocument = (id) => {
      if (confirm('确定要删除这个文档吗？')) {
        // TODO: 实现删除文档功能
        console.log('删除文档:', id)
      }
    }
    
    const testConnection = (datasource) => {
      // TODO: 实现测试连接功能
      console.log('测试连接:', datasource)
      alert('连接测试成功！')
    }
    
    const editDatasource = (datasource) => {
      // TODO: 实现编辑数据源功能
      console.log('编辑数据源:', datasource)
    }
    
    const deleteDatasource = async (id) => {
      if (confirm('确定要删除这个数据源吗？')) {
        try {
          // TODO: 实现删除数据源API
          await loadDatasources()
        } catch (error) {
          console.error('删除数据源失败:', error)
        }
      }
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
    
    const getStatusText = (status) => {
      const statusMap = {
        'published': '已发布',
        'draft': '草稿',
        'offline': '已下线',
        'active': '启用',
        'inactive': '禁用'
      }
      return statusMap[status] || status
    }
    
    // 生命周期
    onMounted(() => {
      loadAgentDetail()
    })
    
    return {
      activeTab,
      agent,
      businessKnowledgeList,
      semanticModelList,
      knowledgeDocuments,
      datasourceList,
      promptConfig,
      showCreateKnowledgeModal,
      showCreateModelModal,
      showUploadModal,
      showAddDatasourceModal,
      setActiveTab,
      goBack,
      updateAgent,
      editKnowledge,
      deleteKnowledge,
      editModel,
      deleteModel,
      testConnection,
      editDatasource,
      deleteDatasource,
      savePromptConfig,
      viewDocument,
      deleteDocument,
      getRandomColor,
      getRandomIcon,
      getStatusText
    }
  }
}
</script>

<style scoped>
.agent-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
}

/* 头部导航样式 */
.top-nav {
  background: white;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8e8e8;
}

.nav-items {
  display: flex;
  gap: 32px;
}

.nav-item {
  padding: 8px 16px;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.nav-item.active,
.nav-item:hover {
  color: #1890ff;
  background: #f0f8ff;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.project-select {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: white;
}

/* 智能体头部样式 */
.agent-header {
  background: white;
  padding: 24px 0;
  border-bottom: 1px solid #e8e8e8;
}

.container {
  width: 100%;
  padding: 0 24px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  padding: 8px;
  border: none;
  background: #f5f5f5;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.agent-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.agent-avatar .avatar-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
  font-weight: bold;
}

.agent-name {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.agent-description {
  margin: 0 0 8px 0;
  color: #666;
}

.agent-tags {
  display: flex;
  gap: 8px;
}

.tag {
  padding: 4px 8px;
  background: #f0f0f0;
  border-radius: 12px;
  font-size: 12px;
  color: #666;
}

.status-tag.published {
  background: #f6ffed;
  color: #52c41a;
}

.status-tag.draft {
  background: #fff7e6;
  color: #fa8c16;
}

.status-tag.offline {
  background: #fff2f0;
  color: #ff4d4f;
}

.status-tag.active {
  background: #f6ffed;
  color: #52c41a;
}

.status-tag.inactive {
  background: #fff2f0;
  color: #ff4d4f;
}

/* 主要内容区域样式 */
.main-content {
  padding: 24px 0;
}

.content-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* 左侧导航样式 */
.sidebar {
  width: 280px;
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.sidebar-nav {
  padding: 16px 0;
}

.nav-section {
  margin-bottom: 8px;
}

.nav-section-title {
  padding: 8px 16px;
  font-size: 12px;
  color: #999;
  text-transform: uppercase;
  font-weight: 600;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  color: #333;
  text-decoration: none;
  transition: all 0.2s;
}

.nav-link:hover {
  background: #f5f5f5;
  color: #1890ff;
}

.nav-link.active {
  background: #e6f7ff;
  color: #1890ff;
  border-right: 3px solid #1890ff;
}

.nav-link i {
  font-size: 16px;
}

/* 右侧内容面板样式 */
.main-panel {
  flex: 1;
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.tab-content {
  padding: 24px;
}

.content-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.content-header h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
}

.content-subtitle {
  margin: 0;
  color: #666;
  font-size: 14px;
}

/* 表单样式 */
.basic-info-form {
  max-width: 600px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.form-control {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.form-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e8e8e8;
}

/* 按钮样式 */
.btn {
  padding: 8px 16px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-primary {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.btn-primary:hover {
  background: #40a9ff;
  border-color: #40a9ff;
}

.btn-outline {
  background: white;
  color: #1890ff;
  border-color: #1890ff;
}

.btn-outline:hover {
  background: #f0f8ff;
}

.btn-danger {
  background: #ff4d4f;
  color: white;
  border-color: #ff4d4f;
}

.btn-danger:hover {
  background: #ff7875;
  border-color: #ff7875;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

/* 表格样式 */
.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
}

.table th,
.table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e8e8e8;
}

.table th {
  background: #f5f5f5;
  font-weight: 600;
  color: #333;
}

.table tr:hover {
  background: #f9f9f9;
}

/* 业务知识和语义模型样式 */
.business-knowledge-section,
.semantic-model-section {
  margin-top: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active {
  background: #f6ffed;
  color: #52c41a;
}

.status-badge.inactive {
  background: #fff2f0;
  color: #ff4d4f;
}

/* Prompt配置样式 */
.prompt-config-section {
  max-width: 800px;
}

/* 审计日志样式 */
/* 数据源配置样式 */
.datasource-section {
  margin-top: 16px;
}

.datasource-table {
  margin-top: 16px;
}

.datasource-table .status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.datasource-table .status-badge.active {
  background: #f6ffed;
  color: #52c41a;
}

.datasource-table .status-badge.inactive {
  background: #fff2f0;
  color: #ff4d4f;
}

/* 知识库配置样式 */
.knowledge-config-section {
  margin-top: 16px;
}

.document-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.document-card {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: white;
  transition: all 0.2s;
}

.document-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.document-icon {
  margin-bottom: 12px;
}

.document-icon i {
  font-size: 32px;
  color: #1890ff;
}

.document-info h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
}

.document-info p {
  margin: 0 0 8px 0;
  color: #666;
  font-size: 14px;
}

.document-size {
  font-size: 12px;
  color: #999;
}

.document-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 4px 8px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.action-btn.text-danger {
  color: #ff4d4f;
  border-color: #ff4d4f;
}

.action-btn.text-danger:hover {
  background: #fff2f0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .content-layout {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
  }
  
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .action-buttons {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
