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
      title="业务知识管理配置"
      subtitle="管理企业知识引擎，配置业务术语、黑话和常用表达"
      icon="bi bi-book"
    />

    <div class="container">
      <div class="toolbar">
        <div class="search-container">
          <input 
            type="text" 
            v-model="searchKeyword"
            class="search-input" 
            placeholder="搜索业务名词、说明或同义词..."
          >
          <button class="btn btn-primary" @click="searchKnowledge">
            <i class="bi bi-search"></i> 搜索
          </button>
        </div>
        <button class="btn btn-success" @click="showAddModal">
          <i class="bi bi-plus-circle"></i> 新增知识
        </button>
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
              <tr v-if="loading">
                <td colspan="7" class="loading">
                  <div class="spinner"></div>
                  加载中...
                </td>
              </tr>
              <tr v-else-if="knowledgeList.length === 0">
                <td colspan="7" class="empty-state">
                  <div class="empty-icon"><i class="bi bi-inbox"></i></div>
                  <div>暂无数据</div>
                </td>
              </tr>
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
            <p>模态框内容（简化版本）</p>
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
  name: 'BusinessKnowledge',
  components: {
    HeaderComponent
  },
  setup() {
    const searchKeyword = ref('')
    const loading = ref(false)
    const modalVisible = ref(false)
    const modalTitle = ref('新增业务知识')
    const knowledgeList = ref([
      {
        id: 1,
        businessTerm: '年龄分布',
        description: '用户年龄的分布情况分析',
        synonyms: '年龄画像,年龄构成',
        defaultRecall: true,
        datasetId: 'user_dataset',
        createTime: '2025-07-28T10:00:00'
      },
      {
        id: 2,
        businessTerm: '销售业绩',
        description: '产品或服务的销售表现指标',
        synonyms: '销售表现,业绩指标',
        defaultRecall: false,
        datasetId: 'sales_dataset',
        createTime: '2025-07-28T11:00:00'
      }
    ])

    const searchKnowledge = () => {
      console.log('搜索:', searchKeyword.value)
    }

    const showAddModal = () => {
      modalTitle.value = '新增业务知识'
      modalVisible.value = true
    }

    const closeModal = () => {
      modalVisible.value = false
    }

    const editItem = (id) => {
      modalTitle.value = '编辑业务知识'
      modalVisible.value = true
      console.log('编辑项目:', id)
    }

    const deleteItem = (id) => {
      if (confirm('确定删除吗？')) {
        knowledgeList.value = knowledgeList.value.filter(item => item.id !== id)
      }
    }

    const formatDateTime = (dateTimeStr) => {
      if (!dateTimeStr) return '-'
      const date = new Date(dateTimeStr)
      return date.toLocaleString('zh-CN')
    }

    onMounted(() => {
      console.log('BusinessKnowledge 页面加载完成')
    })

    return {
      searchKeyword,
      loading,
      modalVisible,
      modalTitle,
      knowledgeList,
      searchKnowledge,
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

.btn-danger {
  background-color: #ff4d4f;
  color: white;
}

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.875rem;
}

.card {
  background-color: var(--card-bg);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
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

.table th {
  background-color: #fafafa;
  font-weight: 500;
  color: #666;
}

.description-cell {
  max-width: 300px;
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

.action-buttons {
  display: flex;
  gap: 0.5rem;
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

.modal-body {
  padding: 1.5rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
