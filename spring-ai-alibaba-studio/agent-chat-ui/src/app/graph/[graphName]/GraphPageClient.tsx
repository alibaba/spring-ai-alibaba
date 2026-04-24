"use client";

import React from "react";
import { Toaster } from "@/components/ui/sonner";
import { GraphThreadProvider } from "@/providers/GraphThread";
import { GraphStreamProvider } from "@/providers/GraphStream";
import { GraphWorkspace } from "@/components/graph/GraphWorkspace";

interface GraphPageClientProps {
  graphName: string;
}

export function GraphPageClient({ graphName }: GraphPageClientProps) {
  if (!graphName) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-muted-foreground">Invalid graph</p>
      </div>
    );
  }

  return (
    <>
      <Toaster />
      <GraphThreadProvider graphName={graphName}>
        <GraphStreamProvider>
          <GraphWorkspace />
        </GraphStreamProvider>
      </GraphThreadProvider>
    </>
  );
}
