"use client";

import React, { useEffect, useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { createApiClient } from "@/lib/spring-ai-api";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Bot, GitBranch, Loader2 } from "lucide-react";
import { GitHubSVG } from "@/components/icons/github";
import { AgentPageClient } from "@/app/agent/[agentName]/AgentPageClient";
import { GraphPageClient } from "@/app/graph/[graphName]/GraphPageClient";

function SelectionPageContent(): React.ReactNode {
  const searchParams = useSearchParams();
  const agentParam = searchParams.get("agent");
  const graphParam = searchParams.get("graph");

  const [agentList, setAgentList] = useState<string[]>([]);
  const [graphList, setGraphList] = useState<string[]>([]);
  const [agentsLoading, setAgentsLoading] = useState(true);
  const [graphsLoading, setGraphsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setAgentsLoading(true);
    createApiClient()
      .listApps()
      .then((names) => {
        if (cancelled) return;
        setAgentList([...names].sort());
      })
      .catch((err) => {
        if (!cancelled) {
          console.error("Failed to fetch agents:", err);
          setAgentList([]);
        }
      })
      .finally(() => {
        if (!cancelled) setAgentsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setGraphsLoading(true);
    createApiClient()
      .listGraphs()
      .then((names) => {
        if (cancelled) return;
        setGraphList([...(names || [])].sort());
      })
      .catch((err) => {
        if (!cancelled) {
          console.warn("Graph list not available:", err.message);
          setGraphList([]);
        }
      })
      .finally(() => {
        if (!cancelled) setGraphsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (agentParam) {
    return <AgentPageClient agentName={agentParam} />;
  }
  if (graphParam) {
    return <GraphPageClient graphName={graphParam} />;
  }

  const isLoading = agentsLoading || graphsLoading;
  const hasAgents = agentList.length > 0;
  const hasGraphs = graphList.length > 0;
  const isEmpty = !hasAgents && !hasGraphs && !isLoading;

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 to-white">
      <header className="border-b bg-white/80 backdrop-blur-sm">
        <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4">
          <span className="text-lg font-semibold tracking-tight">
            <span className="text-green-600 italic">Spring AI Alibaba</span> Studio
          </span>
          <a
            href="https://github.com/alibaba/spring-ai-alibaba/"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center text-muted-foreground hover:text-foreground"
          >
            <GitHubSVG width="24" height="24" />
          </a>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-12">
        <div className="mb-10 text-center">
          <h1 className="text-3xl font-bold tracking-tight text-foreground">
            Choose an Agent or Graph
          </h1>
          <p className="mt-2 text-muted-foreground">
            Select an agent or graph to start chatting and exploring.
          </p>
        </div>

        {isLoading && (
          <div className="flex justify-center py-16">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {isEmpty && !isLoading && (
          <div className="rounded-xl border border-dashed border-muted-foreground/30 bg-muted/30 p-12 text-center">
            <p className="text-muted-foreground">
              No agents or graphs available. Configure your application to expose agents or graphs.
            </p>
          </div>
        )}

        {!isLoading && (hasAgents || hasGraphs) && (
          <div className="grid gap-8 md:grid-cols-2">
            {hasAgents && (
              <section>
                <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold">
                  <Bot className="h-5 w-5" />
                  Agents
                </h2>
                <div className="grid gap-3 sm:grid-cols-2">
                  {agentList.map((name) => (
                    <Link key={name} href={`/index.html?agent=${encodeURIComponent(name)}`}>
                      <Card className="cursor-pointer transition-all hover:border-primary hover:shadow-md">
                        <CardHeader className="pb-2">
                          <CardTitle className="text-base">{name}</CardTitle>
                          <CardDescription>Chat with this agent</CardDescription>
                        </CardHeader>
                        <CardContent className="pt-0">
                          <Button variant="outline" size="sm" className="w-full">
                            Open
                          </Button>
                        </CardContent>
                      </Card>
                    </Link>
                  ))}
                </div>
              </section>
            )}

            {hasGraphs && (
              <section>
                <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold">
                  <GitBranch className="h-5 w-5" />
                  Graphs
                </h2>
                <div className="grid gap-3 sm:grid-cols-2">
                  {graphList.map((name) => (
                    <Link key={name} href={`/index.html?graph=${encodeURIComponent(name)}`}>
                      <Card className="cursor-pointer transition-all hover:border-primary hover:shadow-md">
                        <CardHeader className="pb-2">
                          <CardTitle className="text-base">{name}</CardTitle>
                          <CardDescription>Run this workflow graph</CardDescription>
                        </CardHeader>
                        <CardContent className="pt-0">
                          <Button variant="outline" size="sm" className="w-full">
                            Open
                          </Button>
                        </CardContent>
                      </Card>
                    </Link>
                  ))}
                </div>
              </section>
            )}
          </div>
        )}
      </main>
    </div>
  );
}

export default function SelectionPage(): React.ReactNode {
  return (
    <Suspense fallback={
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    }>
      <SelectionPageContent />
    </Suspense>
  );
}
