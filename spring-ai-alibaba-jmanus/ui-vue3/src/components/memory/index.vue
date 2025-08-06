<template>
  <div class="app-container" :class="{ 'memory-wrapper-collapsed': memoryStore.isCollapsed }">
    <!-- 标题栏 -->
    <div class="header">
      <div class="relative">
        <div class="title-edit-group">
          <h1 id="main-title" class="main-title">
            <span>{{ title }}</span>
          </h1>
        </div>

        <!-- 标题编辑框 -->
        <div
            id="title-edit-container"
            class="title-edit-container"
            v-if="showTitleEdit"
        >
        </div>
      </div>
    </div>

    <!-- 搜索框 -->
    <div class="search-bar">
      <div class="search-container">
        <input
            type="text"
            placeholder="搜索消息..."
            class="search-input"
            v-model="searchQuery"
            @input="handleSearch"
        >
        <i class="fa fa-search search-icon"></i>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" id="message-list">
      <!-- 消息项列表 -->
      <div>
        <div
            class="message-item"
            v-for="message in filteredMessages"
            :key="message.memoryId"
            :class="{ 'expanded': message.expanded }"
        >
          <!-- 消息头部 -->
          <div class="message-header" @click="selectMemory(message.memoryId)">
            <!-- 消息内容 -->
            <div class="message-content">
              <div class="sender-info">
                <div style="display: flex; align-items: center;">
                  <h3
                      class="sender-name"
                      @click.stop="showNameEditModal(message.memoryId, message.memoryName)"
                  >
                    {{ message.memoryName }}
                  </h3>
                  <span
                      class="edit-indicator"
                      title="点击修改名称"
                      @click.stop="showNameEditModal(message.memoryId, message.memoryName)"
                  ></span>
                </div>
              </div>

              <!-- 消息预览 -->
              <div class="message-preview">
                <p class="preview-line">
                  {{ message.messages.length > 0 ? message.messages[0].text : 'none message' }}
                </p>
                <p
                    class="preview-line"
                    style="opacity: 0.8;"
                    v-if="message.messages.length > 1"
                >
                  {{ message.messages[1].text }}
                </p>
              </div>

              <!-- ID信息和时间 -->
              <div class="message-meta">
                <span class="message-id">ID: {{ message.memoryId }}</span>
                <div class="meta-right">
                  <span class="unread-count" v-if="message.messages.length > 0">
                    {{ message.messages.length }}条消息
                  </span>
                  <span class="message-time">{{ formatTimestamp(message.createTime) }}</span>
                </div>
              </div>
            </div>

            <!-- 展开/收起按钮 -->
            <div class="toggle-container" @click.stop="toggleMessage(message.memoryId)">
              <Icon
                  :id="'toggle-' + message.memoryId"
                  icon="carbon:close"
              >
              </Icon>
            </div>

            <!-- 操作按钮 -->
            <div class="action-buttons">
              <button
                  class="delete-btn"
                  @click.stop="showDeleteConfirm(message.memoryId)"
              >
                <Icon icon="carbon:close"></Icon>
              </button>
            </div>
          </div>

          <!-- 展开的消息内容 -->
          <div
              :id="'content-' + message.memoryId"
              class="expanded-content"
              v-if="message.expanded"
          >
            <div style="display: flex; flex-direction: column; gap: 0.75rem;">
              <div class="message-bubble" v-for="(bubble, idx) in message.messages" :key="idx">
                <div class="bubble-avatar">
                  {{ bubble.messageType }}
                </div>
                <div class="bubble-content">
                  <p class="bubble-text">{{ bubble.text }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 搜索无结果提示 -->
      <div v-if="filteredMessages.length === 0 && searchQuery" class="empty-state">
        <p class="state-text">none message</p>
      </div>
    </div>

    <!-- 名称编辑弹窗 -->
    <div
        id="name-edit-modal"
        class="modal-overlay"
        v-if="showNameModal"
        @click.self="closeNameModal"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">修改发送者名称</h3>
          <input
              type="text"
              v-model="nameInput"
              class="edit-input"
              placeholder="输入新名称..."
              maxlength="100"
          >
          <span id="name-char-count" class="char-count" style="text-align: right; display: block; margin-top: 0.25rem;">
            {{ nameInput.length }}/100
          </span>
        </div>

        <div class="modal-footer">
          <button
              id="cancel-name"
              class="modal-btn cancel-btn"
              @click="closeNameModal"
          >
            取消
          </button>
          <button
              id="save-name"
              class="modal-btn confirm-btn"
              @click="saveName"
          >
            保存
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <div
        id="delete-modal"
        class="modal-overlay"
        v-if="showDeleteModal"
        @click.self="closeDeleteModal"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">确认删除</h3>
          <p class="state-text" id="delete-message">
            你确定要删除ID为 {{ currentDeleteId }} 的消息吗？此操作不可撤销。
          </p>
        </div>

        <div class="modal-footer">
          <button
              id="cancel-delete"
              class="modal-btn cancel-btn"
              @click="closeDeleteModal"
          >
            取消
          </button>
          <button
              id="confirm-delete"
              class="modal-btn delete-btn-confirm"
              @click="confirmDelete"
          >
            确认删除
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {MemoryApiService} from '@/api/memory-api-service'
import type { Message} from '@/api/memory-api-service'
import {Icon} from '@iconify/vue'
import {memoryStore} from "@/stores/memory";

// 状态管理
const title = ref('记忆列表');
const showTitleEdit = ref(false);
const searchQuery = ref('');
const messages = ref<Message[]>([]);
const filteredMessages = ref<Message[]>([]);
const showToast = ref(false);

// 名称编辑相关
const showNameModal = ref(false);
const currentEditMessageId = ref<string | null>(null);
const nameInput = ref('');

// 删除相关
const showDeleteModal = ref(false);
const currentDeleteId = ref<string | null>(null);

// 初始化数据
onMounted(() => {
  // 从本地存储加载标题
  const savedTitle = localStorage.getItem('messageListTitle');
  if (savedTitle) {
    title.value = savedTitle;
  }

  memoryStore.setLoadMessages(loadMessages)
});

// 加载消息数据
const loadMessages = async () => {
  try {
    const mes = await MemoryApiService.getMemories()
    if(messages.value){
      // 创建一个以 memoryId 为键的映射表，存储已有的 expanded 状态
      const expandedMap = new Map(
          messages.value.map(msg => [msg.memoryId, msg.expanded])
      );
      // 同步状态并更新 messages
      messages.value = mes.map((mesMsg: Message) => ({
        ...mesMsg,
        // 如果存在对应的状态则使用，否则保持原有值或设为默认值
        expanded: expandedMap.has(mesMsg.memoryId)
            ? expandedMap.get(mesMsg.memoryId)
            : mesMsg.expanded // 或设置默认值如 false
      }));
    } else {
      // 为每条消息添加expanded属性，默认为false
      messages.value = mes.map((msg: Message) => ({...msg, expanded: false}));
    }
    // 初始化过滤结果
    filteredMessages.value = [...messages.value];
    handleSearch()
  } catch (e) {
    console.error('加载消息失败:', e);
    messages.value = [];
    filteredMessages.value = [];
  }
};

const selectMemory = (memoryId: string) => {
  memoryStore.selectMemory(memoryId);
};

// 格式化时间戳
const formatTimestamp = (timestamp: number | string): string => {
  // 处理字符串类型的时间戳
  const timestampNum = typeof timestamp === 'string' ? parseInt(timestamp, 10) : timestamp;

  // 检查时间戳是否有效
  if (isNaN(timestampNum) || timestampNum <= 0) {
    return '未知时间';
  }

  // 处理毫秒级时间戳（如果是13位）
  const date = new Date(
      timestampNum.toString().length === 13
          ? timestampNum
          : timestampNum * 1000
  );

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).replace(',', ' ');
};

