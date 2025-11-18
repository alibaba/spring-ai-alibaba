// Spring AI Alibaba Studio API Client

export interface Session {
  thread_id: string;
  appName: string;
  userId: string;
  values: {
    messages?: MessageDTO[];
  };
}

export interface UserMessage {
  messageType: 'user';
  content: string;
  metadata?: Record<string, any>;
  media?: MediaDTO[];
}

export interface AgentRunRequest {
  appName: string;
  userId?: string;
  threadId: string;
  newMessage: UserMessage;
  streaming?: boolean;
  stateDelta?: Record<string, any>;
}

export interface AgentResumeRequest {
  appName: string;
  userId?: string;
  threadId: string;
  toolFeedbacks?: ToolFeedbackDTO[];
  streaming?: boolean;
}

export interface ToolFeedbackDTO {
  id: string;
  name: string;
  arguments: string;
  result?: 'APPROVED' | 'REJECTED' | 'EDITED';
  description?: string;
}

// Base MessageDTO interface
export interface MessageDTO {
  messageType: 'assistant' | 'user' | 'tool' | 'tool-request' | 'tool-confirm';
  content: string;
  metadata?: Record<string, any>;
}

// AssistantMessageDTO
export interface AssistantMessageDTO extends MessageDTO {
  messageType: 'assistant';
  toolCalls?: ToolCallDTO[];
}

export interface ToolCallDTO {
  id: string;
  type: string;
  name: string;
  arguments: string;
}

// ToolRequestMessageDTO - similar to AssistantMessageDTO but with different messageType
export interface ToolRequestMessageDTO extends MessageDTO {
  messageType: 'tool-request';
  toolCalls?: ToolCallDTO[];
}

// ToolRequestConfirmMessageDTO - for tool execution confirmation
export interface ToolRequestConfirmMessageDTO extends MessageDTO {
  messageType: 'tool-confirm';
  toolCalls?: ToolCallConfigDTO[];
}

export interface ToolCallConfigDTO {
  id: string;
  type: string;
  name: string;
  arguments: string;
  description?: string;
}

// UserMessageDTO
export interface UserMessageDTO extends MessageDTO {
  messageType: 'user';
  media?: MediaDTO[];
}

export interface MediaDTO {
  mimeType: string;
  data: any;
}

// ToolResponseMessageDTO
export interface ToolResponseMessageDTO extends MessageDTO {
  messageType: 'tool';
  responses?: ToolResponseDTO[];
}

export interface ToolResponseDTO {
  id: string;
  name: string;
  responseData: string;
}

// Legacy Message interface for internal use
export interface Message {
  messageType: string;
  content: string;
  metadata?: Record<string, any>;
}

export interface Usage {
  promptTokens?: number;
  generationTokens?: number;
  totalTokens?: number;
}

export interface AgentRunResponse {
  node: string;
  agent: string;
  message: MessageDTO | null;
  tokenUsage: Usage | null;
  chunk: string | null;
}

export interface NodeOutput {
  nodeName: string;
  state: Record<string, any>;
}

export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
  timestamp: number;
  requestId?: string;
}

class SpringAIApiClient {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  // 获取应用列表
  async listApps(): Promise<string[]> {
    const response = await fetch(`${this.baseUrl}/list-apps`);
    if (!response.ok) {
      throw new Error(`Failed to fetch apps: ${response.statusText}`);
    }
    return await response.json();
  }

  // 获取会话列表
  async listSessions(appName: string, userId: string): Promise<Session[]> {
    const response = await fetch(
      `${this.baseUrl}/apps/${appName}/users/${userId}/threads`
    );
    if (!response.ok) {
      throw new Error(`Failed to fetch sessions: ${response.statusText}`);
    }
    return await response.json();
  }

  // 获取单个会话
  async getSession(sessionId: string): Promise<Session> {
    // Extract appName and userId from env or use defaults
    const appName = process.env.NEXT_PUBLIC_APP_NAME || 'research_agent';
    const userId = process.env.NEXT_PUBLIC_USER_ID || 'user-001';

    const response = await fetch(
      `${this.baseUrl}/apps/${appName}/users/${userId}/threads/${sessionId}`
    );
    if (!response.ok) {
      throw new Error(`Failed to fetch session: ${response.statusText}`);
    }
    return await response.json();
  }

