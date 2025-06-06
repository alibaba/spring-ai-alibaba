<!-- /*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the const messagesRef = ref<HTMLElement>()
const inputRef = ref<HTMLTextAreaElement>()
const currentInput = ref('')
const isLoading = ref(false)nse");
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
  <div class="direct-page">
    <Sidebar />
    <div class="direct-chat">
      <!-- Left Panel - Chat -->
      <div class="left-panel">
        <div class="chat-header">
          <button class="back-button" @click="goBack">
            <Icon icon="carbon:arrow-left" />
          </button>
          <h2>Direct Chat</h2>
        </div>

        <ChatContainer 
          :initial-prompt="prompt" 
          mode="direct"
          @[EVENTS.USER_MESSAGE_SEND_REQUESTED]="handleMessageSent"
        />
      </div>

      <!-- Right Panel - Preview -->
      <RightPanel />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import Sidebar from '@/components/sidebar/index.vue'
import RightPanel from '@/components/right-panel/index.vue'
import ChatContainer from '@/components/chat/index.vue'
import { EVENTS } from '@/constants/events'

const route = useRoute()
const router = useRouter()

const prompt = ref<string>('')

onMounted(() => {
  // Initialize with prompt from conversation page
  prompt.value = (route.query.prompt as string) || ''
})

const handleMessageSent = (message: string) => {
  console.log('Message sent:', message)
  // 可以在这里处理消息发送的逻辑
}

const goBack = () => {
  router.push('/home')
}
</script>

<style lang="less" scoped>
.direct-page {
  width: 100%;
  display: flex;
  position: relative;
}

.direct-chat {
  height: 100vh;
  width: 100%;
  background: #0a0a0a;
  display: flex;
}

.left-panel {
  position: relative;
  width: 50%;
  border-right: 1px solid #1a1a1a;
  display: flex;
  flex-direction: column;
  max-height: 100vh;
}

.chat-header {
  padding: 20px 24px;
  border-bottom: 1px solid #1a1a1a;
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.02);

  h2 {
    flex: 1;
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #ffffff;
  }
}

.back-button {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
  }
}
</style>
