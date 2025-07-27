<template>
  <div class="agent-detail-page">
    <!-- 消息提示组件 -->
    <div v-if="message.show" class="message-toast" :class="message.type">
      <div class="message-content">
        <i :class="getMessageIcon(message.type)"></i>
        <span>{{ message.text }}</span>
      </div>
      <button class="message-close" @click="hideMessage">×</button>
    </div>

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
                  <label>分类</label>
                  <input type="text" v-model="agent.category" class="form-control" placeholder="请输入智能体分类">
                </div>
                <div class="form-group">
                  <label>标签</label>
                  <input type="text" v-model="agent.tags" class="form-control" placeholder="多个标签用逗号分隔">
                </div>
                <div class="form-group">
                  <label>状态</label>
                  <select v-model="agent.status" class="form-control">
                    <option value="draft">待发布</option>
                    <option value="published">已发布</option>
                    <option value="offline">已下线</option>
                  </select>
                </div>
                <div class="form-group">
                  <label>创建时间</label>
                  <input type="text" :value="formatDateTime(agent.createTime)" class="form-control" readonly>
                </div>
                <div class="form-group">
                  <label>更新时间</label>
                  <input type="text" :value="formatDateTime(agent.updateTime)" class="form-control" readonly>
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
                            <button class="btn btn-sm btn-outline" @click="editBusinessKnowledge(knowledge)">编辑</button>
                            <button class="btn btn-sm btn-danger" @click="deleteBusinessKnowledge(knowledge.id)">删除</button>
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
                  <label>智能体Prompt</label>
                  <textarea v-model="agent.prompt" class="form-control" rows="8" 
                    placeholder="请输入智能体的提示词，定义智能体的基本行为和角色"></textarea>
                </div>
                <div class="form-actions">
                  <button class="btn btn-primary" @click="updateAgent">保存配置</button>
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
                  <button class="btn btn-primary" @click="openAddDatasourceModal">
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
                        <th>连接状态</th>
                        <th>状态</th>
                        <th>创建时间</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="agentDatasourceList.length === 0">
                        <td colspan="7" class="text-center text-muted">暂无数据源</td>
                      </tr>
                      <tr v-for="agentDatasource in agentDatasourceList" :key="agentDatasource.id">
                        <td>{{ agentDatasource.datasource?.name }}</td>
                        <td>{{ getDatasourceTypeText(agentDatasource.datasource?.type) }}</td>
                        <td>{{ agentDatasource.datasource?.connectionUrl }}</td>
                        <td>
                          <span class="status-badge" :class="agentDatasource.datasource?.testStatus">
                            {{ getTestStatusText(agentDatasource.datasource?.testStatus) }}
                          </span>
                        </td>
                        <td>
                          <span class="status-badge" :class="agentDatasource.isActive === 1 ? 'active' : 'inactive'">
                            {{ agentDatasource.isActive === 1 ? '启用' : '禁用' }}
                          </span>
                        </td>
                        <td>{{ formatDate(agentDatasource.createTime) }}</td>
                        <td>
                          <div class="action-buttons">
                            <button 
                              class="btn btn-sm"
                              :class="agentDatasource.isActive === 1 ? 'btn-warning' : 'btn-success'"
                              @click="toggleDatasourceStatus(agentDatasource.datasource.id, agentDatasource.isActive !== 1)"
                            >
                              {{ agentDatasource.isActive === 1 ? '禁用' : '启用' }}
                            </button>
                            <button class="btn btn-sm btn-outline" @click="testDatasourceConnection(agentDatasource.datasource.id)">测试连接</button>
                            <button class="btn btn-sm btn-danger" @click="removeDatasourceFromAgent(agentDatasource.datasource.id)">移除</button>
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
                <p class="content-subtitle">管理智能体的知识库和文档资源</p>
              </div>
              
              <!-- 搜索和筛选 -->
              <div class="knowledge-filters">
                <div class="filter-group">
                  <div class="search-box">
                    <i class="bi bi-search"></i>
                    <input 
                      type="text" 
                      v-model="knowledgeFilters.keyword" 
                      placeholder="搜索知识..." 
                      class="form-control"
                      @input="searchKnowledge"
                    >
                  </div>
                </div>
                <div class="filter-group">
                  <select v-model="knowledgeFilters.type" @change="filterKnowledge" class="form-control">
                    <option value="">全部类型</option>
                    <option value="document">文档</option>
                    <option value="qa">问答</option>
                    <option value="faq">常见问题</option>
                  </select>
                </div>
                <div class="filter-group">
                  <select v-model="knowledgeFilters.status" @change="filterKnowledge" class="form-control">
                    <option value="">全部状态</option>
                    <option value="active">启用</option>
                    <option value="inactive">禁用</option>
                  </select>
                </div>
                <div class="filter-group">
                  <button class="btn btn-primary" @click="openCreateKnowledgeModal">
                    <i class="bi bi-plus"></i>
                    添加知识
                  </button>
                </div>
              </div>

              <!-- 知识统计 -->
              <div class="knowledge-stats">
                <div class="stat-card">
                  <div class="stat-number">{{ knowledgeStats.totalCount }}</div>
                  <div class="stat-label">总知识数</div>
                </div>
                <div class="stat-card" v-for="typeStat in knowledgeStats.typeStats" :key="typeStat.type">
                  <div class="stat-number">{{ typeStat.count }}</div>
                  <div class="stat-label">{{ getTypeText(typeStat.type) }}</div>
                </div>
              </div>

              <!-- 知识列表 -->
              <div class="knowledge-list">
                <div v-if="knowledgeList.length === 0" class="empty-state">
                  <i class="bi bi-inbox"></i>
                  <p>暂无知识数据</p>
                </div>
                <div v-else class="knowledge-table">
                  <table class="table">
                    <thead>
                      <tr>
                        <th>标题</th>
                        <th>类型</th>
                        <th>分类</th>
                        <th>状态</th>
                        <th>向量化状态</th>
                        <th>创建时间</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="knowledge in knowledgeList" :key="knowledge.id">
                        <td>
                          <div class="knowledge-title">
                            <i :class="getKnowledgeIcon(knowledge.type)"></i>
                            {{ knowledge.title }}
                          </div>
                        </td>
                        <td>
                          <span class="type-badge" :class="knowledge.type">
                            {{ getTypeText(knowledge.type) }}
                          </span>
                        </td>
                        <td>{{ knowledge.category || '-' }}</td>
                        <td>
                          <span class="status-badge" :class="knowledge.status">
                            {{ getStatusText(knowledge.status) }}
                          </span>
                        </td>
                        <td>
                          <span class="embedding-badge" :class="knowledge.embeddingStatus">
                            {{ getEmbeddingStatusText(knowledge.embeddingStatus) }}
                          </span>
                        </td>
                        <td>{{ formatDate(knowledge.createTime) }}</td>
                        <td>
                          <div class="action-buttons">
                            <button class="btn btn-sm btn-outline" @click="viewKnowledge(knowledge)">查看</button>
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
          </div>
        </div>
      </div>
    </div>

    <!-- 知识创建/编辑模态框 -->
    <div v-if="showKnowledgeModal" class="modal-overlay" @click="closeKnowledgeModal">
      <div class="modal-dialog" @click.stop>
        <div class="modal-header">
          <h3>{{ isEditingKnowledge ? '编辑知识' : '创建知识' }}</h3>
          <button class="close-btn" @click="closeKnowledgeModal">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveKnowledge">
            <div class="form-group">
              <label>知识标题 <span class="required">*</span></label>
              <input 
                type="text" 
                v-model="knowledgeForm.title" 
                class="form-control" 
                placeholder="请输入知识标题"
                required
              >
            </div>
            <div class="form-group">
              <label>知识类型</label>
              <select v-model="knowledgeForm.type" class="form-control">
                <option value="document">文档</option>
                <option value="qa">问答</option>
                <option value="faq">常见问题</option>
              </select>
            </div>
            <div class="form-group">
              <label>知识分类</label>
              <input 
                type="text" 
                v-model="knowledgeForm.category" 
                class="form-control" 
                placeholder="请输入知识分类"
              >
            </div>
            <div class="form-group">
              <label>知识内容 <span class="required">*</span></label>
              <textarea 
                v-model="knowledgeForm.content" 
                class="form-control" 
                rows="8"
                placeholder="请输入知识内容"
                required
              ></textarea>
            </div>
            <div class="form-group">
              <label>标签</label>
              <input 
                type="text" 
                v-model="knowledgeForm.tags" 
                class="form-control" 
                placeholder="多个标签用逗号分隔"
              >
            </div>
            <div class="form-group">
              <label>来源URL</label>
              <input 
                type="url" 
                v-model="knowledgeForm.sourceUrl" 
                class="form-control" 
                placeholder="请输入来源URL（可选）"
              >
            </div>
            <div class="form-group">
              <label>状态</label>
              <select v-model="knowledgeForm.status" class="form-control">
                <option value="active">启用</option>
                <option value="inactive">禁用</option>
              </select>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeKnowledgeModal">取消</button>
          <button type="button" class="btn btn-primary" @click="saveKnowledge" :disabled="!knowledgeForm.title || !knowledgeForm.content">
            {{ isEditingKnowledge ? '更新' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 知识查看模态框 -->
    <div v-if="showViewKnowledgeModal" class="modal-overlay" @click="closeViewKnowledgeModal">
      <div class="modal-dialog modal-lg" @click.stop>
        <div class="modal-header">
          <h3>查看知识</h3>
          <button class="close-btn" @click="closeViewKnowledgeModal">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <div v-if="viewingKnowledge" class="knowledge-detail">
            <div class="detail-section">
              <h4>{{ viewingKnowledge.title }}</h4>
              <div class="knowledge-meta">
                <span class="type-badge" :class="viewingKnowledge.type">
                  {{ getTypeText(viewingKnowledge.type) }}
                </span>
                <span class="status-badge" :class="viewingKnowledge.status">
                  {{ getStatusText(viewingKnowledge.status) }}
                </span>
                <span v-if="viewingKnowledge.category" class="category-tag">
                  {{ viewingKnowledge.category }}
                </span>
              </div>
            </div>
            <div v-if="viewingKnowledge.content" class="detail-section">
              <h5>内容</h5>
              <div class="knowledge-content">{{ viewingKnowledge.content }}</div>
            </div>
            <div v-if="viewingKnowledge.tags" class="detail-section">
              <h5>标签</h5>
              <div class="tags-list">
                <span v-for="tag in viewingKnowledge.tags.split(',')" :key="tag" class="tag">
                  {{ tag.trim() }}
                </span>
              </div>
            </div>
            <div v-if="viewingKnowledge.sourceUrl" class="detail-section">
              <h5>来源</h5>
              <a :href="viewingKnowledge.sourceUrl" target="_blank" class="source-link">
                {{ viewingKnowledge.sourceUrl }}
                <i class="bi bi-box-arrow-up-right"></i>
              </a>
            </div>
            <div class="detail-section">
              <h5>基本信息</h5>
              <div class="info-grid">
                <div class="info-item">
                  <span class="label">创建时间：</span>
                  <span>{{ formatDate(viewingKnowledge.createTime) }}</span>
                </div>
                <div class="info-item">
                  <span class="label">更新时间：</span>
                  <span>{{ formatDate(viewingKnowledge.updateTime) }}</span>
                </div>
                <div class="info-item">
                  <span class="label">向量化状态：</span>
                  <span class="embedding-badge" :class="viewingKnowledge.embeddingStatus">
                    {{ getEmbeddingStatusText(viewingKnowledge.embeddingStatus) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeViewKnowledgeModal">关闭</button>
          <button type="button" class="btn btn-primary" @click="editKnowledge(viewingKnowledge)">编辑</button>
        </div>
      </div>
    </div>

    <!-- 添加数据源模态框 -->
    <div v-if="showAddDatasourceModal" class="modal-overlay" @click="closeAddDatasourceModal">
      <div class="modal-dialog datasource-modal" @click.stop>
        <div class="modal-header">
          <h3>{{ editingDatasource ? '编辑数据源' : '添加数据源' }}</h3>
          <button class="close-btn" @click="closeAddDatasourceModal">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="datasource-tabs">
            <button 
              class="tab-btn" 
              :class="{ active: datasourceTabMode === 'select' }"
              @click="datasourceTabMode = 'select'"
            >
              选择已有数据源
            </button>
            <button 
              class="tab-btn" 
              :class="{ active: datasourceTabMode === 'create' }"
              @click="datasourceTabMode = 'create'"
            >
              创建新数据源
            </button>
          </div>

          <!-- 选择已有数据源 -->
          <div v-if="datasourceTabMode === 'select'" class="tab-content">
            <div class="datasource-list">
              <div class="search-box">
                <i class="bi bi-search"></i>
                <input 
                  type="text" 
                  v-model="datasourceSearchKeyword" 
                  placeholder="搜索数据源名称"
                  @input="filterDatasources"
                >
              </div>
              <div class="datasource-items">
                <div 
                  v-for="datasource in filteredDatasources" 
                  :key="datasource.id"
                  class="datasource-item"
                  :class="{ selected: selectedDatasourceId === datasource.id }"
                  @click="selectedDatasourceId = datasource.id"
                >
                  <div class="datasource-info">
                    <h4>{{ datasource.name }}</h4>
                    <p>{{ getDatasourceTypeText(datasource.type) }} - {{ datasource.host }}:{{ datasource.port }}</p>
                    <p class="description">{{ datasource.description || '无描述' }}</p>
                  </div>
                </div>
                <div v-if="filteredDatasources.length === 0" class="empty-state">
                  <i class="bi bi-database"></i>
                  <p>暂无可用的数据源</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 创建新数据源 -->
          <div v-if="datasourceTabMode === 'create'" class="tab-content">
            <div class="datasource-form-container">
              <form @submit.prevent="saveDatasource">
              <div class="form-row">
                <div class="form-group">
                  <label for="datasourceName">数据源名称 *</label>
                  <input 
                    type="text" 
                    id="datasourceName"
                    v-model="datasourceForm.name" 
                    placeholder="请输入数据源名称"
                    required
                  >
                </div>
                <div class="form-group">
                  <label for="datasourceType">数据源类型 *</label>
                  <select 
                    id="datasourceType"
                    v-model="datasourceForm.type" 
                    required
                  >
                    <option value="">请选择数据源类型</option>
                    <option value="mysql">MySQL</option>
                    <option value="postgresql">PostgreSQL</option>
                  </select>
                </div>
              </div>
              <div class="form-row">
                <div class="form-group">
                  <label for="datasourceHost">主机地址 *</label>
                  <input 
                    type="text" 
                    id="datasourceHost"
                    v-model="datasourceForm.host" 
                    placeholder="例如：localhost 或 192.168.1.100"
                    required
                  >
                </div>
                <div class="form-group">
                  <label for="datasourcePort">端口号 *</label>
                  <input 
                    type="number" 
                    id="datasourcePort"
                    v-model.number="datasourceForm.port" 
                    placeholder="3306"
                    required
                  >
                </div>
              </div>
              <div class="form-row">
                <div class="form-group">
                  <label for="datasourceDatabase">数据库名 *</label>
                  <input 
                    type="text" 
                    id="datasourceDatabase"
                    v-model="datasourceForm.databaseName" 
                    placeholder="请输入数据库名称"
                    required
                  >
                </div>
              </div>
              <div class="form-row">
                <div class="form-group">
                  <label for="datasourceUsername">用户名 *</label>
                  <input 
                    type="text" 
                    id="datasourceUsername"
                    v-model="datasourceForm.username" 
                    placeholder="请输入数据库用户名"
                    required
                  >
                </div>
                <div class="form-group">
                  <label for="datasourcePassword">密码 *</label>
                  <input 
                    type="password" 
                    id="datasourcePassword"
                    v-model="datasourceForm.password" 
                    placeholder="请输入数据库密码"
                    required
                  >
                </div>
              </div>
              <div class="form-group">
                <label for="datasourceDescription">描述</label>
                <textarea 
                  id="datasourceDescription"
                  v-model="datasourceForm.description" 
                  placeholder="请输入数据源描述（可选）"
                  rows="3"
                ></textarea>
              </div>
              </form>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeAddDatasourceModal">取消</button>
          <button 
            v-if="datasourceTabMode === 'select'" 
            type="button" 
            class="btn btn-primary" 
            :disabled="!selectedDatasourceId"
            @click="addSelectedDatasource"
          >
            添加选中数据源
          </button>
          <button 
            v-if="datasourceTabMode === 'create'" 
            type="button" 
            class="btn btn-primary" 
            @click="saveDatasource"
          >
            {{ editingDatasource ? '保存' : '创建并添加' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { agentApi, businessKnowledgeApi, semanticModelApi, agentKnowledgeApi, datasourceApi } from '../utils/api.js'

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
      status: 'draft',
      createdAt: '',
      updatedAt: '',
      avatar: '',
      prompt: '',
      category: '',
      adminId: '',
      tags: ''
    })
    
    // 消息提示
    const message = reactive({
      show: false,
      text: '',
      type: 'success' // success, error, warning, info
    })
    
    const businessKnowledgeList = ref([])
    const semanticModelList = ref([])
    const knowledgeDocuments = ref([])
    const datasourceList = ref([])
    
    const showCreateKnowledgeModal = ref(false)
    const showCreateModelModal = ref(false)
    const showUploadModal = ref(false)
    const showAddDatasourceModal = ref(false)
    
    // 数据源相关数据
    const agentDatasourceList = ref([])
    const allDatasourceList = ref([])
    const filteredDatasources = ref([])
    const datasourceSearchKeyword = ref('')
    const datasourceTabMode = ref('select') // 'select' 或 'create'
    const selectedDatasourceId = ref(null)
    const editingDatasource = ref(null)
    const datasourceForm = reactive({
      name: '',
      type: '',
      host: '',
      port: 3306,
      databaseName: '',
      username: '',
      password: '',
      description: ''
    })
    
    // 数据源测试相关
    const showTestResult = ref(false)
    const testResultMessage = ref('')
    
    // 智能体知识相关数据
    const knowledgeList = ref([])
    const knowledgeStats = reactive({
      totalCount: 0,
      typeStats: []
    })
    const knowledgeFilters = reactive({
      keyword: '',
      type: '',
      status: ''
    })
    const showKnowledgeModal = ref(false)
    const showViewKnowledgeModal = ref(false)
    const isEditingKnowledge = ref(false)
    const editingKnowledgeId = ref(null)
    const viewingKnowledge = ref(null)
    const knowledgeForm = reactive({
      title: '',
      content: '',
      type: 'document',
      category: '',
      tags: '',
      status: 'active',
      sourceUrl: ''
    })
    
    // 方法
    const setActiveTab = (tab) => {
      activeTab.value = tab
      loadTabData(tab)
    }
    
    const goBack = () => {
      router.push('/agents')
    }
    
    const loadAgentDetail = async () => {
      try {
        const agentId = route.params.id
        const response = await agentApi.getDetail(agentId)
        // 后端直接返回智能体对象，不是包装在data字段中
        Object.assign(agent, {
          id: response.id,
          name: response.name || '',
          description: response.description || '',
          status: response.status || 'draft',
          createTime: response.createTime || '',
          updateTime: response.updateTime || '',
          avatar: response.avatar || '',
          prompt: response.prompt || '',
          category: response.category || '',
          adminId: response.adminId || '',
          tags: response.tags || ''
        })
        console.log('智能体详情加载成功:', agent)
      } catch (error) {
        console.error('加载智能体详情失败:', error)
        alert('加载智能体详情失败，请重试')
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
          await loadAgentKnowledge()
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
        // 加载智能体关联的数据源
        const agentDatasources = await datasourceApi.getAgentDatasources(agent.id)
        agentDatasourceList.value = agentDatasources
        console.log('智能体数据源加载成功:', agentDatasources)
      } catch (error) {
        console.error('加载智能体数据源失败:', error)
      }
    }

    const loadAllDatasources = async () => {
      try {
        // 加载所有可用的数据源
        const datasources = await datasourceApi.getList({ status: 'active' })
        allDatasourceList.value = datasources
        filteredDatasources.value = datasources
        console.log('所有数据源加载成功:', datasources)
      } catch (error) {
        console.error('加载数据源列表失败:', error)
      }
    }

    const filterDatasources = () => {
      const keyword = datasourceSearchKeyword.value.toLowerCase()
      if (!keyword) {
        filteredDatasources.value = allDatasourceList.value
      } else {
        filteredDatasources.value = allDatasourceList.value.filter(datasource =>
          datasource.name.toLowerCase().includes(keyword) ||
          datasource.description?.toLowerCase().includes(keyword)
        )
      }
    }

    const openAddDatasourceModal = () => {
      console.log('打开数据源模态框')
      showAddDatasourceModal.value = true
      datasourceTabMode.value = 'select'
      selectedDatasourceId.value = null
      resetDatasourceForm()
      loadAllDatasources()
    }

    const closeAddDatasourceModal = () => {
      showAddDatasourceModal.value = false
      datasourceTabMode.value = 'select'
      selectedDatasourceId.value = null
      editingDatasource.value = null
      resetDatasourceForm()
    }

    const resetDatasourceForm = () => {
      Object.assign(datasourceForm, {
        name: '',
        type: '',
        host: '',
        port: 3306,
        databaseName: '',
        username: '',
        password: '',
        description: ''
      })
    }

    const addSelectedDatasource = async () => {
      if (!selectedDatasourceId.value) {
        showMessage('请选择一个数据源', 'warning')
        return
      }

      try {
        await datasourceApi.addToAgent(agent.id, selectedDatasourceId.value)
        showMessage('数据源添加成功', 'success')
        loadDatasources()
        closeAddDatasourceModal()
      } catch (error) {
        console.error('添加数据源失败:', error)
        showMessage('添加数据源失败，请重试', 'error')
      }
    }

    const saveDatasource = async () => {
      try {
        // 创建新数据源
        const newDatasource = await datasourceApi.create({ ...datasourceForm })
        console.log('数据源创建成功:', newDatasource)
        
        // 将新数据源添加到智能体
        await datasourceApi.addToAgent(agent.id, newDatasource.id)
        showMessage('数据源创建并添加成功', 'success')
        
        loadDatasources()
        closeAddDatasourceModal()
      } catch (error) {
        console.error('创建数据源失败:', error)
        showMessage('创建数据源失败，请重试', 'error')
      }
    }

    const testDatasourceConnection = async (datasourceId) => {
      try {
        const result = await datasourceApi.testConnection(datasourceId)
        if (result.success) {
          showMessage('连接测试成功', 'success')
        } else {
          showMessage('连接测试失败：' + result.message, 'error')
        }
        // 重新加载数据源状态
        loadDatasources()
      } catch (error) {
        console.error('连接测试失败:', error)
        showMessage('连接测试失败，请重试', 'error')
      }
    }

    const removeDatasourceFromAgent = async (datasourceId) => {
      if (!confirm('确定要移除这个数据源吗？')) {
        return
      }

      try {
        await datasourceApi.removeFromAgent(agent.id, datasourceId)
        showMessage('数据源移除成功', 'success')
        loadDatasources()
      } catch (error) {
        console.error('移除数据源失败:', error)
        showMessage('移除数据源失败，请重试', 'error')
      }
    }

    const toggleDatasourceStatus = async (datasourceId, isActive) => {
      try {
        await datasourceApi.toggleDatasource(agent.id, datasourceId, isActive)
        showMessage(isActive ? '数据源已启用' : '数据源已禁用', 'success')
        loadDatasources()
      } catch (error) {
        console.error('切换数据源状态失败:', error)
        console.error('错误响应数据:', error.response?.data)
        
        let errorMessage = '操作失败，请重试'
        
        // 优先使用服务器返回的错误信息
        if (error.response?.data?.message) {
          errorMessage = error.response.data.message
        } else if (error.message) {
          errorMessage = error.message
        }
        
        showMessage(errorMessage, 'error')
      }
    }
    
    // 消息提示方法
    const showMessage = (text, type = 'success') => {
      message.text = text
      message.type = type
      message.show = true
      
      // 3秒后自动隐藏
      setTimeout(() => {
        hideMessage()
      }, 3000)
    }
    
    const hideMessage = () => {
      message.show = false
    }
    
    const getMessageIcon = (type) => {
      const iconMap = {
        success: 'bi bi-check-circle-fill',
        error: 'bi bi-exclamation-circle-fill',
        warning: 'bi bi-exclamation-triangle-fill',
        info: 'bi bi-info-circle-fill'
      }
      return iconMap[type] || iconMap.info
    }
    
    // 数据源相关的辅助方法
    const getDatasourceTypeText = (type) => {
      const typeMap = {
        mysql: 'MySQL',
        postgresql: 'PostgreSQL'
      }
      return typeMap[type] || type
    }

    const getTestStatusText = (testStatus) => {
      const statusMap = {
        success: '连接成功',
        failed: '连接失败',
        unknown: '未测试'
      }
      return statusMap[testStatus] || testStatus
    }
    
    const loadAgentKnowledge = async () => {
      try {
        const agentId = route.params.id
        const response = await agentKnowledgeApi.getByAgentId(agentId, {
          type: knowledgeFilters.type,
          status: knowledgeFilters.status,
          keyword: knowledgeFilters.keyword
        })
        if (response.success) {
          knowledgeList.value = response.data || []
        }
        
        // 加载统计信息
        const statsResponse = await agentKnowledgeApi.getStatistics(agentId)
        if (statsResponse.success) {
          knowledgeStats.totalCount = statsResponse.data.totalCount || 0
          knowledgeStats.typeStats = (statsResponse.data.typeStatistics || []).map(stat => ({
            type: stat[0],
            count: stat[1]
          }))
        }
      } catch (error) {
        console.error('加载智能体知识失败:', error)
        knowledgeList.value = []
      }
    }

    // 智能体知识管理方法
    const openCreateKnowledgeModal = () => {
      resetKnowledgeForm()
      isEditingKnowledge.value = false
      editingKnowledgeId.value = null
      showKnowledgeModal.value = true
    }

    const closeKnowledgeModal = () => {
      showKnowledgeModal.value = false
      resetKnowledgeForm()
    }

    const resetKnowledgeForm = () => {
      Object.assign(knowledgeForm, {
        title: '',
        content: '',
        type: 'document',
        category: '',
        tags: '',
        status: 'active',
        sourceUrl: ''
      })
    }

    const editKnowledge = (knowledge) => {
      Object.assign(knowledgeForm, {
        title: knowledge.title || '',
        content: knowledge.content || '',
        type: knowledge.type || 'document',
        category: knowledge.category || '',
        tags: knowledge.tags || '',
        status: knowledge.status || 'active',
        sourceUrl: knowledge.sourceUrl || ''
      })
      isEditingKnowledge.value = true
      editingKnowledgeId.value = knowledge.id
      showKnowledgeModal.value = true
      if (showViewKnowledgeModal.value) {
        showViewKnowledgeModal.value = false
      }
    }

    const viewKnowledge = (knowledge) => {
      viewingKnowledge.value = knowledge
      showViewKnowledgeModal.value = true
    }

    const closeViewKnowledgeModal = () => {
      showViewKnowledgeModal.value = false
      viewingKnowledge.value = null
    }

    const saveKnowledge = async () => {
      try {
        const agentId = route.params.id
        const knowledgeData = {
          ...knowledgeForm,
          agentId: parseInt(agentId),
          creatorId: 2100246635 // 默认创建者ID
        }

        if (isEditingKnowledge.value) {
          const response = await agentKnowledgeApi.update(editingKnowledgeId.value, knowledgeData)
          if (response.success) {
            alert('知识更新成功')
            await loadAgentKnowledge()
            closeKnowledgeModal()
          } else {
            alert('更新失败：' + (response.message || '未知错误'))
          }
        } else {
          const response = await agentKnowledgeApi.create(knowledgeData)
          if (response.success) {
            alert('知识创建成功')
            await loadAgentKnowledge()
            closeKnowledgeModal()
          } else {
            alert('创建失败：' + (response.message || '未知错误'))
          }
        }
      } catch (error) {
        console.error('保存知识失败:', error)
        alert('保存失败：' + error.message)
      }
    }

    const deleteKnowledge = async (id) => {
      if (confirm('确定要删除这条知识吗？')) {
        try {
          const response = await agentKnowledgeApi.delete(id)
          if (response.success) {
            alert('删除成功')
            await loadAgentKnowledge()
          } else {
            alert('删除失败：' + (response.message || '未知错误'))
          }
        } catch (error) {
          console.error('删除知识失败:', error)
          alert('删除失败：' + error.message)
        }
      }
    }

    const searchKnowledge = async () => {
      await loadAgentKnowledge()
    }

    const filterKnowledge = async () => {
      await loadAgentKnowledge()
    }

    // 工具方法
    const getTypeText = (type) => {
      const typeMap = {
        'document': '文档',
        'qa': '问答',
        'faq': '常见问题'
      }
      return typeMap[type] || type
    }

    const getStatusText = (status) => {
      const statusMap = {
        'active': '启用',
        'inactive': '禁用',
        'draft': '待发布',
        'published': '已发布',
        'offline': '已下线'
      }
      return statusMap[status] || status
    }

    const getEmbeddingStatusText = (status) => {
      const statusMap = {
        'pending': '待处理',
        'processing': '处理中',
        'completed': '已完成',
        'failed': '失败'
      }
      return statusMap[status] || status
    }

    const getKnowledgeIcon = (type) => {
      const iconMap = {
        'document': 'bi-file-text',
        'qa': 'bi-question-circle',
        'faq': 'bi-chat-square-text'
      }
      return iconMap[type] || 'bi-file-text'
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return '-'
      const date = new Date(dateStr)
      return date.toLocaleString('zh-CN')
    }
    
    const formatDateTime = (dateStr) => {
      if (!dateStr) return '-'
      const date = new Date(dateStr)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }
    
    const updateAgent = async () => {
      try {
        // 准备提交的数据，过滤掉只读字段
        const updateData = {
          name: agent.name,
          description: agent.description,
          status: agent.status,
          category: agent.category,
          tags: agent.tags,
          prompt: agent.prompt || '',
          adminId: agent.adminId || null
        }
        
        const response = await agentApi.update(agent.id, updateData)
        console.log('更新响应:', response)
        alert('更新成功')
        
        // 重新加载智能体详情以获取最新的更新时间
        await loadAgentDetail()
      } catch (error) {
        console.error('更新智能体失败:', error)
        alert('更新失败：' + (error.message || '未知错误'))
      }
    }
    
    const editBusinessKnowledge = (knowledge) => {
      // TODO: 实现编辑业务知识功能
      console.log('编辑业务知识:', knowledge)
    }
    
    const deleteBusinessKnowledge = async (id) => {
      if (confirm('确定要删除这条业务知识吗？')) {
        try {
          await businessKnowledgeApi.delete(id)
          await loadBusinessKnowledge()
        } catch (error) {
          console.error('删除业务知识失败:', error)
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
    onMounted(async () => {
      await loadAgentDetail()
      // 如果默认打开知识库配置tab，则加载知识数据
      if (activeTab.value === 'knowledge') {
        await loadAgentKnowledge()
      }
    })
    
    return {
      activeTab,
      agent,
      message,
      businessKnowledgeList,
      semanticModelList,
      knowledgeDocuments,
      datasourceList,
      showCreateKnowledgeModal,
      showCreateModelModal,
      showUploadModal,
      showAddDatasourceModal,
      // 智能体知识相关
      knowledgeList,
      knowledgeStats,
      knowledgeFilters,
      showKnowledgeModal,
      showViewKnowledgeModal,
      isEditingKnowledge,
      viewingKnowledge,
      knowledgeForm,
      // 数据源相关
      agentDatasourceList,
      allDatasourceList,
      filteredDatasources,
      datasourceSearchKeyword,
      datasourceTabMode,
      selectedDatasourceId,
      editingDatasource,
      datasourceForm,
      showTestResult,
      testResultMessage,
      // 方法
      setActiveTab,
      goBack,
      updateAgent,
      editBusinessKnowledge,
      deleteBusinessKnowledge,
      editModel,
      deleteModel,
      viewDocument,
      deleteDocument,
      // 智能体知识方法
      openCreateKnowledgeModal,
      closeKnowledgeModal,
      editKnowledge,
      viewKnowledge,
      closeViewKnowledgeModal,
      saveKnowledge,
      deleteKnowledge,
      searchKnowledge,
      filterKnowledge,
      // 数据源方法
      openAddDatasourceModal,
      closeAddDatasourceModal,
      filterDatasources,
      addSelectedDatasource,
      saveDatasource,
      testDatasourceConnection,
      removeDatasourceFromAgent,
      toggleDatasourceStatus,
      getDatasourceTypeText,
      getTestStatusText,
      // 工具方法
      getTypeText,
      getStatusText,
      getEmbeddingStatusText,
      getKnowledgeIcon,
      formatDate,
      formatDateTime,
      getRandomColor,
      getRandomIcon,
      // 消息提示方法
      showMessage,
      hideMessage,
      getMessageIcon
    }
  }
}
</script>

<style scoped>
.agent-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  position: relative;
}

/* 消息提示样式 */
.message-toast {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 9999;
  min-width: 320px;
  max-width: 480px;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  animation: slideInRight 0.3s ease-out;
  font-size: 14px;
  font-weight: 500;
}

.message-toast.success {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #52c41a;
}

.message-toast.error {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #ff4d4f;
}

.message-toast.warning {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  color: #faad14;
}

.message-toast.info {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  color: #1890ff;
}

.message-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.message-content i {
  font-size: 16px;
}

.message-close {
  background: none;
  border: none;
  color: inherit;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  padding: 0;
  margin-left: 12px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.message-close:hover {
  opacity: 1;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
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
  background: #f5f5f5;
  color: #999;
  border: 1px solid #d9d9d9;
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

/* 启用/禁用按钮样式 */
.toggle-btn {
  position: relative;
  transition: all 0.3s ease;
  border: none;
  border-radius: 20px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 80px;
  justify-content: center;
}

.btn-active {
  background: linear-gradient(135deg, #52c41a, #73d13d);
  color: white;
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3);
}

.btn-active:hover {
  background: linear-gradient(135deg, #389e0d, #52c41a);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.4);
}

.btn-inactive {
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  color: #666;
  border: 1px solid #d9d9d9;
}

.btn-inactive:hover {
  background: linear-gradient(135deg, #52c41a, #73d13d);
  color: white;
  border-color: #52c41a;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.3);
}

/* 图标样式 */
.icon-toggle-on::before {
  content: '●';
  color: white;
  font-size: 12px;
}

.icon-toggle-off::before {
  content: '○';
  color: #999;
  font-size: 12px;
}

/* 模态框基础样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
  box-sizing: border-box;
}

.modal-dialog {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  max-width: 600px;
  width: 100%;
  max-height: 90vh;
  overflow: hidden;
  animation: modalSlideIn 0.3s ease-out;
}

.modal-dialog.modal-lg {
  max-width: 800px;
}

.modal-header {
  padding: 24px 24px 16px 24px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.modal-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.modal-footer {
  padding: 16px 24px 24px 24px;
  border-top: 1px solid #e8e8e8;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #666;
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 数据源模态框专用样式 */
.modal-dialog.datasource-modal {
  max-width: 900px;
  max-height: 85vh;
  height: auto;
}

/* 数据源模态框内的Tab切换 */
.datasource-modal .datasource-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  border-radius: 8px;
  padding: 4px;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #e6e6e6;
}

.datasource-modal .tab-btn {
  flex: 1;
  padding: 10px 20px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 500;
  color: #666;
  font-size: 14px;
  position: relative;
  overflow: hidden;
}

.datasource-modal .tab-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.6s;
}

.datasource-modal .tab-btn:hover {
  color: #1890ff;
  background: rgba(24, 144, 255, 0.08);
  transform: translateY(-1px);
}

.datasource-modal .tab-btn:hover::before {
  left: 100%;
}

.datasource-modal .tab-btn.active {
  background: linear-gradient(135deg, white, #fafbfc);
  color: #1890ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15), 0 2px 4px rgba(0, 0, 0, 0.08);
  font-weight: 600;
  transform: translateY(-1px);
}

/* 数据源列表容器 */
.datasource-modal .datasource-list {
  background: linear-gradient(135deg, #fafcff, #f8fafc);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e8f0fe;
  box-shadow: inset 0 1px 4px rgba(24, 144, 255, 0.03);
  height: 100%;
  max-height: 50vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 搜索框样式 - 仅限数据源模态框 */
.datasource-modal .search-box {
  position: relative;
  margin-bottom: 12px;
  width: 100%;
  flex-shrink: 0;
}

.datasource-modal .search-box i {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  font-size: 14px;
  z-index: 2;
  transition: color 0.3s ease;
}

.datasource-modal .search-box input {
  width: 100%;
  padding: 10px 12px 10px 36px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.datasource-modal .search-box input:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.1), 0 4px 12px rgba(24, 144, 255, 0.15);
  background: #fafcff;
  transform: translateY(-1px);
}

.datasource-modal .search-box input:focus + i,
.datasource-modal .search-box:hover i {
  color: #1890ff;
}

.datasource-modal .search-box input::placeholder {
  color: #bbb;
  transition: color 0.3s ease;
}

.datasource-modal .search-box input:focus::placeholder {
  color: #999;
}

/* 数据源项目列表 */
.datasource-modal .datasource-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
  margin-right: -4px;
}

/* 单个数据源项目卡片 */
.datasource-modal .datasource-item {
  padding: 16px;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: block;
  background: linear-gradient(135deg, white, #fafbfc);
  position: relative;
  overflow: hidden;
  min-height: 80px;
}

.datasource-modal .datasource-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: transparent;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.datasource-modal .datasource-item::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(24, 144, 255, 0.03) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.datasource-modal .datasource-item:hover {
  border-color: #1890ff;
  box-shadow: 0 12px 28px rgba(24, 144, 255, 0.12), 0 4px 8px rgba(0, 0, 0, 0.04);
  transform: translateY(-3px);
  background: linear-gradient(135deg, #fafcff, #f0f8ff);
}

.datasource-modal .datasource-item:hover::before {
  background: linear-gradient(180deg, #1890ff, #40a9ff);
  box-shadow: 2px 0 8px rgba(24, 144, 255, 0.3);
}

.datasource-modal .datasource-item:hover::after {
  opacity: 1;
}

.datasource-modal .datasource-item.selected {
  border-color: #1890ff;
  background: linear-gradient(135deg, #f0f8ff, #e6f4ff);
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.18), 0 4px 8px rgba(24, 144, 255, 0.08);
  transform: translateY(-2px);
}

.datasource-modal .datasource-item.selected::before {
  background: linear-gradient(180deg, #1890ff, #40a9ff);
  box-shadow: 2px 0 12px rgba(24, 144, 255, 0.4);
  width: 6px;
}

.datasource-modal .datasource-item.selected::after {
  opacity: 1;
}

/* 数据源信息区域 */
.datasource-modal .datasource-info {
  width: 100%;
}

.datasource-modal .datasource-info h4 {
  margin: 0 0 6px 0;
  color: #1a1a1a;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.4;
  transition: color 0.3s ease;
}

.datasource-modal .datasource-item:hover .datasource-info h4 {
  color: #1890ff;
}

.datasource-modal .datasource-info p {
  margin: 0 0 4px 0;
  color: #666;
  font-size: 13px;
  line-height: 1.5;
  transition: color 0.3s ease;
}

.datasource-modal .datasource-item:hover .datasource-info p {
  color: #444;
}

.datasource-modal .datasource-info .description {
  color: #999;
  font-style: italic;
  font-size: 12px;
  margin-top: 6px;
  padding: 6px 10px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 6px;
  border-left: 3px solid #e8e8e8;
  transition: all 0.3s ease;
}

.datasource-modal .datasource-item:hover .datasource-info .description {
  background: rgba(24, 144, 255, 0.05);
  border-left-color: #1890ff;
  color: #666;
}

/* 数据源状态区域 */
.datasource-modal .datasource-status {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-end;
  min-width: 85px;
}

.datasource-modal .test-status,
.datasource-modal .status-badge {
  padding: 6px 10px;
  border-radius: 16px;
  font-size: 11px;
  font-weight: 600;
  text-align: center;
  min-width: 70px;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.datasource-modal .test-status.success {
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  color: #389e0d;
  border: 1px solid #95de64;
  box-shadow: 0 2px 4px rgba(56, 158, 13, 0.1);
}

.datasource-modal .test-status.failed {
  background: linear-gradient(135deg, #fff2f0, #ffccc7);
  color: #cf1322;
  border: 1px solid #ff7875;
  box-shadow: 0 2px 4px rgba(207, 19, 34, 0.1);
}

.datasource-modal .test-status.unknown {
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  color: #666;
  border: 1px solid #d9d9d9;
  box-shadow: 0 2px 4px rgba(102, 102, 102, 0.1);
}

.datasource-modal .status-badge.active {
  background: linear-gradient(135deg, #e6f4ff, #bae7ff);
  color: #0958d9;
  border: 1px solid #69c0ff;
  box-shadow: 0 2px 4px rgba(9, 88, 217, 0.1);
}

.datasource-modal .status-badge.inactive {
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  color: #666;
  border: 1px solid #d9d9d9;
  box-shadow: 0 2px 4px rgba(102, 102, 102, 0.1);
}

/* 自定义滚动条样式 - 仅限数据源模态框 */
.datasource-modal .datasource-items::-webkit-scrollbar {
  width: 8px;
}

.datasource-modal .datasource-items::-webkit-scrollbar-track {
  background: #f0f4f8;
  border-radius: 8px;
  margin: 4px 0;
}

.datasource-modal .datasource-items::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #c1d5e0, #a0b4c7);
  border-radius: 8px;
  transition: all 0.3s ease;
  border: 2px solid #f0f4f8;
}

.datasource-modal .datasource-items::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #1890ff, #40a9ff);
  border-color: #e6f4ff;
}

/* 空状态样式 - 仅限数据源模态框 */
.datasource-modal .empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  background: linear-gradient(135deg, #fafcff, #f0f8ff);
  border-radius: 12px;
  border: 2px dashed #d0e4ff;
}

.datasource-modal .empty-state i {
  font-size: 42px;
  margin-bottom: 12px;
  color: #bbb;
  background: linear-gradient(135deg, #e6f4ff, #d0e4ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.datasource-modal .empty-state p {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: #666;
}

/* 数据源表单样式 - 仅限数据源模态框内 */
.datasource-modal .form-row {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.datasource-modal .form-row .form-group {
  flex: 1;
}

.datasource-modal .form-group {
  margin-bottom: 12px;
}

.datasource-modal .form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: #333;
  font-size: 13px;
}

.datasource-modal .form-group input,
.datasource-modal .form-group select,
.datasource-modal .form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  font-size: 13px;
  background: white;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;
}

.datasource-modal .form-group input:focus,
.datasource-modal .form-group select:focus,
.datasource-modal .form-group textarea:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.1);
  background: #fafcff;
  transform: translateY(-1px);
}

.datasource-modal .form-group input::placeholder,
.datasource-modal .form-group textarea::placeholder {
  color: #bbb;
}

.datasource-modal .form-group textarea {
  resize: vertical;
  min-height: 60px;
}

/* 数据源表单容器 */
.datasource-modal .datasource-form-container {
  background: linear-gradient(135deg, #fafcff, #f8fafc);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e8f0fe;
  box-shadow: inset 0 1px 4px rgba(24, 144, 255, 0.03);
  max-height: 50vh;
  overflow-y: auto;
}

/* 数据源表单容器滚动条 */
.datasource-modal .datasource-form-container::-webkit-scrollbar {
  width: 6px;
}

.datasource-modal .datasource-form-container::-webkit-scrollbar-track {
  background: #f0f4f8;
  border-radius: 6px;
  margin: 4px 0;
}

.datasource-modal .datasource-form-container::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #c1d5e0, #a0b4c7);
  border-radius: 6px;
  transition: all 0.3s ease;
}

.datasource-modal .datasource-form-container::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #1890ff, #40a9ff);
}
</style>