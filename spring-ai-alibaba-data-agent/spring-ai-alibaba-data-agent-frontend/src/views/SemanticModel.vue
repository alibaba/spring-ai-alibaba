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
  <div class="semantic-model-page">
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
          <h1 class="page-title">语义模型管理</h1>
          <p class="page-description">配置数据字段的语义映射和同义词，提升AI数据理解能力</p>
        </div>
        <div class="header-stats">
          <div class="stat-card">
            <div class="stat-number">{{ modelList.length }}</div>
            <div class="stat-label">字段配置</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ modelList.filter(m => m.enabled).length }}</div>
            <div class="stat-label">已启用</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ datasets.length }}</div>
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
              placeholder="搜索字段名称、同义词或描述..."
              @keyup.enter="searchModel"
            >
            <button 
              v-if="searchKeyword"
              class="clear-btn"
              @click="clearSearch"
            >
              <i class="bi bi-x"></i>
            </button>
          </div>
          <button class="btn btn-outline" @click="searchModel">
            <i class="bi bi-search"></i>
            搜索
          </button>
        </div>
        
        <div class="filter-area">
          <select v-model="selectedDataset" class="form-control dataset-filter" @change="filterByDataset">
            <option value="">所有数据集</option>
            <option v-for="dataset in datasets" :key="dataset" :value="dataset">
              {{ dataset }}
            </option>
          </select>
        </div>
      </div>

      <!-- 批量操作栏 -->
      <div class="batch-actions-section">
        <div class="selection-info">
          <span class="selection-count">已选择 {{ selectedItems.length }} 项</span>
        </div>
        <div class="batch-buttons">
          <button 
            class="btn btn-outline btn-sm" 
            @click="batchEnableByDataset(true)"
            :disabled="!selectedDataset"
            data-tooltip="按数据集批量启用"
          >
            <i class="bi bi-database-check"></i>
            数据集启用
          </button>
          <button 
            class="btn btn-outline btn-sm" 
            @click="batchEnableByDataset(false)"
            :disabled="!selectedDataset"
            data-tooltip="按数据集批量禁用"
          >
            <i class="bi bi-database-x"></i>
            数据集禁用
          </button>
          <button class="btn btn-success btn-sm" @click="batchUpdateSelectedItems(true)">
            <i class="bi bi-check-circle"></i>
            批量启用
          </button>
          <button class="btn btn-warning btn-sm" @click="batchUpdateSelectedItems(false)">
            <i class="bi bi-x-circle"></i>
            批量禁用
          </button>
          <button class="btn btn-primary" @click="showAddModal">
            <i class="bi bi-plus-lg"></i>
            新增配置
          </button>
        </div>
      </div>

      <div class="card">
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th width="40px">
                  <input 
                    type="checkbox" 
                    v-model="selectAll" 
                    @change="toggleSelectAll"
                    class="form-control"
                  >
                </th>
                <th>数据集ID</th>
                <th>原始字段名</th>
                <th>智能体字段名称</th>
                <th>字段名称同义词</th>
                <th>字段描述</th>
                <th>字段类型</th>
                <th>默认召回</th>
                <th>启用状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <!-- 加载状态 -->
              <tr v-if="loading">
                <td colspan="11" class="loading">
                  <div class="spinner"></div>
                  加载中...
                </td>
              </tr>
              <!-- 空状态 -->
              <tr v-else-if="filteredModelList.length === 0">
                <td colspan="11" class="empty-state">
                  <div class="empty-icon"><i class="bi bi-inbox"></i></div>
                  <div>暂无数据</div>
                </td>
              </tr>
              <!-- 数据行 -->
              <tr v-else v-for="item in filteredModelList" :key="item.id">
                <td>
                  <input 
                    type="checkbox" 
                    v-model="selectedItems"
                    :value="item.id"
                    class="form-control row-checkbox" 
                    :disabled="selectedDataset && item.datasetId !== selectedDataset"
                  >
                </td>
                <td><span class="badge badge-primary">{{ item.datasetId || '-' }}</span></td>
                <td><strong>{{ item.originalFieldName }}</strong></td>
                <td>{{ item.agentFieldName || '-' }}</td>
                <td class="synonyms-cell">{{ item.fieldSynonyms || '-' }}</td>
                <td class="description-cell">{{ item.fieldDescription || '-' }}</td>
                <td><span class="badge badge-secondary">{{ item.fieldType || '-' }}</span></td>
                <td>
                  <span class="badge" :class="item.defaultRecall ? 'badge-success' : 'badge-secondary'">
                    {{ item.defaultRecall ? '是' : '否' }}
                  </span>
                </td>
                <td>
                  <span class="badge" :class="item.enabled ? 'badge-success' : 'badge-secondary'">
                    {{ item.enabled ? '启用' : '禁用' }}
                  </span>
                </td>
                <td>{{ formatDateTime(item.createTime) }}</td>
                <td>
                  <div class="action-buttons">
                    <button class="btn btn-primary btn-sm" @click="showEditModal(item.id)">
                      <i class="bi bi-pencil"></i> 编辑
                    </button>
                    <button class="btn btn-danger btn-sm" @click="deleteModel(item.id)">
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
          <form @submit.prevent="saveModel">
            <div class="form-group">
              <label class="form-label" for="datasetId">数据集ID *</label>
              <input 
                type="text" 
                id="datasetId"
                v-model="formData.datasetId"
                class="form-control" 
                required 
                placeholder="如：dataset_001"
              >
            </div>
            <div class="form-group">
              <label class="form-label" for="originalFieldName">原始字段名 *</label>
              <input 
                type="text" 
                id="originalFieldName"
                v-model="formData.originalFieldName"
                class="form-control" 
                required
                placeholder="如：user_age"
              >
            </div>
            <div class="form-group">
              <label class="form-label" for="agentFieldName">智能体字段名称</label>
              <input 
                type="text" 
                id="agentFieldName"
                v-model="formData.agentFieldName"
                class="form-control" 
                placeholder="如：用户年龄（为空时与原始字段名保持一致）"
              >
            </div>
            <div class="form-group">
              <label class="form-label" for="fieldSynonyms">字段名称同义词</label>
              <input 
                type="text" 
                id="fieldSynonyms"
                v-model="formData.fieldSynonyms"
                class="form-control" 
                placeholder="多个同义词用逗号分隔，如：年龄,岁数"
              >
            </div>
            <div class="form-group">
              <label class="form-label" for="fieldDescription">字段描述</label>
              <textarea 
                id="fieldDescription"
                v-model="formData.fieldDescription"
                class="form-control" 
                rows="3"
                placeholder="用于帮助对字段的理解（为空时与原始字段描述保持一致）"
              ></textarea>
            </div>
            <div class="form-group">
              <label class="form-label" for="fieldType">字段类型</label>
              <select id="fieldType" v-model="formData.fieldType" class="form-control">
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
                v-model="formData.originalDescription"
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
                  v-model="formData.defaultRecall"
                  class="form-control"
                >
                <label class="form-label" for="defaultRecall">默认召回</label>
              </div>
              <small style="color: #666; margin-left: 1.5rem;">勾选后，该字段每次提问时都会作为提示词传输给大模型</small>
            </div>
            <div class="form-group">
              <div class="checkbox-group">
                <input 
                  type="checkbox" 
                  id="enabled"
                  v-model="formData.enabled"
                  class="form-control"
                >
                <label class="form-label" for="enabled">启用状态</label>
              </div>
              <small style="color: #666; margin-left: 1.5rem;">勾选后，该语义模型配置将生效</small>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
          <button type="button" class="btn btn-primary" @click="saveModel">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import HeaderComponent from '../components/HeaderComponent.vue'
