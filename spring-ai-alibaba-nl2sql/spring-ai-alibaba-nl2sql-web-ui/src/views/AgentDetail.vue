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
  <div class="agent-detail-page">
    <!-- 消息提示组件 -->
    <div v-if="message.show" class="message-toast" :class="message.type">
      <div class="message-content">
        <i :class="getMessageIcon(message.type)"></i>
        <span>{{ message.text }}</span>
      </div>
      <button class="message-close" @click="hideMessage">×</button>
    </div>

    <!-- 现代化头部导航 -->
    <header class="page-header">
      <div class="header-content">
        <div class="brand-section">
          <div class="brand-logo" @click="goToHome">
            <i class="bi bi-robot"></i>
            <span class="brand-text">数据智能体</span>
          </div>
          <nav class="header-nav">
            <div class="nav-item" @click="goToAgentList">
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
              </div>

              <div class="nav-section">
                <div class="nav-section-title">预设问题配置</div>
                <a href="#" class="nav-link" :class="{ active: activeTab === 'preset-questions' }" @click="setActiveTab('preset-questions')">
                  <i class="bi bi-question-circle"></i>
                  预设问题管理
                </a>
              </div>

              <div class="nav-section">
                <div class="nav-section-title">调试工具</div>
                <a href="#" class="nav-link" :class="{ active: activeTab === 'debug' }" @click="setActiveTab('debug')">
                  <i class="bi bi-bug"></i>
                  智能体调试
                </a>
              </div>

              <div class="nav-section">
                <div class="nav-section-title">应用入口</div>
                <a href="#" class="nav-link" @click="goToWorkspace">
                  <i class="bi bi-chat-dots"></i>
                  智能体工作台
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
                        <th>业务名词</th>
                        <th>描述</th>
                        <th>同义词</th>
                        <th>默认召回</th>
                        <th>创建时间</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="knowledge in businessKnowledgeList" :key="knowledge.id">
                        <td>{{ knowledge.id }}</td>
                        <td>{{ knowledge.businessTerm }}</td>
                        <td>{{ knowledge.description ? knowledge.description.substring(0, 50) + '...' : '-' }}</td>
                        <td>{{ knowledge.synonyms || '-' }}</td>
                        <td>
                          <span class="status-badge" :class="knowledge.defaultRecall ? 'active' : 'inactive'">
                            {{ knowledge.defaultRecall ? '是' : '否' }}
                          </span>
                        </td>
                        <td>{{ formatDate(knowledge.createTime) }}</td>
                        <td>
                          <div class="action-buttons">
                            <button class="btn btn-sm btn-outline" @click="editBusinessKnowledge(knowledge)">编辑</button>
                            <button class="btn btn-sm btn-danger" @click="deleteBusinessKnowledge(knowledge.id)">删除</button>
                          </div>
                        </td>
                      </tr>
                      <tr v-if="businessKnowledgeList.length === 0">
                        <td colspan="7" class="text-center">暂无业务知识数据</td>
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
              
              <!-- 搜索和筛选 -->
              <div class="model-filters">
                <div class="filter-group">
                  <div class="search-box">
                    <i class="bi bi-search"></i>
                    <input 
                      type="text" 
                      v-model="modelFilters.keyword" 
                      placeholder="搜索字段名..." 
                      class="form-control"
                      @input="filterSemanticModels"
                    >
                  </div>
                </div>
                <div class="filter-group">
                  <select v-model="modelFilters.fieldType" @change="filterSemanticModels" class="form-control">
                    <option value="">全部类型</option>
                    <option value="VARCHAR">VARCHAR</option>
                    <option value="INTEGER">INTEGER</option>
                    <option value="DECIMAL">DECIMAL</option>
                    <option value="DATE">DATE</option>
                    <option value="DATETIME">DATETIME</option>
                    <option value="TEXT">TEXT</option>
                  </select>
                </div>
                <div class="filter-group">
                  <select v-model="modelFilters.enabled" @change="filterSemanticModels" class="form-control">
                    <option value="">全部状态</option>
                    <option value="true">启用</option>
                    <option value="false">禁用</option>
                  </select>
                </div>
                <div class="filter-group">
                  <select v-model="modelFilters.defaultRecall" @change="filterSemanticModels" class="form-control">
                    <option value="">全部召回状态</option>
                    <option value="true">默认召回</option>
                    <option value="false">非默认召回</option>
                  </select>
                </div>
              </div>
              
              <div class="semantic-model-section">
                <div class="section-header">
                  <h3>语义模型列表</h3>
                  <div class="header-actions">
                    <div class="batch-actions" v-if="selectedModels.length > 0">
                      <span class="selected-count">已选择 {{ selectedModels.length }} 项</span>
                      <button class="btn btn-sm btn-outline" @click="batchToggleStatus(true)">批量启用</button>
                      <button class="btn btn-sm btn-outline" @click="batchToggleStatus(false)">批量禁用</button>
                      <button class="btn btn-sm btn-danger" @click="batchDeleteModels">批量删除</button>
                    </div>
                    <button class="btn btn-primary" @click="showCreateModelModal = true">
                      <i class="bi bi-plus"></i>
                      添加模型
                    </button>
                  </div>
                </div>
                <div class="semantic-model-table">
                  <table class="table">
                    <thead>
                      <tr>
                        <th>
                          <input type="checkbox" @change="toggleSelectAll" 
                            :checked="isAllSelected" 
                            :indeterminate="isPartialSelected">
                        </th>
                        <th>ID</th>
                        <th>原始字段名</th>
                        <th>智能体字段名</th>
                        <th>字段同义词</th>
                        <th>字段类型</th>
                        <th>默认召回</th>
                        <th>启用状态</th>
                        <th>创建时间</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="filteredSemanticModels.length === 0">
                        <td colspan="11" class="text-center text-muted">
                          {{ semanticModelList.length === 0 ? '暂无数据' : '无符合条件的数据' }}
                        </td>
                      </tr>
                      <tr v-else v-for="model in filteredSemanticModels" :key="model.id">
                        <td>
                          <input type="checkbox" v-model="selectedModels" :value="model.id">
                        </td>
                        <td>{{ model.id }}</td>
                        <td><strong>{{ model.originalFieldName }}</strong></td>
                        <td>{{ model.agentFieldName || '-' }}</td>
                        <td>{{ model.fieldSynonyms || '-' }}</td>
                        <td><span class="badge badge-secondary">{{ model.fieldType || '-' }}</span></td>
                        <td>
                          <span class="badge" :class="model.defaultRecall ? 'badge-success' : 'badge-secondary'">
                            {{ model.defaultRecall ? '是' : '否' }}
                          </span>
                        </td>
                        <td>
                          <span class="badge" :class="model.enabled ? 'badge-success' : 'badge-secondary'">
                            {{ model.enabled ? '启用' : '禁用' }}
                          </span>
                        </td>
                        <td>{{ formatDateTime(model.createTime) }}</td>
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

            <!-- 预设问题管理 -->
            <div v-if="activeTab === 'preset-questions'" class="tab-content">
              <div class="content-header">
                <h2>预设问题管理</h2>
                <p class="content-subtitle">配置智能体工作台显示的预设问题</p>
              </div>
              <div class="preset-questions-section">
                <div class="section-header">
                  <h3>预设问题列表</h3>
                  <button class="btn btn-primary" @click="addPresetQuestion">
                    <i class="bi bi-plus"></i>
                    添加问题
                  </button>
                </div>
                <div class="questions-list">
                  <div v-if="presetQuestions.length === 0" class="empty-questions">
                    <i class="bi bi-question-circle"></i>
                    <p>暂无预设问题，点击"添加问题"开始配置</p>
                  </div>
                  <div v-else>
                    <div 
                      v-for="(question, index) in presetQuestions" 
                      :key="index"
                      class="question-item"
                    >
                      <div class="question-content">
                        <div class="question-number">{{ index + 1 }}</div>
                        <input 
                          type="text" 
                          v-model="question.question" 
                          class="question-input"
                          placeholder="请输入预设问题..."
                        >
                      </div>
                      <div class="question-actions">
                        <button class="btn btn-sm btn-outline" @click="moveQuestionUp(index)" :disabled="index === 0">
                          <i class="bi bi-arrow-up"></i>
                        </button>
                        <button class="btn btn-sm btn-outline" @click="moveQuestionDown(index)" :disabled="index === presetQuestions.length - 1">
                          <i class="bi bi-arrow-down"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" @click="removePresetQuestion(index)">
                          <i class="bi bi-trash"></i>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="form-actions" v-if="presetQuestions.length > 0">
                  <button class="btn btn-primary" @click="savePresetQuestions">保存配置</button>
                  <button class="btn btn-secondary" @click="loadPresetQuestions">重置</button>
                </div>
              </div>
            </div>

            <!-- 智能体调试 -->
            <div v-if="activeTab === 'debug'" class="tab-content">
              <AgentDebugPanel :agent-id="agent.id" />
            </div>

          </div>
        </div>
      </div>
    </div>

    <!-- 语义模型模态框 -->
    <div v-if="showCreateModelModal" class="modal-overlay" @click="closeModelModal">
      <div class="modal-dialog" @click.stop>
        <div class="modal-header">
          <h3>{{ isEditingModel ? '编辑语义模型' : '新增语义模型' }}</h3>
          <button class="close-btn" @click="closeModelModal">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveModel">
            <div class="form-group">
              <label class="form-label" for="originalFieldName">原始字段名 *</label>
              <input 
                type="text" 
                id="originalFieldName"
                v-model="semanticModelForm.originalFieldName"
                class="form-control" 
                :class="{ 'is-invalid': formErrors.originalFieldName }"
                required
                placeholder="如：user_age"
                @input="clearFieldError('originalFieldName')"
              >
              <div v-if="formErrors.originalFieldName" class="invalid-feedback">
                {{ formErrors.originalFieldName }}
              </div>
            </div>
            <div class="form-group">
              <label class="form-label" for="agentFieldName">智能体字段名称 *</label>
              <input 
                type="text" 
                id="agentFieldName"
                v-model="semanticModelForm.agentFieldName"
                class="form-control" 
                :class="{ 'is-invalid': formErrors.agentFieldName }"
                required
                placeholder="如：用户年龄"
                @input="clearFieldError('agentFieldName')"
              >
              <div v-if="formErrors.agentFieldName" class="invalid-feedback">
                {{ formErrors.agentFieldName }}
              </div>
            </div>
            <div class="form-group">
              <label class="form-label" for="fieldSynonyms">字段名称同义词</label>
              <input 
                type="text" 
                id="fieldSynonyms"
                v-model="semanticModelForm.fieldSynonyms"
                class="form-control" 
                placeholder="多个同义词用逗号分隔，如：年龄,岁数"
              >
            </div>
            <div class="form-group">
              <label class="form-label" for="fieldDescription">字段描述</label>
              <textarea 
                id="fieldDescription"
                v-model="semanticModelForm.fieldDescription"
                class="form-control" 
                rows="3"
                placeholder="用于帮助对字段的理解（为空时与原始字段描述保持一致）"
              ></textarea>
            </div>
            <div class="form-group">
              <label class="form-label" for="fieldType">字段类型</label>
              <select id="fieldType" v-model="semanticModelForm.fieldType" class="form-control">
                <option value="VARCHAR">VARCHAR</option>
                <option value="INTEGER">INTEGER</option>
                <option value="DECIMAL">DECIMAL</option>
                <option value="DATE">DATE</option>
                <option value="DATETIME">DATETIME</option>
                <option value="TEXT">TEXT</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label" for="originalDescription">原始字段描述</label>
              <textarea 
                id="originalDescription"
                v-model="semanticModelForm.originalDescription"
                class="form-control" 
                rows="2"
                placeholder="数据集中原始字段的描述"
              ></textarea>
            </div>
            <div class="form-group">
              <div class="checkbox-group">
                <input 
                  type="checkbox" 
                  id="defaultRecall"
                  v-model="semanticModelForm.defaultRecall"
                  class="form-checkbox"
                >
                <label class="checkbox-label" for="defaultRecall">默认召回</label>
              </div>
              <small class="form-text">勾选后，该字段每次提问时都会作为提示词传输给大模型</small>
            </div>
            <div class="form-group">
              <div class="checkbox-group">
                <input 
                  type="checkbox" 
                  id="enabled"
                  v-model="semanticModelForm.enabled"
                  class="form-checkbox"
                >
                <label class="checkbox-label" for="enabled">启用状态</label>
              </div>
              <small class="form-text">勾选后，该语义模型配置将生效</small>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeModelModal">取消</button>
          <button type="button" class="btn btn-primary" @click="saveModel">
            {{ isEditingModel ? '更新' : '创建' }}
          </button>
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

    <!-- 业务知识创建/编辑模态框 -->
    <div v-if="showCreateKnowledgeModal" class="modal-overlay" @click="closeBusinessKnowledgeModal">
      <div class="modal-dialog" @click.stop>
        <div class="modal-header">
          <h3>{{ isEditingBusinessKnowledge ? '编辑业务知识' : '创建业务知识' }}</h3>
          <button class="close-btn" @click="closeBusinessKnowledgeModal">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveBusinessKnowledge">
            <div class="form-group">
              <label>业务名词 <span class="required">*</span></label>
              <input 
                type="text" 
                v-model="businessKnowledgeForm.businessTerm" 
                class="form-control" 
                placeholder="请输入业务名词"
                required
              >
            </div>
            <div class="form-group">
              <label>描述 <span class="required">*</span></label>
              <textarea 
                v-model="businessKnowledgeForm.description" 
                class="form-control" 
                rows="4"
                placeholder="请输入业务知识描述"
                required
              ></textarea>
            </div>
            <div class="form-group">
              <label>同义词</label>
              <input 
                type="text" 
                v-model="businessKnowledgeForm.synonyms" 
                class="form-control" 
                placeholder="多个同义词用逗号分隔"
              >
            </div>
            <div class="form-group">
              <label>数据集ID</label>
              <input 
                type="text" 
                v-model="businessKnowledgeForm.datasetId" 
                class="form-control" 
                placeholder="请输入数据集ID（可选）"
              >
            </div>
            <div class="form-group">
              <label>
                <input 
                  type="checkbox" 
                  v-model="businessKnowledgeForm.defaultRecall"
                >
                默认召回
              </label>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeBusinessKnowledgeModal">取消</button>
          <button type="button" class="btn btn-primary" @click="saveBusinessKnowledge">
            {{ isEditingBusinessKnowledge ? '更新' : '创建' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { agentApi, businessKnowledgeApi, semanticModelApi, datasourceApi, presetQuestionApi } from '../utils/api.js'
import AgentDebugPanel from '../components/AgentDebugPanel.vue'

export default {
  name: 'AgentDetail',
  components: {
    AgentDebugPanel
  },
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
    
    // 预设问题相关数据
    const presetQuestions = ref([])
    
    // 业务知识相关数据
    const isEditingBusinessKnowledge = ref(false)
    const editingBusinessKnowledgeId = ref(null)
    const businessKnowledgeForm = reactive({
      businessTerm: '',
      description: '',
      synonyms: '',
      datasetId: '',
      defaultRecall: true
    })
    
    // 语义模型相关数据
    const isEditingModel = ref(false)
    const editingModelId = ref(null)
    const semanticModelForm = reactive({
      originalFieldName: '',
      agentFieldName: '',
      fieldSynonyms: '',
      fieldDescription: '',
      fieldType: 'VARCHAR',
      originalDescription: '',
      defaultRecall: false,
      enabled: true
    })
    
    // 语义模型筛选和批量操作
    const filteredSemanticModels = ref([])
    const selectedModels = ref([])
    const modelFilters = reactive({
      keyword: '',
      fieldType: '',
      enabled: '',
      defaultRecall: ''
    })
    
    // 表单验证错误
    const formErrors = reactive({
      originalFieldName: '',
      agentFieldName: ''
    })
    
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
    
    // 方法
    const setActiveTab = (tab) => {
      activeTab.value = tab
      loadTabData(tab)
    }
    
    const goBack = () => {
      router.push('/agents')
    }
    
    const goToAgentList = () => {
      router.push('/agents')
    }

    const goToWorkspace = () => {
      router.push('/workspace')
    }

    const createNewAgent = () => {
      router.push('/agent/create')
    }

    const openHelp = () => {
      window.open('https://github.com/alibaba/spring-ai-alibaba/blob/main/spring-ai-alibaba-nl2sql/README.md', '_blank')
    }

    const goToHome = () => {
      router.push('/')
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
        case 'preset-questions':
          await loadPresetQuestions()
          break
      }
    }
    
    const loadBusinessKnowledge = async () => {
      try {
        // 使用智能体ID获取对应的业务知识
        const knowledgeList = await businessKnowledgeApi.getByAgentId(agent.id)
        businessKnowledgeList.value = knowledgeList || []
        console.log('业务知识加载成功:', knowledgeList)
      } catch (error) {
        console.error('加载业务知识失败:', error)
        businessKnowledgeList.value = []
      }
    }
    
    const loadSemanticModels = async () => {
      try {
        // 按智能体ID筛选语义模型
        const params = { agentId: agent.id }
        const response = await semanticModelApi.getList(params)
        semanticModelList.value = response || []
        // 初始化筛选结果
        filteredSemanticModels.value = semanticModelList.value
        console.log('语义模型加载成功:', semanticModelList.value)
      } catch (error) {
        console.error('加载语义模型失败:', error)
        semanticModelList.value = []
        filteredSemanticModels.value = []
        showMessage('加载语义模型失败：' + (error.message || '未知错误'), 'error')
      }
    }
    
    // 语义模型筛选功能
    const filterSemanticModels = () => {
      let filtered = semanticModelList.value
      
      // 关键字搜索
      if (modelFilters.keyword) {
        const keyword = modelFilters.keyword.toLowerCase()
        filtered = filtered.filter(model => 
          model.originalFieldName?.toLowerCase().includes(keyword) ||
          model.agentFieldName?.toLowerCase().includes(keyword) ||
          model.fieldSynonyms?.toLowerCase().includes(keyword)
        )
      }
      
      // 字段类型筛选
      if (modelFilters.fieldType) {
        filtered = filtered.filter(model => model.fieldType === modelFilters.fieldType)
      }
      
      // 启用状态筛选
      if (modelFilters.enabled !== '') {
        const isEnabled = modelFilters.enabled === 'true'
        filtered = filtered.filter(model => model.enabled === isEnabled)
      }
      
      // 默认召回筛选
      if (modelFilters.defaultRecall !== '') {
        const isDefaultRecall = modelFilters.defaultRecall === 'true'
        filtered = filtered.filter(model => model.defaultRecall === isDefaultRecall)
      }
      
      filteredSemanticModels.value = filtered
      // 清空选中状态
      selectedModels.value = []
    }
    
    // 计算选中状态
    const isAllSelected = computed(() => {
      return filteredSemanticModels.value.length > 0 && 
        selectedModels.value.length === filteredSemanticModels.value.length
    })
    
    const isPartialSelected = computed(() => {
      return selectedModels.value.length > 0 && 
        selectedModels.value.length < filteredSemanticModels.value.length
    })
    
    // 全选/反选功能
    const toggleSelectAll = () => {
      if (isAllSelected.value) {
        selectedModels.value = []
      } else {
        selectedModels.value = filteredSemanticModels.value.map(model => model.id)
      }
    }
    
    // 批量切换状态
    const batchToggleStatus = async (enabled) => {
      if (selectedModels.value.length === 0) {
        showMessage('请选择要操作的语义模型', 'warning')
        return
      }
      
      try {
        // 批量更新选中的语义模型状态
        const promises = selectedModels.value.map(id => {
          const model = semanticModelList.value.find(m => m.id === id)
          if (model) {
            return semanticModelApi.update(id, { ...model, enabled })
          }
        }).filter(Boolean)
        
        await Promise.all(promises)
        showMessage(`批量${enabled ? '启用' : '禁用'}成功`, 'success')
        selectedModels.value = []
        await loadSemanticModels()
      } catch (error) {
        console.error('批量操作失败:', error)
        showMessage('批量操作失败：' + (error.message || '未知错误'), 'error')
      }
    }
    
    // 批量删除
    const batchDeleteModels = async () => {
      if (selectedModels.value.length === 0) {
        showMessage('请选择要删除的语义模型', 'warning')
        return
      }
      
      if (!confirm(`确定要删除选中的 ${selectedModels.value.length} 个语义模型吗？`)) {
        return
      }
      
      try {
        const promises = selectedModels.value.map(id => semanticModelApi.delete(id))
        await Promise.all(promises)
        showMessage('批量删除成功', 'success')
        selectedModels.value = []
        await loadSemanticModels()
      } catch (error) {
        console.error('批量删除失败:', error)
        showMessage('批量删除失败：' + (error.message || '未知错误'), 'error')
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
    
    // 预设问题相关方法
    const loadPresetQuestions = async () => {
      try {
        const questions = await presetQuestionApi.getByAgentId(agent.id)
        presetQuestions.value = questions.map(q => ({ question: q.question }))
        console.log('预设问题加载成功:', questions)
      } catch (error) {
        console.error('加载预设问题失败:', error)
        presetQuestions.value = []
      }
    }

    // 添加预设问题
    const addPresetQuestion = () => {
      presetQuestions.value.push({ question: '' })
    }

    // 移除预设问题
    const removePresetQuestion = (index) => {
      presetQuestions.value.splice(index, 1)
    }

    // 上移问题
    const moveQuestionUp = (index) => {
      if (index > 0) {
        const temp = presetQuestions.value[index]
        presetQuestions.value[index] = presetQuestions.value[index - 1]
        presetQuestions.value[index - 1] = temp
      }
    }

    // 下移问题
    const moveQuestionDown = (index) => {
      if (index < presetQuestions.value.length - 1) {
        const temp = presetQuestions.value[index]
        presetQuestions.value[index] = presetQuestions.value[index + 1]
        presetQuestions.value[index + 1] = temp
      }
    }

    // 保存预设问题
    const savePresetQuestions = async () => {
      try {
        // 过滤掉空问题
        const validQuestions = presetQuestions.value.filter(q => q.question.trim())
        await presetQuestionApi.batchSave(agent.id, validQuestions)
        showMessage('预设问题保存成功', 'success')
      } catch (error) {
        console.error('保存预设问题失败:', error)
        showMessage('保存预设问题失败：' + (error.message || '未知错误'), 'error')
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
      // 设置编辑模式并填充表单
      isEditingBusinessKnowledge.value = true
      editingBusinessKnowledgeId.value = knowledge.id
      businessKnowledgeForm.businessTerm = knowledge.businessTerm || ''
      businessKnowledgeForm.description = knowledge.description || ''
      businessKnowledgeForm.synonyms = knowledge.synonyms || ''
      businessKnowledgeForm.datasetId = knowledge.datasetId || ''
      businessKnowledgeForm.defaultRecall = knowledge.defaultRecall !== false
      showCreateKnowledgeModal.value = true
    }
    
    const deleteBusinessKnowledge = async (id) => {
      if (confirm('确定要删除这条业务知识吗？')) {
        try {
          await businessKnowledgeApi.delete(id)
          await loadBusinessKnowledge()
          showMessage('删除成功', 'success')
        } catch (error) {
          console.error('删除业务知识失败:', error)
          showMessage('删除失败：' + (error.message || '未知错误'), 'error')
        }
      }
    }
    
    const closeBusinessKnowledgeModal = () => {
      showCreateKnowledgeModal.value = false
      isEditingBusinessKnowledge.value = false
      editingBusinessKnowledgeId.value = null
      // 重置表单
      businessKnowledgeForm.businessTerm = ''
      businessKnowledgeForm.description = ''
      businessKnowledgeForm.synonyms = ''
      businessKnowledgeForm.datasetId = ''
      businessKnowledgeForm.defaultRecall = true
    }
    
    const saveBusinessKnowledge = async () => {
      try {
        // 验证必填字段
        if (!businessKnowledgeForm.businessTerm || !businessKnowledgeForm.description) {
          showMessage('请填写必填字段', 'warning')
          return
        }
        
        const knowledgeData = {
          businessTerm: businessKnowledgeForm.businessTerm,
          description: businessKnowledgeForm.description,
          synonyms: businessKnowledgeForm.synonyms,
          datasetId: businessKnowledgeForm.datasetId,
          defaultRecall: businessKnowledgeForm.defaultRecall,
          agentId: agent.id
        }
        
        if (isEditingBusinessKnowledge.value) {
          // 更新业务知识
          await businessKnowledgeApi.update(editingBusinessKnowledgeId.value, knowledgeData)
          showMessage('更新成功', 'success')
        } else {
          // 创建业务知识
          await businessKnowledgeApi.createForAgent(agent.id, knowledgeData)
          showMessage('创建成功', 'success')
        }
        
        closeBusinessKnowledgeModal()
        await loadBusinessKnowledge()
      } catch (error) {
        console.error('保存业务知识失败:', error)
        showMessage('保存失败：' + (error.message || '未知错误'), 'error')
      }
    }
    
    const editModel = (model) => {
      isEditingModel.value = true
      editingModelId.value = model.id
      semanticModelForm.originalFieldName = model.originalFieldName || ''
      semanticModelForm.agentFieldName = model.agentFieldName || ''
      semanticModelForm.fieldSynonyms = model.fieldSynonyms || ''
      semanticModelForm.fieldDescription = model.fieldDescription || ''
      semanticModelForm.fieldType = model.fieldType || 'VARCHAR'
      semanticModelForm.originalDescription = model.originalDescription || ''
      semanticModelForm.defaultRecall = model.defaultRecall || false
      semanticModelForm.enabled = model.enabled !== undefined ? model.enabled : true
      showCreateModelModal.value = true
    }
    
    const closeModelModal = () => {
      showCreateModelModal.value = false
      isEditingModel.value = false
      editingModelId.value = null
      resetModelForm()
    }
    
    const resetModelForm = () => {
      semanticModelForm.originalFieldName = ''
      semanticModelForm.agentFieldName = ''
      semanticModelForm.fieldSynonyms = ''
      semanticModelForm.fieldDescription = ''
      semanticModelForm.fieldType = 'VARCHAR'
      semanticModelForm.originalDescription = ''
      semanticModelForm.defaultRecall = false
      semanticModelForm.enabled = true
      
      // 重置表单错误
      formErrors.datasetId = ''
      formErrors.originalFieldName = ''
      formErrors.agentFieldName = ''
    }
    
    const saveModel = async () => {
      // 重置表单错误
      formErrors.datasetId = ''
      formErrors.originalFieldName = ''
      formErrors.agentFieldName = ''
      
      // 验证必填字段
      let hasError = false
      
      if (!semanticModelForm.originalFieldName.trim()) {
        formErrors.originalFieldName = '原始字段名不能为空'
        hasError = true
      }
      
      if (!semanticModelForm.agentFieldName.trim()) {
        formErrors.agentFieldName = '智能体字段名称不能为空'
        hasError = true
      }
      
      if (hasError) {
        showMessage('请修正表单错误后重试', 'warning')
        return
      }

      try {
        const modelData = {
          agentId: agent.id, // 关联当前智能体
          originalFieldName: semanticModelForm.originalFieldName.trim(),
          agentFieldName: semanticModelForm.agentFieldName.trim(),
          fieldSynonyms: semanticModelForm.fieldSynonyms.trim() || null,
          fieldDescription: semanticModelForm.fieldDescription.trim() || null,
          fieldType: semanticModelForm.fieldType,
          originalDescription: semanticModelForm.originalDescription.trim() || null,
          defaultRecall: semanticModelForm.defaultRecall,
          enabled: semanticModelForm.enabled
        }

        if (isEditingModel.value) {
          // 更新语义模型
          await semanticModelApi.update(editingModelId.value, modelData)
          showMessage('更新成功', 'success')
        } else {
          // 创建语义模型
          await semanticModelApi.create(modelData)
          showMessage('创建成功', 'success')
        }
        
        closeModelModal()
        await loadSemanticModels()
      } catch (error) {
        console.error('保存语义模型失败:', error)
        showMessage('保存失败：' + (error.message || '未知错误'), 'error')
      }
    }
    
    // 清除字段错误
    const clearFieldError = (fieldName) => {
      if (formErrors[fieldName]) {
        formErrors[fieldName] = ''
      }
    }
    
    const deleteModel = async (id) => {
      if (confirm('确定要删除这个语义模型吗？')) {
        try {
          await semanticModelApi.delete(id)
          showMessage('删除成功', 'success')
          await loadSemanticModels()
        } catch (error) {
          console.error('删除语义模型失败:', error)
          showMessage('删除失败：' + (error.message || '未知错误'), 'error')
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
    
    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'published': '已发布',
        'draft': '待发布',
        'offline': '已下线'
      }
      return statusMap[status] || status
    }
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      })
    }
    
    // 格式化日期时间
    const formatDateTime = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }
    
    // 生命周期
    onMounted(async () => {
      await loadAgentDetail()
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
      // 业务知识相关
      businessKnowledgeForm,
      isEditingBusinessKnowledge,
      editingBusinessKnowledgeId,
      // 语义模型相关
      semanticModelForm,
      isEditingModel,
      editingModelId,
      filteredSemanticModels,
      selectedModels,
      modelFilters,
      isAllSelected,
      isPartialSelected,
      formErrors,
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
      goToAgentList,
      updateAgent,
      editBusinessKnowledge,
      deleteBusinessKnowledge,
      editModel,
      deleteModel,
      viewDocument,
      deleteDocument,
      // 业务知识方法
      saveBusinessKnowledge,
      closeBusinessKnowledgeModal,
      // 语义模型方法
      saveModel,
      closeModelModal,
      resetModelForm,
      filterSemanticModels,
      toggleSelectAll,
      batchToggleStatus,
      batchDeleteModels,
      clearFieldError,
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
      // 导航方法
      goToAgentList,
      goToWorkspace,
      createNewAgent,
      openHelp,
      goToHome,
      // 工具方法
      getStatusText,
      formatDate,
      formatDateTime,
      getRandomColor,
      getRandomIcon,
      // 消息提示方法
      showMessage,
      hideMessage,
      getMessageIcon,
      // 预设问题方法
      presetQuestions,
      addPresetQuestion,
      removePresetQuestion,
      moveQuestionUp,
      moveQuestionDown,
      savePresetQuestions,
      loadPresetQuestions
    }
  }
}
</script>

<style scoped>
.agent-detail-page {
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
  border-bottom: 1px solid #e5e5e5;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  max-width: 100%;
}

.nav-items {
  display: flex;
  gap: 32px;
}

.nav-item {
  padding: 8px 16px;
  color: #666;
  border-radius: 4px;
  transition: all 0.2s;
}

.nav-item.logo-item {
  cursor: default;
  font-weight: 600;
  color: #1890ff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-item.logo-item i {
  font-size: 18px;
}

.nav-item.clickable {
  cursor: pointer;
}

.nav-item.active,
.nav-item.clickable:hover {
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
  padding: 0 1rem;
  max-width: 100%;
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
  padding: 1rem 0;
  max-width: 100%;
}

.content-layout {
  display: flex;
  gap: 24px;
  align-items: stretch;
  min-height: 600px;
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
  gap: var(--space-base);
  padding: var(--space-base) var(--space-md);
  color: var(--text-primary);
  text-decoration: none;
  transition: all var(--transition-base);
  border-radius: var(--radius-base);
  margin: var(--space-xs) var(--space-sm);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  position: relative;
  border: 1px solid transparent;
}

.nav-link:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
  border-color: var(--border-primary);
  transform: translateX(2px);
}