// 搜索功能
const handleSearch = () => {
  const query = searchQuery.value.toLowerCase().trim();

  if (!query) {
    filteredMessages.value = [...messages.value];
    return;
  }

  filteredMessages.value = messages.value.filter(message => {
    // 搜索发送者名称
    const matchesName = message.memoryName.toLowerCase().includes(query);

    // 搜索消息ID
    const matchesId = message.memoryId.toLowerCase().includes(query);

    // 搜索消息内容
    const matchesContent = message.messages.some(bubble =>
        bubble.text.toLowerCase().includes(query)
    );

    return matchesName || matchesId || matchesContent;
  });
};

// 名称编辑功能
const showNameEditModal = (messageId: string, currentName: string) => {
  currentEditMessageId.value = messageId;
  nameInput.value = currentName;
  showNameModal.value = true;
};

const closeNameModal = () => {
  showNameModal.value = false;
  currentEditMessageId.value = null;
};

const saveName = async () => {
  if (!currentEditMessageId.value) return;

  const newName = nameInput.value.trim() || 'unknow name';

  // 更新消息列表中的名称
  const messageIndex = messages.value.findIndex(
      msg => msg.memoryId === currentEditMessageId.value
  );

  if (messageIndex !== -1) {
    const currentMessage = messages.value[messageIndex]
    currentMessage.memoryName = newName;
    currentMessage.messages = []
    try {
      const returnMemory = await MemoryApiService.update(currentMessage)
      if (!returnMemory.messages) {
        returnMemory.messages = [];
      }
      // 保留expanded状态
      messages.value[messageIndex] = {...returnMemory, expanded: currentMessage.expanded};
      // 更新过滤列表
      handleSearch();
      showNameModal.value = false;
      showSuccessToast();
    } catch (error) {
      console.error('error:', error);
    }
  }
};

