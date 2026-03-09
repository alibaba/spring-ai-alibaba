"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Plus, PanelLeftClose, PanelLeftOpen } from "lucide-react";
import { GraphDiagram } from "./GraphDiagram";
import { NodeTimeline } from "./NodeTimeline";
import { StateInspector } from "./StateInspector";
import { GraphChatArea } from "./GraphChatArea";
import { useGraphThreads } from "@/providers/GraphThread";

export function GraphWorkspace() {
  const { graphName, threads, currentThreadId, setCurrentThreadId, createThread, isLoading } =
    useGraphThreads();
  const [diagramOpen, setDiagramOpen] = useState(true);

  if (!graphName) return null;

  return (
    <div className="flex h-screen flex-col bg-slate-50">
      <header className="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4">
        <div className="flex items-center gap-3">
          <Link href="/index.html">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back
            </Button>
          </Link>
          <span className="text-lg font-semibold">Graph: {graphName}</span>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => createThread()}
            disabled={isLoading}
          >
            <Plus className="mr-2 h-4 w-4" />
            New thread
          </Button>
          {threads.length > 0 && (
            <select
              value={currentThreadId ?? ""}
              onChange={(e) => setCurrentThreadId(e.target.value || null)}
              className="rounded-md border border-input bg-background px-2 py-1 text-sm"
            >
              <option value="">Select thread...</option>
              {threads.map((t) => (
                <option key={t.thread_id} value={t.thread_id}>
                  {t.thread_id.slice(0, 8)}...
                </option>
              ))}
            </select>
          )}
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {diagramOpen && (
          <aside className="w-80 shrink-0 border-r bg-white overflow-y-auto flex flex-col">
            <div className="flex items-center justify-between p-2 border-b">
              <span className="text-sm font-medium">Graph Diagram</span>
              <Button variant="ghost" size="sm" onClick={() => setDiagramOpen(false)}>
                <PanelLeftClose className="h-4 w-4" />
              </Button>
            </div>
            <div className="flex-1 min-h-0 overflow-auto p-3">
              <GraphDiagram graphName={graphName} />
            </div>
            <div className="p-3 border-t bg-muted/20">
              <GraphChatArea />
            </div>
          </aside>
        )}
        {!diagramOpen && (
          <Button
            variant="outline"
            size="sm"
            className="absolute left-2 top-20 z-10"
            onClick={() => setDiagramOpen(true)}
          >
            <PanelLeftOpen className="h-4 w-4" />
          </Button>
        )}

        <main className="flex-1 min-w-0 overflow-hidden flex flex-col p-4 gap-4">
          <div className="shrink-0">
            <NodeTimeline />
          </div>
          <div className="flex-1 min-h-0 flex flex-col overflow-hidden">
            <StateInspector />
          </div>
        </main>
      </div>
    </div>
  );
}
