"use client";

import React, { useState, FormEvent, useMemo } from "react";
import { useGraphStream } from "@/providers/GraphStream";
import { useGraphThreads } from "@/providers/GraphThread";
import { Button } from "@/components/ui/button";

function extractTextFromState(state: Record<string, unknown> | null): string {
  if (!state?.messages || !Array.isArray(state.messages)) return "";
  const messages = state.messages as Array<{ content?: string; text?: string }>;
  const last = messages[messages.length - 1];
  return last?.content ?? last?.text ?? "";
}

export function GraphChatArea() {
  const { sendMessage, accumulatedChunk, currentState, nodeOutputs, isStreaming } = useGraphStream();
  const { currentThreadId, createThread } = useGraphThreads();
  const [input, setInput] = useState("");

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isStreaming) return;
    let threadId = currentThreadId;
    if (!threadId) {
      const session = await createThread();
      if (!session) return;
      threadId = session.thread_id;
    }
    await sendMessage(input.trim(), threadId);
    setInput("");
  };

  const displayText = useMemo(() => {
    if (accumulatedChunk) return accumulatedChunk;
    const fromState = extractTextFromState(currentState);
    if (fromState) return fromState;
    const lastWithMessage = [...nodeOutputs].reverse().find((o) => o.message?.content);
    return lastWithMessage?.message?.content ?? "";
  }, [accumulatedChunk, currentState, nodeOutputs]);

  return (
    <div className="flex flex-col gap-2">
      <form onSubmit={handleSubmit} className="flex gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Type a message..."
          disabled={isStreaming}
          className="flex-1 min-w-0 rounded-md border border-input bg-background px-2 py-1.5 text-sm"
        />
        <Button type="submit" size="sm" disabled={isStreaming || !input.trim()}>
          {isStreaming ? "..." : "Run"}
        </Button>
      </form>
      {displayText && (
        <div className="text-xs text-muted-foreground line-clamp-2 truncate" title={displayText}>
          {displayText}
        </div>
      )}
    </div>
  );
}