// 切换消息展开/收起
const toggleMessage = (id: string) => {
  const message = messages.value.find(msg => msg.memoryId === id);
  if (message) {
    message.expanded = !message.expanded;
    // 更新过滤列表中的对应项
    const filteredIndex = filteredMessages.value.findIndex(msg => msg.memoryId === id);
    if (filteredIndex !== -1) {
      filteredMessages.value[filteredIndex] = {...message};
    }
  }
};

// 删除功能
const showDeleteConfirm = (id: string) => {
  currentDeleteId.value = id;
  showDeleteModal.value = true;
};

const closeDeleteModal = () => {
  showDeleteModal.value = false;
  currentDeleteId.value = null;
};

const confirmDelete = async () => {
  if (!currentDeleteId.value) return;

  try {
    // 调用API删除
    await MemoryApiService.delete(currentDeleteId.value);
    // 从列表中移除消息
    messages.value = messages.value.filter(msg => msg.memoryId !== currentDeleteId.value);
    // 更新过滤列表
    handleSearch();
    if(messages.value.length === 0){
      memoryStore.clearMemoryId()
    }
    showDeleteModal.value = false;
    currentDeleteId.value = null;
    showSuccessToast();
  } catch (error) {
    console.error('删除失败:', error);
  }
};

// 显示成功提示
const showSuccessToast = () => {
  showToast.value = true;
  setTimeout(() => {
    showToast.value = false;
  }, 2000);
};
</script>

