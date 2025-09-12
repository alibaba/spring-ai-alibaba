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

import { ref, computed, readonly } from 'vue'
import type { PlanExecutionRecord, AgentExecutionRecord } from '@/types/plan-execution-record'

// Local interface to handle readonly compatibility issues
export interface CompatiblePlanExecutionRecord extends Omit<PlanExecutionRecord, 'agentExecutionSequence'> {
  agentExecutionSequence?: AgentExecutionRecord[]
}

// Message interface
export interface ChatMessage {
  id: string
  type: 'user' | 'assistant'
  content: string
  timestamp: Date
  thinking?: string
  thinkingDetails?: CompatiblePlanExecutionRecord
  planExecution?: CompatiblePlanExecutionRecord
  stepActions?: any[]
  genericInput?: string
  isStreaming?: boolean
  error?: string
  attachments?: File[]
}

// Input message interface
export interface InputMessage {
  input: string
  attachments?: File[]
}

/**
 * Utility type to convert readonly arrays to mutable ones
 */
type MakeMutable<T> = T extends readonly (infer U)[] ? U[] : T

/**
 * Convert readonly PlanExecutionRecord to mutable compatible version with strong typing
 */
function convertPlanExecutionRecord<T extends Record<string, any>>(
  record: T
): CompatiblePlanExecutionRecord {
  const converted = { ...record } as unknown as CompatiblePlanExecutionRecord
  
  if ('agentExecutionSequence' in record && Array.isArray(record.agentExecutionSequence)) {
    converted.agentExecutionSequence = record.agentExecutionSequence.map(
      (agent: any) => convertAgentExecutionRecord(agent)
    )
  }
  
  return converted
}

/**
 * Convert readonly AgentExecutionRecord to mutable compatible version with strong typing
 */
function convertAgentExecutionRecord<T extends Record<string, any>>(
  record: T
): AgentExecutionRecord {
  const converted = { ...record } as unknown as AgentExecutionRecord
  
  if ('subPlanExecutionRecords' in record && Array.isArray(record.subPlanExecutionRecords)) {
    converted.subPlanExecutionRecords = record.subPlanExecutionRecords.map(
      (subPlan: any) => convertPlanExecutionRecord(subPlan)
    )
  }
  
  return converted
}

/**
 * Convert a message with potentially readonly arrays to a fully mutable version
 */
export function convertMessageToCompatible<T extends Record<string, any>>(message: T): ChatMessage {
  const converted = { ...message } as unknown as ChatMessage
  
  if ('thinkingDetails' in message && message.thinkingDetails) {
    converted.thinkingDetails = convertPlanExecutionRecord(message.thinkingDetails)
  }
  
  if ('planExecution' in message && message.planExecution) {
    converted.planExecution = convertPlanExecutionRecord(message.planExecution)
  }
  
  if ('attachments' in message && Array.isArray(message.attachments)) {
    converted.attachments = [...message.attachments] as MakeMutable<typeof message.attachments>
  }
  
  return converted
}

/**
 * Chat messages state management
 */
export function useChatMessages() {
  // State
  const messages = ref<ChatMessage[]>([])
  const isLoading = ref(false)
  const streamingMessageId = ref<string | null>(null)
  const activeMessageId = ref<string | null>(null)

  // Computed properties
  const lastMessage = computed(() => {
    return messages.value.length > 0 ? messages.value[messages.value.length - 1] : null
  })

  const isStreaming = computed(() => {
    return streamingMessageId.value !== null
  })

  const hasMessages = computed(() => {
    return messages.value.length > 0
  })

  // Methods
  const addMessage = (type: 'user' | 'assistant', content: string, options?: Partial<ChatMessage>): ChatMessage => {
    const message: ChatMessage = {
      id: `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      type,
      content,
      timestamp: new Date(),
      isStreaming: false,
      ...options
    }

    messages.value.push(message)
    return message
  }

  const updateMessage = (id: string, updates: Partial<ChatMessage>) => {
    const index = messages.value.findIndex(m => m.id === id)
    if (index !== -1) {
      messages.value[index] = { ...messages.value[index], ...updates }
    }
  }

  const removeMessage = (id: string) => {
    const index = messages.value.findIndex(m => m.id === id)
    if (index !== -1) {
      messages.value.splice(index, 1)
    }
  }

  const clearMessages = () => {
    messages.value = []
    streamingMessageId.value = null
    activeMessageId.value = null
  }

  const startStreaming = (messageId: string) => {
    streamingMessageId.value = messageId
    updateMessage(messageId, { isStreaming: true })
  }

  const stopStreaming = (messageId?: string) => {
    if (messageId) {
      updateMessage(messageId, { isStreaming: false })
    }
    if (streamingMessageId.value === messageId) {
      streamingMessageId.value = null
    }
  }

  const setActiveMessage = (id: string | null) => {
    activeMessageId.value = id
  }

  const appendToMessage = (id: string, content: string, field: keyof ChatMessage = 'content') => {
    const message = messages.value.find(m => m.id === id)
    if (message && typeof message[field] === 'string') {
      updateMessage(id, { [field]: (message[field] as string) + content })
    }
  }

  const updateMessageThinkingDetails = (id: string, thinkingDetails: PlanExecutionRecord) => {
    updateMessage(id, { thinkingDetails })
  }

  const updateMessageThinking = (id: string, thinking: string) => {
    updateMessage(id, { thinking })
  }

  const updateMessagePlanExecution = (id: string, planExecution: PlanExecutionRecord) => {
    updateMessage(id, { planExecution })
  }

  const findMessage = (id: string): ChatMessage | undefined => {
    return messages.value.find(m => m.id === id)
  }

  const getMessageIndex = (id: string): number => {
    return messages.value.findIndex(m => m.id === id)
  }

  return {
    // State
    messages: readonly(messages),
    isLoading,
    streamingMessageId: readonly(streamingMessageId),
    activeMessageId: readonly(activeMessageId),

    // Computed
    lastMessage,
    isStreaming,
    hasMessages,

    // Methods
    addMessage,
    updateMessage,
    removeMessage,
    clearMessages,
    startStreaming,
    stopStreaming,
    setActiveMessage,
    appendToMessage,
    updateMessageThinking,
    updateMessageThinkingDetails,
    updateMessagePlanExecution,
    findMessage,
    getMessageIndex
  }
}

// Message utilities
export const createUserMessage = (content: string): Omit<ChatMessage, 'id' | 'timestamp'> => ({
  type: 'user',
  content,
  isStreaming: false
})

export const createAssistantMessage = (options?: Partial<ChatMessage>): Omit<ChatMessage, 'id' | 'timestamp'> => ({
  type: 'assistant',
  content: '',
  thinking: '',
  isStreaming: true,
  ...options
})
