import React, {
  createContext,
  useContext,
  ReactNode,
  useState,
  useEffect,
  useCallback,
  useRef,
  useMemo,
} from "react";
import { useQueryState } from "nuqs";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useThreads } from "./Thread";
import { toast } from "sonner";
import { createApiClient, ToolFeedbackDTO } from "@/lib/spring-ai-api";
import { UIMessage, createUIMessage, fromMessageDTO } from "@/types/messages";

interface StreamContextType {
  messages: UIMessage[];
  isStreaming: boolean;
  sendMessage: (content: string) => Promise<void>;
  resumeFeedback: (toolFeedbacks: ToolFeedbackDTO[]) => Promise<void>;
  clearMessages: () => void;
}

const StreamContext = createContext<StreamContextType | undefined>(undefined);

export const useStream = () => {
  const context = useContext(StreamContext);
  const { currentThreadId } = useThreads();

  if (!context) {
    throw new Error("useStream must be used within a StreamProvider");
  }

  const { clearMessages } = context;

  useEffect(() => {
    if (currentThreadId === null) {
      clearMessages();
    }
  }, [currentThreadId, clearMessages]);

  return context;
};

// Alias for backward compatibility
export const useStreamContext = useStream;

interface StreamProviderProps {
  children: ReactNode;
}

