import { UIMessage, isToolRequestMessage } from "@/types/messages";
import { ToolCalls } from "./tool-calls";

export function ToolRequestMessage({
  message,
}: {
  message: UIMessage;
}) {
  console.log('[ToolRequestMessage] Rendering:', {
    id: message.id,
    messageType: message.message.messageType,
    fullMessage: message
  });

  // Verify it's a tool request message
  if (!isToolRequestMessage(message.message)) {
    console.warn('[ToolRequestMessage] Not a tool request message:', message);
    return null;
  }

  const toolCalls = message.message.toolCalls || [];

  if (toolCalls.length === 0) {
    console.warn('[ToolRequestMessage] No tool calls found');
    return null;
  }

  return (
    <ToolCalls
      toolCalls={toolCalls}
      variant="request"
      title="Tool Request"
      description={message.message.content}
    />
  );
}

