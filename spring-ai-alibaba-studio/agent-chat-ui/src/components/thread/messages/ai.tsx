import { UIMessage, isAssistantMessage } from "@/types/messages";
import { MarkdownText } from "../markdown-text";
import { LoaderCircle } from "lucide-react";

export function AssistantMessage({
  message,
}: {
  message: UIMessage;
  isLoading?: boolean;
}) {
  // Type guard: This component should ONLY handle 'assistant' type messages
  // tool-request, tool-confirm, etc. are handled by their own components
  if (!isAssistantMessage(message.message)) {
    console.warn(
      '[AssistantMessage] Warning: Received non-assistant message type:',
      message.message.messageType,
      'This should be handled by the appropriate component.'
    );
    return null;
  }

  console.log('[AssistantMessage] Rendering:', {
    id: message.id,
    messageType: message.message.messageType,
    content: message.message.content,
    contentLength: message.message.content?.length,
    toolCalls: message.message.toolCalls,
    fullMessage: message
  });

  const contentString = message.message.content;

  // DESIGN NOTE: Assistant messages should NOT render toolCalls
  // - toolCalls in assistant messages are metadata about tool usage
  // - Actual tool execution is displayed via tool-request and tool-response messages
  // - This component only renders the text response from the AI

  // If you see toolCalls here, it likely means:
  // 1. Backend is sending assistant messages with toolCalls (should be tool-request)
  // 2. Or this is historical data that needs type correction

  if (message.message.toolCalls && message.message.toolCalls.length > 0) {
    console.warn(
      '[AssistantMessage] Warning: Assistant message contains toolCalls.',
      'This should typically be a tool-request message instead.',
      'toolCalls:', message.message.toolCalls
    );
  }

  return (
    <div className="group flex flex-col gap-2">
      {/* Render main content only - no tool calls for assistant messages */}
      {contentString && (
        <div className="prose prose-sm max-w-none">
          <MarkdownText>{contentString}</MarkdownText>
        </div>
      )}

      {/*
        Note: Don't show "Empty response" placeholder in streaming scenarios
        - During streaming, content accumulates progressively
        - An empty update should not clear previously accumulated content
        - If truly empty after streaming completes, the component simply won't render content
      */}
    </div>
  );
}

export function AssistantMessageLoading() {
  return (
    <div className="flex items-center gap-2 text-muted-foreground">
      <LoaderCircle className="h-4 w-4 animate-spin" />
      <span className="text-sm">Thinking...</span>
    </div>
  );
}

