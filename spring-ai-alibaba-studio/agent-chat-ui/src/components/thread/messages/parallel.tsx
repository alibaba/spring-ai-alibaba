import { UIMessage, isParallelMessage } from "@/types/messages";
import { MarkdownText } from "../markdown-text";
import { cn } from "@/lib/utils";
import { Bot } from "lucide-react";

/**
 * Renders parallel sub-agent results as a grid of cards.
 * Each card displays the agent name and its content (supports Markdown).
 */
export function ParallelAgentMessage({ message }: { message: UIMessage }) {
  if (!isParallelMessage(message.message)) {
    console.warn("[ParallelAgentMessage] Not a parallel message:", message);
    return null;
  }

  const { subAgents } = message.message;

  if (!subAgents || subAgents.length === 0) {
    return null;
  }

  // When only 1 sub-agent, render as normal message (no card wrapper)
  if (subAgents.length === 1) {
    return (
      <div className="prose prose-sm max-w-none">
        <MarkdownText>{subAgents[0].content}</MarkdownText>
      </div>
    );
  }

  // Use a human-friendly label from the agent key
  const formatAgentName = (name: string) =>
    name
      .replace(/_result$/i, "")
      .replace(/_/g, " ")
      .replace(/\b\w/g, (c) => c.toUpperCase());

  return (
    <div className="group flex flex-col gap-3">
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Bot className="h-4 w-4" />
        <span className="font-medium">Multi Agents ({subAgents.length})</span>
      </div>

      <div
        className={cn(
          "grid gap-3",
          subAgents.length === 2 && "grid-cols-1 sm:grid-cols-2",
          subAgents.length >= 3 && "grid-cols-1 sm:grid-cols-2 lg:grid-cols-3"
        )}
      >
        {subAgents.map((sub, idx) => (
          <div
            key={sub.name || idx}
            className="rounded-xl border bg-card shadow-sm overflow-hidden flex flex-col"
          >
            {/* Card header — agent name */}
            <div className="flex items-center gap-2 border-b bg-muted/40 px-4 py-2">
              <span className="shrink-0 h-5 w-5 rounded-full bg-primary/10 flex items-center justify-center text-[10px] font-bold text-primary">
                {idx + 1}
              </span>
              <span className="text-sm font-medium text-foreground truncate">
                {formatAgentName(sub.name)}
              </span>
            </div>

            {/* Card body — content */}
            <div className="px-4 py-3 flex-1 overflow-y-auto max-h-[400px]">
              {sub.content ? (
                <div className="prose prose-sm max-w-none">
                  <MarkdownText>{sub.content}</MarkdownText>
                </div>
              ) : (
                <span className="text-sm text-muted-foreground italic">
                  Generating...
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
