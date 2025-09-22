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
<!-- 提示词优化配置组件 -->
<template>
  <div class="prompt-optimization-config">
    <!-- 消息提示 -->
    <div v-if="message.show" class="message-toast" :class="message.type">
      <span>{{ message.text }}</span>
      <button class="message-close" @click="hideMessage">×</button>
    </div>
    <div class="config-header">
      <h3>增强式Prompt优化配置</h3>
      <p class="config-description">
        配置的Prompt仅用作效果优化，支持多个提示词配置，在原始模板基础上进行增强。示例配置：
      </p>
      <ul class="optimization-tips">
        <li>1. 查询的年销售额精确到小数点后两位。</li>
        <li>2. 报告格式第一章节请先总结年销售额</li>
      </ul>
    </div>

    <!-- 智能体Prompt -->
    <div class="agent-prompt-section">
      <h4>智能体Prompt</h4>
      <div class="prompt-display">
        {{ agentPrompt || '你是一个销售数据分析专家，能够帮助用户分析销售趋势，客户行为和业务指标。' }}
      </div>
    </div>

    <!-- 优化配置列表 -->
    <div class="optimization-configs">
      <div class="config-list-header">
        <h4>优化配置列表</h4>
        <div class="header-actions">
          <button 
            v-if="optimizationConfigs.length > 0"
            class="batch-action-btn" 
            @click="showBatchActions = !showBatchActions"
          >
            <i class="icon-settings"></i>
            批量操作
          </button>
          <button class="add-config-btn" @click="showAddConfigDialog = true">
            <i class="icon-plus"></i>
            添加优化配置
          </button>
        </div>
      </div>

      <!-- 批量操作面板 -->
      <div v-if="showBatchActions" class="batch-actions-panel">
        <div class="batch-actions-content">
          <div class="batch-selection">
            <input 
              type="checkbox" 
              :checked="isAllSelected" 
              :indeterminate="isIndeterminate"
              @change="toggleSelectAll"
              class="select-all-checkbox"
            />
            <span class="batch-info">
              {{ isAllSelected ? '已全选' : `已选择 ${selectedConfigs.length} 个配置` }}
            </span>
          </div>
          <div class="batch-buttons">
            <button class="batch-btn enable" @click="batchEnable" :disabled="selectedConfigs.length === 0">
              批量启用
            </button>
            <button class="batch-btn disable" @click="batchDisable" :disabled="selectedConfigs.length === 0">
              批量禁用
            </button>
            <button class="batch-btn cancel" @click="clearSelection">
              取消选择
            </button>
          </div>
        </div>
      </div>

      <div v-if="optimizationConfigs.length === 0" class="empty-state">
        <p>暂无优化配置，点击"添加优化配置"开始配置</p>
      </div>

      <div v-else class="config-list">
        <div
          v-for="config in optimizationConfigs"
          :key="config.id"
          class="config-item"
          :class="{ disabled: !config.enabled, selected: selectedConfigs.includes(config.id) }"
        >
          <div class="config-header">
            <div class="config-info">
              <input 
                type="checkbox" 
                :value="config.id" 
                v-model="selectedConfigs"
                class="config-checkbox"
              />
              <span class="config-name">{{ config.name }}</span>
              <span class="config-priority" v-if="config.priority !== undefined">
                优先级: {{ config.priority }}
              </span>
            </div>
            <div class="config-actions">
              <button
                class="toggle-btn"
                :class="{ active: config.enabled }"
                @click="toggleConfig(config)"
              >
                {{ config.enabled ? '已启用' : '已禁用' }}
              </button>
              <button class="edit-btn" @click="editConfig(config)">编辑</button>
              <button class="priority-btn" @click="showPriorityDialog(config)">优先级</button>
              <button class="delete-btn" @click="deleteConfig(config.id)">删除</button>
            </div>
          </div>
          <div class="config-content">
            <p class="config-description">{{ config.description }}</p>
            <div class="optimization-prompt">
              {{ config.optimizationPrompt }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑配置对话框 -->
    <div v-if="showAddConfigDialog || editingConfig" class="dialog-overlay" @click="closeDialog">
      <div class="dialog-content" @click.stop>
        <div class="dialog-header">
          <h3>{{ editingConfig ? '编辑优化配置' : '添加优化配置' }}</h3>
          <button class="close-btn" @click="closeDialog">×</button>
        </div>
        <form @submit.prevent="saveConfig" class="config-form">
          <div class="form-group">
            <label for="configName">配置名称</label>
            <input
              id="configName"
              v-model="formData.name"
              type="text"
              placeholder="请输入配置名称"
              required
            />
          </div>
          


          <div class="form-group">
            <label for="description">配置描述</label>
            <input
              id="description"
              v-model="formData.description"
              type="text"
              placeholder="请输入配置描述"
            />
          </div>

          <div class="form-group">
            <label for="optimizationPrompt">优化提示词内容</label>
            <textarea
              id="optimizationPrompt"
              v-model="formData.optimizationPrompt"
              rows="6"
              placeholder="请输入优化提示词内容，支持模板变量如 {user_requirements_and_plan}"
              required
            ></textarea>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="priority">优先级</label>
              <input
                id="priority"
                v-model.number="formData.priority"
                type="number"
                min="0"
                max="100"
                placeholder="0-100，数字越大优先级越高"
              />
            </div>
            <div class="form-group">
              <label for="displayOrder">显示顺序</label>
              <input
                id="displayOrder"
                v-model.number="formData.displayOrder"
                type="number"
                min="0"
                placeholder="显示顺序，数字越小越靠前"
              />
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="closeDialog">取消</button>
            <button type="submit" class="save-btn">保存配置</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 优先级设置对话框 -->
    <div v-if="showPriorityDialog" class="dialog-overlay" @click="closePriorityDialog">
      <div class="dialog-content" @click.stop>
        <div class="dialog-header">
          <h3>设置优先级</h3>
          <button class="close-btn" @click="closePriorityDialog">×</button>
        </div>
        <form @submit.prevent="updatePriority" class="priority-form">
          <div class="form-group">
            <label for="priorityValue">优先级 (0-100)</label>
            <input
              id="priorityValue"
              v-model.number="priorityForm.priority"
              type="number"
              min="0"
              max="100"
              placeholder="数字越大优先级越高"
              required
            />
            <p class="form-hint">优先级越高，该配置在多个配置中的执行顺序越靠前</p>
          </div>
          <div class="form-group">
            <label for="displayOrderValue">显示顺序</label>
            <input
              id="displayOrderValue"
              v-model.number="priorityForm.displayOrder"
              type="number"
              min="0"
              placeholder="数字越小越靠前"
            />
            <p class="form-hint">控制配置在列表中的显示顺序</p>
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="closePriorityDialog">取消</button>
            <button type="submit" class="save-btn">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PromptOptimizationConfig',
  props: {
    promptType: {
      type: String,
      default: 'report-generator'
    },
    agentPrompt: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      optimizationConfigs: [],
      showAddConfigDialog: false,
      editingConfig: null,
      showBatchActions: false,
      showPriorityDialog: false,
      selectedConfigs: [],
      editingPriorityConfig: null,
      formData: {
        name: '',
        description: '',
        optimizationPrompt: '',
        priority: 0,
        displayOrder: 0
      },
      priorityForm: {
        priority: 0,
        displayOrder: 0
      },
      loading: false,
      message: {
        show: false,
        text: '',
        type: 'success'
      }
    }
  },
  computed: {
    // 是否全选
    isAllSelected() {
      return this.optimizationConfigs.length > 0 && 
             this.selectedConfigs.length === this.optimizationConfigs.length
    },
    // 是否部分选择（半选状态）
    isIndeterminate() {
      return this.selectedConfigs.length > 0 && 
             this.selectedConfigs.length < this.optimizationConfigs.length
    }
  },
  mounted() {
    this.loadOptimizationConfigs()
  },
  methods: {
    async loadOptimizationConfigs() {
      try {
        this.loading = true
        const response = await fetch(`/api/prompt-config/list-by-type/${this.promptType}`)
        const result = await response.json()
        if (result.success) {
          this.optimizationConfigs = result.data || []
          // 如果配置列表为空，自动关闭批量操作面板
          if (this.optimizationConfigs.length === 0) {
            this.showBatchActions = false
            this.selectedConfigs = []
          }
        }
      } catch (error) {
        console.error('加载优化配置失败:', error)
        this.showMessage('加载优化配置失败', 'error')
      } finally {
        this.loading = false
      }
    },

    async saveConfig() {
      try {
        const configData = {
          ...this.formData,
          promptType: this.promptType,
          enabled: true,
          creator: 'user',
          priority: this.formData.priority || 0,
          displayOrder: this.formData.displayOrder || 0
        }

        if (this.editingConfig) {
          configData.id = this.editingConfig.id
        }

        const response = await fetch('/api/prompt-config/save', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(configData)
        })

        const result = await response.json()
        if (result.success) {
          this.showMessage(result.message || '保存成功', 'success')
          this.closeDialog()
          this.loadOptimizationConfigs()
        } else {
          this.showMessage(result.message || '保存失败', 'error')
        }
      } catch (error) {
        console.error('保存配置失败:', error)
        this.showMessage('保存配置失败', 'error')
      }
    },

    async toggleConfig(config) {
      try {
        const url = config.enabled 
          ? `/api/prompt-config/${config.id}/disable`
          : `/api/prompt-config/${config.id}/enable`
        
        const response = await fetch(url, { method: 'POST' })
        const result = await response.json()
        
        if (result.success) {
          this.showMessage(result.message, 'success')
          this.loadOptimizationConfigs()
        } else {
          this.showMessage(result.message, 'error')
        }
      } catch (error) {
        console.error('切换配置状态失败:', error)
        this.showMessage('操作失败', 'error')
      }
    },

    async deleteConfig(configId) {
      if (!confirm('确定要删除这个优化配置吗？')) {
        return
      }

      try {
        const response = await fetch(`/api/prompt-config/${configId}`, {
          method: 'DELETE'
        })
        const result = await response.json()
        
        if (result.success) {
          this.showMessage(result.message, 'success')
          this.loadOptimizationConfigs()
          // 删除后从选中列表中移除
          this.selectedConfigs = this.selectedConfigs.filter(id => id !== configId)
        } else {
          this.showMessage(result.message, 'error')
        }
      } catch (error) {
        console.error('删除配置失败:', error)
        this.showMessage('删除配置失败', 'error')
      }
    },

    editConfig(config) {
      this.editingConfig = config
      this.formData = {
        name: config.name,
        description: config.description,
        optimizationPrompt: config.optimizationPrompt,
        priority: config.priority || 0,
        displayOrder: config.displayOrder || 0
      }
    },

    closeDialog() {
      this.showAddConfigDialog = false
      this.editingConfig = null
      this.formData = {
        name: '',
        description: '',
        optimizationPrompt: '',
        priority: 0,
        displayOrder: 0
      }
    },

    // 批量操作相关方法
    async batchEnable() {
      if (this.selectedConfigs.length === 0) return
      
      try {
        const response = await fetch('/api/prompt-config/batch-enable', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.selectedConfigs)
        })
        
        const result = await response.json()
        if (result.success) {
          this.showMessage(result.message, 'success')
          this.loadOptimizationConfigs()
          this.clearSelection()
        } else {
          this.showMessage(result.message, 'error')
        }
      } catch (error) {
        console.error('批量启用失败:', error)
        this.showMessage('批量启用失败', 'error')
      }
    },

    async batchDisable() {
      if (this.selectedConfigs.length === 0) return
      
      try {
        const response = await fetch('/api/prompt-config/batch-disable', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.selectedConfigs)
        })
        
        const result = await response.json()
        if (result.success) {
          this.showMessage(result.message, 'success')
          this.loadOptimizationConfigs()
          this.clearSelection()
        } else {
          this.showMessage(result.message, 'error')
        }
      } catch (error) {
        console.error('批量禁用失败:', error)
        this.showMessage('批量禁用失败', 'error')
      }
    },

    clearSelection() {
      this.selectedConfigs = []
      this.showBatchActions = false
    },

    // 全选/取消全选
    toggleSelectAll() {
      if (this.isAllSelected) {
        // 如果已全选，则取消全选
        this.selectedConfigs = []
      } else {
        // 如果未全选，则全选所有配置
        this.selectedConfigs = this.optimizationConfigs.map(config => config.id)
      }
    },

    // 优先级相关方法
    showPriorityDialog(config) {
      this.editingPriorityConfig = config
      this.priorityForm = {
        priority: config.priority || 0,
        displayOrder: config.displayOrder || 0
      }
      this.showPriorityDialog = true
    },

    async updatePriority() {
      try {
        const response = await fetch(`/api/prompt-config/${this.editingPriorityConfig.id}/priority`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ priority: this.priorityForm.priority })
        })
        
        const result = await response.json()
        if (result.success) {
          this.showMessage('优先级更新成功', 'success')
          this.loadOptimizationConfigs()
          this.closePriorityDialog()
        } else {
          this.showMessage(result.message, 'error')
        }
      } catch (error) {
        console.error('更新优先级失败:', error)
        this.showMessage('更新优先级失败', 'error')
      }
    },

    closePriorityDialog() {
      this.showPriorityDialog = false
      this.editingPriorityConfig = null
      this.priorityForm = {
        priority: 0,
        displayOrder: 0
      }
    },



    showMessage(text, type = 'success') {
      this.message = {
        show: true,
        text,
        type
      }
      setTimeout(() => {
        this.message.show = false
      }, 3000)
    },

    hideMessage() {
      this.message.show = false
    }
  }
}
</script>

