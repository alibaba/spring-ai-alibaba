<template>
  <div>
    <HeaderComponent 
      title="语义模型配置"
      subtitle="对数据集字段进行语义重新设定，提升智能体自动选择数据集和问答的准确性"
      icon="bi bi-diagram-3"
    />

    <div class="container">
      <div class="toolbar">
        <div class="search-container">
          <input 
            type="text" 
            v-model="searchKeyword"
            class="search-input" 
            placeholder="搜索字段名称或同义词..."
            @keyup.enter="searchModel"
          >
          <button class="btn btn-primary" @click="searchModel">
            <i class="bi bi-search"></i> 搜索
          </button>
        </div>
        <div class="batch-actions">
          <select v-model="selectedDataset" class="dataset-filter" @change="filterByDataset">
            <option value="">所有数据集</option>
            <option v-for="dataset in datasets" :key="dataset" :value="dataset">
              {{ dataset }}
            </option>
          </select>
          <button 
            class="btn btn-info" 
            @click="batchEnableByDataset(true)"
            :disabled="!selectedDataset"
            title="按数据集批量启用"
          >
            <i class="bi bi-database-check"></i> 数据集启用
          </button>
          <button 
            class="btn btn-warning" 
            @click="batchEnableByDataset(false)"
            :disabled="!selectedDataset"
            title="按数据集批量禁用"
          >
            <i class="bi bi-database-x"></i> 数据集禁用
          </button>
          <button class="btn btn-success" @click="batchUpdateSelectedItems(true)">
            <i class="bi bi-check-circle"></i> 批量启用
          </button>
          <button class="btn btn-warning" @click="batchUpdateSelectedItems(false)">
            <i class="bi bi-x-circle"></i> 批量禁用
          </button>
          <button class="btn btn-primary" @click="showAddModal">
            <i class="bi bi-plus-circle"></i> 新增配置
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
.container {
  max-width: 100%;
  margin: 0 auto;
  padding: 1rem 2rem;
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
