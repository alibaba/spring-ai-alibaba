import { UIMessage, isToolRequestConfirmMessage, ToolFeedback } from "@/types/messages";
import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronDown, ChevronUp, CheckCircle, XCircle, Edit, Send } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import { useStreamContext } from "@/providers/Stream";
import { toast } from "sonner";

/**
 * Single tool feedback item with Approve/Reject/Edit actions
 */
function ToolFeedbackItem({
  feedback,
  onUpdate,
  disabled,
}: {
  feedback: ToolFeedback;
  onUpdate: (id: string, result: 'APPROVED' | 'REJECTED' | 'EDITED', editedArgs?: string) => void;
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
      {!disabled && !localResult && (
        <div className="p-2 bg-white border-b border-gray-200 flex gap-2">
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

  if (toolFeedback.length === 0) {
    console.warn('[ToolFeedbackConfirm] No tool feedback found');
    return null;
  }

  const handleUpdateFeedback = (id: string, result: 'APPROVED' | 'REJECTED' | 'EDITED', editedArgs?: string) => {
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

  return (
    <div className="group flex flex-col gap-3">
      <Card className={cn(
        "p-4 border-2",
        isSubmitted ? "bg-gray-50 border-gray-300" : "bg-yellow-50 border-yellow-200"
      )}>
        {/* Header */}
        <div className={cn(
          "flex items-center gap-2 mb-3",
          isSubmitted ? "text-gray-600" : "text-yellow-700"
        )}>
          <CheckCircle className="size-5" />
          <span className="font-medium">
            {isSubmitted ? "Feedback Submitted - Processing..." : "Tool Execution Confirmation Required"}
          </span>
        </div>

        {/* Description */}
        {message.message.content && (
          <p className="mb-3 text-sm text-gray-700">{message.message.content}</p>
        )}

        {/* Tool feedback items */}
        <div className="space-y-2 mb-3">
          {toolFeedback.map((feedback) => (
            <ToolFeedbackItem
              key={feedback.id}
              feedback={feedback}
              onUpdate={handleUpdateFeedback}
              disabled={isSubmitting || isSubmitted}
            />
          ))}
        </div>

        {/* Submit button */}
        {hasAnyFeedback && (
          <div className="flex justify-end pt-2 border-t border-yellow-200">
            <Button
              onClick={handleSubmitFeedback}
              disabled={!allFeedbackProvided || isSubmitting || isSubmitted}
              className={cn(
                "text-white",
                isSubmitted
                  ? "bg-green-600 hover:bg-green-600 cursor-not-allowed"
                  : "bg-yellow-600 hover:bg-yellow-700"
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
                  Submit Feedback & Continue
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

