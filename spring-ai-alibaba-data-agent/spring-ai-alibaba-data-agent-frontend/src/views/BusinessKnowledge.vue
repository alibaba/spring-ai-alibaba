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
  <div class="business-knowledge-page">
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

    <div class="main-content">
      <!-- 页面头部信息 -->
      <div class="page-header">
        <div class="header-info">
          <h1 class="page-title">业务知识库</h1>
          <p class="page-description">管理业务术语、同义词和专业表达，提升AI理解准确性</p>
        </div>
        <div class="header-stats">
          <div class="stat-card">
            <div class="stat-number">{{ knowledgeList.length }}</div>
            <div class="stat-label">知识条目</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ knowledgeList.filter(k => k.defaultRecall).length }}</div>
            <div class="stat-label">默认召回</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ new Set(knowledgeList.map(k => k.datasetId).filter(Boolean)).size }}</div>
            <div class="stat-label">数据集</div>
          </div>
        </div>
      </div>

      <!-- 操作工具栏 -->
      <div class="toolbar-section">
        <div class="search-area">
          <div class="search-box">
            <i class="search-icon bi bi-search"></i>
            <input 
              type="text" 
              v-model="searchKeyword"
              class="form-control" 
              placeholder="搜索业务名词、说明或同义词..."
              @keyup.enter="searchKnowledge"
            >
            <button 
              v-if="searchKeyword"
              class="clear-btn"
              @click="clearSearch"
            >
              <i class="bi bi-x"></i>
            </button>
          </div>
          <button class="btn btn-outline" @click="searchKnowledge">
            <i class="bi bi-search"></i>
            搜索
          </button>
        </div>
        <div class="action-area">
          <button class="btn btn-outline" @click="exportKnowledge">
            <i class="bi bi-download"></i>
            导出
          </button>
          <button class="btn btn-outline" @click="importKnowledge">
            <i class="bi bi-upload"></i>
            导入
          </button>
          <button class="btn btn-primary" @click="showAddModal">
            <i class="bi bi-plus-lg"></i>
            新增知识
          </button>
        </div>
      </div>

      <div class="card">
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>业务名词</th>
                <th>说明</th>
                <th>同义词</th>
                <th>默认召回</th>
                <th>数据集ID</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <!-- 加载状态 -->
              <tr v-if="loading">
                <td colspan="7" class="loading">
                  <div class="spinner"></div>
                  加载中...
                </td>
              </tr>
              <!-- 空状态 -->
              <tr v-else-if="knowledgeList.length === 0">
                <td colspan="7" class="empty-state">
                  <div class="empty-icon"><i class="bi bi-inbox"></i></div>
                  <div>暂无数据</div>
                </td>
              </tr>
              <!-- 数据行 -->
              <tr v-else v-for="item in knowledgeList" :key="item.id">
                <td><strong>{{ item.businessTerm }}</strong></td>
                <td class="description-cell">{{ item.description }}</td>
                <td>{{ item.synonyms || '-' }}</td>
                <td>
                  <span class="badge" :class="item.defaultRecall ? 'badge-success' : 'badge-secondary'">
                    {{ item.defaultRecall ? '是' : '否' }}
                  </span>
                </td>
                <td>{{ item.datasetId || '-' }}</td>
                <td>{{ formatDateTime(item.createTime) }}</td>
                <td>
                  <div class="action-buttons">
                    <button class="btn btn-primary btn-sm" @click="showEditModal(item.id)">
                      <i class="bi bi-pencil"></i> 编辑
                    </button>
                    <button class="btn btn-danger btn-sm" @click="deleteKnowledge(item.id)">
                      <i class="bi bi-trash"></i> 删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 新增/编辑模态框 -->
    <div v-if="modalVisible" class="modal show" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">{{ modalTitle }}</h3>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveKnowledge">
            <div class="form-group">
              <label class="form-label" for="businessTerm">业务名词 *</label>
              <input 
                type="text" 
                id="businessTerm"
                v-model="formData.businessTerm"
                class="form-control" 
                required 
                placeholder="如：年龄分布、搜索业绩口径"
              >
            </div>
            <div class="form-group">
              <label class="form-label" for="description">说明 *</label>
              <textarea 
                id="description"
                v-model="formData.description"
                class="form-control" 
                rows="3" 
                required 
                placeholder="输入对知识的定义、解释说明等"
              ></textarea>
            </div>
            <div class="form-group">
              <label class="form-label" for="synonyms">同义词</label>
              <input 
                type="text" 
                id="synonyms"
                v-model="formData.synonyms"
                class="form-control" 
                placeholder="多个同义词用逗号分隔，如：年龄画像,年龄构成,年龄结构"
              >
            </div>
            <div class="form-group">
              <label class="form-label" for="datasetId">数据集ID</label>
              <input 
                type="text" 
                id="datasetId"
                v-model="formData.datasetId"
                class="form-control" 
                placeholder="关联的数据集ID"
              >
            </div>
            <div class="form-group">
              <div class="checkbox-group">
                <input 
                  type="checkbox" 
                  id="defaultRecall"
                  v-model="formData.defaultRecall"
                  class="form-control"
                >
                <label class="form-label" for="defaultRecall">默认召回</label>
              </div>
              <small style="color: #666; margin-left: 1.5rem;">勾选后，该知识每次提问时都会作为提示词传输给大模型</small>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
          <button type="button" class="btn btn-primary" @click="saveKnowledge">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import HeaderComponent from '../components/HeaderComponent.vue'

