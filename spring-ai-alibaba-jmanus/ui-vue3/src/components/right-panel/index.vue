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
  <div class="right-panel">
    <div class="preview-header">
      <div class="preview-tabs">
        <button
          v-for="tab in previewTabs"
          :key="tab.id"
          class="tab-button"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <Icon :icon="tab.icon" />
          {{ tab.name }}
        </button>
      </div>
      <div class="preview-actions">
        <button class="action-button" @click="copyCode" v-if="activeTab === 'code'">
          <Icon icon="carbon:copy" />
        </button>
        <button class="action-button" @click="downloadCode" v-if="activeTab === 'code'">
          <Icon icon="carbon:download" />
        </button>
      </div>
    </div>

    <div class="preview-content">
      <!-- Code Preview -->
      <div v-if="activeTab === 'code'" class="code-preview">
        <MonacoEditor
          v-model="codeContent"
          :language="codeLanguage"
          :theme="'vs-dark'"
          :height="'100%'"
          :readonly="true"
          :editor-options="{
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            wordWrap: 'on',
          }"
        />
      </div>

      <!-- Chat Preview -->
      <div v-else-if="activeTab === 'chat'" class="chat-preview">
        <div class="chat-bubbles">
          <div
            v-for="bubble in chatBubbles"
            :key="bubble.id"
            class="chat-bubble"
            :class="bubble.type"
          >
            <div class="bubble-header">
              <Icon :icon="bubble.icon" />
              <span>{{ bubble.title }}</span>
              <span class="timestamp">{{ bubble.timestamp }}</span>
            </div>
            <div class="bubble-content">
              {{ bubble.content }}
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="empty-preview">
        <Icon icon="carbon:document" class="empty-icon" />
        <h3>No preview available</h3>
        <p>Start a conversation to see the generated content here.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Icon } from '@iconify/vue'
import MonacoEditor from '@/components/editor/index.vue'

const activeTab = ref('chat')

const previewTabs = [
  { id: 'chat', name: 'Chat', icon: 'carbon:chat' },
  { id: 'code', name: 'Code', icon: 'carbon:code' },
]

const codeContent = ref(`// Generated Spring Boot REST API
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        user.setId(id);
        User updatedUser = userService.save(user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}`)

const codeLanguage = ref('java')

const chatBubbles = ref([
  {
    id: '1',
    type: 'thinking',
    icon: 'carbon:thinking',
    title: '分析需求',
    content:
      '将您的请求分解为可操作的步骤：1) 创建用户实体，2) 实现用户服务，3) 构建 REST 端点，4) 添加验证和错误处理。',
    timestamp: '2 分钟前',
  },
  {
    id: '2',
    type: 'progress',
    icon: 'carbon:in-progress',
    title: '生成代码',
    content:
      '创建具有用户管理 CRUD 操作的 Spring Boot REST API。包括正确的 HTTP 状态代码和错误处理。',
    timestamp: '1 分钟前',
  },
  {
    id: '3',
    type: 'success',
    icon: 'carbon:checkmark',
    title: '代码已生成',
    content:
      '成功生成具有所有 CRUD 操作的 UserController。代码包含正确的 REST 约定、错误处理，并遵循 Spring Boot 最佳实践。',
    timestamp: '刚刚',
  },
])

const copyCode = () => {
  navigator.clipboard.writeText(codeContent.value)
}

const downloadCode = () => {
  const blob = new Blob([codeContent.value], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'UserController.java'
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<style lang="less" scoped>
.right-panel {
  width: 50%;
  display: flex;
  flex-direction: column;
}

.preview-header {
  padding: 20px 24px;
  border-bottom: 1px solid #1a1a1a;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.02);
}

.preview-tabs {
  display: flex;
  gap: 8px;
}

.tab-button {
  padding: 8px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #888888;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #ffffff;
  }

  &.active {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
    border-color: #667eea;
    color: #667eea;
  }
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.action-button {
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #888888;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #ffffff;
  }
}

.preview-content {
  flex: 1;
  overflow: hidden;
}

.code-preview {
  height: 100%;
}

.chat-preview {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

.chat-bubbles {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-bubble {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;

  &.thinking {
    border-left: 4px solid #f39c12;
  }

  &.progress {
    border-left: 4px solid #3498db;
  }

  &.success {
    border-left: 4px solid #27ae60;
  }
}

.bubble-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;

  .timestamp {
    margin-left: auto;
    font-size: 12px;
    color: #666666;
    font-weight: 400;
  }
}

.bubble-content {
  color: #cccccc;
  line-height: 1.5;
  font-size: 14px;
}

.empty-preview {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666666;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  h3 {
    margin: 0 0 8px 0;
    font-size: 18px;
    color: #888888;
  }

  p {
    margin: 0;
    font-size: 14px;
    text-align: center;
  }
}
</style>
