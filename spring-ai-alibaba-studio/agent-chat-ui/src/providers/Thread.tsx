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

export type Mode = 'agent' | 'graph';

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
  /** Mode: agent or graph. When graph, use graph APIs. */
  mode: Mode;
  setMode: (mode: Mode) => void;
  /** List of available graph names from backend (list-graphs). */
  graphList: string[];
  /** Currently selected graph (when mode=graph). */
  selectedGraph: string;
  setSelectedGraph: (graph: string) => void;
  isGraphsLoading: boolean;
  isLoading: boolean;
  isAgentsLoading: boolean;
  isNewlyCreatedThread: (threadId: string) => boolean;
  /** When true, mode/agent/graph are fixed from URL - hide selectors and show back link. */
  isLocked: boolean;
}

const ThreadContext = createContext<ThreadContextType | undefined>(undefined);

export function useThreads() {
  const context = useContext(ThreadContext);
  if (!context) {
    throw new Error('useThreads must be used within a ThreadProvider');
  }
  return context;
}

export type LockedMode = { mode: 'agent'; agent: string } | { mode: 'graph'; graph: string };

interface ThreadProviderProps {
  children: ReactNode;
  /** When set, mode and selection are fixed (e.g. from URL). Hides selectors. */
  initialLock?: LockedMode;
}

export function ThreadProvider({ children, initialLock }: ThreadProviderProps) {
  const [threads, setThreads] = useState<Session[]>([]);
  const [currentThreadId, setCurrentThreadId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [newlyCreatedThreadIds, setNewlyCreatedThreadIds] = useState<Set<string>>(new Set());
  const [agentList, setAgentList] = useState<string[]>([]);
  const [selectedAgent, setSelectedAgentState] = useState<string>(
    initialLock?.mode === 'agent' ? initialLock.agent : ''
  );
  const [isAgentsLoading, setIsAgentsLoading] = useState(!initialLock);
  const [mode, setModeState] = useState<Mode>(
    initialLock?.mode === 'graph' ? 'graph' : 'agent'
  );
  const [graphList, setGraphList] = useState<string[]>([]);
  const [selectedGraph, setSelectedGraphState] = useState<string>(
    initialLock?.mode === 'graph' ? initialLock.graph : ''
  );
  const [isGraphsLoading, setIsGraphsLoading] = useState(!initialLock);

  const isLocked = !!initialLock;
  const userId = process.env.NEXT_PUBLIC_USER_ID || 'user-001';
  const appName = mode === 'agent' ? selectedAgent : (selectedGraph ? `graph:${selectedGraph}` : '');

  // Fetch available agents on mount (skip when locked - we already have the agent/graph)
  useEffect(() => {
    if (initialLock) return;
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
  }, [initialLock]);

  // Fetch available graphs on mount (graceful if backend has no graph support)
  useEffect(() => {
    if (initialLock) return;
    let cancelled = false;
    setIsGraphsLoading(true);
    createApiClient()
      .listGraphs()
      .then((names) => {
        if (cancelled) return;
        const sorted = [...(names || [])].sort();
        setGraphList(sorted);
        setSelectedGraphState((prev) => {
          if (sorted.length === 0) return prev;
          if (!prev || !sorted.includes(prev)) return sorted[0];
          return prev;
        });
      })
      .catch((err) => {
        if (!cancelled) {
          console.warn('Graph list not available (may not be configured):', err.message);
          setGraphList([]);
        }
      })
      .finally(() => {
        if (!cancelled) setIsGraphsLoading(false);
      });
    return () => { cancelled = true; };
  }, [initialLock]);

  const setSelectedAgent = useCallback((agent: string) => {
    if (isLocked) return;
    setSelectedAgentState(agent);
    setCurrentThreadId(null);
  }, [isLocked]);

  const setMode = useCallback((m: Mode) => {
    if (isLocked) return;
    setModeState(m);
    setCurrentThreadId(null);
  }, [isLocked]);

  const setSelectedGraph = useCallback((graph: string) => {
    if (isLocked) return;
    setSelectedGraphState(graph);
    setCurrentThreadId(null);
  }, [isLocked]);

  const isNewlyCreatedThread = useCallback((threadId: string) => {
    return newlyCreatedThreadIds.has(threadId);
  }, [newlyCreatedThreadIds]);

  const loadThreads = useCallback(async () => {
    if (mode === 'agent' && !selectedAgent) return;
    if (mode === 'graph' && !selectedGraph) return;
    setIsLoading(true);
    try {
      const apiClient = createApiClient();
      const sessions = mode === 'graph'
        ? await apiClient.listGraphSessions(selectedGraph, userId)
        : await apiClient.listSessions(selectedAgent, userId);

      const sortedSessions = sessions.sort(
        (a, b) => a.thread_id.localeCompare(b.thread_id)
      );

      setThreads(sortedSessions);
    } catch (error: any) {
      console.error('Failed to load threads:', error);
      toast.error('Failed to load threads: ' + (error.message || 'Unknown error'));
    } finally {
      setIsLoading(false);
    }
  }, [mode, selectedAgent, selectedGraph, userId]);

  const createThread = useCallback(async (): Promise<Session | null> => {
    try {
      const apiClient = createApiClient();
      const newSession = mode === 'graph'
        ? await apiClient.createGraphSession(selectedGraph, userId, {})
        : await apiClient.createSession(selectedAgent, userId, {});

      setNewlyCreatedThreadIds((prev) => new Set(prev).add(newSession.thread_id));
      setThreads((prev) => [newSession, ...prev]);
      setCurrentThreadId(newSession.thread_id);

      toast.success('New thread created');
      return newSession;
    } catch (error: any) {
      console.error('Failed to create thread:', error);
      toast.error('Failed to create thread: ' + (error.message || 'Unknown error'));
      return null;
    }
  }, [mode, selectedAgent, selectedGraph, userId]);

  const deleteThread = useCallback(async (threadId: string) => {
    try {
      const apiClient = createApiClient();
      if (mode === 'graph') {
        await apiClient.deleteGraphSession(selectedGraph, userId, threadId);
      } else {
        await apiClient.deleteSession(selectedAgent, userId, threadId);
      }

      setThreads((prev) => prev.filter((t) => t.thread_id !== threadId));

      if (currentThreadId === threadId) {
        setCurrentThreadId(null);
      }

      toast.success('Thread deleted');
    } catch (error: any) {
      console.error('Failed to delete thread:', error);
      toast.error('Failed to delete thread: ' + (error.message || 'Unknown error'));
    }
  }, [mode, selectedAgent, selectedGraph, userId, currentThreadId]);

  // When selected agent/graph changes, reload threads
  useEffect(() => {
    if (mode === 'agent' && selectedAgent) {
      setCurrentThreadId(null);
      loadThreads();
    } else if (mode === 'graph' && selectedGraph) {
      setCurrentThreadId(null);
      loadThreads();
    }
  }, [mode, selectedAgent, selectedGraph, loadThreads]);

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
    mode,
    setMode,
    graphList,
    selectedGraph,
    setSelectedGraph,
    isGraphsLoading,
    isLoading,
    isAgentsLoading,
    isNewlyCreatedThread,
    isLocked,
  }), [threads, currentThreadId, setCurrentThreadId, loadThreads, createThread, deleteThread, appName, userId, agentList, selectedAgent, setSelectedAgent, mode, setMode, graphList, selectedGraph, setSelectedGraph, isGraphsLoading, isLoading, isAgentsLoading, isNewlyCreatedThread, isLocked]);

  return (
    <ThreadContext.Provider value={contextValue}>
      {children}
    </ThreadContext.Provider>
  );
}