  // 创建会话(自动生成ID)
  async createSession(
    appName: string,
    userId: string,
    initialState?: Record<string, any>
  ): Promise<Session> {
    const response = await fetch(
      `${this.baseUrl}/apps/${appName}/users/${userId}/threads`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(initialState || {}),
      }
    );
    if (!response.ok) {
      throw new Error(`Failed to create session: ${response.statusText}`);
    }
    return await response.json();
  }

  // 创建会话(指定ID)
  async createSessionWithId(
    appName: string,
    userId: string,
    sessionId: string,
    initialState?: Record<string, any>
  ): Promise<Session> {
    const response = await fetch(
      `${this.baseUrl}/apps/${appName}/users/${userId}/threads/${sessionId}`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(initialState || {}),
      }
    );
    if (!response.ok) {
      throw new Error(`Failed to create session with ID: ${response.statusText}`);
    }
    return await response.json();
  }

  // 删除会话
  async deleteSession(
    appName: string,
    userId: string,
    sessionId: string
  ): Promise<void> {
    const response = await fetch(
      `${this.baseUrl}/apps/${appName}/users/${userId}/threads/${sessionId}`,
      {
        method: 'DELETE',
      }
    );
    if (!response.ok) {
      throw new Error(`Failed to delete session: ${response.statusText}`);
    }
  }

  // 执行Agent(非流式)
  async runAgent(request: AgentRunRequest): Promise<NodeOutput> {
    const response = await fetch(`${this.baseUrl}/run`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      throw new Error(`Failed to run agent: ${response.statusText}`);
    }
    return await response.json();
  }

  // 执行Agent(流式SSE)
  async *runAgentStream(
    threadId: string,
    message: UserMessage,
    signal?: AbortSignal
  ): AsyncGenerator<AgentRunResponse, void, unknown> {
    const appName = process.env.NEXT_PUBLIC_APP_NAME || 'research_agent';
    const userId = process.env.NEXT_PUBLIC_USER_ID || 'user-001';

    const request: AgentRunRequest = {
      appName,
      userId,
      threadId,
      newMessage: {
        messageType: 'user',
        content: message.content,
        metadata: message.metadata || {},
        media: message.media || []
      },
      streaming: true,
    };

    const response = await fetch(`${this.baseUrl}/run_sse`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      body: JSON.stringify(request),
      signal,
    });

    if (!response.ok) {
      throw new Error(`Failed to run agent stream: ${response.statusText}`);
    }

    yield* this._processSSEStream(response);
  }

  // Resume Agent execution with tool feedback (Human-in-the-Loop)
  async *resumeAgentStream(
    threadId: string,
    toolFeedbacks: ToolFeedbackDTO[],
    signal?: AbortSignal
  ): AsyncGenerator<AgentRunResponse, void, unknown> {
    const appName = process.env.NEXT_PUBLIC_APP_NAME || 'research_agent';
    const userId = process.env.NEXT_PUBLIC_USER_ID || 'user-001';

    const request: AgentResumeRequest = {
      appName,
      userId,
      threadId,
      toolFeedbacks,
      streaming: true,
    };

    const response = await fetch(`${this.baseUrl}/resume_sse`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      body: JSON.stringify(request),
      signal,
    });

    if (!response.ok) {
      throw new Error(`Failed to resume agent stream: ${response.statusText}`);
    }

    yield* this._processSSEStream(response);
  }

  // Private helper to process SSE stream
  private async *_processSSEStream(
    response: Response
  ): AsyncGenerator<AgentRunResponse, void, unknown> {
    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error('Response body is not readable');
    }

    const decoder = new TextDecoder();
    let buffer = '';

    console.log('[API] Starting SSE stream reading...');

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          console.log('[API] SSE stream done');
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.trim().startsWith('data:')) {
            const data = line.slice(5).trim();
            if (data) {
              try {
                console.log('[API] Received SSE data:', data);
                const agentResponse: AgentRunResponse = JSON.parse(data);
                console.log('[API] Parsed agent response:', agentResponse);
                yield agentResponse;
              } catch (e) {
                console.error('[API] Failed to parse SSE data:', e, 'Raw data:', data);
              }
            }
          } else if (line.trim()) {
            // Skip non-data lines (like comments or empty event names)
            console.log('[API] Skipping non-data line:', line);
          }
        }
      }
    } finally {
      reader.releaseLock();
      console.log('[API] SSE stream ended');
    }
  }

  // 获取会话追踪信息
  async getSessionTrace(threadId: string): Promise<any[]> {
    const response = await fetch(
      `${this.baseUrl}/debug/trace/thread/${threadId}`
    );
    if (!response.ok) {
      throw new Error(`Failed to fetch session trace: ${response.statusText}`);
    }
    return await response.json();
  }
}

// Factory function to create API client
export function createApiClient(): SpringAIApiClient {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || '';
  return new SpringAIApiClient(baseUrl);
}

// Default export
export default SpringAIApiClient;

