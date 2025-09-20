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
  <div class="response-section">
    <div class="response-header">
      <div class="response-avatar">
        <Icon icon="carbon:bot" class="bot-icon" />
      </div>
      <div class="response-name">{{ $t('chat.botName') }}</div>
      
      <!-- Response timestamp -->
      <div v-if="timestamp" class="response-timestamp">
        {{ formatTimestamp(timestamp) }}
      </div>
    </div>
    
    <div class="response-content">
      <!-- User input form -->
      <UserInputForm
        v-if="userInputWaitState?.waiting"
        :user-input-wait-state="userInputWaitState"
        v-bind="planId ? { 'plan-id': planId } : {}"
        :generic-input="genericInput ?? ''"
        @user-input-submitted="handleUserInputSubmitted"
      />
      
      <!-- Final response with content -->
      <div v-if="content" class="final-response">
        <div class="response-text" v-html="formatResponseText(content)"></div>
        
        <!-- Response actions -->
        <div class="response-actions">
          <button
            class="action-btn copy-btn"
            @click="copyToClipboard"
            :title="$t('chat.copyResponse')"
          >
            <Icon icon="carbon:copy" />
          </button>
          
          <button
            class="action-btn regenerate-btn"
            @click="handleRegenerate"
            :title="$t('chat.regenerateResponse')"
          >
            <Icon icon="carbon:renew" />
          </button>
        </div>
      </div>
      
      <!-- Loading/streaming state -->
      <div v-else-if="isStreaming" class="response-placeholder">
        <div class="typing-indicator">
          <div class="typing-dots">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <span class="typing-text">{{ $t('chat.thinkingResponse') }}</span>
        </div>
      </div>
      
      <!-- Error state -->
      <div v-else-if="error" class="response-error">
        <Icon icon="carbon:warning" class="error-icon" />
        <span class="error-text">{{ error }}</span>
        <button class="retry-btn" @click="handleRetry">
          {{ $t('chat.retry') }}
        </button>
      </div>
      
      <!-- Empty state -->
      <div v-else class="response-empty">
        <span class="empty-text">{{ $t('chat.waitingForResponse') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { useMessageFormatting } from './composables/useMessageFormatting'
import UserInputForm from './UserInputForm.vue'
import type { UserInputWaitState } from '@/types/plan-execution-record'

interface Props {
  content?: string
  isStreaming?: boolean
  error?: string
  timestamp?: Date
  userInputWaitState?: UserInputWaitState
  planId?: string
  genericInput?: string
}

interface Emits {
  (e: 'copy'): void
  (e: 'regenerate'): void
  (e: 'retry'): void
  (e: 'user-input-submitted', inputData: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const { formatResponseText, formatTimestamp } = useMessageFormatting()

// Methods
const copyToClipboard = async () => {
  if (!props.content) return
  
  try {
    // Strip HTML tags for clipboard
    const plainText = props.content.replace(/<[^>]*>/g, '')
    await navigator.clipboard.writeText(plainText)
    emit('copy')
  } catch (error) {
    console.error('Failed to copy to clipboard:', error)
  }
}

const handleRegenerate = () => {
  emit('regenerate')
}

const handleRetry = () => {
  emit('retry')
}

const handleUserInputSubmitted = (inputData: any) => {
  console.log('[ResponseSection] User input submitted:', inputData)
  emit('user-input-submitted', inputData)
}
</script>

<style lang="less" scoped>
.response-section {
  .response-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    
    .response-avatar {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
      border-radius: 50%;
      flex-shrink: 0;
      
      .bot-icon {
        font-size: 16px;
        color: #ffffff;
      }
    }
    
    .response-name {
      font-weight: 600;
      color: #ffffff;
      font-size: 14px;
      flex: 1;
    }
    
    .response-timestamp {
      font-size: 11px;
      color: #aaaaaa;
    }
  }
  
  .response-content {
    .final-response {
      position: relative;
      
      .response-text {
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 12px;
        padding: 16px;
        color: #ffffff;
        line-height: 1.6;
        font-size: 14px;
        
        :deep(strong) {
          font-weight: 600;
          color: #ffffff;
        }
        
        :deep(em) {
          font-style: italic;
          color: #cccccc;
        }
        
        :deep(code) {
          background: rgba(0, 0, 0, 0.3);
          padding: 2px 6px;
          border-radius: 4px;
          font-family: 'Monaco', 'Menlo', monospace;
          font-size: 13px;
          color: #8be9fd;
        }
        
        :deep(pre) {
          background: rgba(0, 0, 0, 0.4);
          padding: 12px;
          border-radius: 8px;
          overflow-x: auto;
          margin: 8px 0;
          
          code {
            background: none;
            padding: 0;
            color: #f8f8f2;
            font-size: 12px;
          }
        }
      }
      
      .response-actions {
        display: flex;
        gap: 8px;
        margin-top: 8px;
        opacity: 0;
        transition: opacity 0.2s ease;
        
        .action-btn {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 28px;
          height: 28px;
          background: rgba(255, 255, 255, 0.1);
          border: none;
          border-radius: 6px;
          color: #aaaaaa;
          cursor: pointer;
          transition: all 0.2s ease;
          
          &:hover {
            background: rgba(255, 255, 255, 0.2);
            color: #ffffff;
          }
          
          svg {
            font-size: 14px;
          }
        }
      }
      
      &:hover .response-actions {
        opacity: 1;
      }
    }
    
    .response-placeholder {
      .typing-indicator {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 16px;
        color: #aaaaaa;
        font-size: 14px;
        
        .typing-dots {
          display: flex;
          gap: 4px;
          
          span {
            width: 6px;
            height: 6px;
            background: #4f46e5;
            border-radius: 50%;
            animation: typing-pulse 1.5s ease-in-out infinite;
            
            &:nth-child(1) {
              animation-delay: 0s;
            }
            
            &:nth-child(2) {
              animation-delay: 0.2s;
            }
            
            &:nth-child(3) {
              animation-delay: 0.4s;
            }
          }
        }
        
        .typing-text {
          font-style: italic;
        }
      }
    }
    
    .response-error {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: rgba(239, 68, 68, 0.1);
      border: 1px solid rgba(239, 68, 68, 0.2);
      border-radius: 8px;
      color: #ff9999;
      font-size: 13px;
      
      .error-icon {
        font-size: 16px;
        color: #ef4444;
        flex-shrink: 0;
      }
      
      .error-text {
        flex: 1;
      }
      
      .retry-btn {
        background: rgba(239, 68, 68, 0.2);
        border: none;
        padding: 4px 8px;
        border-radius: 4px;
        color: #ffffff;
        font-size: 12px;
        cursor: pointer;
        transition: background 0.2s ease;
        
        &:hover {
          background: rgba(239, 68, 68, 0.3);
        }
      }
    }
    
    .response-empty {
      padding: 16px;
      text-align: center;
      color: #888888;
      font-style: italic;
      font-size: 13px;
    }
  }
}

@keyframes typing-pulse {
  0%, 80%, 100% {
    opacity: 0.3;
    transform: scale(1);
  }
  40% {
    opacity: 1;
    transform: scale(1.2);
  }
}

@media (max-width: 768px) {
  .response-section {
    .response-header {
      gap: 10px;
      
      .response-avatar {
        width: 28px;
        height: 28px;
        
        .bot-icon {
          font-size: 14px;
        }
      }
      
      .response-name {
        font-size: 13px;
      }
    }
    
    .response-content {
      .final-response {
        .response-text {
          padding: 14px;
          font-size: 13px;
        }
        
        .response-actions {
          margin-top: 6px;
          
          .action-btn {
            width: 26px;
            height: 26px;
            
            svg {
              font-size: 12px;
            }
          }
        }
      }
    }
  }
}
</style>