export const StreamProvider: React.FC<StreamProviderProps> = ({ children }) => {
  const [messages, setMessages] = useState<UIMessage[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const { currentThreadId, createThread, isNewlyCreatedThread } = useThreads();
  const abortControllerRef = useRef<AbortController | null>(null);

  // Load messages when thread changes
  useEffect(() => {
    if (currentThreadId) {
      loadThreadMessages(currentThreadId);
    } else {
      setMessages([]);
    }
  }, [currentThreadId]);



  const loadThreadMessages = async (threadId: string) => {
    setIsLoadingMessages(true);
    try {
      console.log('[Stream] Loading thread messages for threadId:', threadId);

      // Call backend API to get thread details
      const apiClient = createApiClient();
      const session = await apiClient.getSession(threadId);

      console.log('[Stream] Loaded session:', session);

      // Extract messages from session.values.messages
      if (session.values && Array.isArray(session.values.messages) && session.values.messages.length > 0) {
        const loadedMessages = session.values.messages.map((msg, index) =>
          createUIMessage(fromMessageDTO(msg), `${threadId}-${index}`)
        );
        setMessages(loadedMessages);
        console.log('[Stream] Loaded messages:', loadedMessages);
      } else {
        // Check if this is a newly created thread
        const isNewThread = isNewlyCreatedThread(threadId);

        if (isNewThread) {
          // For newly created threads, just show empty - user can start chatting
          console.log('[Stream] Newly created thread - showing empty state');
          setMessages([]);
        } else {
          // For existing threads with no messages, show development notice
          console.log('[Stream] Existing thread with empty values - showing development notice');
          const placeholderMessage = createUIMessage(
            {
              messageType: 'assistant',
              content: '💡 Support for agent message loading is under development.\n\nThis thread exists but its message history cannot be displayed yet.\n\nPlease create a new thread to start a conversation.',
              metadata: { isPlaceholder: true }
            },
            `${threadId}-placeholder`
          );
          setMessages([placeholderMessage]);
        }
      }
    } catch (error) {
      console.error("Failed to load thread messages:", error);
      toast.error("Failed to load thread messages");
      setMessages([]);
    } finally {
      setIsLoadingMessages(false);
    }
  };

  const sendMessage = useCallback(
    async (content: string) => {
      if (!content.trim()) {
        return;
      }

      // Auto-create thread if none exists
      let activeThreadId = currentThreadId;
      if (!activeThreadId) {
        toast.info("Creating new thread...");
        const newThread = await createThread();
        if (!newThread) {
          toast.error("Failed to create thread");
          return;
        }
        activeThreadId = newThread.thread_id;
      }

      // Add user message immediately - create proper UIMessage structure
      const userUIMessage: UIMessage = {
        id: `user-${Date.now()}`,
        message: {
          messageType: 'user',
          content: content.trim(),
          metadata: {}
        },
        timestamp: Date.now()
      };

      setMessages((prev) => [...prev, userUIMessage]);
      setIsStreaming(true);

      // Create new abort controller for this request
      abortControllerRef.current = new AbortController();

      try {
        const apiClient = createApiClient();

        // Start streaming - convert Message to UserMessage (with messageType for backend)
        const userMessageForApi: import("@/lib/spring-ai-api").UserMessage = {
          messageType: "user",
          content: content.trim(),
          metadata: {},
          media: []
        };

        const stream = apiClient.runAgentStream(
          activeThreadId,
          userMessageForApi,
          abortControllerRef.current.signal
        );

        let isFirstChunk = true;
        console.log('[Stream] Starting to process agent responses...');

        for await (const agentResponse of stream) {
          console.log('[Stream] Received agent response:', agentResponse);

          // Skip heartbeat messages
          if (agentResponse.node === "heartbeat") {
            console.log('[Stream] Skipping heartbeat message');
            continue;
          }

          // Use chunk for streaming updates if available
          if (agentResponse.chunk) {
            console.log('[Stream] Processing chunk:', agentResponse.chunk);

            // Defensive check: if message exists, verify it's an assistant message (streaming only for assistant)
            if (agentResponse.message) {
              if (agentResponse.message.messageType !== 'assistant') {
                console.warn(
                  '[Stream] Warning: chunk exists but message type is not assistant:',
                  agentResponse.message.messageType,
                  'This indicates a backend data format issue.'
                );
              }
            }

            if (isFirstChunk) {
              // Create new assistant message for first chunk
              const newAssistantMessage: UIMessage = {
                id: `assistant-${Date.now()}`,
                message: {
                  messageType: 'assistant',
                  content: agentResponse.chunk,
                  metadata: {},
                  toolCalls: []
                },
                timestamp: Date.now()
              };
              setMessages((prev) => {
                console.log('[Stream] Adding first chunk, prev messages:', prev.length);
                return [...prev, newAssistantMessage];
              });
              isFirstChunk = false;
            } else {
              // Append chunk to existing content - create completely new objects
              setMessages((prev) => {
                const newMessages = [...prev];
                const lastMessage = newMessages[newMessages.length - 1];
                newMessages[newMessages.length - 1] = {
                  ...lastMessage,
                  message: {
                    ...lastMessage.message,
                    content: lastMessage.message.content + agentResponse.chunk
                  }
                };
                console.log('[Stream] Updated message content length:', newMessages[newMessages.length - 1].message.content.length);
                return newMessages;
              });
            }
          } else if (agentResponse.message) {
            console.log('[Stream] Processing message:', agentResponse.message);

            // Convert from backend DTO to frontend Message type
            const backendMessage = fromMessageDTO(agentResponse.message);

            // Handle different message types appropriately
            // For streaming responses, we typically expect assistant messages with chunks
            // But tool-request, tool-confirm, and tool responses come as complete messages
            const messageType = agentResponse.message.messageType;

            if (messageType === 'assistant' || messageType === 'tool-request') {
              // Assistant and tool-request messages can be streamed or complete
              if (isFirstChunk) {
                const newMessage: UIMessage = {
                  id: `${messageType}-${Date.now()}`,
                  message: backendMessage,
                  timestamp: Date.now()
                };
                setMessages((prev) => {
                  console.log('[Stream] Adding first message, prev messages:', prev.length);
                  return [...prev, newMessage];
                });
                isFirstChunk = false;
              } else {
                setMessages((prev) => {
                  const newMessages = [...prev];
                  const lastMessage = newMessages[newMessages.length - 1];

                  // IMPORTANT: In streaming scenarios, preserve existing content if new content is empty
                  // This prevents accumulated content from being cleared by empty updates
                  const updatedContent = backendMessage.content || lastMessage.message.content;

                  newMessages[newMessages.length - 1] = {
                    ...lastMessage,
                    message: {
                      ...backendMessage,
                      content: updatedContent
                    }
                  };
                  console.log('[Stream] Updated message content:', updatedContent?.length, 'chars');
                  return newMessages;
                });
              }
            } else if (messageType === 'tool-confirm' || messageType === 'tool') {
              // Tool-confirm and tool response messages are always complete, add as new messages
              const newMessage: UIMessage = {
                id: `${messageType}-${Date.now()}`,
                message: backendMessage,
                timestamp: Date.now()
              };
              setMessages((prev) => [...prev, newMessage]);
              // Reset isFirstChunk for next potential assistant message
              isFirstChunk = true;
            } else {
              console.warn('[Stream] Unknown message type:', messageType);
            }
          } else {
            console.log('[Stream] No chunk or message in response');
          }
        }

        console.log('[Stream] Streaming complete');
        // Streaming complete - messages are already updated in state
        // Backend doesn't provide a separate API to fetch message history

      } catch (error: any) {
        if (error.name === "AbortError") {
          console.log("Request was aborted");
          toast.info("Request cancelled");
        } else {
          console.error("Failed to send message:", error);
          toast.error(error.message || "Failed to send message");
        }
      } finally {
        setIsStreaming(false);
        abortControllerRef.current = null;
      }
    },
    [currentThreadId, createThread]
  );

  const clearMessages = useCallback(() => {
    setMessages([]);
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
  }, []);

  const resumeFeedback = useCallback(
    async (toolFeedbacks: ToolFeedbackDTO[]) => {
      if (!currentThreadId) {
        toast.error("No active thread");
        return;
      }

      setIsStreaming(true);

      // Create new abort controller for this request
      abortControllerRef.current = new AbortController();

      try {
        const apiClient = createApiClient();

        // Start streaming with feedback
        const stream = apiClient.resumeAgentStream(
          currentThreadId,
          toolFeedbacks,
          abortControllerRef.current.signal
        );

        let isFirstChunk = true;
        console.log('[Stream] Starting to process resume agent responses...');

        for await (const agentResponse of stream) {
          console.log('[Stream] Received agent response:', agentResponse);

          // Skip heartbeat messages
          if (agentResponse.node === "heartbeat") {
            console.log('[Stream] Skipping heartbeat message');
            continue;
          }

          // Use chunk for streaming updates if available
          if (agentResponse.chunk) {
            console.log('[Stream] Processing chunk:', agentResponse.chunk);

            if (isFirstChunk) {
              // Create new assistant message for first chunk
              const newAssistantMessage: UIMessage = {
                id: `assistant-${Date.now()}`,
                message: {
                  messageType: 'assistant',
                  content: agentResponse.chunk,
                  metadata: {},
                  toolCalls: []
                },
                timestamp: Date.now()
              };
              setMessages((prev) => {
                console.log('[Stream] Adding first chunk, prev messages:', prev.length);
                return [...prev, newAssistantMessage];
              });
              isFirstChunk = false;
            } else {
              // Append chunk to existing content
              setMessages((prev) => {
                const newMessages = [...prev];
                const lastMessage = newMessages[newMessages.length - 1];
                newMessages[newMessages.length - 1] = {
                  ...lastMessage,
                  message: {
                    ...lastMessage.message,
                    content: lastMessage.message.content + agentResponse.chunk
                  }
                };
                return newMessages;
              });
            }
          } else if (agentResponse.message) {
            console.log('[Stream] Processing message:', agentResponse.message);

            const backendMessage = fromMessageDTO(agentResponse.message);
            const messageType = agentResponse.message.messageType;

            if (messageType === 'assistant' || messageType === 'tool-request') {
              if (isFirstChunk) {
                const newMessage: UIMessage = {
                  id: `${messageType}-${Date.now()}`,
                  message: backendMessage,
                  timestamp: Date.now()
                };
                setMessages((prev) => [...prev, newMessage]);
                isFirstChunk = false;
              } else {
                setMessages((prev) => {
                  const newMessages = [...prev];
                  const lastMessage = newMessages[newMessages.length - 1];
                  const updatedContent = backendMessage.content || lastMessage.message.content;
                  newMessages[newMessages.length - 1] = {
                    ...lastMessage,
                    message: {
                      ...backendMessage,
                      content: updatedContent
                    }
                  };
                  return newMessages;
                });
              }
            } else if (messageType === 'tool-confirm' || messageType === 'tool') {
              const newMessage: UIMessage = {
                id: `${messageType}-${Date.now()}`,
                message: backendMessage,
                timestamp: Date.now()
              };
              setMessages((prev) => [...prev, newMessage]);
              isFirstChunk = true;
            }
          }
        }

        console.log('[Stream] Resume streaming complete');
      } catch (error: any) {
        if (error.name === "AbortError") {
          console.log("Resume request was aborted");
          toast.info("Request cancelled");
        } else {
          console.error("Failed to resume with feedback:", error);
          toast.error(error.message || "Failed to resume execution");
        }
      } finally {
        setIsStreaming(false);
        abortControllerRef.current = null;
      }
    },
    [currentThreadId]
  );

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  const contextValue = useMemo(() => ({
    messages,
    isStreaming,
    sendMessage,
    resumeFeedback,
    clearMessages,
  }), [messages, isStreaming, sendMessage, resumeFeedback, clearMessages]);

  return (
    <StreamContext.Provider value={contextValue}>
      {children}
    </StreamContext.Provider>
  );
};

// Configuration component (optional, can be used for settings)
export const StreamConfigurationView = () => {
  const [apiUrl, setApiUrl] = useQueryState("apiUrl", {
    defaultValue: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
  });

  const [appName, setAppName] = useQueryState("appName", {
    defaultValue: process.env.NEXT_PUBLIC_APP_NAME || "research_agent",
  });

  const [userId, setUserId] = useQueryState("userId", {
    defaultValue: process.env.NEXT_PUBLIC_USER_ID || "user-001",
  });

  return (
    <div className="flex flex-col gap-4 p-4 border rounded-lg">
      <h3 className="text-lg font-semibold">Configuration</h3>

      <div className="flex flex-col gap-2">
        <Label htmlFor="apiUrl">API URL</Label>
        <div className="flex gap-2">
          <Input
            id="apiUrl"
            value={apiUrl || ""}
            onChange={(e) => setApiUrl(e.target.value)}
            placeholder="http://localhost:8080"
          />
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="appName">Application Name</Label>
        <div className="flex gap-2">
          <Input
            id="appName"
            value={appName || ""}
            onChange={(e) => setAppName(e.target.value)}
            placeholder="research_agent"
          />
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor="userId">User ID</Label>
        <div className="flex gap-2">
          <Input
            id="userId"
            value={userId || ""}
            onChange={(e) => setUserId(e.target.value)}
            placeholder="user-001"
          />
        </div>
      </div>

      <div className="text-sm text-muted-foreground">
        <p>Current configuration:</p>
        <ul className="list-disc list-inside">
          <li>API URL: {apiUrl}</li>
          <li>App Name: {appName}</li>
          <li>User ID: {userId}</li>
        </ul>
      </div>
    </div>
  );
};

