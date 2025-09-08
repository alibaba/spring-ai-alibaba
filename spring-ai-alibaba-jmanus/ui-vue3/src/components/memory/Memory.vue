<template>
  <Teleport to="body">
    <Transition name="modal">
      <div class="app-container" v-if="memoryStore.isCollapsed">
        <div class="app-content">
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
            <button class="close-btn" @click="memoryStore.toggleSidebar()">
              <Icon icon="carbon:close" />
            </button>
          </div>

          <div class="search-bar">
            <div class="search-container">
              <Icon class="search-container-icon" icon="carbon:search" />
              <input
                  type="text"
                  :placeholder="$t('memory.searchPlaceholder')"
                  class="search-input"
                  v-model="searchQuery"
                  @input="handleSearch"
              >
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
                <div class="message-header">
                  <div class="message-content">
                    <div class="sender-info">
                      <div class="sender-div" @click.stop="selectMemory(message.memoryId)">
                        <h3
                            class="sender-name"
                        >
                          {{ message.memoryName }}
                        </h3>
                      </div>

                      <div class="toggle-container" @click.stop="showNameEditModal(message.memoryId, message.memoryName)">
                        <Icon
                            icon="carbon:edit"
                            class="edit-btn"
                        >
                        </Icon>
                      </div>
                      <div class="action-buttons">
                        <button
                            class="delete-btn"
                            @click.stop="toggleMessage(message.memoryId)"
                        >
                          <Icon :id="'toggle-' + message.memoryId"
                                icon="carbon:chevron-down"
                                class="down-btn"
                          ></Icon>
                        </button>
                      </div>
                      <div class="action-buttons">
                        <button
                            class="delete-btn"
                            @click.stop="showDeleteConfirm(message.memoryId)"
                        >
                          <Icon icon="carbon:delete"></Icon>
                        </button>
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
                >
                <span id="name-char-count" class="char-count" style="text-align: right; display: block; margin-top: 0.25rem;">
                {{ nameInput.length }}
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
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import {onMounted, onUnmounted, ref} from 'vue';
import {MemoryApiService} from '@/api/memory-api-service'
import type { Message} from '@/api/memory-api-service'
import {Icon} from '@iconify/vue'
import {memoryStore} from "@/stores/memory";
import type {MemoryEmits} from "@/stores/memory";

const showTitleEdit = ref(false);
const searchQuery = ref('');
const messages = ref<Message[]>([]);
const filteredMessages = ref<Message[]>([]);

const showNameModal = ref(false);
const currentEditMessageId = ref<string | null>(null);
const nameInput = ref('');

const showDeleteModal = ref(false);
const currentDeleteId = ref<string | null>(null);
const expandedMap = new Map()
const emit = defineEmits<MemoryEmits>()

// Handle ESC key to close modals
const handleEscKey = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    if (showDeleteModal.value) {
      closeDeleteModal()
    } else if (showNameModal.value) {
      closeNameModal()
    }
  }
}

onMounted(() => {
  memoryStore.setLoadMessages(loadMessages)
  document.addEventListener('keydown', handleEscKey)
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscKey)
});

const loadMessages = async () => {
  try {
    const mes = await MemoryApiService.getMemories()
    if(messages.value){
      messages.value = mes.map((mesMsg: Message) => ({
        ...mesMsg,
        expanded: expandedMap.has(mesMsg.memoryId)
            ? expandedMap.get(mesMsg.memoryId)
            : false
      }));
    } else {
      messages.value = mes.map((msg: Message) => ({...msg, expanded: false}));
    }
    filteredMessages.value = [...messages.value];
    handleSearch()
  } catch (e) {
    console.error('error:', e);
    messages.value = [];

    filteredMessages.value = [];
  }
};

const selectMemory = (memoryId: string) => {
  memoryStore.selectMemory(memoryId);
  emit('memory-selected')
};

const formatTimestamp = (timestamp: number | string): string => {
  const timestampNum = typeof timestamp === 'string' ? parseInt(timestamp, 10) : timestamp;
  if (isNaN(timestampNum) || timestampNum <= 0) {
    return 'unknow time';
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
    try {
      const returnMemory = await MemoryApiService.update(currentMessage.memoryId, newName)
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
    expandedMap.set(id, message.expanded)
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

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.app-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.app-content {
  position: relative;
  transition: all 0.3s ease-in-out;
  overflow-y: auto;
  border-radius: 16px;
  width: 90%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.15));
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  max-width: 800px;
  max-height: 80vh;
  min-height: 500px;
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

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.close-btn:hover {
  color: rgba(255, 255, 255, 0.9);
}

.search-bar {
  padding: 1rem;
  border-bottom: 1px solid #333333;
}

.search-container {
  position: relative;
}
.search-container-icon {
  position: absolute;
  top: 10px;
  left: 8px;
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
  cursor: text;
  padding: 0.75rem;
  align-items: center;
  width: 100%;
}

.message-content {
  flex: 1;
  min-width: 0;
  padding-right: 0.5rem;
}
.message.user .message-content {
  flex: none !important;
}

.sender-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.25rem;
}

.sender-div {
  display: flex;
  align-items: center;
  width: 85%;
  cursor: pointer;
}

.sender-div:hover:not(:has(.edit-btn:hover, .down-btn:hover, .delete-btn:hover)) .sender-name {
  color: #667eea;
}

.sender-name {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.message-preview {
  margin-bottom: 0.25rem;
  padding: 8px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
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
  margin-top: 2px;
  border-radius: 1rem;
}

.toggle-container {
  margin-left: auto;
  margin-top: 4px;
  display: flex;
  align-items: center;
}

.action-buttons {
  margin-left: 0.5rem;
  transition: opacity 0.2s ease;
}

.message-item:hover .action-buttons {
  opacity: 1;
}

.edit-btn {
  margin-left: 10px;
  cursor: pointer;
}

.edit-btn:hover {
  color: #667eea;
}

.down-btn {
  cursor: pointer;
}

.down-btn:hover {
  color: #667eea;
}

.delete-btn {
  color: rgba(255, 255, 255, 0.7);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem 0;
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
}

.bubble-avatar {
  width: 5rem;
  overflow: hidden;
  margin-right: 0.5rem;
  flex-shrink: 0;
  margin-bottom: 6px;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.bubble-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bubble-content {
  padding: 8px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
}

.bubble-text {
  font-size: 12px;
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
