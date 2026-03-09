"use client";

import React, { useEffect, useState, useRef } from "react";
import { createApiClient } from "@/lib/spring-ai-api";
import { Skeleton } from "@/components/ui/skeleton";

interface GraphDiagramProps {
  graphName: string;
  className?: string;
}

export function GraphDiagram({ graphName, className = "" }: GraphDiagramProps) {
  const [mermaidSrc, setMermaidSrc] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
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
    const render = async () => {
      try {
        const mermaid = (await import("mermaid")).default;
        mermaid.initialize({ startOnLoad: false, theme: "neutral", securityLevel: "loose" });
        const id = `mermaid-graph-${graphName.replace(/\W/g, "_")}-${Date.now()}`;
        const { svg } = await mermaid.render(id, mermaidSrc);
        if (containerRef.current) containerRef.current.innerHTML = svg;
      } catch (e) {
        console.warn("Mermaid render failed:", e);
        if (containerRef.current) {
          containerRef.current.innerHTML = `<pre class="p-3 text-xs overflow-auto text-muted-foreground">${mermaidSrc.replace(/</g, "&lt;")}</pre>`;
        }
      }
    };
    render();
  }, [mermaidSrc, graphName]);

  if (loading) {
    return <Skeleton className={`h-48 w-full rounded-lg ${className}`} />;
  }
  if (error || !mermaidSrc) {
    return (
      <div className={`rounded-lg border border-dashed p-4 text-sm text-muted-foreground ${className}`}>
        {error || "No diagram available"}
      </div>
    );
  }
  return (
    <div
      ref={containerRef}
      className={`rounded-lg border bg-white overflow-auto min-h-[200px] [&_svg]:max-w-full [&_svg]:h-auto ${className}`}
    />
  );
}
