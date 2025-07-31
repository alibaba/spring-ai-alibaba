<template>
  <div class="agent-debug-panel">
    <!-- 调试头部 -->
    <div class="debug-header">
      <h2>智能体调试</h2>
      <p class="debug-subtitle">测试智能体的响应能力和配置正确性</p>
    </div>

    <!-- 调试结果区域 -->
    <div class="debug-result-section">
      <div class="result-header">
        <div class="result-title">
          <i class="bi bi-terminal"></i>
          调试结果
        </div>
        <div class="result-status" v-if="debugStatus">
          <span class="badge" :class="getStatusClass()">{{ debugStatus }}</span>
        </div>
      </div>
      <div class="result-content" ref="resultContainer">
        <!-- 空状态 -->
        <div v-if="!hasResults" class="empty-state">
          <div class="empty-icon">
            <i class="bi bi-chat-square-text"></i>
          </div>
          <div class="empty-text">
            输入测试问题，查看智能体的响应结果
          </div>
          <div class="example-queries" v-if="exampleQueries.length > 0">
            <div 
              class="example-query" 
              v-for="example in exampleQueries" 
              :key="example"
              @click="useExampleQuery(example)"
            >
              {{ example }}
            </div>
          </div>
        </div>
        
        <!-- 结果展示 -->
        <div v-else id="debug-results-container">
          <!-- 动态生成的结果区块将在这里显示 -->
        </div>
      </div>
    </div>

    <!-- 调试输入区域 -->
    <div class="debug-input-section">
      <div class="input-container">
        <input 
          type="text" 
          v-model="debugQuery" 
          class="debug-input" 
          placeholder="请输入测试问题..."
          :disabled="isDebugging || isInitializing"
          @keyup.enter="startDebug"
          ref="debugInput"
        >
        <button 
          class="debug-button" 
          :disabled="isDebugging || isInitializing || !debugQuery.trim()"
          @click="startDebug"
        >
          <i class="bi bi-play-circle" v-if="!isDebugging"></i>
          <div class="spinner" v-else></div>
          {{ isDebugging ? '调试中...' : '开始调试' }}
        </button>
        <button 
          class="schema-init-button" 
          :disabled="isDebugging || isInitializing"
          @click="openSchemaInitModal"
        >
          <i class="bi bi-database-gear"></i>
          初始化信息源
        </button>
        <button 
          class="init-button" 
          :disabled="isInitializing || isDebugging"
          :class="{ loading: isInitializing }"
          @click="initializeDataSource"
        >
          <i class="bi bi-database-add" v-if="!isInitializing && !isInitialized"></i>
          <i class="bi bi-check-circle" v-if="!isInitializing && isInitialized"></i>
          <div class="spinner" v-if="isInitializing"></div>
          {{ getInitButtonText() }}
        </button>
      </div>
    </div>

    <!-- 初始化信息源模态框 -->
    <div v-if="showSchemaInitModal" class="modal-overlay" @click="closeSchemaInitModal">
      <div class="modal-dialog" @click.stop>
        <div class="modal-header">
          <h3>
            <i class="bi bi-database-gear"></i>
            初始化信息源
          </h3>
          <button class="close-btn" @click="closeSchemaInitModal">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <!-- 初始化状态显示 -->
          <div class="init-status-card" :class="{ 'initialized': isInitialized, 'not-initialized': !isInitialized }">
            <div class="status-indicator">
              <div class="status-icon">
                <i class="bi bi-check-circle-fill" v-if="isInitialized"></i>
                <i class="bi bi-exclamation-triangle-fill" v-else></i>
              </div>
              <div class="status-info">
                <div class="status-title">
                  {{ isInitialized ? '数据源已初始化' : '数据源未初始化' }}
                </div>
                <div class="status-desc">
                  {{ isInitialized ? '智能体已准备就绪，可以开始调试' : '请配置并初始化数据源，然后再进行调试' }}
                </div>
              </div>
            </div>
            
            <!-- 统计信息 -->
            <div class="stats-info" v-if="schemaStatistics && isInitialized">
              <div class="stat-item">
                <span class="stat-label">文档总数</span>
                <span class="stat-value">{{ schemaStatistics.documentCount || 0 }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">智能体ID</span>
                <span class="stat-value">{{ schemaStatistics.agentId }}</span>
              </div>
            </div>
          </div>

          <!-- 初始化配置表单 -->
          <div class="init-config-form" v-if="!isInitialized || showConfigForm">
            <div class="form-row">
              <div class="form-group">
                <label>选择数据源</label>
                <select v-model="schemaInitForm.selectedDatasource" class="form-control" @change="onDatasourceChange">
                  <option value="">请选择数据源</option>
                  <option v-for="ds in availableDatasources" 
                          :key="ds.id" 
                          :value="ds">
                    {{ ds.name }} ({{ getDatasourceTypeText(ds.type) }})
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label>数据库Schema</label>
                <input type="text" v-model="schemaInitForm.schema" class="form-control" 
                       placeholder="请输入数据库Schema名称">
              </div>
            </div>
            
            <div class="form-group" v-if="schemaInitForm.selectedDatasource">
              <label>选择表 ({{ selectedTables.length }} 个已选择)</label>
              <div class="table-selection">
                <div class="table-search">
                  <input type="text" v-model="tableSearchKeyword" placeholder="搜索表名..." class="form-control">
                  <div class="table-actions">
                    <button type="button" class="btn btn-sm btn-outline" @click="selectAllTables">全选</button>
                    <button type="button" class="btn btn-sm btn-outline" @click="clearAllTables">清空</button>
                    <button type="button" class="btn btn-sm btn-primary" @click="loadTables" :disabled="!schemaInitForm.selectedDatasource">
                      <i class="bi bi-arrow-clockwise"></i>
                      刷新表列表
                    </button>
                  </div>
                </div>
                <div class="table-list" v-if="availableTables.length > 0">
                  <div class="table-grid">
                    <label v-for="table in filteredTables" :key="table" class="table-checkbox">
                      <input type="checkbox" v-model="selectedTables" :value="table">
                      <span class="table-name">{{ table }}</span>
                    </label>
                  </div>
                </div>
                <div v-else-if="schemaInitForm.selectedDatasource" class="empty-tables">
                  <i class="bi bi-database"></i>
                  <p>暂无可用表，请检查数据源连接或点击"刷新表列表"</p>
                </div>
                <div v-else class="empty-tables">
                  <i class="bi bi-database"></i>
                  <p>请先选择数据源</p>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeSchemaInitModal">取消</button>
          <button type="button" class="btn btn-primary" @click="initializeSchema" 
                  :disabled="!canInitialize || schemaInitializing" v-if="!isInitialized || showConfigForm">
            <i class="bi bi-database-add" v-if="!schemaInitializing"></i>
            <i class="bi bi-arrow-repeat spin" v-else></i>
            {{ schemaInitializing ? '初始化中...' : '初始化信息源' }}
          </button>
          <button type="button" class="btn btn-outline" @click="getSchemaStatistics" v-if="isInitialized">
            <i class="bi bi-bar-chart"></i>
            刷新统计
          </button>
          <button type="button" class="btn btn-warning" @click="clearSchemaData" v-if="isInitialized">
            <i class="bi bi-trash"></i>
            清空数据
          </button>
          <button type="button" class="btn btn-secondary" @click="toggleConfigForm" v-if="isInitialized">
            <i class="bi bi-gear"></i>
            {{ showConfigForm ? '隐藏配置' : '重新配置' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template><script>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'

export default {
  name: 'AgentDebugPanel',
  props: {
    agentId: {
      type: [String, Number],
      required: true
    }
  },
  setup(props) {
    // 响应式数据
    const debugQuery = ref('')
    const isDebugging = ref(false)
    const isInitializing = ref(false)
    const isInitialized = ref(false)
    const debugStatus = ref('')
    const hasResults = ref(false)
    const debugInput = ref(null)
    const resultContainer = ref(null)
    const exampleQueries = ref([])

    // 获取状态样式类
    const getStatusClass = () => {
      if (debugStatus.value.includes('完成')) return 'badge-success'
      if (debugStatus.value.includes('错误') || debugStatus.value.includes('失败')) return 'badge-error'
      if (debugStatus.value.includes('处理中') || debugStatus.value.includes('调试中')) return 'badge-warning'
      return 'badge-info'
    }

    // 使用示例问题
    const useExampleQuery = (example) => {
      debugQuery.value = example
      nextTick(() => {
        if (debugInput.value) {
          debugInput.value.focus()
        }
      })
    }

    // 获取初始化按钮文本
    const getInitButtonText = () => {
      if (isInitializing.value) return '检查中...'
      if (isInitialized.value) return '已初始化'
      return '检查初始化状态'
    }

    // 开始调试
    const startDebug = async () => {
      if (!debugQuery.value.trim() || isDebugging.value) return

      try {
        isDebugging.value = true
        debugStatus.value = '正在处理中...'
        hasResults.value = true

        // 简单的模拟调试过程
        setTimeout(() => {
          debugStatus.value = '调试完成'
          isDebugging.value = false
          
          // 显示简单结果
          const container = document.getElementById('debug-results-container')
          if (container) {
            container.innerHTML = `<div style="padding: 1rem; background: #f0f8ff; border-radius: 6px; margin: 1rem 0;">
              <h4>调试结果</h4>
              <p>问题: ${debugQuery.value}</p>
              <p>状态: 调试完成</p>
            </div>`
          }
        }, 2000)

      } catch (error) {
        console.error('启动调试失败:', error)
        debugStatus.value = '启动调试失败: ' + error.message
        isDebugging.value = false
      }
    }

    // 初始化数据源
    const initializeDataSource = async () => {
      if (isInitializing.value || isDebugging.value) return

      try {
        isInitializing.value = true
        debugStatus.value = '正在检查初始化状态...'

        // 模拟检查过程
        setTimeout(() => {
          isInitialized.value = true
          debugStatus.value = '数据源已初始化，可以开始调试'
          isInitializing.value = false
          
          setTimeout(() => {
            debugStatus.value = ''
          }, 3000)
        }, 1000)

      } catch (error) {
        console.error('检查初始化状态错误:', error)
        debugStatus.value = '检查失败，请确保智能体配置正确'
        isInitialized.value = false
        isInitializing.value = false
      }
    }

    // 模态框相关状态
    const showSchemaInitModal = ref(false)
    const showConfigForm = ref(false)
    const schemaInitializing = ref(false)
    const schemaStatistics = ref(null)
    
    // 表单数据
    const schemaInitForm = reactive({
      selectedDatasource: '',
      schema: ''
    })
    
    // 数据源和表相关
    const availableDatasources = ref([])
    const availableTables = ref([])
    const selectedTables = ref([])
    const tableSearchKeyword = ref('')
    
    // 计算属性
    const filteredTables = computed(() => {
      if (!tableSearchKeyword.value) return availableTables.value
      return availableTables.value.filter(table => 
        table.toLowerCase().includes(tableSearchKeyword.value.toLowerCase())
      )
    })
    
    const canInitialize = computed(() => {
      return schemaInitForm.selectedDatasource && 
             schemaInitForm.schema && 
             selectedTables.value.length > 0
    })

    // 打开模态框
    const openSchemaInitModal = async () => {
      showSchemaInitModal.value = true
      await loadAvailableDatasources()
      await getSchemaStatistics()
    }
    
    // 关闭模态框
    const closeSchemaInitModal = () => {
      showSchemaInitModal.value = false
      showConfigForm.value = false
    }
    
    // 加载可用数据源
    const loadAvailableDatasources = async () => {
      try {
        // TODO: 调用实际的API获取数据源列表
        // const response = await fetch('/api/datasources')
        // const data = await response.json()
        // availableDatasources.value = data
        
        // 临时使用模拟数据
        availableDatasources.value = [
          { id: 1, name: 'MySQL主库', type: 'mysql' },
          { id: 2, name: 'PostgreSQL数据仓库', type: 'postgresql' },
          { id: 3, name: 'Oracle生产库', type: 'oracle' }
        ]
      } catch (error) {
        console.error('加载数据源失败:', error)
      }
    }
    
    // 获取数据源类型文本
    const getDatasourceTypeText = (type) => {
      const typeMap = {
        mysql: 'MySQL',
        postgresql: 'PostgreSQL', 
        oracle: 'Oracle',
        sqlserver: 'SQL Server'
      }
      return typeMap[type] || type
    }
    
    // 数据源变化处理
    const onDatasourceChange = () => {
      availableTables.value = []
      selectedTables.value = []
      if (schemaInitForm.selectedDatasource) {
        loadTables()
      }
    }
    
    // 加载表列表
    const loadTables = async () => {
      if (!schemaInitForm.selectedDatasource) return
      
      try {
        // 模拟表列表加载
        availableTables.value = [
          'users', 'orders', 'products', 'categories',
          'order_items', 'payments', 'reviews', 'inventory'
        ]
      } catch (error) {
        console.error('加载表列表失败:', error)
      }
    }
    
    // 全选表
    const selectAllTables = () => {
      selectedTables.value = [...filteredTables.value]
    }
    
    // 清空选择
    const clearAllTables = () => {
      selectedTables.value = []
    }
    
    // 初始化Schema
    const initializeSchema = async () => {
      if (!canInitialize.value || schemaInitializing.value) return
      
      try {
        schemaInitializing.value = true
        
        // 构建请求数据
        const requestData = {
          dbConfig: {
            id: schemaInitForm.selectedDatasource.id,
            name: schemaInitForm.selectedDatasource.name,
            type: schemaInitForm.selectedDatasource.type
          },
          schema: schemaInitForm.schema,
          tables: selectedTables.value
        }
        
        // 调用后端API
        const response = await fetch(`/api/agent/${props.agentId}/schema/init`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(requestData)
        })
        
        const result = await response.json()
        
        if (result.success) {
          isInitialized.value = true
          schemaStatistics.value = {
            agentId: props.agentId,
            documentCount: result.tablesCount * 10, // 估算文档数量
            lastUpdated: new Date().toLocaleString()
          }
          
          debugStatus.value = '信息源初始化完成'
          setTimeout(() => {
            debugStatus.value = ''
          }, 3000)
          
          showConfigForm.value = false
        } else {
          throw new Error(result.message || '初始化失败')
        }
        
      } catch (error) {
        console.error('初始化失败:', error)
        debugStatus.value = '初始化失败: ' + error.message
      } finally {
        schemaInitializing.value = false
      }
    }
    
    // 获取统计信息
    const getSchemaStatistics = async () => {
      try {
        const response = await fetch(`/api/agent/${props.agentId}/schema/statistics`)
        const result = await response.json()
        
        if (result.success) {
          schemaStatistics.value = {
            agentId: props.agentId,
            documentCount: result.data.documentCount || 0,
            hasData: result.data.hasData,
            lastUpdated: new Date().toLocaleString()
          }
          isInitialized.value = result.data.hasData
        } else {
          console.error('获取统计信息失败:', result.message)
        }
      } catch (error) {
        console.error('获取统计信息失败:', error)
        // 如果API调用失败，使用默认值
        schemaStatistics.value = null
        isInitialized.value = false
      }
    }
    
    // 清空Schema数据
    const clearSchemaData = async () => {
      if (!confirm('确定要清空所有Schema数据吗？此操作不可恢复。')) return
      
      try {
        // 调用后端API清空数据
        const response = await fetch(`/api/agent/${props.agentId}/schema/clear`, {
          method: 'DELETE'
        })
        
        const result = await response.json()
        
        if (result.success) {
          isInitialized.value = false
          schemaStatistics.value = null
          debugStatus.value = 'Schema数据已清空'
          
          setTimeout(() => {
            debugStatus.value = ''
          }, 3000)
        } else {
          throw new Error(result.message || '清空失败')
        }
        
      } catch (error) {
        console.error('清空数据失败:', error)
        debugStatus.value = '清空失败: ' + error.message
      }
    }
    
    // 切换配置表单显示
    const toggleConfigForm = () => {
      showConfigForm.value = !showConfigForm.value
    }

    // 组件挂载时的初始化
    onMounted(async () => {
      // 加载示例问题
      exampleQueries.value = [
        '查询用户总数',
        '显示最近一周的订单统计',
        '分析销售趋势'
      ]
    })

    // 组件卸载时清理资源
    onUnmounted(() => {
      // 清理资源
    })

    return {
      debugQuery,
      isDebugging,
      isInitializing,
      isInitialized,
      debugStatus,
      hasResults,
      debugInput,
      resultContainer,
      exampleQueries,
      getStatusClass,
      getInitButtonText,
      useExampleQuery,
      startDebug,
      initializeDataSource,
      openSchemaInitModal,
      closeSchemaInitModal,
      showSchemaInitModal,
      showConfigForm,
      schemaInitializing,
      schemaStatistics,
      schemaInitForm,
      availableDatasources,
      availableTables,
      selectedTables,
      tableSearchKeyword,
      filteredTables,
      canInitialize,
      loadAvailableDatasources,
      getDatasourceTypeText,
      onDatasourceChange,
      loadTables,
      selectAllTables,
      clearAllTables,
      initializeSchema,
      getSchemaStatistics,
      clearSchemaData,
      toggleConfigForm
    }
  }
}
</script>
<style scoped>
.agent-debug-panel {
  padding: 0;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.debug-header {
  margin-bottom: 1rem;
  flex-shrink: 0;
}

.debug-header h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.5rem;
}

.debug-subtitle {
  color: #666;
  font-size: 0.95rem;
  margin: 0;
}

.debug-result-section {
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.result-header {
  padding: 1rem 1.5rem;
  background-color: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title {
  font-size: 1.1rem;
  font-weight: 500;
  color: #333;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.result-status .badge {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 500;
}

.badge-success {
  background-color: #f6ffed;
  color: #52c41a;
}

.badge-error {
  background-color: #fff2f0;
  color: #ff4d4f;
}

.badge-warning {
  background-color: #fffbe6;
  color: #faad14;
}

.badge-info {
  background-color: #e6f7ff;
  color: #1890ff;
}

.result-content {
  flex: 1;
  padding: 1.5rem;
  overflow-y: auto;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: #999;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
  color: #ddd;
}

.empty-text {
  font-size: 1.1rem;
  margin-bottom: 2rem;
}

.example-queries {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
  max-width: 600px;
  margin: 0 auto;
}

.example-query {
  padding: 0.5rem 1rem;
  background: #f0f8ff;
  border: 1px solid #d6e4ff;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 0.9rem;
  color: #1890ff;
}

.example-query:hover {
  background: #1890ff;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.debug-input-section {
  margin-top: 1.5rem;
  margin-bottom: 0;
}

.input-container {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.debug-input {
  flex: 1;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  transition: all 0.3s;
  outline: none;
}

.debug-input:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.debug-input:disabled {
  background-color: #f5f5f5;
  color: #999;
  cursor: not-allowed;
  border-color: #ddd;
}

.debug-button {
  padding: 0.75rem 1.5rem;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 120px;
  justify-content: center;
}

.debug-button:hover:not(:disabled) {
  background-color: #40a9ff;
}

.debug-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
}

.schema-init-button {
  padding: 0.75rem 1.5rem;
  background-color: #722ed1;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 140px;
  justify-content: center;
}

.schema-init-button:hover:not(:disabled) {
  background-color: #9254de;
}

.schema-init-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
}

.init-button {
  padding: 0.75rem 1.5rem;
  background-color: #52c41a;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 140px;
  justify-content: center;
}

.init-button:hover:not(:disabled) {
  background-color: #73d13d;
}

.init-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-dialog {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  max-width: 800px;
  width: 90%;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 1.5rem;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
  padding: 0.25rem;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid #e8e8e8;
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  background: #fafafa;
}

/* 初始化状态卡片 */
.init-status-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  border: 1px solid #e9ecef;
}

.init-status-card.initialized {
  background: #f6ffed;
  border-color: #b7eb8f;
}

.init-status-card.not-initialized {
  background: #fff7e6;
  border-color: #ffd591;
}

.status-indicator {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.status-icon {
  font-size: 1.5rem;
  margin-top: 0.25rem;
}

.init-status-card.initialized .status-icon {
  color: #52c41a;
}

.init-status-card.not-initialized .status-icon {
  color: #faad14;
}

.status-info {
  flex: 1;
}

.status-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.init-status-card.initialized .status-title {
  color: #389e0d;
}

.init-status-card.not-initialized .status-title {
  color: #d48806;
}

.status-desc {
  color: #666;
  line-height: 1.5;
}

.stats-info {
  display: flex;
  gap: 2rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #d9f7be;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-label {
  font-size: 0.85rem;
  color: #666;
}

.stat-value {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

/* 表单样式 */
.init-config-form {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  border: 1px solid #e8e8e8;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 500;
  color: #333;
  font-size: 0.9rem;
}

.form-control {
  padding: 0.75rem;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* 表选择区域 */
.table-selection {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
}

.table-search {
  padding: 1rem;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  gap: 1rem;
  align-items: center;
}

.table-search .form-control {
  flex: 1;
}

.table-actions {
  display: flex;
  gap: 0.5rem;
}

.table-list {
  max-height: 300px;
  overflow-y: auto;
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 0.5rem;
  padding: 1rem;
}

.table-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.table-checkbox:hover {
  background: #f0f8ff;
}

.table-checkbox input[type="checkbox"] {
  margin: 0;
}

.table-name {
  font-size: 0.9rem;
  color: #333;
}

.empty-tables {
  padding: 2rem;
  text-align: center;
  color: #999;
}

.empty-tables i {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  display: block;
}

/* 按钮样式 */
.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #40a9ff;
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #d9d9d9;
}

.btn-secondary:hover:not(:disabled) {
  background: #e6f7ff;
  border-color: #91d5ff;
  color: #1890ff;
}

.btn-outline {
  background: white;
  color: #1890ff;
  border: 1px solid #1890ff;
}

.btn-outline:hover:not(:disabled) {
  background: #1890ff;
  color: white;
}

.btn-warning {
  background: #faad14;
  color: white;
}

.btn-warning:hover:not(:disabled) {
  background: #ffc53d;
}

.btn-sm {
  padding: 0.25rem 0.75rem;
  font-size: 0.8rem;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spin {
  animation: spin 1s linear infinite;
}

@media (max-width: 768px) {
  .input-container {
    flex-direction: column;
  }

  .debug-button, .init-button, .schema-init-button {
    width: 100%;
  }

  .example-queries {
    flex-direction: column;
    align-items: center;
  }

  .example-query {
    width: 100%;
    max-width: 300px;
    text-align: center;
  }

  .modal-dialog {
    width: 95%;
    max-height: 95vh;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .table-grid {
    grid-template-columns: 1fr;
  }

  .table-search {
    flex-direction: column;
    align-items: stretch;
  }

  .table-actions {
    justify-content: center;
  }

  .stats-info {
    flex-direction: column;
    gap: 1rem;
  }

  .modal-footer {
    flex-wrap: wrap;
  }
}
</style>