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
  /** List of available agent names from backend (list-apps). */
  agentList: string[];
  /** Currently selected agent (appName). Defaults to first in agentList. */
  selectedAgent: string;
  setSelectedAgent: (agent: string) => void;
  isLoading: boolean;
  isAgentsLoading: boolean;
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
  const [agentList, setAgentList] = useState<string[]>([]);
  const [selectedAgent, setSelectedAgentState] = useState<string>('');
  const [isAgentsLoading, setIsAgentsLoading] = useState(true);

  const userId = process.env.NEXT_PUBLIC_USER_ID || 'user-001';
  const appName = selectedAgent || '';

  // Fetch available agents on mount and default to first
  useEffect(() => {
    let cancelled = false;
    setIsAgentsLoading(true);
    createApiClient()
      .listApps()
      .then((names) => {
        if (cancelled) return;
        const sorted = [...names].sort();
        setAgentList(sorted);
        setSelectedAgentState((prev) => {
          if (sorted.length === 0) return prev;
          if (!prev || !sorted.includes(prev)) return sorted[0];
          return prev;
        });
      })
      .catch((err) => {
        if (!cancelled) {
          console.error('Failed to fetch agent list:', err);
          setAgentList([]);
        }
      })
      .finally(() => {
        if (!cancelled) setIsAgentsLoading(false);
      });
    return () => { cancelled = true; };
  }, []);

  const setSelectedAgent = useCallback((agent: string) => {
    setSelectedAgentState(agent);
    setCurrentThreadId(null);
  }, []);

  const isNewlyCreatedThread = useCallback((threadId: string) => {
    return newlyCreatedThreadIds.has(threadId);
  }, [newlyCreatedThreadIds]);

  const loadThreads = useCallback(async () => {
    if (!appName) return;
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

  // When selected agent changes, clear current thread and reload threads for the new agent
  useEffect(() => {
    if (selectedAgent) {
      setCurrentThreadId(null);
      loadThreads();
    }
  }, [selectedAgent, loadThreads]);

  const contextValue = useMemo(() => ({
    threads,
    currentThreadId,
    setCurrentThreadId,
    loadThreads,
    createThread,
    deleteThread,
    appName,
    userId,
    agentList,
    selectedAgent,
    setSelectedAgent,
    isLoading,
    isAgentsLoading,
    isNewlyCreatedThread,
  }), [threads, currentThreadId, setCurrentThreadId, loadThreads, createThread, deleteThread, appName, userId, agentList, selectedAgent, setSelectedAgent, isLoading, isAgentsLoading, isNewlyCreatedThread]);

  return (
    <ThreadContext.Provider value={contextValue}>
      {children}
    </ThreadContext.Provider>
  );
}

