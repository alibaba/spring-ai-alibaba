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
      <h2>Tools/MCP配置</h2>
      <div class="panel-actions">
        <button class="action-btn" @click="showToolModal()">
          <Icon icon="carbon:add" />
          新建工具
        </button>
      </div>
    </div>

    <div class="tools-grid">
      <div v-for="tool in tools" :key="tool.id" class="tool-card">
        <div class="tool-header">
          <span class="tool-name">{{ tool.name }}</span>
          <div class="tool-actions">
            <Switch
              update:switchValue="handleSwitchChange(tool, $event)"
              :enabled="tool.enabled"
              :label="tool.enabled ? '已启用' : '已禁用'"
              @update:switchValue="handleSwitchChange(tool, $event)"
            />
            <button class="edit-btn" @click="showToolModal(tool)">
              <Icon icon="carbon:edit" />
              编辑
            </button>
          </div>
        </div>
        <p class="tool-desc">{{ tool.description }}</p>
      </div>
    </div>

    <!-- 工具配置弹窗 -->
    <Modal v-model="showModal" :title="isEdit ? '编辑工具' : '新建工具'" @confirm="handleSaveTool">
      <div class="modal-form">
        <div class="form-item">
          <label>工具名称</label>
          <input type="text" v-model="editingTool.name" placeholder="输入工具名称" />
        </div>

        <div class="form-item">
          <label>描述</label>
          <textarea
            v-model="editingTool.description"
            rows="3"
            placeholder="描述这个工具的功能和用途"
          ></textarea>
        </div>
        <div class="form-item">
          <label>连接类型</label>
          <Flex gap="12px" align="baseline">
            <Flex align="baseline" gap="4px">
              <input
                type="radio"
                id="studio"
                name="connectionType"
                value="studio"
                class="radio-input"
              />
              <label for="studio">Studio</label>
            </Flex>
            <Flex align="baseline" gap="4px">
              <input type="radio" id="sse" name="connectionType" value="sse" />
              <label for="sse">SSE</label>
            </Flex>
          </Flex>
        </div>

        <div class="form-item">
          <label>MCP配置</label>
          <textarea v-model="editingTool.config" rows="6" placeholder="输入MCP配置内容"></textarea>
        </div>
        <div class="form-item">
          <Switch
            style="font-size: 12px"
            :enabled="editingTool.enabled"
            :label="editingTool.enabled ? '已启用' : '已禁用'"
          />
        </div>
      </div>

      <template #footer>
        <Flex justify="space-between" gap="8px" class="footer-buttons">
          <button v-if="isEdit" class="footer-btn delete-btn" @click="showDeleteConfirm = true">
            <Icon icon="carbon:trash-can" />
            删除
          </button>
          <Flex gap="8px">
            <button class="footer-btn cancel-btn" @click="showModal = false">取消</button>
            <button class="footer-btn save-btn" @click="handleSaveTool">
              <Icon icon="carbon:save" />
              保存
            </button>
          </Flex>
        </Flex>
        <!-- 编辑模式下显示删除按钮 -->
      </template>
    </Modal>

    <!-- 删除确认弹窗 -->
    <Modal v-model="showDeleteConfirm" title="删除确认" @confirm="handleDeleteTool">
      <div class="delete-confirm">
        <p>确定要删除 {{ editingTool.name }} 吗？此操作不可恢复。</p>
      </div>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Icon } from '@iconify/vue'
import Modal from '@/components/modal/index.vue'
import Flex from '@/components/flex/index.vue'
import Switch from '@/components/switch/index.vue'

interface Tool {
  id: number
  name: string
  description: string
  connectionType: string
  config: string
  enabled: boolean
}

const tools = reactive<Tool[]>([
  {
    id: 1,
    name: '文件读取',
    description: '读取本地或远程文件内容',
    connectionType: 'Studio',
    config: 'file_reader:\n  type: local\n  permissions: ["read"]',
    enabled: true,
  },
  {
    id: 2,
    name: '命令执行',
    description: '执行系统命令',
    connectionType: 'Studio',
    config: 'command_executor:\n  shell: bash\n  timeout: 30s',
    enabled: true,
  },
  {
    id: 3,
    name: '网络请求',
    description: '发送HTTP请求',
    connectionType: 'Studio',
    config: 'network_request:\n  method: GET\n  url: https://example.com',
    enabled: true,
  },
  {
    id: 4,
    name: '文本生成',
    description: '基于提示词生成文本',
    connectionType: 'Studio',
    config: 'text_generator:\n  model: gpt-3.5-turbo\n  prompt: "Write a story"',
    enabled: true,
  },
  {
    id: 5,
    name: '代码分析',
    description: '分析和优化代码',
    connectionType: 'Studio',
    config: 'code_analysis:\n  language: python\n  file: code.py',
    enabled: true,
  },
])

const showModal = ref(false)
const showDeleteConfirm = ref(false)
const isEdit = ref(false)

const defaultTool: Tool = {
  id: 0,
  name: '',
  description: '',
  connectionType: '',
  config: '',
  enabled: true,
}

const editingTool = reactive<Tool>({ ...defaultTool })

// 显示工具配置弹窗
const showToolModal = (tool?: Tool) => {
  isEdit.value = !!tool
  if (tool) {
    Object.assign(editingTool, tool)
  } else {
    Object.assign(editingTool, defaultTool)
  }
  showModal.value = true
}

// 保存工具
const handleSaveTool = () => {
  if (isEdit.value) {
    // 更新现有工具
    const index = tools.findIndex(t => t.id === editingTool.id)
    if (index !== -1) {
      Object.assign(tools[index], editingTool)
    }
  } else {
    // 创建新工具
    const newTool: Tool = {
      ...editingTool,
      id: Date.now(),
    }
    tools.push(newTool)
  }
  showModal.value = false
}

// 删除工具
const handleDeleteTool = () => {
  const index = tools.findIndex(t => t.id === editingTool.id)
  if (index !== -1) {
    tools.splice(index, 1)
  }
  showDeleteConfirm.value = false
  showModal.value = false
}

// 开关切换
const handleSwitchChange = (tool: Tool, value: boolean) => {
  tool.enabled = value
}
</script>

<style scoped>
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.tool-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 20px;
}

.tool-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tool-name {
  font-weight: 500;
  font-size: 16px;
}

.tool-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.tool-desc {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  margin-bottom: 16px;
  line-height: 1.5;
}

.tool-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.edit-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.3s;
  &:hover {
    background: rgba(255, 255, 255, 0.1);
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
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  margin-bottom: 16px;
  & label {
    display: block;
    margin-bottom: 8px;
    color: rgba(255, 255, 255, 0.8);
  }

  & input[type='text'],
  & textarea {
    width: 100%;
    padding: 8px 12px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    color: #fff;
    transition: all 0.3s;
  }

  & input[type='text']:focus,
  & textarea:focus {
    border-color: #667eea;
    outline: none;
  }
}

.delete-confirm {
  text-align: center;
  padding: 20px 0;
}

.delete-confirm p {
  color: rgba(255, 255, 255, 0.8);
}

.footer-buttons {
  width: 100%;
  .footer-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px;
    cursor: pointer;
    transition: all 0.3s;
    border-radius: 6px;
  }

  .cancel-btn {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    color: #fff;
    cursor: pointer;
  }

  .save-btn {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    color: #fff;
    cursor: pointer;
  }

  .delete-btn {
    background: rgba(234, 102, 102, 0.1);
    border: 1px solid rgba(234, 102, 102, 0.2);
    color: #ea6666;
    &:hover {
      background: rgba(234, 102, 102, 0.2);
    }
  }
}
</style>