export default {
  name: 'BusinessKnowledge',
  components: {
    HeaderComponent
  },
  setup() {
    const knowledgeList = ref([])
    const loading = ref(true)
    const searchKeyword = ref('')
    const modalVisible = ref(false)
    const modalTitle = ref('新增业务知识')
    const currentEditId = ref(null)

    const formData = reactive({
      businessTerm: '',
      description: '',
      synonyms: '',
      datasetId: '',
      defaultRecall: false
    })

    // 模拟数据配置
    const useMockData = ref(false) // 设置为 true 使用模拟数据，false 使用真实 API
    
    // 模拟数据
    const mockKnowledgeList = [
      {
        id: 1,
        term: '销售额',
        description: '某个时间段内的总销售金额',
        synonyms: '营收,收入,销售金额',
        defaultRecall: true,
        datasetId: 'dataset_001',
        createTime: '2024-01-15 10:30:00'
      },
      {
        id: 2,
        term: '客户',
        description: '购买产品或服务的个人或组织',
        synonyms: '用户,顾客,买家',
        defaultRecall: true,
        datasetId: 'dataset_002',
        createTime: '2024-01-16 14:20:00'
      },
      {
        id: 3,
        term: '订单',
        description: '客户购买商品或服务的记录',
        synonyms: '单据,购买记录',
        defaultRecall: false,
        datasetId: 'dataset_003',
        createTime: '2024-01-17 09:15:00'
      }
    ]

    const loadKnowledgeList = async (keyword = '') => {
      try {
        loading.value = true
        
        if (useMockData.value) {
          // 使用模拟数据
          await new Promise(resolve => setTimeout(resolve, 500)) // 模拟网络延迟
          let filteredData = mockKnowledgeList
          if (keyword) {
            const lowerKeyword = keyword.toLowerCase()
            filteredData = mockKnowledgeList.filter(item => 
              item.term.toLowerCase().includes(lowerKeyword) ||
              item.description.toLowerCase().includes(lowerKeyword) ||
              (item.synonyms && item.synonyms.toLowerCase().includes(lowerKeyword))
            )
          }
          knowledgeList.value = filteredData
        } else {
          // 使用真实 API
          const url = keyword ? `/api/business-knowledge?keyword=${encodeURIComponent(keyword)}` : '/api/business-knowledge'
          const response = await fetch(url)
          const data = await response.json()
          knowledgeList.value = data
        }
      } catch (error) {
        console.error('加载数据失败:', error)
        if (useMockData.value) {
          knowledgeList.value = mockKnowledgeList // 回退到模拟数据
        } else {
          alert('加载数据失败，请刷新页面重试')
        }
      } finally {
        loading.value = false
      }
    }

    const searchKnowledge = () => {
      loadKnowledgeList(searchKeyword.value.trim())
    }

    const showAddModal = () => {
      currentEditId.value = null
      modalTitle.value = '新增业务知识'
      resetFormData()
      modalVisible.value = true
    }

    const showEditModal = (id) => {
      const item = knowledgeList.value.find(k => k.id === id)
      if (!item) return

      currentEditId.value = id
      modalTitle.value = '编辑业务知识'
      formData.businessTerm = item.businessTerm
      formData.description = item.description
      formData.synonyms = item.synonyms || ''
      formData.datasetId = item.datasetId || ''
      formData.defaultRecall = item.defaultRecall
      modalVisible.value = true
    }

    const closeModal = () => {
      modalVisible.value = false
      resetFormData()
    }

    const resetFormData = () => {
      formData.businessTerm = ''
      formData.description = ''
      formData.synonyms = ''
      formData.datasetId = ''
      formData.defaultRecall = false
    }

    const saveKnowledge = async () => {
      // 验证必填字段
      if (!formData.businessTerm.trim() || !formData.description.trim()) {
        alert('请填写必填字段')
        return
      }

      try {
        const data = {
          businessTerm: formData.businessTerm.trim(),
          description: formData.description.trim(),
          synonyms: formData.synonyms.trim() || null,
          datasetId: formData.datasetId.trim() || null,
          defaultRecall: formData.defaultRecall
        }

        if (useMockData.value) {
          // 使用模拟数据
          await new Promise(resolve => setTimeout(resolve, 300)) // 模拟网络延迟
          
          if (currentEditId.value) {
            // 编辑现有数据
            const index = mockKnowledgeList.findIndex(item => item.id === currentEditId.value)
            if (index !== -1) {
              mockKnowledgeList[index] = {
                ...mockKnowledgeList[index],
                term: data.businessTerm,
                description: data.description,
                synonyms: data.synonyms || '',
                datasetId: data.datasetId || '',
                defaultRecall: data.defaultRecall
              }
            }
          } else {
            // 添加新数据
            const newItem = {
              id: Date.now(), // 简单的 ID 生成
              term: data.businessTerm,
              description: data.description,
              synonyms: data.synonyms || '',
              defaultRecall: data.defaultRecall,
              datasetId: data.datasetId || '',
              createTime: new Date().toLocaleString()
            }
            mockKnowledgeList.push(newItem)
          }
          
          closeModal()
          loadKnowledgeList()
        } else {
          // 使用真实 API
          const url = currentEditId.value ? `/api/business-knowledge/${currentEditId.value}` : '/api/business-knowledge'
          const method = currentEditId.value ? 'PUT' : 'POST'

          const response = await fetch(url, {
            method: method,
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
          })

          if (response.ok) {
            closeModal()
            loadKnowledgeList()
          } else {
            throw new Error('保存失败')
          }
        }
      } catch (error) {
        console.error('保存失败:', error)
        alert('保存失败，请重试')
      }
    }

    const deleteKnowledge = async (id) => {
      if (!confirm('确定要删除这条业务知识吗？')) {
        return
      }

      try {
        if (useMockData.value) {
          // 使用模拟数据
          await new Promise(resolve => setTimeout(resolve, 200)) // 模拟网络延迟
          const index = mockKnowledgeList.findIndex(item => item.id === id)
          if (index !== -1) {
            mockKnowledgeList.splice(index, 1)
          }
          loadKnowledgeList()
        } else {
          // 使用真实 API
          const response = await fetch(`/api/business-knowledge/${id}`, {
            method: 'DELETE'
          })

          if (response.ok) {
            loadKnowledgeList()
          } else {
            throw new Error('删除失败')
          }
        }
      } catch (error) {
        console.error('删除失败:', error)
        alert('删除失败，请重试')
      }
    }

    const clearSearch = () => {
      searchKeyword.value = ''
      loadKnowledgeList()
    }

    const exportKnowledge = () => {
      // 导出功能实现
      console.log('导出知识库')
    }

    const importKnowledge = () => {
      // 导入功能实现
      console.log('导入知识库')
    }

    const formatDateTime = (dateTimeStr) => {
      if (!dateTimeStr) return '-'
      const date = new Date(dateTimeStr)
      return date.toLocaleString('zh-CN')
    }

    onMounted(() => {
      loadKnowledgeList()
    })

    return {
      knowledgeList,
      loading,
      searchKeyword,
      modalVisible,
      modalTitle,
      formData,
      loadKnowledgeList,
      searchKnowledge,
      clearSearch,
      exportKnowledge,
      importKnowledge,
      showAddModal,
      showEditModal,
      closeModal,
      saveKnowledge,
      deleteKnowledge,
      formatDateTime
    }
  }
}
</script>

