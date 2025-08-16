import React, { createContext, useContext, useReducer, ReactNode } from 'react';

export interface Message {
  id: string;
  type: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  toolCalls?: any[];
  error?: string;
  attachments?: File[];
}

export interface ChatSession {
  id: string;
  title: string;
  messages: Message[];
  createdAt: Date;
  updatedAt: Date;
}

interface ChatState {
  sessions: ChatSession[];
  currentSessionId: string | null;
  isLoading: boolean;
  isStreaming: boolean;
  error: string | null;
}

type ChatAction =
  | { type: 'SET_SESSIONS'; payload: ChatSession[] }
  | { type: 'ADD_SESSION'; payload: ChatSession }
  | { type: 'SET_CURRENT_SESSION'; payload: string }
  | { type: 'ADD_MESSAGE'; payload: { sessionId: string; message: Message } }
  | { type: 'UPDATE_MESSAGE'; payload: { sessionId: string; messageId: string; updates: Partial<Message> } }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_STREAMING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'DELETE_SESSION'; payload: string };

const initialState: ChatState = {
  sessions: [],
  currentSessionId: null,
  isLoading: false,
  isStreaming: false,
  error: null,
};

const chatReducer = (state: ChatState, action: ChatAction): ChatState => {
  switch (action.type) {
    case 'SET_SESSIONS':
      return { ...state, sessions: action.payload };

    case 'ADD_SESSION':
      return {
        ...state,
        sessions: [action.payload, ...state.sessions],
        currentSessionId: action.payload.id,
      };

    case 'SET_CURRENT_SESSION':
      return { ...state, currentSessionId: action.payload };

    case 'ADD_MESSAGE':
      return {
        ...state,
        sessions: state.sessions.map(session =>
          session.id === action.payload.sessionId
            ? {
                ...session,
                messages: [...session.messages, action.payload.message],
                updatedAt: new Date(),
              }
            : session
        ),
      };

    case 'UPDATE_MESSAGE':
      return {
        ...state,
        sessions: state.sessions.map(session =>
          session.id === action.payload.sessionId
            ? {
                ...session,
                messages: session.messages.map(msg =>
                  msg.id === action.payload.messageId
                    ? { ...msg, ...action.payload.updates }
                    : msg
                ),
                updatedAt: new Date(),
              }
            : session
        ),
      };

    case 'SET_LOADING':
      return { ...state, isLoading: action.payload };

    case 'SET_STREAMING':
      return { ...state, isStreaming: action.payload };

    case 'SET_ERROR':
      return { ...state, error: action.payload };

    case 'DELETE_SESSION':
      const filteredSessions = state.sessions.filter(s => s.id !== action.payload);
      return {
        ...state,
        sessions: filteredSessions,
        currentSessionId: state.currentSessionId === action.payload
          ? (filteredSessions.length > 0 ? filteredSessions[0].id : null)
          : state.currentSessionId,
      };

    default:
      return state;
  }
};

interface ChatContextValue {
  state: ChatState;
  dispatch: React.Dispatch<ChatAction>;
  currentSession: ChatSession | null;
  createNewSession: () => void;
  sendMessage: (content: string, attachments?: File[]) => Promise<void>;
  deleteSession: (sessionId: string) => void;
}

const ChatContext = createContext<ChatContextValue | undefined>(undefined);

export const ChatProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [state, dispatch] = useReducer(chatReducer, initialState);

  const currentSession = state.sessions.find(s => s.id === state.currentSessionId) || null;

  const createNewSession = () => {
    const newSession: ChatSession = {
      id: `session_${Date.now()}`,
      title: `对话 ${state.sessions.length + 1}`,
      messages: [],
      createdAt: new Date(),
      updatedAt: new Date(),
    };
    dispatch({ type: 'ADD_SESSION', payload: newSession });
  };

  const sendMessage = async (content: string, attachments?: File[]) => {
    if (!currentSession) {
      createNewSession();
      return;
    }

    const userMessage: Message = {
      id: `msg_${Date.now()}`,
      type: 'user',
      content,
      timestamp: new Date(),
      attachments,
    };

    dispatch({
      type: 'ADD_MESSAGE',
      payload: { sessionId: currentSession.id, message: userMessage }
    });

    dispatch({ type: 'SET_STREAMING', payload: true });

    try {
      // TODO: Replace with actual API call
      await simulateAPICall(content, currentSession.id, dispatch);
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: '发送消息失败' });
    } finally {
      dispatch({ type: 'SET_STREAMING', payload: false });
    }
  };

  const deleteSession = (sessionId: string) => {
    dispatch({ type: 'DELETE_SESSION', payload: sessionId });
  };

  const value: ChatContextValue = {
    state,
    dispatch,
    currentSession,
    createNewSession,
    sendMessage,
    deleteSession,
  };

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
};

// Simulate API call for demonstration
const simulateAPICall = async (
  content: string,
  sessionId: string,
  dispatch: React.Dispatch<ChatAction>
) => {
  // Simulate processing delay
  await new Promise(resolve => setTimeout(resolve, 1000));

  const assistantMessage: Message = {
    id: `msg_${Date.now()}`,
    type: 'assistant',
    content: `我收到了你的消息："${content}"。这是一个模拟回复，用于演示聊天界面功能。`,
    timestamp: new Date(),
    toolCalls: content.includes('工具') ? [{
      name: 'example_tool',
      arguments: { query: content },
      result: '工具调用示例结果'
    }] : undefined,
  };

  dispatch({
    type: 'ADD_MESSAGE',
    payload: { sessionId, message: assistantMessage }
  });
};

export const useChatContext = () => {
  const context = useContext(ChatContext);
  if (context === undefined) {
    throw new Error('useChatContext must be used within a ChatProvider');
  }
  return context;
};
