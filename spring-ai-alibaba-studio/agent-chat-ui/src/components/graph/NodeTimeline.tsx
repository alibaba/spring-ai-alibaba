"use client";

import React from "react";
import { useGraphStream } from "@/providers/GraphStream";
import { cn } from "@/lib/utils";

export function NodeTimeline() {
  const { nodeOutputs, selectedNodeIndex, setSelectedNodeIndex } = useGraphStream();

  if (nodeOutputs.length === 0) {
    return (
      <div className="rounded-lg border bg-muted/30 p-4 text-sm text-muted-foreground">
        No nodes executed yet. Send a message to run the graph.
      </div>
    );
  }

  return (
    <div className="rounded-lg border bg-white overflow-hidden">
      <div className="border-b px-3 py-2 text-sm font-medium bg-muted/50">
        Execution Timeline
      </div>
      <ul className="divide-y divide-border max-h-[280px] overflow-y-auto">
        {nodeOutputs.map((out, i) => {
          const hasState = out.state && Object.keys(out.state).length > 0;
          const hasOutput = !!(out.chunk || out.message?.content);
          const isSelected = selectedNodeIndex === i;
          return (
            <li
              key={`${out.node}-${i}`}
              role="button"
              tabIndex={0}
              onClick={() => setSelectedNodeIndex(isSelected ? null : i)}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault();
                  setSelectedNodeIndex(isSelected ? null : i);
                }
              }}
              className={cn(
                "px-3 py-2.5 text-sm flex flex-col gap-1 cursor-pointer transition-colors",
                isSelected && "bg-primary/10 border-l-2 border-l-primary",
                !isSelected && "hover:bg-muted/50",
                (out.node === "__start__" || out.node === "START") && "text-muted-foreground"
              )}
            >
              <div className="flex items-center gap-2">
                <span
                  className={cn(
                    "shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-xs font-medium",
                    isSelected ? "bg-primary text-primary-foreground" : "bg-primary/10 text-primary"
                  )}
                >
                  {out.index + 1}
                </span>
                <span className="font-mono font-medium">{out.node}</span>
                {out.agent && (
                  <span className="text-muted-foreground text-xs">({out.agent})</span>
                )}
              </div>
              {(hasOutput || hasState) && (
                <div className="flex gap-2 pl-8 text-xs text-muted-foreground">
                  {hasState && <span>state</span>}
                  {hasOutput && (
                    <span className="truncate">
                      {(() => {
                        const t = (out.chunk || out.message?.content) ?? "";
                        return t.length > 40 ? t.slice(0, 40) + "…" : t;
                      })()}
                    </span>
                  )}
                </div>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