.nav-link.active {
  background: linear-gradient(135deg, var(--primary-light), var(--accent-light));
  color: var(--primary-color);
  border-color: var(--primary-color);
  box-shadow: var(--shadow-sm);
  font-weight: var(--font-weight-semibold);
}

.nav-link.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
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
  padding: 1rem;
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

/* 语义模型筛选样式 */
.model-filters {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.model-filters .filter-group {
  flex: 1;
}

.model-filters .search-box {
  position: relative;
  flex: 2;
}

.model-filters .search-box i {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  z-index: 1;
}

.model-filters .search-box input {
  padding-left: 36px;
}

/* 批量操作样式 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f0f7ff;
  border: 1px solid #d6e4ff;
  border-radius: 6px;
  margin-right: 12px;
}

.selected-count {
  color: #1890ff;
  font-weight: 500;
  margin-right: 8px;
}

.batch-actions .btn {
  margin: 0 2px;
  font-size: 12px;
  padding: 4px 8px;
}

/* 复选框样式增强 */
.semantic-model-table th input[type="checkbox"],
.semantic-model-table td input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.semantic-model-table th:first-child,
.semantic-model-table td:first-child {
  width: 40px;
  text-align: center;
}

/* 表格行hover效果 */
.semantic-model-table tbody tr:hover {
  background-color: #fafafa;
}

/* 选中行样式 */
.semantic-model-table tbody tr:has(input[type="checkbox"]:checked) {
  background-color: #e6f7ff;
}

/* 表单验证样式 */
.form-control.is-invalid {
  border-color: #ff4d4f;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.2);
}

.invalid-feedback {
  display: block;
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 4px;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  font-weight: 500;
  margin-bottom: 4px;
  display: block;
}

/* 复选框样式 */
.checkbox-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-checkbox {
  width: 16px;
  height: 16px;
  margin: 0;
  cursor: pointer;
  accent-color: #1890ff;
}

.checkbox-label {
  font-weight: 500;
  margin: 0;
  cursor: pointer;
  user-select: none;
}

.form-text {
  color: #666;
  font-size: 12px;
  margin-top: 4px;
  display: block;
}

/* 预设问题配置样式 */
.preset-questions-section {
  margin-top: 16px;
}

.questions-list {
  margin-top: 16px;
}

.empty-questions {
  text-align: center;
  padding: 3rem 1rem;
  color: #999;
}

.empty-questions i {
  font-size: 3rem;
  margin-bottom: 1rem;
  color: #ccc;
}

.question-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 0.75rem;
  border: 1px solid #e8e8e8;
}

.question-content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.question-number {
  width: 24px;
  height: 24px;
  background: #1890ff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 500;
  flex-shrink: 0;
}

.question-input {
  flex: 1;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.question-input:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.question-actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.question-actions .btn {
  padding: 0.25rem 0.5rem;
  font-size: 0.8rem;
}
</style>
