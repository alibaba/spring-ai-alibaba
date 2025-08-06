<template>
  <div class="app-container" :class="{ 'memory-wrapper-collapsed': memoryStore.isCollapsed }">
    <div class="header">
      <div class="relative">
        <div class="title-edit-group">
          <h1 id="main-title" class="main-title">
            <span>{{ $t('memory.title') }}</span>
          </h1>
        </div>

        <div
            id="title-edit-container"
            class="title-edit-container"
            v-if="showTitleEdit"
        >
        </div>
      </div>
    </div>

    <div class="search-bar">
      <div class="search-container">
        <input
            type="text"
            :placeholder="$t('memory.searchPlaceholder')"
            class="search-input"
            v-model="searchQuery"
            @input="handleSearch"
        >
        <i class="fa fa-search search-icon"></i>
      </div>
    </div>

    <div class="message-list" id="message-list">
      <div>
        <div
            class="message-item"
            v-for="message in filteredMessages"
            :key="message.memoryId"
            :class="{ 'expanded': message.expanded }"
        >
          <div class="message-header" @click="selectMemory(message.memoryId)">
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
                      @click.stop="showNameEditModal(message.memoryId, message.memoryName)"
                  ></span>
                </div>
              </div>

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

              <div class="message-meta">
                <span class="message-id">ID: {{ message.memoryId }}</span>
                <div class="meta-right">
                  <span class="unread-count" v-if="message.messages.length > 0">
                    {{ message.messages.length }} {{$t('memory.size')}}
                  </span>
                  <span class="message-time">{{ formatTimestamp(message.createTime) }}</span>
                </div>
              </div>
            </div>

            <div class="toggle-container" @click.stop="toggleMessage(message.memoryId)">
              <Icon
                  :id="'toggle-' + message.memoryId"
                  icon="carbon:close"
              >
              </Icon>
            </div>

            <div class="action-buttons">
              <button
                  class="delete-btn"
                  @click.stop="showDeleteConfirm(message.memoryId)"
              >
                <Icon icon="carbon:close"></Icon>
              </button>
            </div>
          </div>

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

      <div v-if="filteredMessages.length === 0 && searchQuery" class="empty-state">
        <p class="state-text">none message</p>
      </div>
    </div>

    <div
        id="name-edit-modal"
        class="modal-overlay"
        v-if="showNameModal"
        @click.self="closeNameModal"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">{{$t('memory.changeName')}}</h3>
          <input
              type="text"
              v-model="nameInput"
              class="edit-input"
              :placeholder="$t('memory.newNamePlaceholder')"
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
            {{$t('memory.cancel')}}
          </button>
          <button
              id="save-name"
              class="modal-btn confirm-btn"
              @click="saveName"
          >
            {{$t('memory.save')}}
          </button>
        </div>
      </div>
    </div>

    <div
        id="delete-modal"
        class="modal-overlay"
        v-if="showDeleteModal"
        @click.self="closeDeleteModal"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">{{$t('memory.deleteHint')}}</h3>
          <p class="state-text" id="delete-message">
            {{$t('memory.deleteHintPrefix')}} {{ currentDeleteId }} {{$t('memory.deleteHintSuffix')}}
          </p>
        </div>

        <div class="modal-footer">
          <button
              id="cancel-delete"
              class="modal-btn cancel-btn"
              @click="closeDeleteModal"
          >
            {{$t('memory.cancel')}}
          </button>
          <button
              id="confirm-delete"
              class="modal-btn delete-btn-confirm"
              @click="confirmDelete"
          >
            {{$t('memory.delete')}}
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

const showTitleEdit = ref(false);
const searchQuery = ref('');
const messages = ref<Message[]>([]);
const filteredMessages = ref<Message[]>([]);

const showNameModal = ref(false);
const currentEditMessageId = ref<string | null>(null);
const nameInput = ref('');

const showDeleteModal = ref(false);
const currentDeleteId = ref<string | null>(null);

onMounted(() => {
  memoryStore.setLoadMessages(loadMessages)
});

const loadMessages = async () => {
  try {
    const mes = await MemoryApiService.getMemories()
    if(messages.value){
      const expandedMap = new Map(
          messages.value.map(msg => [msg.memoryId, msg.expanded])
      );
      messages.value = mes.map((mesMsg: Message) => ({
        ...mesMsg,
        expanded: expandedMap.has(mesMsg.memoryId)
            ? expandedMap.get(mesMsg.memoryId)
            : mesMsg.expanded // 或设置默认值如 false
      }));
    } else {
      messages.value = mes.map((msg: Message) => ({...msg, expanded: false}));
    }
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

const formatTimestamp = (timestamp: number | string): string => {
  const timestampNum = typeof timestamp === 'string' ? parseInt(timestamp, 10) : timestamp;
  if (isNaN(timestampNum) || timestampNum <= 0) {
    return '未知时间';
  }
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

const handleSearch = () => {
  const query = searchQuery.value.toLowerCase().trim();

  if (!query) {
    filteredMessages.value = [...messages.value];
    return;
  }

  filteredMessages.value = messages.value.filter(message => {
    const matchesName = message.memoryName.toLowerCase().includes(query);
    const matchesId = message.memoryId.toLowerCase().includes(query);
    const matchesContent = message.messages.some(bubble =>
        bubble.text.toLowerCase().includes(query)
    );
    return matchesName || matchesId || matchesContent;
  });
};

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
      messages.value[messageIndex] = {...returnMemory, expanded: currentMessage.expanded};
      handleSearch();
      showNameModal.value = false;
    } catch (error) {
      console.error('error:', error);
    }
  }
};

const toggleMessage = (id: string) => {
  const message = messages.value.find(msg => msg.memoryId === id);
  if (message) {
    message.expanded = !message.expanded;
    const filteredIndex = filteredMessages.value.findIndex(msg => msg.memoryId === id);
    if (filteredIndex !== -1) {
      filteredMessages.value[filteredIndex] = {...message};
    }
  }
};

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
    await MemoryApiService.delete(currentDeleteId.value);
    messages.value = messages.value.filter(msg => msg.memoryId !== currentDeleteId.value);
    handleSearch();
    if(messages.value.length === 0){
      memoryStore.clearMemoryId()
    }
    showDeleteModal.value = false;
    currentDeleteId.value = null;
  } catch (error) {
    console.error('error:', error);
  }
};
</script>

<style>
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
