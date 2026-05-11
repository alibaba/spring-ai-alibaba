"use client";

import React, { useEffect, useState, useRef } from "react";
import { createApiClient } from "@/lib/spring-ai-api";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { ChevronDown, ChevronUp } from "lucide-react";

interface GraphVisualizationProps {
  graphName: string;
  className?: string;
}

/**
 * Fetches and displays the graph representation (Mermaid format).
 * Falls back to showing raw Mermaid source if diagram rendering is not available.
 */
export function GraphVisualization({ graphName, className = "" }: GraphVisualizationProps) {
  const [mermaidSrc, setMermaidSrc] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!graphName) {
      setMermaidSrc(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    createApiClient()
      .getGraphRepresentation(graphName)
      .then((res) => {
        const src = res.mermaidSrc || res.dotSrc || "";
        setMermaidSrc(src || null);
      })
      .catch((err) => {
        setError(err.message || "Failed to load graph");
        setMermaidSrc(null);
      })
      .finally(() => setLoading(false));
  }, [graphName]);

  useEffect(() => {
    if (!mermaidSrc || !containerRef.current) return;
    const renderMermaid = async () => {
      try {
        const mermaid = (await import("mermaid")).default;
        mermaid.initialize({
          startOnLoad: false,
          theme: "neutral",
          securityLevel: "loose",
        });
        const id = `mermaid-${graphName.replace(/\W/g, "_")}-${Date.now()}`;
        const { svg } = await mermaid.render(id, mermaidSrc);
        if (containerRef.current) {
          containerRef.current.innerHTML = svg;
        }
      } catch (e) {
        console.warn("Mermaid render failed, showing source:", e);
        if (containerRef.current) {
          containerRef.current.innerHTML = `<pre class="p-4 text-sm overflow-auto"><code>${mermaidSrc.replace(/</g, "&lt;")}</code></pre>`;
        }
      }
    };
    renderMermaid();
  }, [mermaidSrc, graphName]);

  if (loading) {
    return (
      <div className={className}>
        <Skeleton className="h-32 w-full rounded-lg" />
      </div>
    );
  }

  if (error || !mermaidSrc) {
    return (
      <div className={`rounded-lg border border-dashed p-4 text-sm text-muted-foreground ${className}`}>
        {error || "No graph representation available"}
      </div>
    );
  }

  return (
    <div className={`rounded-lg border bg-muted/30 overflow-hidden ${className}`}>
      <Button
        variant="ghost"
        size="sm"
        className="w-full justify-between rounded-none"
        onClick={() => setExpanded((p) => !p)}
      >
        <span className="text-sm font-medium">Graph: {graphName}</span>
        {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
      </Button>
      {expanded && (
        <div
          ref={containerRef}
          className="min-h-[120px] p-4 overflow-auto [&_svg]:max-w-full [&_svg]:h-auto"
        />
      )}
    </div>
  );
}
