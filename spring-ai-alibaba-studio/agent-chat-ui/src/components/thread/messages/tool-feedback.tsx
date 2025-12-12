import { UIMessage, isToolRequestConfirmMessage, ToolFeedback, ToolCall } from "@/types/messages";
import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronDown, ChevronUp, CheckCircle, XCircle, Edit, Send, Zap } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import { useStreamContext } from "@/providers/Stream";
import { toast } from "sonner";

/**
 * Single automatically approved tool item (read-only display)
 */
function AutomaticallyApprovedToolItem({
  toolCall,
  index,
}: {
  toolCall: ToolCall;
  index: number;
}) {
  const [isExpanded, setIsExpanded] = useState(false);

  let parsedArgs;
  try {
    parsedArgs = JSON.parse(toolCall.arguments);
  } catch {
    parsedArgs = { raw: toolCall.arguments };
  }

  return (
    <div className={cn(
      "rounded-md border overflow-hidden transition-all",
      "border-green-300 bg-green-50"
    )}>
      {/* Header with tool name and status */}
      <div className="p-3 border-b border-green-200 bg-white">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Zap className="size-4 text-green-600" />
            <span className="font-mono text-sm font-semibold text-gray-900">
              {toolCall.name}
            </span>
            {toolCall.id && (
              <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600">
                {toolCall.id}
              </code>
            )}
          </div>
          <span className="text-xs font-medium px-2 py-1 rounded bg-green-100 text-green-700">
            Auto-Approved
          </span>
        </div>
      </div>

      {/* Expandable arguments section */}
      <AnimatePresence initial={false}>
        {isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="overflow-hidden"
          >
            <div className="p-3 bg-white">
              <pre className="text-xs bg-gray-50 p-2 rounded overflow-x-auto">
                {JSON.stringify(parsedArgs, null, 2)}
              </pre>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Expand/collapse button */}
      <motion.button
        onClick={() => setIsExpanded(!isExpanded)}
        className="flex w-full cursor-pointer items-center justify-center border-t-[1px] border-green-200 py-1.5 text-gray-500 transition-all duration-200 ease-in-out hover:bg-gray-50 hover:text-gray-600"
        initial={{ scale: 1 }}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
      >
        {isExpanded ? <ChevronUp className="size-4" /> : <ChevronDown className="size-4" />}
      </motion.button>
    </div>
  );
}

/**
 * Single tool feedback item with Approve/Reject/Edit actions
 */