import { semanticModelApi } from '../utils/api.js'

export default {
  name: 'SemanticModel',
  components: {
    HeaderComponent
  },
  setup() {
    // 模拟数据配置
    const useMockData = ref(false) // 设置为 true 使用模拟数据，false 使用真实 API
    
    // 模拟数据
    const mockSemanticModelList = [
      {
        id: 1,
        datasetId: 'dataset_001',
        originalFieldName: 'sales_amount',
        agentFieldName: '销售额',
        fieldSynonyms: '营收,收入,销售金额',
        fieldDescription: '某个时间段内的总销售金额',
        fieldType: 'DECIMAL',
        originalDescription: '销售订单表中的销售金额字段',
        defaultRecall: true,
        enabled: true,
        createTime: '2024-01-15T10:30:00',
        updateTime: '2024-01-15T10:30:00'
      },
      {
        id: 2,
        datasetId: 'dataset_002',
        originalFieldName: 'customer_id',
        agentFieldName: '客户ID',
        fieldSynonyms: '用户ID,顾客编号',
        fieldDescription: '客户的唯一标识符',
        fieldType: 'VARCHAR',
        originalDescription: '客户信息表中的主键字段',
        defaultRecall: false,
        enabled: true,
        createTime: '2024-01-16T14:20:00',
        updateTime: '2024-01-16T14:20:00'
      },
      {
        id: 3,
        datasetId: 'dataset_003',
        originalFieldName: 'order_date',
        agentFieldName: '订单日期',
        fieldSynonyms: '下单时间,购买日期',
        fieldDescription: '订单创建的日期',
        fieldType: 'DATE',
        originalDescription: '订单表中的日期字段',
        defaultRecall: false,
        enabled: false,
        createTime: '2024-01-17T09:15:00',
        updateTime: '2024-01-17T09:15:00'
      }
    ]

    const modelList = ref([])
    const loading = ref(true)
    const searchKeyword = ref('')
    const selectedDataset = ref('')
    const selectedItems = ref([])
    const selectAll = ref(false)
    const modalVisible = ref(false)
    const modalTitle = ref('新增语义模型配置')
    const currentEditId = ref(null)

    const formData = reactive({
      datasetId: '',
      originalFieldName: '',
      agentFieldName: '',
      fieldSynonyms: '',
      fieldDescription: '',
      fieldType: 'VARCHAR',
      originalDescription: '',
      defaultRecall: false,
      enabled: true
    })

    // 计算属性
    const datasets = computed(() => {
      const datasetSet = new Set()
      modelList.value.forEach(item => {
        if (item.datasetId) {
          datasetSet.add(item.datasetId)
        }
      })
      return Array.from(datasetSet).sort()
    })

    const filteredModelList = computed(() => {
      return selectedDataset.value 
        ? modelList.value.filter(item => item.datasetId === selectedDataset.value)
        : modelList.value
    })

    // 监听选中项变化，更新全选状态
    watch([selectedItems, filteredModelList], () => {
      const enabledItems = filteredModelList.value.filter(item => 
        !selectedDataset.value || item.datasetId === selectedDataset.value
      )
      selectAll.value = enabledItems.length > 0 && selectedItems.value.length === enabledItems.length
    })

    const loadModelList = async (keyword = '') => {
      try {
        loading.value = true
        
        if (useMockData.value) {
          // 使用模拟数据
          await new Promise(resolve => setTimeout(resolve, 500)) // 模拟网络延迟
          let filteredData = mockSemanticModelList
          if (keyword) {
            const lowerKeyword = keyword.toLowerCase()
            filteredData = mockSemanticModelList.filter(item => 
              item.originalFieldName.toLowerCase().includes(lowerKeyword) ||
              item.agentFieldName.toLowerCase().includes(lowerKeyword) ||
              item.fieldDescription.toLowerCase().includes(lowerKeyword) ||
              (item.fieldSynonyms && item.fieldSynonyms.toLowerCase().includes(lowerKeyword)) ||
              item.datasetId.toLowerCase().includes(lowerKeyword)
            )
          }
          modelList.value = filteredData
        } else {
          // 使用真实 API
          const data = keyword ? 
            await semanticModelApi.search(keyword) : 
            await semanticModelApi.getList()
          modelList.value = data || []
        }
      } catch (error) {
        console.error('加载数据失败:', error)
        if (useMockData.value) {
          modelList.value = mockSemanticModelList // 回退到模拟数据
        } else {
          alert('加载数据失败，请刷新页面重试')
          modelList.value = []
        }
      } finally {
        loading.value = false
      }
    }

    const searchModel = () => {
      loadModelList(searchKeyword.value.trim())
    }

    const filterByDataset = () => {
      // 重置选择状态
      selectedItems.value = []
      selectAll.value = false
    }

    const toggleSelectAll = () => {
      if (selectAll.value) {
        selectedItems.value = filteredModelList.value
          .filter(item => !selectedDataset.value || item.datasetId === selectedDataset.value)
          .map(item => item.id)
      } else {
        selectedItems.value = []
      }
    }

    const showAddModal = () => {
      currentEditId.value = null
      modalTitle.value = '新增语义模型配置'
      resetFormData()
      modalVisible.value = true
    }

    const showEditModal = (id) => {
      const item = modelList.value.find(m => m.id === id)
      if (!item) return

      currentEditId.value = id
      modalTitle.value = '编辑语义模型配置'
      formData.datasetId = item.datasetId || ''
      formData.originalFieldName = item.originalFieldName || ''
      formData.agentFieldName = item.agentFieldName || ''
      formData.fieldSynonyms = item.fieldSynonyms || ''
      formData.fieldDescription = item.fieldDescription || ''
      formData.fieldType = item.fieldType || 'VARCHAR'
      formData.originalDescription = item.originalDescription || ''
      formData.defaultRecall = item.defaultRecall || false
      formData.enabled = item.enabled !== undefined ? item.enabled : true
      modalVisible.value = true
    }

    const closeModal = () => {
      modalVisible.value = false
      resetFormData()
    }

    const resetFormData = () => {
      formData.datasetId = ''
      formData.originalFieldName = ''
      formData.agentFieldName = ''
      formData.fieldSynonyms = ''
      formData.fieldDescription = ''
      formData.fieldType = 'VARCHAR'
      formData.originalDescription = ''
      formData.defaultRecall = false
      formData.enabled = true
    }

    const saveModel = async () => {
      // 验证必填字段
      if (!formData.datasetId.trim() || !formData.originalFieldName.trim()) {
        alert('请填写必填字段')
        return
      }

      try {
        const data = {
          datasetId: formData.datasetId.trim(),
          originalFieldName: formData.originalFieldName.trim(),
          agentFieldName: formData.agentFieldName.trim() || null,
          fieldSynonyms: formData.fieldSynonyms.trim() || null,
          fieldDescription: formData.fieldDescription.trim() || null,
          fieldType: formData.fieldType,
          originalDescription: formData.originalDescription.trim() || null,
          defaultRecall: formData.defaultRecall,
          enabled: formData.enabled
        }

        if (useMockData.value) {
          // 使用模拟数据
          await new Promise(resolve => setTimeout(resolve, 300)) // 模拟网络延迟
          
          if (currentEditId.value) {
            // 编辑现有数据
            const index = mockSemanticModelList.findIndex(item => item.id === currentEditId.value)
            if (index !== -1) {
              mockSemanticModelList[index] = {
                ...mockSemanticModelList[index],
                datasetId: data.datasetId,
                originalFieldName: data.originalFieldName,
                agentFieldName: data.agentFieldName || '',
                fieldSynonyms: data.fieldSynonyms || '',
                fieldDescription: data.fieldDescription || '',
                fieldType: data.fieldType,
                originalDescription: data.originalDescription || '',
                defaultRecall: data.defaultRecall,
                enabled: data.enabled,
                updateTime: new Date().toISOString()
              }
            }
          } else {
            // 添加新数据
            const newItem = {
              id: Date.now(), // 简单的 ID 生成
              datasetId: data.datasetId,
              originalFieldName: data.originalFieldName,
              agentFieldName: data.agentFieldName || '',
              fieldSynonyms: data.fieldSynonyms || '',
              fieldDescription: data.fieldDescription || '',
              fieldType: data.fieldType,
              originalDescription: data.originalDescription || '',
              defaultRecall: data.defaultRecall,
              enabled: data.enabled,
              createTime: new Date().toISOString(),
              updateTime: new Date().toISOString()
            }
            mockSemanticModelList.push(newItem)
          }
          
          closeModal()
          loadModelList()
        } else {
          // 使用真实 API
          const savedData = currentEditId.value ? 
            await semanticModelApi.update(currentEditId.value, data) :
            await semanticModelApi.create(data)
          
          closeModal()
          loadModelList()
        }
      } catch (error) {
        console.error('保存失败:', error)
        alert('保存失败，请重试')
      }
    }

    const deleteModel = async (id) => {
      if (!confirm('确定要删除这条语义模型配置吗？')) {
        return
      }

      try {
        if (useMockData.value) {
          // 使用模拟数据
          await new Promise(resolve => setTimeout(resolve, 200)) // 模拟网络延迟
          const index = mockSemanticModelList.findIndex(item => item.id === id)
          if (index !== -1) {
            mockSemanticModelList.splice(index, 1)
          }
          loadModelList()
        } else {
          // 使用真实 API
          await semanticModelApi.delete(id)
          loadModelList()
        }
      } catch (error) {
        console.error('删除失败:', error)
        alert('删除失败，请重试')
      }
    }

    const batchEnableByDataset = async (enabled) => {
      if (!selectedDataset.value) {
        alert('请先选择数据集')
        return
      }
      
      const action = enabled ? '启用' : '禁用'
      
      if (!confirm(`确定要${action}数据集 "${selectedDataset.value}" 的所有配置吗？`)) {
        return
      }
      
      try {
        if (useMockData.value) {
          // 使用模拟数据
          await new Promise(resolve => setTimeout(resolve, 500)) // 模拟网络延迟
          mockSemanticModelList.forEach(item => {
            if (item.datasetId === selectedDataset.value) {
              item.enabled = enabled
              item.updateTime = new Date().toISOString()
            }
          })
          loadModelList()
          alert(`数据集批量${action}操作成功`)
        } else {
          // 使用真实 API
          await semanticModelApi.batchEnable(selectedDataset.value, enabled)
          loadModelList()
          alert(`数据集批量${action}操作成功`)
        }
      } catch (error) {
        console.error('数据集批量操作失败:', error)
        alert('数据集批量操作失败，请重试')
      }
    }

    const batchUpdateSelectedItems = async (enabled) => {
      if (selectedItems.value.length === 0) {
        alert('请先选择要操作的项')
        return
      }
      
      const action = enabled ? '启用' : '禁用'
      
      if (!confirm(`确定要${action}选中的 ${selectedItems.value.length} 项吗？`)) {
        return
      }
      
      try {
        if (useMockData.value) {
          // 使用模拟数据
          await new Promise(resolve => setTimeout(resolve, 500)) // 模拟网络延迟
          selectedItems.value.forEach(id => {
            const index = mockSemanticModelList.findIndex(item => item.id === id)
            if (index !== -1) {
              mockSemanticModelList[index].enabled = enabled
              mockSemanticModelList[index].updateTime = new Date().toISOString()
            }
          })
          loadModelList()
          selectedItems.value = []
          selectAll.value = false
          alert(`批量${action}操作成功`)
        } else {
          // 使用真实 API - 通过逐个更新实现批量操作
          const promises = selectedItems.value.map(async (id) => {
              const item = modelList.value.find(m => m.id == id)
              if (!item) return Promise.resolve()
              
              const data = {
                datasetId: item.datasetId,
                originalFieldName: item.originalFieldName,
                agentFieldName: item.agentFieldName,
                fieldSynonyms: item.fieldSynonyms,
                fieldDescription: item.fieldDescription,
                fieldType: item.fieldType,
                originalDescription: item.originalDescription,
                defaultRecall: item.defaultRecall,
                enabled: enabled
              }
              
              return await semanticModelApi.update(id, data)
            })
          
          await Promise.all(promises)
          loadModelList()
          selectedItems.value = []
          selectAll.value = false
          alert(`批量${action}操作成功`)
        }
      } catch (error) {
        console.error('批量操作失败:', error)
        alert('批量操作失败，请重试')
      }
    }

    const clearSearch = () => {
      searchKeyword.value = ''
      loadModelList()
    }

    const formatDateTime = (dateTimeStr) => {
      if (!dateTimeStr) return '-'
      const date = new Date(dateTimeStr)
      return date.toLocaleString('zh-CN')
    }

    onMounted(() => {
      loadModelList()
    })

    return {
      modelList,
      loading,
      searchKeyword,
      selectedDataset,
      selectedItems,
      selectAll,
      modalVisible,
      modalTitle,
      formData,
      datasets,
      filteredModelList,
      loadModelList,
      searchModel,
      clearSearch,
      filterByDataset,
      toggleSelectAll,
      showAddModal,
      showEditModal,
      closeModal,
      saveModel,
      deleteModel,
      batchEnableByDataset,
      batchUpdateSelectedItems,
      formatDateTime
    }
  }
}
</script>