<style scoped>
.business-knowledge-page {
  min-height: 100vh;
  background: var(--bg-layout);
  font-family: var(--font-family);
}

.main-content {
  max-width: 100%;
  margin: 0 auto;
  padding: var(--space-lg);
}

/* 页面头部信息 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-2xl);
  padding: var(--space-xl);
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-secondary);
}

.header-info {
  flex: 1;
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-sm) 0;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-description {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}

.header-stats {
  display: flex;
  gap: var(--space-lg);
}

.stat-card {
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

/* 操作工具栏 */
.toolbar-section {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  margin-bottom: var(--space-xl);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-secondary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-lg);
}

.search-area {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex: 1;
  max-width: 500px;
}

.search-box {
  position: relative;
  flex: 1;
}

.action-area {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

/* 数据表格区域 */
.card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-secondary);
  overflow: hidden;
}

.table-container {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-sm);
}

.table th {
  background: var(--bg-secondary);
  padding: var(--space-md) var(--space-lg);
  text-align: left;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-secondary);
  font-size: var(--font-size-sm);
}

.table td {
  padding: var(--space-md) var(--space-lg);
  border-bottom: 1px solid var(--border-tertiary);
  color: var(--text-secondary);
  vertical-align: top;
}

.table tbody tr:hover {
  background: var(--bg-tertiary);
}