function ToolFeedbackItem({
  feedback,
  onUpdate,
  disabled,
}: {
  feedback: ToolFeedback;
  onUpdate: (id: string, result: 'APPROVED' | 'REJECTED' | 'EDITED' | null, editedArgs?: string) => void;
  disabled: boolean;
}) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editedArguments, setEditedArguments] = useState(feedback.arguments);
  const [localResult, setLocalResult] = useState<'APPROVED' | 'REJECTED' | 'EDITED' | undefined>(feedback.result);

  const handleApprove = () => {
    setLocalResult('APPROVED');
    onUpdate(feedback.id, 'APPROVED');
    toast.success(`Tool "${feedback.name}" approved`);
  };

  const handleReject = () => {
    setLocalResult('REJECTED');
    onUpdate(feedback.id, 'REJECTED');
    toast.success(`Tool "${feedback.name}" rejected`);
  };

  const handleEdit = () => {
    setIsEditing(true);
    setIsExpanded(true);
  };

  const handleSaveEdit = () => {
    setLocalResult('EDITED');
    onUpdate(feedback.id, 'EDITED', editedArguments);
    setIsEditing(false);
    toast.success(`Tool "${feedback.name}" arguments edited`);
  };

  const handleCancelEdit = () => {
    setEditedArguments(feedback.arguments);
    setIsEditing(false);
  };

  let parsedArgs;
  try {
    parsedArgs = JSON.parse(editedArguments);
  } catch {
    parsedArgs = { raw: editedArguments };
  }

  // Determine border and text color based on result
  const resultStyles = {
    APPROVED: "border-green-300 bg-green-50",
    REJECTED: "border-red-300 bg-red-50",
    EDITED: "border-blue-300 bg-blue-50",
    default: disabled && localResult ? "border-gray-300 bg-gray-100" : "border-yellow-300 bg-yellow-50",
  };

  const borderClass = localResult ? resultStyles[localResult] : resultStyles.default;
  const isDisabledWithResult = disabled && localResult;

  return (
    <div className={cn(
      "rounded-md border overflow-hidden transition-all",
      borderClass,
      isDisabledWithResult && "opacity-75"
    )}>
      {/* Header with tool name and status */}
      <div className="p-3 border-b border-gray-200 bg-white">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="font-mono text-sm font-semibold text-gray-900">
              {feedback.name}
            </span>
            {feedback.id && (
              <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600">
                {feedback.id}
              </code>
            )}
          </div>
          {localResult && (
            <span className={cn(
              "text-xs font-medium px-2 py-1 rounded",
              localResult === 'APPROVED' && "bg-green-100 text-green-700",
              localResult === 'REJECTED' && "bg-red-100 text-red-700",
              localResult === 'EDITED' && "bg-blue-100 text-blue-700",
              isDisabledWithResult && "ring-1 ring-gray-400"
            )}>
              {localResult}
              {isDisabledWithResult && " ✓"}
            </span>
          )}
        </div>
        {feedback.description && (
          <p className="mt-1.5 text-xs text-gray-600 italic">
            {feedback.description}
          </p>
        )}
      </div>

      {/* Action buttons */}
      {!disabled && (
        <div className="p-2 bg-white border-b border-gray-200 flex gap-2">
          {!localResult ? (
            <>
              <Button
                size="sm"
                variant="outline"
                className="flex-1 text-green-600 border-green-300 hover:bg-green-50"
                onClick={handleApprove}
              >
                <CheckCircle className="size-4 mr-1" />
                Approve
              </Button>
              <Button
                size="sm"
                variant="outline"
                className="flex-1 text-red-600 border-red-300 hover:bg-red-50"
                onClick={handleReject}
              >
                <XCircle className="size-4 mr-1" />
                Reject
              </Button>
              <Button
                size="sm"
                variant="outline"
                className="flex-1 text-blue-600 border-blue-300 hover:bg-blue-50"
                onClick={handleEdit}
              >
                <Edit className="size-4 mr-1" />
                Edit
              </Button>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center gap-2">
              <span className="text-xs text-gray-600">
                Feedback: <span className="font-medium">{localResult}</span>
              </span>
              <Button
                size="sm"
                variant="ghost"
                className="text-xs text-gray-500 hover:text-gray-700"
                onClick={() => {
                  setLocalResult(undefined);
                  onUpdate(feedback.id, null, undefined); // Reset by clearing the state
                  setEditedArguments(feedback.arguments);
                  setIsEditing(false);
                }}
              >
                Change
              </Button>
            </div>
          )}
        </div>
      )}

      {/* Expandable arguments section */}
      <AnimatePresence initial={false}>
        {isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="overflow-hidden"
          >
            <div className="p-3 bg-white">
              {isEditing ? (
                <div className="space-y-2">
                  <label className="text-xs font-medium text-gray-700">
                    Edit Arguments (JSON):
                  </label>
                  <Textarea
                    value={editedArguments}
                    onChange={(e) => setEditedArguments(e.target.value)}
                    className="font-mono text-xs min-h-[120px]"
                    placeholder="Enter valid JSON"
                  />
                  <div className="flex gap-2 justify-end">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={handleCancelEdit}
                    >
                      Cancel
                    </Button>
                    <Button
                      size="sm"
                      onClick={handleSaveEdit}
                      className="bg-blue-600 hover:bg-blue-700"
                    >
                      <Send className="size-3.5 mr-1" />
                      Save Edit
                    </Button>
                  </div>
                </div>
              ) : (
                <pre className="text-xs bg-gray-50 p-2 rounded overflow-x-auto">
                  {JSON.stringify(parsedArgs, null, 2)}
                </pre>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Expand/collapse button */}
      <motion.button
        onClick={() => setIsExpanded(!isExpanded)}
        className="flex w-full cursor-pointer items-center justify-center border-t-[1px] border-gray-200 py-1.5 text-gray-500 transition-all duration-200 ease-in-out hover:bg-gray-50 hover:text-gray-600"
        initial={{ scale: 1 }}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
      >
        {isExpanded ? <ChevronUp className="size-4" /> : <ChevronDown className="size-4" />}
      </motion.button>
    </div>
  );
}

/**
 * Main component for tool request confirmation with Human-in-the-Loop feedback
 */
export function ToolFeedbackConfirm({
  message,
}: {
  message: UIMessage;
}) {
  const stream = useStreamContext();
  const [feedbackState, setFeedbackState] = useState<Record<string, { result: 'APPROVED' | 'REJECTED' | 'EDITED'; editedArgs?: string }>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  // Track if feedback has been submitted for this message (prevent duplicate submissions)
  const [isSubmitted, setIsSubmitted] = useState(false);

  console.log('[ToolFeedbackConfirm] Rendering:', {
    id: message.id,
    messageType: message.message.messageType,
    fullMessage: message,
    isSubmitted
  });

  // Verify it's a tool request confirm message
  if (!isToolRequestConfirmMessage(message.message)) {
    console.warn('[ToolFeedbackConfirm] Not a tool request confirm message:', message);
    return null;
  }

  const toolFeedback = message.message.toolFeedback || [];
  const toolsAutomaticallyApproved = message.message.toolsAutomaticallyApproved || [];

  // If there are no tools to confirm and no auto-approved tools, don't render
  if (toolFeedback.length === 0 && toolsAutomaticallyApproved.length === 0) {
    console.warn('[ToolFeedbackConfirm] No tool feedback or auto-approved tools found');
    return null;
  }

  const totalToolCount = toolFeedback.length + toolsAutomaticallyApproved.length;

  const handleUpdateFeedback = (id: string, result: 'APPROVED' | 'REJECTED' | 'EDITED' | null, editedArgs?: string) => {
    // If result is null, it means we're clearing the feedback (allowing user to change their choice)
    if (result === null) {
      setFeedbackState(prev => {
        const newState = { ...prev };
        delete newState[id];
        return newState;
      });
      return;
    }
    setFeedbackState(prev => ({
      ...prev,
      [id]: { result, editedArgs }
    }));
  };

  const handleSubmitFeedback = async () => {
    // Prevent duplicate submissions
    if (isSubmitted || isSubmitting) {
      toast.warning("Feedback has already been submitted");
      return;
    }

    // Check if all tools have feedback
    const allFeedbackProvided = toolFeedback.every(tf => feedbackState[tf.id]);

    if (!allFeedbackProvided) {
      toast.error("Please provide feedback for all tools");
      return;
    }

    setIsSubmitting(true);

    try {
      // Prepare feedback for API
      const feedbackForApi = toolFeedback.map(tf => {
        const feedback = feedbackState[tf.id];
        return {
          id: tf.id,
          name: tf.name,
          arguments: feedback.result === 'EDITED' && feedback.editedArgs ? feedback.editedArgs : tf.arguments,
          result: feedback.result,
          description: tf.description,
        };
      });

      console.log('[ToolFeedbackConfirm] Submitting feedback:', feedbackForApi);

      // Call resume API (SSE stream)
      await stream.resumeFeedback(feedbackForApi);

      // Mark as submitted to prevent duplicate submissions
      setIsSubmitted(true);
      setIsSubmitting(false);

      toast.success("Feedback submitted successfully");
    } catch (error: any) {
      console.error('[ToolFeedbackConfirm] Failed to submit feedback:', error);
      toast.error(error.message || "Failed to submit feedback");
      setIsSubmitting(false);
    }
  };

  const allFeedbackProvided = toolFeedback.every(tf => feedbackState[tf.id]);
  const hasAnyFeedback = Object.keys(feedbackState).length > 0;
  const pendingCount = toolFeedback.filter(tf => !feedbackState[tf.id]).length;
  const completedCount = toolFeedback.length - pendingCount;

  return (
    <div className="group flex flex-col gap-3">
      <Card className={cn(
        "p-4 border-2",
        isSubmitted ? "bg-gray-50 border-gray-300" : "bg-yellow-50 border-yellow-200"
      )}>
        {/* Header */}
        <div className={cn(
          "flex items-center justify-between mb-3",
          isSubmitted ? "text-gray-600" : "text-yellow-700"
        )}>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-5" />
            <span className="font-medium">
              {isSubmitted ? "Feedback Submitted - Processing..." : "Tool Execution Confirmation Required"}
            </span>
          </div>
          {!isSubmitted && !isSubmitting && toolFeedback.length > 1 && (
            <div className="text-xs font-medium px-2 py-1 rounded bg-yellow-100 text-yellow-700">
              {completedCount} / {toolFeedback.length} completed
            </div>
          )}
        </div>

        {/* Description */}
        {message.message.content && (
          <p className="mb-3 text-sm text-gray-700">{message.message.content}</p>
        )}

        {/* Summary: Show total tool count if there are auto-approved tools */}
        {toolsAutomaticallyApproved.length > 0 && (
          <div className="mb-3 p-2 bg-blue-50 rounded border border-blue-200">
            <div className="flex items-center gap-2 text-xs text-blue-800">
              <Zap className="size-3.5" />
              <span>
                {toolsAutomaticallyApproved.length} tool{toolsAutomaticallyApproved.length > 1 ? 's' : ''} automatically approved
                {toolFeedback.length > 0 && `, ${toolFeedback.length} tool${toolFeedback.length > 1 ? 's' : ''} require${toolFeedback.length > 1 ? '' : 's'} confirmation`}
              </span>
            </div>
          </div>
        )}

        {/* Progress indicator for multiple tool calls */}
        {!isSubmitted && !isSubmitting && toolFeedback.length > 1 && (
          <div className="mb-3 p-2 bg-yellow-100 rounded border border-yellow-200">
            <p className="text-xs text-yellow-800 mb-1">
              Please provide feedback for each tool call below. All feedback will be submitted together when complete.
            </p>
            {pendingCount > 0 && (
              <p className="text-xs text-yellow-700 italic">
                {pendingCount} tool call{pendingCount > 1 ? 's' : ''} remaining
              </p>
            )}
          </div>
        )}

        {/* All tool items - showing both auto-approved and feedback tools together */}
        <div className="space-y-2 mb-3">
          {/* Automatically approved tools (read-only) */}
          {toolsAutomaticallyApproved.length > 0 && (
            <>
              {toolsAutomaticallyApproved.map((toolCall, index) => (
                <AutomaticallyApprovedToolItem
                  key={toolCall.id || `auto-${index}`}
                  toolCall={toolCall}
                  index={index}
                />
              ))}
              {/* Separator if there are both types */}
              {toolFeedback.length > 0 && (
                <div className="flex items-center gap-2 my-2">
                  <div className="flex-1 border-t border-gray-300"></div>
                  <span className="text-xs text-gray-500 px-2">Requires Confirmation</span>
                  <div className="flex-1 border-t border-gray-300"></div>
                </div>
              )}
            </>
          )}

          {/* Tool feedback items (require user confirmation) */}
          {toolFeedback.map((feedback) => (
            <ToolFeedbackItem
              key={feedback.id}
              feedback={feedback}
              onUpdate={handleUpdateFeedback}
              disabled={isSubmitting || isSubmitted}
            />
          ))}
        </div>

        {/* Submit button - only show if there are tools requiring confirmation */}
        {toolFeedback.length > 0 && (
          <div className="flex justify-between items-center pt-2 border-t border-yellow-200">
            {!isSubmitted && !isSubmitting && (
              <div className="text-xs text-gray-600">
                {allFeedbackProvided ? (
                  <span className="text-green-600 font-medium">✓ All tool calls have feedback</span>
                ) : (
                  <span>Please provide feedback for all {toolFeedback.length} tool call{toolFeedback.length > 1 ? 's' : ''} before submitting</span>
                )}
              </div>
            )}
            <Button
              onClick={handleSubmitFeedback}
              disabled={!allFeedbackProvided || isSubmitting || isSubmitted}
              className={cn(
                "text-white",
                isSubmitted
                  ? "bg-green-600 hover:bg-green-600 cursor-not-allowed"
                  : allFeedbackProvided
                  ? "bg-yellow-600 hover:bg-yellow-700"
                  : "bg-gray-400 cursor-not-allowed"
              )}
            >
              {isSubmitting ? (
                <>
                  <motion.div
                    animate={{ rotate: 360 }}
                    transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                    className="mr-2"
                  >
                    <Send className="size-4" />
                  </motion.div>
                  Submitting...
                </>
              ) : isSubmitted ? (
                <>
                  <CheckCircle className="size-4 mr-2" />
                  Submitted
                </>
              ) : (
                <>
                  <Send className="size-4 mr-2" />
                  Submit All Feedback
                </>
              )}
            </Button>
          </div>
        )}

        {/* Submitted status message */}
        {isSubmitted && (
          <div className="flex justify-center pt-2 border-t border-gray-300">
            <div className="flex items-center gap-2 text-sm text-gray-600">
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
              >
                <CheckCircle className="size-4 text-green-600" />
              </motion.div>
              <span className="font-medium">
                Feedback submitted. Waiting for agent response...
              </span>
            </div>
          </div>
        )}

        {!hasAnyFeedback && !isSubmitted && (
          <p className="text-xs text-yellow-600 italic text-center">
            Please review each tool and provide your feedback (Approve, Reject, or Edit)
          </p>
        )}
      </Card>
    </div>
  );
}