<style scoped>
.semantic-model-page {
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
  margin-bottom: var(--space-lg);
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

.filter-area {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.dataset-filter {
  min-width: 180px;
}

/* 批量操作栏 */
.batch-actions-section {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-lg) var(--space-xl);
  margin-bottom: var(--space-xl);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-secondary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-lg);
}

.selection-info {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.selection-count {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
}

.batch-buttons {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
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
  min-width: 1400px;
}

.table th {
  background: var(--bg-secondary);
  padding: var(--space-md) var(--space-lg);
  text-align: left;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-secondary);
  font-size: var(--font-size-sm);
  position: sticky;
  top: 0;
  z-index: 10;
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

.synonyms-cell,
.description-cell {
  max-width: 200px;
  word-wrap: break-word;
  line-height: 1.5;
}

/* 复选框样式 */
.row-checkbox {
  width: 16px;
  height: 16px;
  accent-color: var(--primary-color);
  cursor: pointer;
}

.row-checkbox:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

.badge-primary {
  background: var(--primary-light);
  color: var(--primary-color);
  border: 1px solid rgba(95, 112, 225, 0.2);
}

.badge-secondary {
  background: var(--bg-secondary);
  color: var(--text-tertiary);
  border: 1px solid var(--border-primary);
}

.badge-success {
  background: var(--success-light);
  color: var(--success-color);
  border: 1px solid rgba(82, 196, 26, 0.2);
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

/* 工具提示 */
.batch-buttons [data-tooltip] {
  position: relative;
}

.batch-buttons [data-tooltip]::before,
.batch-buttons [data-tooltip]::after {
  position: absolute;
  opacity: 0;
  pointer-events: none;
  transition: all var(--transition-base);
  z-index: var(--z-tooltip);
}

.batch-buttons [data-tooltip]::before {
  content: attr(data-tooltip);
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%) translateY(-4px);
  background: var(--text-primary);
  color: var(--bg-primary);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  white-space: nowrap;
  box-shadow: var(--shadow-md);
}

.batch-buttons [data-tooltip]::after {
  content: '';
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 4px solid transparent;
  border-top-color: var(--text-primary);
}

.batch-buttons [data-tooltip]:hover::before,
.batch-buttons [data-tooltip]:hover::after {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
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
@media (max-width: 1400px) {
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

@media (max-width: 1200px) {
  .toolbar-section {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-md);
  }
  
  .search-area {
    max-width: none;
  }
  
  .batch-actions-section {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-md);
  }
  
  .batch-buttons {
    justify-content: center;
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
  
  .toolbar-section,
  .batch-actions-section {
    padding: var(--space-md);
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
  
  .batch-buttons {
    flex-direction: column;
    gap: var(--space-sm);
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
  
  .synonyms-cell,
  .description-cell {
    max-width: 150px;
  }
  
  /* 隐藏部分列以适应小屏幕 */
  .table th:nth-child(5),
  .table td:nth-child(5),
  .table th:nth-child(7),
  .table td:nth-child(7),
  .table th:nth-child(10),
  .table td:nth-child(10) {
    display: none;
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  gap: 1rem;
  flex-wrap: wrap;
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

.batch-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}

.dataset-filter {
  padding: 0.75rem 1rem;
  font-size: 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  background-color: var(--card-bg);
  min-width: 150px;
}

.dataset-filter:disabled {
  background-color: #f5f5f5;
  color: #999;
  cursor: not-allowed;
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

.btn-warning {
  background-color: #faad14;
  color: white;
}

.btn-warning:hover {
  background-color: #ffc53d;
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

.batch-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.dataset-filter {
  padding: 0.5rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  font-size: 0.9rem;
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
  min-width: 1200px;
  table-layout: auto;
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
  position: sticky;
  top: 0;
  z-index: 10;
}

.table tr:hover {
  background-color: #f9f9f9;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-info {
  background-color: #17a2b8;
  color: white;
  border: 1px solid #17a2b8;
}

.btn-info:hover:not(:disabled) {
  background-color: #138496;
  border-color: #117a8b;
}

.synonyms-cell,
.description-cell {
  max-width: 200px;
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

.badge-primary {
  background-color: #e6f7ff;
  color: var(--primary-color);
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
  width: auto;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  flex-shrink: 0;
  word-break: keep-all;
  overflow-wrap: normal;
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
  max-width: 700px;
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

  .batch-actions {
    flex-wrap: wrap;
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