.table tbody tr:last-child td {
  border-bottom: none;
}

.description-cell {
  max-width: 300px;
  word-wrap: break-word;
  line-height: 1.5;
}

/* 徽章样式 */
.badge {
  display: inline-flex;
  align-items: center;
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.badge-success {
  background: var(--success-light);
  color: var(--success-color);
  border: 1px solid rgba(82, 196, 26, 0.2);
}

.badge-secondary {
  background: var(--bg-secondary);
  color: var(--text-tertiary);
  border: 1px solid var(--border-primary);
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: var(--space-sm);
  justify-content: center;
  flex-wrap: nowrap;
}

.btn-sm {
  padding: var(--space-xs) var(--space-sm);
  font-size: var(--font-size-xs);
  border-radius: var(--radius-sm);
  min-width: 60px;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  flex-shrink: 0;
}

/* 加载和空状态 */
.loading {
  text-align: center;
  padding: var(--space-4xl);
  color: var(--text-secondary);
}

.loading .spinner {
  margin-right: var(--space-sm);
}

.empty-state {
  text-align: center;
  padding: var(--space-4xl);
  color: var(--text-tertiary);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: var(--space-lg);
  color: var(--text-quaternary);
}

/* 模态框样式 */
.modal {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  z-index: var(--z-modal);
}

.modal.show {
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s ease-out;
}

.modal-content {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--border-secondary);
  animation: slideInUp 0.3s ease-out;
}

.modal-header {
  padding: var(--space-xl);
  border-bottom: 1px solid var(--border-secondary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-secondary);
}

.modal-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0;
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
  padding: var(--space-lg) var(--space-xl);
  border-top: 1px solid var(--border-secondary);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-md);
  background: var(--bg-tertiary);
}

/* 表单样式 */
.form-group {
  margin-bottom: var(--space-lg);
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  margin-bottom: var(--space-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
}

.form-control {
  width: 100%;
  padding: var(--space-sm) var(--space-base);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-base);
  font-size: var(--font-size-sm);
  font-family: var(--font-family);
  color: var(--text-primary);
  background: var(--bg-primary);
  transition: all var(--transition-base);
  box-sizing: border-box;
}

