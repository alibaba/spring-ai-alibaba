import { UIMessage, isToolResponseMessage } from "@/types/messages";
import { ToolResult } from "./tool-calls";

export function ToolResponseMessage({
  message,
}: {
  message: UIMessage;
}) {
  console.log('[ToolResponseMessage] Rendering:', {
    id: message.id,
    messageType: message.message.messageType,
    fullMessage: message
  });

  // Verify it's a tool response message
  if (!isToolResponseMessage(message.message)) {
    console.warn('[ToolResponseMessage] Not a tool response message:', message);
    return null;
  }

  const responses = message.message.responses || [];

  if (responses.length === 0) {
    console.warn('[ToolResponseMessage] No responses found');
    return null;
  }

  return (
    <div className="group flex flex-col gap-2">
      {responses.map((response, index) => (
        <ToolResult
          key={response.id || index}
          toolResponse={response}
        />
      ))}
    </div>
  );
}

