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
          >
          <button class="btn btn-primary" @click="searchModel">
            <i class="bi bi-search"></i> 搜索
          </button>
        </div>
        <div class="batch-actions">
          <select v-model="selectedDataset" class="dataset-filter">
            <option value="">所有数据集</option>
            <option value="user_dataset">user_dataset</option>
            <option value="sales_dataset">sales_dataset</option>
          </select>
          <button class="btn btn-success">
            <i class="bi bi-check-circle"></i> 批量启用
          </button>
          <button class="btn btn-warning">
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
                  <input type="checkbox" v-model="selectAll" class="form-control">
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
              <tr v-if="loading">
                <td colspan="11" class="loading">
                  <div class="spinner"></div>
                  加载中...
                </td>
              </tr>
              <tr v-else-if="modelList.length === 0">
                <td colspan="11" class="empty-state">
                  <div class="empty-icon"><i class="bi bi-inbox"></i></div>
                  <div>暂无数据</div>
                </td>
              </tr>
              <tr v-else v-for="item in modelList" :key="item.id">
                <td>
                  <input type="checkbox" class="form-control">
                </td>
                <td><span class="badge badge-primary">{{ item.datasetId }}</span></td>
                <td><strong>{{ item.originalFieldName }}</strong></td>
                <td>{{ item.agentFieldName || '-' }}</td>
                <td class="synonyms-cell">{{ item.fieldSynonyms || '-' }}</td>
                <td class="description-cell">{{ item.fieldDescription || '-' }}</td>
                <td><span class="badge badge-secondary">{{ item.fieldType }}</span></td>
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
                    <button class="btn btn-primary btn-sm" @click="editItem(item.id)">
                      <i class="bi bi-pencil"></i> 编辑
                    </button>
                    <button class="btn btn-danger btn-sm" @click="deleteItem(item.id)">
                      <i class="bi bi-trash"></i> 删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="modalVisible" class="modal-overlay" @click="closeModal">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h3>{{ modalTitle }}</h3>
            <button @click="closeModal">&times;</button>
          </div>
          <div class="modal-body">
            <p>语义模型配置表单（简化版本）</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import HeaderComponent from '../components/HeaderComponent.vue'

export default {
  name: 'SemanticModel',
  components: {
    HeaderComponent
  },
  setup() {
    const searchKeyword = ref('')
    const selectedDataset = ref('')
    const selectAll = ref(false)
    const loading = ref(false)
    const modalVisible = ref(false)
    const modalTitle = ref('新增语义模型配置')
    const modelList = ref([
      {
        id: 1,
        datasetId: 'user_dataset',
        originalFieldName: 'user_age',
        agentFieldName: '用户年龄',
        fieldSynonyms: '年龄,岁数',
        fieldDescription: '用户的年龄信息',
        fieldType: 'INTEGER',
        defaultRecall: true,
        enabled: true,
        createTime: '2025-07-28T10:00:00'
      },
      {
        id: 2,
        datasetId: 'sales_dataset',
        originalFieldName: 'product_price',
        agentFieldName: '产品价格',
        fieldSynonyms: '价格,单价,费用',
        fieldDescription: '产品的销售价格',
        fieldType: 'DECIMAL',
        defaultRecall: false,
        enabled: true,
        createTime: '2025-07-28T11:00:00'
      }
    ])

    const searchModel = () => {
      console.log('搜索:', searchKeyword.value)
    }

    const showAddModal = () => {
      modalTitle.value = '新增语义模型配置'
      modalVisible.value = true
    }

    const closeModal = () => {
      modalVisible.value = false
    }

    const editItem = (id) => {
      modalTitle.value = '编辑语义模型配置'
      modalVisible.value = true
      console.log('编辑项目:', id)
    }

    const deleteItem = (id) => {
      if (confirm('确定删除吗？')) {
        modelList.value = modelList.value.filter(item => item.id !== id)
      }
    }

    const formatDateTime = (dateTimeStr) => {
      if (!dateTimeStr) return '-'
      const date = new Date(dateTimeStr)
      return date.toLocaleString('zh-CN')
    }

    onMounted(() => {
      console.log('SemanticModel 页面加载完成')
    })

    return {
      searchKeyword,
      selectedDataset,
      selectAll,
      loading,
      modalVisible,
      modalTitle,
      modelList,
      searchModel,
      showAddModal,
      closeModal,
      editItem,
      deleteItem,
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
  outline: none;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: var(--radius);
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-primary {
  background-color: var(--primary-color);
  color: white;
}

.btn-success {
  background-color: var(--secondary-color);
  color: white;
}

.btn-warning {
  background-color: #faad14;
  color: white;
}

.btn-danger {
  background-color: #ff4d4f;
  color: white;
}

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.875rem;
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
}

.table th,
.table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.table th {
  background-color: #fafafa;
  font-weight: 500;
  color: #666;
}

.synonyms-cell,
.description-cell {
  max-width: 200px;
  word-wrap: break-word;
}

.badge {
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
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
}

.form-control {
  padding: 0.25rem;
}

.loading, .empty-state {
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

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
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

.modal-body {
  padding: 1.5rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
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
}
</style>