<style scoped>
.prompt-optimization-config {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 消息提示样式 */
.message-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 16px;
  border-radius: 6px;
  color: white;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 1000;
  max-width: 300px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.message-toast.success {
  background: #52c41a;
}

.message-toast.error {
  background: #ff4d4f;
}

.message-close {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 16px;
  padding: 0;
  margin-left: auto;
}

.config-header {
  margin-bottom: 24px;
}

.config-header h3 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

.config-description {
  color: #666;
  margin-bottom: 12px;
  line-height: 1.5;
}

.optimization-tips {
  margin: 0;
  padding-left: 20px;
  color: #888;
}

.optimization-tips li {
  margin-bottom: 4px;
}

.agent-prompt-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.agent-prompt-section h4 {
  margin: 0 0 12px 0;
  color: #333;
  font-size: 14px;
  font-weight: 600;
}

.prompt-display {
  color: #555;
  line-height: 1.5;
  font-size: 14px;
}

.optimization-configs {
  border-top: 1px solid #eee;
  padding-top: 24px;
}

.config-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.config-list-header h4 {
  margin: 0;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.batch-action-btn,
.add-config-btn {
  padding: 8px 16px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.batch-action-btn:hover,
.add-config-btn:hover {
  background: #40a9ff;
}

.batch-action-btn {
  background: #52c41a;
}

.batch-action-btn:hover {
  background: #73d13d;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.config-list {
  space-y: 12px;
}

.config-item {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 12px;
}

.config-item.disabled {
  opacity: 0.6;
  background: #fafafa;
}

.config-item.selected {
  border-color: #1890ff;
  background: #f0f8ff;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.config-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.config-checkbox {
  margin-right: 8px;
}

.config-name {
  font-weight: 600;
  color: #333;
}

.config-priority {
  font-size: 12px;
  color: #666;
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
}



.config-actions {
  display: flex;
  gap: 8px;
}

.toggle-btn,
.edit-btn,
.priority-btn,
.delete-btn {
  padding: 4px 8px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.toggle-btn.active {
  background: #52c41a;
  color: white;
  border-color: #52c41a;
}

.edit-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.priority-btn:hover {
  border-color: #fa8c16;
  color: #fa8c16;
}

.delete-btn:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.config-content {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.config-description {
  margin-bottom: 8px;
  color: #666;
  font-size: 14px;
}

.optimization-prompt {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 4px;
  color: #333;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 对话框样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 20px 0 20px;
  border-bottom: 1px solid #eee;
}

.dialog-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #333;
}

.config-form {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 4px;
  color: #333;
  font-weight: 500;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.cancel-btn,
.save-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn {
  background: white;
  color: #666;
}

.cancel-btn:hover {
  border-color: #999;
}

.save-btn {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.save-btn:hover {
  background: #40a9ff;
}

/* 批量操作面板样式 */
.batch-actions-panel {
  background: #f8f9fa;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 16px;
}

.batch-actions-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.batch-selection {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-info {
  color: #666;
  font-size: 14px;
}

.select-all-checkbox {
  margin-right: 4px;
  cursor: pointer;
}

.batch-buttons {
  display: flex;
  gap: 8px;
}

.batch-btn {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  background: white;
}

.batch-btn.enable {
  color: #52c41a;
  border-color: #52c41a;
}

.batch-btn.enable:hover {
  background: #52c41a;
  color: white;
}

.batch-btn.disable {
  color: #ff4d4f;
  border-color: #ff4d4f;
}

.batch-btn.disable:hover {
  background: #ff4d4f;
  color: white;
}

.batch-btn.cancel {
  color: #666;
  border-color: #ddd;
}

.batch-btn.cancel:hover {
  background: #f5f5f5;
}

.batch-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 表单行样式 */
.form-row {
  display: flex;
  gap: 16px;
}

.form-row .form-group {
  flex: 1;
}

.form-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  margin-bottom: 0;
}
</style>