<style>
/* 基础样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.app-container {
  position: relative;
  width: 600px;
  height: 100vh;
  background: rgba(255, 255, 255, 0.05);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease-in-out;
  overflow-y: auto;
}

.memory-wrapper-collapsed {
  border-right: none;
  width: 0;
}

/* 头部样式 */
.header {
  padding: 1rem;
  border-bottom: 1px solid #333333;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.main-title {
  font-size: 1.25rem;
  font-weight: bold;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.main-title i {
  margin-right: 0.5rem;
}

/* 搜索框样式 */
.search-bar {
  padding: 1rem;
  border-bottom: 1px solid #333333;
}

.search-container {
  position: relative;
}

.search-input {
  width: 100%;
  background-color: #2d2d2d;
  border: 1px solid #333333;
  border-radius: 0.5rem;
  padding: 0.5rem 0.5rem 0.5rem 2.5rem;
  color: #ffffff;
  font-size: 0.875rem;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: #888888;
}

/* 消息列表样式 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem;
}

.message-item {
  background-color: #2d2d2d;
  border: 1px solid #333333;
  border-radius: 0.5rem;
  overflow: hidden;
  margin-bottom: 0.5rem;
  transition: all 0.2s ease;
}

.message-item:hover {
  background-color: #333333;
  border-color: rgba(102, 126, 234, 0.5);
}

.message-header {
  padding: 0.75rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  width: 100%;
}

.message-content {
  flex: 1;
  min-width: 0;
  padding-right: 0.5rem;
}

.sender-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.25rem;
}

.sender-name {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.message-preview {
  margin-bottom: 0.25rem;
}

.preview-line {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 0.125rem;
}

.message-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-top: 0.25rem;
}

.meta-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.message-id {
  font-size: 0.75rem;
  color: #667eea;
}

.message-time {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
  white-space: nowrap;
}

.unread-count {
  font-size: 0.75rem;
  background-color: rgba(102, 126, 234, 0.2);
  color: #667eea;
  padding: 0.125rem 0.375rem;
  border-radius: 1rem;
}

.toggle-container {
  margin-left: 0.5rem;
  display: flex;
  align-items: center;
  min-width: 24px;
}

.toggle-icon {
  color: rgba(255, 255, 255, 0.7);
  transition: transform 0.3s ease;
}

.message-item.expanded .toggle-icon {
  transform: rotate(180deg);
}

.action-buttons {
  margin-left: 0.5rem;
  transition: opacity 0.2s ease;
  min-width: 24px;
}

.message-item:hover .action-buttons {
  opacity: 1;
}

.delete-btn {
  color: rgba(255, 255, 255, 0.7);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem;
  font-size: 1rem;
}

.delete-btn:hover {
  color: #ff6b6b;
}

/* 展开的消息内容 */
.expanded-content {
  background-color: #333333;
  border-top: 1px solid #444444;
  padding: 0.75rem;
  animation: fadeIn 0.3s ease-in-out;
}

.message-bubble {
  display: flex;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

.bubble-avatar {
  width: 5rem;
  height: 2rem;
  overflow: hidden;
  margin-right: 0.5rem;
  flex-shrink: 0;
}

.bubble-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bubble-content {
  background-color: #2d2d2d;
  padding: 0.5rem;
  border-radius: 0.5rem;
  max-width: 85%;
}

.bubble-text {
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

.state-text {
  color: rgba(255, 255, 255, 0.5);
}

/* 弹窗样式 */
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
  z-index: 50;
}

.modal-content {
  background-color: #2d2d2d;
  border: 1px solid #444444;
  border-radius: 0.5rem;
  width: 100%;
  max-width: 28rem;
  padding: 1.25rem;
  transition: all 0.3s ease;
}

.modal-header {
  text-align: center;
  margin-bottom: 1rem;
}

.modal-title {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
}

.modal-btn {
  flex: 1;
  padding: 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.cancel-btn {
  background-color: #333333;
  border: 1px solid #444444;
  color: rgba(255, 255, 255, 0.9);
}

.cancel-btn:hover {
  background-color: #444444;
}

.confirm-btn {
  background-color: #667eea;
  color: #ffffff;
}

.confirm-btn:hover {
  background-color: rgba(102, 126, 234, 0.9);
}

.delete-btn-confirm {
  background-color: #ff6b6b;
  color: #ffffff;
}

.delete-btn-confirm:hover {
  background-color: #ff5252;
}

.delete-icon-container {
  width: 4rem;
  height: 4rem;
  background-color: rgba(255, 107, 107, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
}

.delete-icon {
  color: #ff6b6b;
  font-size: 2rem;
}

/* 编辑框样式 */
.edit-input {
  width: 100%;
  background-color: #333333;
  border: 1px solid #444444;
  border-radius: 0.5rem;
  padding: 0.5rem 0.75rem;
  color: #ffffff;
  font-size: 0.875rem;
}

.edit-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 1px rgba(102, 126, 234, 0.3);
}

.char-count {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
}

/* 提示消息 */
.success-toast {
  position: fixed;
  top: 1rem;
  left: 50%;
  transform: translateX(-50%);
  background-color: #4ade80;
  color: #ffffff;
  padding: 0.5rem 1rem;
  border-radius: 0.25rem;
  font-size: 0.875rem;
  z-index: 50;
  display: none;
  animation: fadeInOut 2s ease-in-out;
}

.success-toast.show {
  display: block;
}

/* 编辑图标样式 */
.edit-indicator {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 50%;
  background-color: #667eea;
  color: white;
  font-size: 0.75rem;
  cursor: pointer;
  margin-left: 0.375rem;
}

.edit-indicator::after {
  content: '✎';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

/* 动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInOut {
  0% {
    opacity: 0;
    transform: translateY(-20px) translateX(-50%);
  }
  20% {
    opacity: 1;
    transform: translateY(0) translateX(-50%);
  }
  80% {
    opacity: 1;
    transform: translateY(0) translateX(-50%);
  }
  100% {
    opacity: 0;
    transform: translateY(-20px) translateX(-50%);
  }
}

/* 响应式调整 */
@media (max-width: 640px) {
  .main-title {
    font-size: 1.125rem;
  }

  .message-id {
    display: none;
  }

  .message-time {
    font-size: 0.7rem;
  }
}
</style>