.form-control:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px var(--primary-light);
}

.form-control::placeholder {
  color: var(--text-quaternary);
}

.form-control[type="checkbox"] {
  width: auto;
  margin-right: var(--space-sm);
  accent-color: var(--primary-color);
}

.checkbox-group {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
}

.checkbox-group .form-label {
  margin-bottom: 0;
  cursor: pointer;
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
  
  .page-header {
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
  .main-content {
    padding: var(--space-md);
  }
  
  .page-header {
    padding: var(--space-lg);
  }
  
  .page-title {
    font-size: var(--font-size-2xl);
  }
  
  .toolbar-section {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-md);
  }
  
  .search-area {
    max-width: none;
  }
  
  .action-area {
    justify-content: center;
  }
  
  .table-container {
    font-size: var(--font-size-xs);
  }
  
  .table th,
  .table td {
    padding: var(--space-sm);
  }
  
  .action-buttons {
    flex-direction: column;
    gap: var(--space-xs);
  }
  
  .btn-sm {
    min-width: auto;
    width: 100%;
  }
  
  .modal-content {
    margin: var(--space-md);
    width: auto;
  }
  
  .modal-header,
  .modal-body,
  .modal-footer {
    padding: var(--space-md);
  }
}

@media (max-width: 480px) {
  .header-stats {
    flex-direction: column;
    gap: var(--space-sm);
  }
  
  .stat-card {
    padding: var(--space-sm);
  }
  
  .toolbar-section {
    padding: var(--space-md);
  }
  
  .description-cell {
    max-width: 200px;
  }
  
  .table th:nth-child(3),
  .table td:nth-child(3),
  .table th:nth-child(5),
  .table td:nth-child(5) {
    display: none;
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  gap: 1rem;
}

.search-container {
  display: flex;
  gap: 0.5rem;
  flex: 1;
  max-width: 400px;
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

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: var(--radius);
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
}

.btn-primary {
  background-color: var(--primary-color);
  color: white;
}

.btn-primary:hover {
  background-color: #40a9ff;
}

.btn-success {
  background-color: var(--secondary-color);
  color: white;
}

.btn-success:hover {
  background-color: #73d13d;
}

.btn-danger {
  background-color: #ff4d4f;
  color: white;
}

.btn-danger:hover {
  background-color: #ff7875;
}

.btn-secondary {
  background-color: #f5f5f5;
  color: #666;
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background-color: #e6e6e6;
}

.card {
  background-color: var(--card-bg);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.table-container {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.table th:last-child,
.table td:last-child {
  width: 200px;
  min-width: 200px;
}

.table th {
  background-color: #fafafa;
  font-weight: 500;
  color: #666;
}

.table tr:hover {
  background-color: #f9f9f9;
}

.description-cell {
  max-width: 300px;
  word-wrap: break-word;
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

.badge-secondary {
  background-color: #f5f5f5;
  color: #666;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
  justify-content: center;
  flex-wrap: nowrap;
  flex-direction: row;
}

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.875rem;
  border-radius: 4px;
  min-width: 60px;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #999;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  color: #ccc;
}

.loading {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  display: inline-block;
  margin-right: 0.5rem;
}

.modal {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

.modal.show {
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  background-color: var(--card-bg);
  border-radius: var(--radius);
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  font-size: 1.2rem;
  font-weight: 600;
}

.modal-body {
  padding: 1.5rem;
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-control {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  font-size: 1rem;
  transition: all 0.3s;
}

.form-control:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.form-control[type="checkbox"] {
  width: auto;
  margin-right: 0.5rem;
}

.checkbox-group {
  display: flex;
  align-items: center;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
}

.close-btn:hover {
  color: #333;
}

@keyframes spin {
  to { 
    transform: rotate(360deg); 
  }
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-container {
    max-width: none;
  }

  .action-buttons {
    flex-direction: row;
    gap: 0.25rem;
    justify-content: center;
  }

  .btn-sm {
    min-width: 60px;
    font-size: 0.8rem;
    padding: 0.25rem 0.5rem;
  }

  .container {
    padding: 1rem;
  }
}
</style>
