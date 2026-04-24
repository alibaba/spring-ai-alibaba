"use client";

import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
  useRef,
  useMemo,
  ReactNode,
} from "react";
import { createApiClient, GraphRunResponse } from "@/lib/spring-ai-api";
import { toast } from "sonner";
import { useGraphThreads } from "./GraphThread";

export interface GraphNodeOutput extends GraphRunResponse {
  /** Index in execution order */
  index: number;
}

interface GraphStreamContextType {
  /** All node outputs from the current run (execution timeline) */
  nodeOutputs: GraphNodeOutput[];
  /** Latest overall state after last node */
  currentState: Record<string, unknown> | null;
  /** Accumulated text from chunks (for display) */
  accumulatedChunk: string;
  isStreaming: boolean;
  /** Selected node index in timeline; when set, StateInspector shows that node's state */
  selectedNodeIndex: number | null;
  setSelectedNodeIndex: (index: number | null) => void;
  sendMessage: (content: string, threadIdOverride?: string) => Promise<void>;
  clearRun: () => void;
}

const GraphStreamContext = createContext<GraphStreamContextType | undefined>(undefined);

export function useGraphStream() {
  const ctx = useContext(GraphStreamContext);
  if (!ctx) throw new Error("useGraphStream must be used within GraphStreamProvider");
  return ctx;
}

interface GraphStreamProviderProps {
  children: ReactNode;
}

export function GraphStreamProvider({ children }: GraphStreamProviderProps) {
  const { graphName, userId, currentThreadId } = useGraphThreads();
  const [nodeOutputs, setNodeOutputs] = useState<GraphNodeOutput[]>([]);
  const [currentState, setCurrentState] = useState<Record<string, unknown> | null>(null);
  const [accumulatedChunk, setAccumulatedChunk] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);
  const [selectedNodeIndex, setSelectedNodeIndex] = useState<number | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const clearRun = useCallback(() => {
    setNodeOutputs([]);
    setCurrentState(null);
    setAccumulatedChunk("");
    setSelectedNodeIndex(null);
  }, []);

  useEffect(() => {
    if (!currentThreadId) clearRun();
  }, [currentThreadId, clearRun]);

  const sendMessage = useCallback(
    async (content: string, threadIdOverride?: string) => {
      if (!content.trim()) return;
      const activeThreadId = threadIdOverride ?? currentThreadId;
      if (!graphName || !activeThreadId) {
        toast.error("No thread selected. Create a new thread first.");
        return;
      }

      abortRef.current?.abort();
      abortRef.current = new AbortController();
      clearRun();
      setIsStreaming(true);

      let index = 0;
      try {
        const api = createApiClient();
        for await (const event of api.runGraphStream(
          graphName,
          userId,
          activeThreadId,
          { messageType: "user", content: content.trim() },
          abortRef.current.signal
        )) {
          setNodeOutputs((prev) => [
            ...prev,
            { ...event, index: index++ } as GraphNodeOutput,
          ]);
          if (event.state && Object.keys(event.state).length > 0) {
            setCurrentState(event.state as Record<string, unknown>);
          }
          const text = event.chunk ?? (event.message?.content ?? "");
          if (text) {
            setAccumulatedChunk((prev) => prev + text);
          }
        }
      } catch (err: unknown) {
        if ((err as Error)?.name === "AbortError") return;
        console.error("Graph stream error:", err);
        toast.error("Graph run failed");
      } finally {
        setIsStreaming(false);
        abortRef.current = null;
      }
    },
    [graphName, userId, currentThreadId, clearRun]
  );

  const value = useMemo(
    () => ({
      nodeOutputs,
      currentState,
      accumulatedChunk,
      isStreaming,
      selectedNodeIndex,
      setSelectedNodeIndex,
      sendMessage,
      clearRun,
    }),
    [nodeOutputs, currentState, accumulatedChunk, isStreaming, selectedNodeIndex, sendMessage, clearRun]
  );

  return (
    <GraphStreamContext.Provider value={value}>{children}</GraphStreamContext.Provider>
  );
}
