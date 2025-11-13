import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  ReactNode,
  useEffect,
  useMemo
} from 'react';
import { createApiClient, Session } from '@/lib/spring-ai-api';
import { toast } from 'sonner';

interface ThreadContextType {
  threads: Session[];
  currentThreadId: string | null;
  setCurrentThreadId: (threadId: string | null) => void;
  loadThreads: () => Promise<void>;
  createThread: () => Promise<Session | null>;
  deleteThread: (threadId: string) => Promise<void>;
  appName: string;
  userId: string;
  isLoading: boolean;
  isNewlyCreatedThread: (threadId: string) => boolean;
}

const ThreadContext = createContext<ThreadContextType | undefined>(undefined);

export function useThreads() {
  const context = useContext(ThreadContext);
  if (!context) {
    throw new Error('useThreads must be used within a ThreadProvider');
  }
  return context;
}

interface ThreadProviderProps {
  children: ReactNode;
}

export function ThreadProvider({ children }: ThreadProviderProps) {
  const [threads, setThreads] = useState<Session[]>([]);
  const [currentThreadId, setCurrentThreadId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [newlyCreatedThreadIds, setNewlyCreatedThreadIds] = useState<Set<string>>(new Set());

  const appName = process.env.NEXT_PUBLIC_APP_NAME || 'research_agent';
  const userId = process.env.NEXT_PUBLIC_USER_ID || 'user-001';

  const isNewlyCreatedThread = useCallback((threadId: string) => {
    return newlyCreatedThreadIds.has(threadId);
  }, [newlyCreatedThreadIds]);

  const loadThreads = useCallback(async () => {
    setIsLoading(true);
    try {
      const apiClient = createApiClient();
      const sessions = await apiClient.listSessions(appName, userId);

      // Sort by lastUpdateTime descending
      const sortedSessions = sessions.sort(
        (a, b) => a.thread_id.localeCompare(b.thread_id)
      );

      setThreads(sortedSessions);

      // Note: We no longer automatically select the first thread
      // Users should explicitly click on a thread to load it
    } catch (error: any) {
      console.error('Failed to load threads:', error);
      toast.error('Failed to load threads: ' + (error.message || 'Unknown error'));
    } finally {
      setIsLoading(false);
    }
  }, [appName, userId]);

  const createThread = useCallback(async (): Promise<Session | null> => {
    try {
      const apiClient = createApiClient();
      // Create session with empty initial state
      const newSession = await apiClient.createSession(appName, userId, {});

      // Mark this thread as newly created
      setNewlyCreatedThreadIds((prev) => new Set(prev).add(newSession.thread_id));

      // Add to threads list
      setThreads((prev) => [newSession, ...prev]);

      // Set as current thread
      setCurrentThreadId(newSession.thread_id);

      toast.success('New thread created');
      return newSession;
    } catch (error: any) {
      console.error('Failed to create thread:', error);
      toast.error('Failed to create thread: ' + (error.message || 'Unknown error'));
      return null;
    }
  }, [appName, userId]);

  const deleteThread = useCallback(async (threadId: string) => {
    try {
      const apiClient = createApiClient();
      await apiClient.deleteSession(appName, userId, threadId);

      // Remove from threads list
      setThreads((prev) => prev.filter((t) => t.thread_id !== threadId));

      // If deleted thread was current, clear current thread
      if (currentThreadId === threadId) {
        setCurrentThreadId(null);
      }

      toast.success('Thread deleted');
    } catch (error: any) {
      console.error('Failed to delete thread:', error);
      toast.error('Failed to delete thread: ' + (error.message || 'Unknown error'));
    }
  }, [appName, userId, currentThreadId]);

  // Note: Threads are loaded on-demand when chat history is opened or when a threadId exists
  // See components/thread/history/index.tsx for the loading logic

  const contextValue = useMemo(() => ({
    threads,
    currentThreadId,
    setCurrentThreadId,
    loadThreads,
    createThread,
    deleteThread,
    appName,
    userId,
    isLoading,
    isNewlyCreatedThread,
  }), [threads, currentThreadId, setCurrentThreadId, loadThreads, createThread, deleteThread, appName, userId, isLoading, isNewlyCreatedThread]);

  return (
    <ThreadContext.Provider value={contextValue}>
      {children}
    </ThreadContext.Provider>
  );
}

