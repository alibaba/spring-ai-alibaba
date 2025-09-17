/*
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
 */

import { computed } from 'vue'
import type { ChatMessage } from './useChatMessages'

/**
 * Message formatting utilities
 */
export function useMessageFormatting() {
  
  /**
   * Format response text with markdown and code highlighting
   */
  const formatResponseText = (text: string): string => {
    if (!text) return ''
    
    return text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\n/g, '<br>')
      .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
  }

  /**
   * Format timestamp for display
   */
  const formatTimestamp = (timestamp: Date): string => {
    const now = new Date()
    const diff = now.getTime() - timestamp.getTime()
    
    // Less than 1 minute
    if (diff < 60000) {
      return 'Just now'
    }
    
    // Less than 1 hour
    if (diff < 3600000) {
      const minutes = Math.floor(diff / 60000)
      return `${minutes} minutes ago`
    }
    
    // Less than 1 day
    if (diff < 86400000) {
      const hours = Math.floor(diff / 3600000)
      return `${hours} hours ago`
    }
    
    // More than 1 day
    return timestamp.toLocaleDateString('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  /**
   * Get message CSS classes
   */
  const getMessageClasses = (message: ChatMessage) => {
    return computed(() => ({
      user: message.type === 'user',
      assistant: message.type === 'assistant',
      streaming: message.isStreaming,
      'has-error': !!message.error,
      'has-thinking': !!message.thinking,
      'has-execution': !!message.planExecution
    }))
  }

  /**
   * Format file size for attachments
   */
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B'
    
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  /**
   * Truncate long text with ellipsis
   */
  const truncateText = (text: string, maxLength: number = 100): string => {
    if (!text || text.length <= maxLength) return text
    return text.substring(0, maxLength) + '...'
  }

  /**
   * Extract plain text from HTML
   */
  const stripHtml = (html: string): string => {
    return html.replace(/<[^>]*>/g, '')
  }

  /**
   * Check if message has content to display
   */
  const hasDisplayableContent = (message: ChatMessage): boolean => {
    return !!(
      message.content ||
      message.thinking ||
      message.planExecution ||
      message.error
    )
  }

  /**
   * Get message status text
   */
  const getMessageStatus = (message: ChatMessage): string => {
    if (message.error) return 'Send failed'
    if (message.isStreaming) return 'Typing...'
    if (message.type === 'assistant' && !message.content && !message.thinking) return 'Waiting for response'
    return ''
  }

  return {
    formatResponseText,
    formatTimestamp,
    getMessageClasses,
    formatFileSize,
    truncateText,
    stripHtml,
    hasDisplayableContent,
    getMessageStatus
  }
}
