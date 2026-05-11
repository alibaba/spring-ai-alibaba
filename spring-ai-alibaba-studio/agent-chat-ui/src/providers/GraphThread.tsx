"use client";

import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
  useMemo,
  ReactNode,
} from "react";
import { createApiClient, Session } from "@/lib/spring-ai-api";
import { toast } from "sonner";

interface GraphThreadContextType {
  graphName: string;
  userId: string;
  threads: Session[];
  currentThreadId: string | null;
  setCurrentThreadId: (id: string | null) => void;
  loadThreads: () => Promise<void>;
  createThread: () => Promise<Session | null>;
  deleteThread: (threadId: string) => Promise<void>;
  isLoading: boolean;
}

const GraphThreadContext = createContext<GraphThreadContextType | undefined>(undefined);

export function useGraphThreads() {
  const ctx = useContext(GraphThreadContext);
  if (!ctx) throw new Error("useGraphThreads must be used within GraphThreadProvider");
  return ctx;
}

interface GraphThreadProviderProps {
  children: ReactNode;
  graphName: string;
}

export function GraphThreadProvider({ children, graphName }: GraphThreadProviderProps) {
  const [threads, setThreads] = useState<Session[]>([]);
  const [currentThreadId, setCurrentThreadId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const userId = process.env.NEXT_PUBLIC_USER_ID || "user-001";

  const loadThreads = useCallback(async () => {
    if (!graphName) return;
    setIsLoading(true);
    try {
      const api = createApiClient();
      const sessions = await api.listGraphSessions(graphName, userId);
      setThreads(sessions.sort((a, b) => a.thread_id.localeCompare(b.thread_id)));
    } catch (err: unknown) {
      console.error("Failed to load graph threads:", err);
      toast.error("Failed to load threads");
      setThreads([]);
    } finally {
      setIsLoading(false);
    }
  }, [graphName, userId]);

  const createThread = useCallback(async (): Promise<Session | null> => {
    if (!graphName) return null;
    try {
      const api = createApiClient();
      const session = await api.createGraphSession(graphName, userId, {});
      setThreads((prev) => [session, ...prev]);
      setCurrentThreadId(session.thread_id);
      toast.success("New thread created");
      return session;
    } catch (err: unknown) {
      console.error("Failed to create graph thread:", err);
      toast.error("Failed to create thread");
      return null;
    }
  }, [graphName, userId]);

  const deleteThread = useCallback(
    async (threadId: string) => {
      if (!graphName) return;
      try {
        const api = createApiClient();
        await api.deleteGraphSession(graphName, userId, threadId);
        setThreads((prev) => prev.filter((t) => t.thread_id !== threadId));
        if (currentThreadId === threadId) setCurrentThreadId(null);
        toast.success("Thread deleted");
      } catch (err: unknown) {
        console.error("Failed to delete graph thread:", err);
        toast.error("Failed to delete thread");
      }
    },
    [graphName, userId, currentThreadId]
  );

  useEffect(() => {
    if (graphName) loadThreads();
  }, [graphName, loadThreads]);

  const value = useMemo(
    () => ({
      graphName,
      userId,
      threads,
      currentThreadId,
      setCurrentThreadId,
      loadThreads,
      createThread,
      deleteThread,
      isLoading,
    }),
    [
      graphName,
      userId,
      threads,
      currentThreadId,
      loadThreads,
      createThread,
      deleteThread,
      isLoading,
    ]
  );

  return (
    <GraphThreadContext.Provider value={value}>{children}</GraphThreadContext.Provider>
  );
}
