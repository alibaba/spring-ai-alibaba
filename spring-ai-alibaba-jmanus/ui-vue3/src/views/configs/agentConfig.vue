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
  <div class="config-panel">
    <div class="panel-header">
      <h2>Agent配置</h2>
      <div class="panel-actions">
        <button class="action-btn">
          <Icon icon="carbon:upload" />
          导入
        </button>
        <button class="action-btn">
          <Icon icon="carbon:download" />
          导出
        </button>
      </div>
    </div>

    <div class="agent-layout">
      <!-- Agent列表 -->
      <div class="agent-list">
        <div
          v-for="agent in agents"
          :key="agent.id"
          class="agent-card"
          :class="{ active: selectedAgent?.id === agent.id }"
          @click="selectedAgent = agent"
        >
          <div class="agent-card-header">
            <span class="agent-name">{{ agent.name }}</span>
            <Icon icon="carbon:chevron-right" />
          </div>
          <p class="agent-desc">{{ agent.description }}</p>
          <div class="agent-tools">
            <span v-for="tool in agent.tools" :key="tool" class="tool-tag">
              {{ tool }}
            </span>
          </div>
        </div>

        <button class="add-btn" @click="showAddAgentModal">
          <Icon icon="carbon:add" />
          新建Agent
        </button>
      </div>

      <!-- Agent详情 -->
      <div class="agent-detail" v-if="selectedAgent">
        <div class="form-item">
          <label>Agent名称</label>
          <input type="text" v-model="selectedAgent.name" />
        </div>
        <div class="form-item">
          <label>描述</label>
          <textarea v-model="selectedAgent.description" rows="3"></textarea>
        </div>
        <div class="form-item">
          <label>提示词配置</label>
          <textarea
            v-model="selectedAgent.prompt"
            rows="6"
            placeholder="设置Agent执行任务时的提示词"
          ></textarea>
        </div>

        <div class="form-item">
          <label>可用工具</label>
          <div class="tools-list">
            <div v-for="tool in selectedAgent.tools" :key="tool" class="tool-item">
              <span>{{ tool }}</span>
              <Icon icon="carbon:close" class="remove-tool" @click="removeTool(tool)" />
            </div>
          </div>
          <button class="add-btn" @click="handleAddTool">
            <Icon icon="carbon:add" />
            添加工具
          </button>
        </div>

        <div class="detail-actions">
          <Flex justify="space-between" style="width: 100%">
            <button class="action-btn primary" @click="handleSave">
              <Icon icon="carbon:save" />
              保存
            </button>
            <button class="action-btn danger" @click="showDeleteConfirm">
              <Icon icon="carbon:trash-can" />
              删除
            </button>
          </Flex>
        </div>
      </div>
    </div>

    <!-- 新建Agent弹窗 -->
    <Modal v-model="showModal" title="新建Agent" @confirm="handleAddAgent">
      <div class="modal-form">
        <div class="form-item">
          <label>Agent名称</label>
          <input type="text" v-model="newAgent.name" placeholder="输入Agent名称" />
        </div>
        <div class="form-item">
          <label>描述</label>
          <textarea
            v-model="newAgent.description"
            rows="3"
            placeholder="描述这个Agent的功能和用途"
          ></textarea>
        </div>
        <div class="form-item">
          <label>提示词配置</label>
          <textarea
            v-model="newAgent.prompt"
            rows="6"
            placeholder="设置Agent执行任务时的提示词"
          ></textarea>
        </div>
      </div>
    </Modal>

    <!-- 删除确认弹窗 -->
    <Modal v-model="showDeleteModal" title="删除确认">
      <div class="delete-confirm">
        <p>确定要删除 {{ selectedAgent?.name }} 吗？此操作不可恢复。</p>
      </div>
      <template #footer>
        <button class="cancel-btn" @click="showDeleteModal = false">取消</button>
        <button class="confirm-btn danger" @click="handleDelete">删除</button>
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Icon } from '@iconify/vue'
import Modal from '@/components/modal/index.vue'
import Flex from '@/components/flex/index.vue'

interface Agent {
  id: number
  name: string
  description: string
  tools: string[]
  prompt: string
}

const agents = reactive<Agent[]>([
  {
    id: 1,
    name: '通用助手',
    description: '这是一个通用的智能助手，可以回答问题和执行多种任务...',
    tools: ['search', 'calculator', 'weather'],
    prompt: '',
  },
  {
    id: 2,
    name: '编程专家',
    description: '专注于解决编程相关问题的AI助手，熟悉多种编程语言和框架...',
    tools: ['code_analysis', 'git', 'docker'],
    prompt: '',
  },
])

