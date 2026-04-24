"use client";

import React, { useState } from "react";
import { useGraphStream } from "@/providers/GraphStream";

export function StateInspector() {
  const { currentState, nodeOutputs, selectedNodeIndex } = useGraphStream();
  const [expandedKeys, setExpandedKeys] = useState<Set<string>>(new Set());

  const selectedOutput = selectedNodeIndex != null ? nodeOutputs[selectedNodeIndex] : null;
  const stateToShow =
    (selectedOutput?.state && Object.keys(selectedOutput.state).length > 0
      ? selectedOutput.state
      : null) ??
    currentState ??
    (nodeOutputs.length > 0 ? (nodeOutputs[nodeOutputs.length - 1] as { state?: Record<string, unknown> }).state : null) ??
    null;

  const title =
    selectedOutput != null
      ? `State after ${selectedOutput.node}`
      : "Current State";

  if (!stateToShow || Object.keys(stateToShow).length === 0) {
    return (
      <div className="rounded-lg border bg-muted/30 p-4 text-sm text-muted-foreground">
        {nodeOutputs.length === 0
          ? "State will appear here as the graph runs."
          : "Click a node in the timeline to view its state."}
      </div>
    );
  }

  const toggle = (key: string) => {
    setExpandedKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const renderValue = (key: string, value: unknown): React.ReactNode => {
    if (value === null || value === undefined) return "null";
    if (typeof value === "string") return <span className="text-green-700">"{value}"</span>;
    if (typeof value === "number" || typeof value === "boolean") return <span className="text-blue-600">{String(value)}</span>;
    if (Array.isArray(value)) {
      const isExpanded = expandedKeys.has(key);
      return (
        <div className="pl-2">
          <button
            type="button"
            onClick={() => toggle(key)}
            className="text-muted-foreground hover:text-foreground"
          >
            {isExpanded ? "▼" : "▶"} Array({value.length})
          </button>
          {isExpanded && (
            <pre className="mt-1 text-xs overflow-auto max-h-32 bg-muted/30 p-2 rounded">
              {JSON.stringify(value, null, 2)}
            </pre>
          )}
        </div>
      );
    }
    if (typeof value === "object") {
      const isExpanded = expandedKeys.has(key);
      return (
        <div className="pl-2">
          <button
            type="button"
            onClick={() => toggle(key)}
            className="text-muted-foreground hover:text-foreground"
          >
            {isExpanded ? "▼" : "▶"} Object
          </button>
          {isExpanded && (
            <pre className="mt-1 text-xs overflow-auto max-h-48 bg-muted/30 p-2 rounded">
              {JSON.stringify(value, null, 2)}
            </pre>
          )}
        </div>
      );
    }
    return String(value);
  };

  return (
    <div className="rounded-lg border bg-white overflow-hidden flex-1 min-h-0 flex flex-col">
      <div className="border-b px-3 py-2 text-sm font-medium bg-muted/50 shrink-0">
        {title}
      </div>
      <div className="p-3 text-sm space-y-2 flex-1 min-h-0 overflow-y-auto">
        {Object.entries(stateToShow).map(([key, value]) => (
          <div key={key} className="break-all">
            <span className="font-mono text-primary">{key}</span>
            <span className="text-muted-foreground">: </span>
            {renderValue(key, value)}
          </div>
        ))}
      </div>
    </div>
  );
}
