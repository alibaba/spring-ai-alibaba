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
  <div class="user-message" :data-message-id="message.id">
    <div class="user-content">
      <div class="message-text">
        {{ message.content }}
      </div>
      
      <!-- Attachments if any -->
      <div v-if="message.attachments?.length" class="attachments">
        <div
          v-for="(attachment, index) in message.attachments"
          :key="index"
          class="attachment-item"
        >
          <Icon icon="carbon:document" class="attachment-icon" />
          <span class="attachment-name">{{ attachment.name }}</span>
          <span class="attachment-size">{{ formatFileSize(attachment.size) }}</span>
        </div>
      </div>
      
      <!-- Timestamp -->
      <div class="message-timestamp">
        {{ formatTimestamp(message.timestamp) }}
      </div>
    </div>
    
    <!-- Message status indicator -->
    <div v-if="message.error" class="message-status error">
      <Icon icon="carbon:warning" class="status-icon" />
      <span class="status-text">{{ message.error }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { useMessageFormatting } from './composables/useMessageFormatting'
import type { ChatMessage } from './composables/useChatMessages'

interface Props {
  message: ChatMessage
}

defineProps<Props>()

const { formatTimestamp, formatFileSize } = useMessageFormatting()
</script>

<style lang="less" scoped>
.user-message {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-bottom: 16px;
  
  .user-content {
    max-width: 70%;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #ffffff;
    padding: 12px 16px;
    border-radius: 18px 18px 4px 18px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    position: relative;
    
    .message-text {
      word-wrap: break-word;
      white-space: pre-wrap;
      line-height: 1.5;
      font-size: 14px;
    }
    
    .attachments {
      margin-top: 8px;
      
      .attachment-item {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 6px 8px;
        background: rgba(255, 255, 255, 0.1);
        border-radius: 8px;
        margin-bottom: 4px;
        font-size: 12px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .attachment-icon {
          font-size: 14px;
          color: rgba(255, 255, 255, 0.8);
        }
        
        .attachment-name {
          flex: 1;
          color: #ffffff;
        }
        
        .attachment-size {
          color: rgba(255, 255, 255, 0.7);
          font-size: 11px;
        }
      }
    }
    
    .message-timestamp {
      margin-top: 6px;
      font-size: 11px;
      color: rgba(255, 255, 255, 0.7);
      text-align: right;
    }
  }
  
  .message-status {
    margin-top: 4px;
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    
    &.error {
      color: #ff6b6b;
      
      .status-icon {
        font-size: 14px;
      }
    }
  }
}

@media (max-width: 768px) {
  .user-message {
    .user-content {
      max-width: 85%;
      padding: 10px 14px;
      border-radius: 16px 16px 4px 16px;
      
      .message-text {
        font-size: 13px;
      }
    }
  }
}
</style>
