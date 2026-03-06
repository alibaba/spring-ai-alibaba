"use client";

import React from "react";
import { Thread } from "@/components/thread";
import { StreamProvider } from "@/providers/Stream";
import { ThreadProvider } from "@/providers/Thread";
import { ArtifactProvider } from "@/components/thread/artifact";
import { Toaster } from "@/components/ui/sonner";

interface AgentPageClientProps {
  agentName: string;
}

export function AgentPageClient({ agentName }: AgentPageClientProps) {
  if (!agentName) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-muted-foreground">Invalid agent</p>
      </div>
    );
  }

  return (
    <React.Suspense fallback={<div>Loading...</div>}>
      <Toaster />
      <ThreadProvider initialLock={{ mode: "agent", agent: agentName }}>
        <StreamProvider>
          <ArtifactProvider>
            <Thread />
          </ArtifactProvider>
        </StreamProvider>
      </ThreadProvider>
    </React.Suspense>
  );
}