const selectedAgent = ref<Agent | null>(agents?.[0])
const showModal = ref(false)
const showDeleteModal = ref(false)

const newAgent = reactive<Omit<Agent, 'id' | 'tools'>>({
  name: '',
  description: '',
  prompt: '',
})

// 显示新建Agent弹窗
const showAddAgentModal = () => {
  // 重置表单
  newAgent.name = ''
  newAgent.description = ''
  newAgent.prompt = ''
  showModal.value = true
}

// 显示删除确认弹窗
const showDeleteConfirm = () => {
  showDeleteModal.value = true
}

// 处理新建Agent
const handleAddAgent = () => {
  // 创建新Agent
  const agent: Agent = {
    id: Date.now(), // 使用时间戳作为临时ID
    name: newAgent.name,
    description: newAgent.description,
    prompt: newAgent.prompt,
    tools: [], // 初始化为空工具列表
  }

  // 添加到agents列表
  agents.push(agent)

  // 选中新创建的agent
  selectedAgent.value = agent

  // 关闭弹窗
  showModal.value = false
}

const handleAddTool = () => {
  // TODO: 实现添加工具的逻辑
}

const removeTool = (tool: string) => {
  if (selectedAgent.value) {
    selectedAgent.value.tools = selectedAgent.value.tools.filter(t => t !== tool)
  }
}

const handleSave = () => {
  // Mock: 保存成功提示
  console.log('Agent saved:', selectedAgent.value)
}

const handleDelete = () => {
  if (selectedAgent.value) {
    // 从列表中移除
    const index = agents.findIndex(a => a.id === selectedAgent.value!.id)
    if (index !== -1) {
      agents.splice(index, 1)
    }

    // 清除选中状态
    selectedAgent.value = null

    // 关闭确认弹窗
    showDeleteModal.value = false
  }
}
</script>

<style scoped>
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.panel-actions {
  display: flex;
  gap: 12px;
}

.agent-layout {
  display: flex;
  gap: 30px;
}

.agent-list {
  width: 300px;
}

.agent-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s;
  &:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  &.active {
    border-color: #667eea;
  }
}

.agent-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.agent-name {
  font-weight: 500;
}

.agent-desc {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  margin-bottom: 12px;
}

.tool-tag {
  display: inline-block;
  padding: 4px 8px;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 4px;
  font-size: 12px;
  margin-right: 8px;
  margin-bottom: 8px;
}

.agent-detail {
  flex: 1;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 24px;
}

.form-item {
  margin-bottom: 16px;
  label {
    display: block;
    margin-bottom: 8px;
    color: rgba(255, 255, 255, 0.8);
  }

  input,
  textarea {
    width: 100%;
    padding: 8px 12px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    color: #fff;
    transition: all 0.3s;
  }

  input:focus,
  textarea:focus {
    border-color: #667eea;
    outline: none;
  }
}

.tools-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tool-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
}

.remove-tool {
  cursor: pointer;
  opacity: 0.6;
  transition: all 0.3s;
  &:hover {
    opacity: 1;
  }
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px dashed rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.3s;
  &:hover {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.3);
  }
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
  &.primary {
    /* background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); */
    color: #ffffff;
    border-color: rgba(102, 126, 234, 0.2);
  }
  &.danger {
    background: rgba(234, 102, 102, 0.1);
    border: 1px solid rgba(234, 102, 102, 0.2);
    color: #ea6666;
    &:hover {
      background: rgba(234, 102, 102, 0.2);
    }
  }
}

.detail-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.delete-confirm {
  text-align: center;
  padding: 20px 0;

  p {
    color: rgba(255, 255, 255, 0.8);
  }
}

.confirm-btn {
  padding: 8px 16px;
  &.danger {
    background: rgba(255, 255, 255, 0.05);
    /* background: rgba(234, 102, 102, 0.2); */
    border-color: rgba(234, 102, 102, 0.2);
    color: #ea6666;
    border-radius: 6px;

    &:hover {
      background: rgba(234, 102, 102, 0.2);
    }
  }
}

.cancel-btn {
  padding: 8px 16px;
  /* background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); */
  color: #ffffff;
  border-radius: 6px;
  &:hover {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }
}
</style>
