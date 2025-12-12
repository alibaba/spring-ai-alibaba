import { UIMessage, isToolRequestConfirmMessage } from "@/types/messages";
import { ToolFeedbackConfirm } from "./tool-feedback";

export function ToolRequestConfirmMessage({
  message,
}: {
  message: UIMessage;
}) {
  console.log('[ToolRequestConfirmMessage] Rendering:', {
    id: message.id,
    messageType: message.message.messageType,
    fullMessage: message
  });

  // Verify it's a tool request confirm message
  if (!isToolRequestConfirmMessage(message.message)) {
    console.warn('[ToolRequestConfirmMessage] Not a tool request confirm message:', message);
    return null;
  }

  const toolFeedback = message.message.toolFeedback || [];
  const toolsAutomaticallyApproved = message.message.toolsAutomaticallyApproved || [];

  // If there are no tools at all, don't render
  if (toolFeedback.length === 0 && toolsAutomaticallyApproved.length === 0) {
    console.warn('[ToolRequestConfirmMessage] No tool feedback or auto-approved tools found');
    return null;
  }

  return <ToolFeedbackConfirm message={message} />;
}

