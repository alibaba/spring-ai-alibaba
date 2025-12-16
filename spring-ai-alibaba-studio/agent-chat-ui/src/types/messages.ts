/**
 * Unified Message types based on backend DTOs
 * Replaces LangChain/LangGraph message types
 */

// Base message interface matching backend MessageDTO
export interface BaseMessage {
  messageType: 'user' | 'assistant' | 'tool' | 'tool-request' | 'tool-confirm' | 'system';
  content: string;
  metadata?: Record<string, any>;
}

// User message - matches backend UserMessageDTO
export interface UserMessage extends BaseMessage {
  messageType: 'user';
  media?: MediaDTO[];
}

export interface MediaDTO {
  mimeType: string;
  data: any;
}

// Assistant message - matches backend AssistantMessageDTO
export interface AssistantMessage extends BaseMessage {
  messageType: 'assistant';
  toolCalls?: ToolCall[];
}

export interface ToolCall {
  id: string;
  type: string;
  name: string;
  arguments: string;
}

// Tool request message - matches backend ToolRequestMessageDTO
export interface ToolRequestMessage extends BaseMessage {
  messageType: 'tool-request';
  toolCalls?: ToolCall[];
}

// Tool request confirm message - matches backend ToolRequestConfirmMessageDTO
export interface ToolRequestConfirmMessage extends BaseMessage {
  messageType: 'tool-confirm';
  toolFeedback?: ToolFeedback[];
  toolsAutomaticallyApproved?: ToolCall[];
}

export interface ToolFeedback {
  id: string;
  name: string;
  arguments: string;
  result?: 'APPROVED' | 'REJECTED' | 'EDITED';
  description?: string;
}

// Tool response message - matches backend ToolResponseMessageDTO
export interface ToolResponseMessage extends BaseMessage {
  messageType: 'tool';
  responses?: ToolResponse[];
}

export interface ToolResponse {
  id: string;
  name: string;
  responseData: string;
}

// Union type for all messages
export type Message =
  | UserMessage
  | AssistantMessage
  | ToolRequestMessage
  | ToolRequestConfirmMessage
  | ToolResponseMessage;

// UI-specific message with additional fields for rendering
export interface UIMessage {
  id?: string;
  message: Message;
  timestamp?: number;
}

// Helper type guards
export function isUserMessage(message: Message): message is UserMessage {
  return message.messageType === 'user';
}

export function isAssistantMessage(message: Message): message is AssistantMessage {
  return message.messageType === 'assistant';
}

export function isToolRequestMessage(message: Message): message is ToolRequestMessage {
  return message.messageType === 'tool-request';
}

export function isToolRequestConfirmMessage(message: Message): message is ToolRequestConfirmMessage {
  return message.messageType === 'tool-confirm';
}

export function isToolResponseMessage(message: Message): message is ToolResponseMessage {
  return message.messageType === 'tool';
}

// Helper to convert backend MessageDTO to UI Message
export function fromMessageDTO(dto: any): Message {
  switch (dto.messageType) {
    case 'user':
      return {
        messageType: 'user',
        content: dto.content || '',
        metadata: dto.metadata || {},
        media: dto.media || []
      } as UserMessage;

    case 'assistant':
      return {
        messageType: 'assistant',
        content: dto.content || '',
        metadata: dto.metadata || {},
        toolCalls: dto.toolCalls || []
      } as AssistantMessage;

    case 'tool-request':
      return {
        messageType: 'tool-request',
        content: dto.content || '',
        metadata: dto.metadata || {},
        toolCalls: dto.toolCalls || []
      } as ToolRequestMessage;

    case 'tool-confirm':
      return {
        messageType: 'tool-confirm',
        content: dto.content || '',
        metadata: dto.metadata || {},
        toolFeedback: dto.toolFeedback || [],
        toolsAutomaticallyApproved: dto.toolsAutomaticallyApproved || []
      } as ToolRequestConfirmMessage;

    case 'tool':
      return {
        messageType: 'tool',
        content: dto.content || '',
        metadata: dto.metadata || {},
        responses: dto.responses || []
      } as ToolResponseMessage;

    default:
      // Fallback to user message
      return {
        messageType: 'user',
        content: dto.content || '',
        metadata: dto.metadata || {}
      } as UserMessage;
  }
}

// Helper to create UI message
export function createUIMessage(message: Message, id?: string, timestamp?: number): UIMessage {
  return {
    id: id || `${message.messageType}-${Date.now()}`,
    message,
    timestamp: timestamp || Date.now()
  };
}

