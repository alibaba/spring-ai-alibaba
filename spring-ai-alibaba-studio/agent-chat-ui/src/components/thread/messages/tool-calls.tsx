import { ToolCall, ToolResponse, ToolFeedback } from "@/types/messages";
import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronDown, ChevronUp, Code, CheckCircle } from "lucide-react";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

function isComplexValue(value: any): boolean {
  return Array.isArray(value) || (typeof value === "object" && value !== null);
}

/**
 * Style variants for ToolCalls component
 */
export type ToolCallsVariant = 'default' | 'request' | 'confirm';

/**
 * Component for a single collapsible tool call in default variant
 */
function DefaultToolCallItem({ toolCall, index }: { toolCall: ToolCall | ToolFeedback; index: number }) {
  const [isExpanded, setIsExpanded] = useState(false);

  let args;
  try {
    args = toolCall.arguments ? JSON.parse(toolCall.arguments) : {};
  } catch {
    args = { raw: toolCall.arguments };
  }
  const hasArgs = Object.keys(args).length > 0;

  const styles = {
    card: "overflow-hidden rounded-lg border border-gray-200",
    header: "border-b border-gray-200 bg-gray-50 px-4 py-2",
    headerText: "font-medium text-gray-900",
  };

  return (
    <div key={toolCall.id || index} className={styles.card}>
      <div className={styles.header}>
        <div className="flex flex-wrap items-center justify-between gap-2 w-full">
          <h3 className={styles.headerText}>
            {toolCall.name}
            {toolCall.id && (
              <code className="ml-2 rounded bg-gray-100 px-2 py-1 text-sm">
                {toolCall.id}
              </code>
            )}
            {'type' in toolCall && (
              <span className="ml-2 text-xs text-gray-500">
                Type: {toolCall.type}
              </span>
            )}
          </h3>
        </div>
      </div>
      <AnimatePresence initial={false}>
        {isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="overflow-hidden"
          >
            {hasArgs ? (
              <table className="min-w-full divide-y divide-gray-200">
                <tbody className="divide-y divide-gray-200">
                  {Object.entries(args).map(([key, value], argIdx) => (
                    <tr key={argIdx}>
                      <td className="px-4 py-2 text-sm font-medium whitespace-nowrap text-gray-900">
                        {key}
                      </td>
                      <td className="px-4 py-2 text-sm text-gray-500">
                        {isComplexValue(value) ? (
                          <code className="rounded bg-gray-50 px-2 py-1 font-mono text-sm break-all">
                            {JSON.stringify(value, null, 2)}
                          </code>
                        ) : (
                          String(value)
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <code className="block p-3 text-sm">{"{}"}</code>
            )}
          </motion.div>
        )}
      </AnimatePresence>
      <motion.button
        onClick={() => setIsExpanded(!isExpanded)}
        className="flex w-full cursor-pointer items-center justify-center border-t-[1px] border-gray-200 py-2 text-gray-500 transition-all duration-200 ease-in-out hover:bg-gray-50 hover:text-gray-600"
        initial={{ scale: 1 }}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
      >
        {isExpanded ? <ChevronUp /> : <ChevronDown />}
      </motion.button>
    </div>
  );
}

/**
 * Component for a single collapsible tool call in request/confirm variants
 */
function CardToolCallItem({
  toolCall,
  index,
  itemBorder,
  itemText
}: {
  toolCall: ToolCall | ToolFeedback;
  index: number;
  itemBorder?: string;
  itemText?: string;
}) {
  const [isExpanded, setIsExpanded] = useState(false);

  let parsedArgs;
  try {
    parsedArgs = JSON.parse(toolCall.arguments);
  } catch {
    parsedArgs = { raw: toolCall.arguments };
  }

  // Check if it's a ToolFeedback (has description)
  const hasDescription = 'description' in toolCall && toolCall.description;

  return (
    <div
      key={toolCall.id || index}
      className={cn(
        "bg-white rounded-md border overflow-hidden",
        itemBorder || ''
      )}
    >
      {/* Header with tool name and id */}
      <div className="p-2 border-b border-gray-200 bg-gray-50">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <span className={cn("font-mono text-xs font-semibold", itemText || '')}>
              {toolCall.name}
            </span>
            {toolCall.id && (
              <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs">
                {toolCall.id}
              </code>
            )}
          </div>
          {'type' in toolCall && (
            <span className="text-xs text-gray-500">{toolCall.type}</span>
          )}
        </div>
      </div>
      {/* Description if present */}
      {hasDescription && (
        <div className="px-2 pt-1.5 text-xs text-gray-600 italic bg-white">
          {(toolCall as ToolFeedback).description}
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
            <div className="p-2 bg-white">
              <pre className="text-xs bg-gray-50 p-1.5 rounded overflow-x-auto">
                {JSON.stringify(parsedArgs, null, 2)}
              </pre>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
      {/* Expand/collapse button */}
      <motion.button
        onClick={() => setIsExpanded(!isExpanded)}
        className={cn(
          "flex w-full cursor-pointer items-center justify-center border-t-[1px] py-1 text-gray-500 transition-all duration-200 ease-in-out hover:text-gray-600",
          itemBorder || '',
          "hover:bg-gray-50"
        )}
        initial={{ scale: 1 }}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
      >
        {isExpanded ? <ChevronUp className="size-3.5" /> : <ChevronDown className="size-3.5" />}
      </motion.button>
    </div>
  );
}

interface ToolCallsProps {
  toolCalls: (ToolCall | ToolFeedback)[];
  variant?: ToolCallsVariant;
  title?: string;
  description?: string;
}

/**
 * Unified ToolCalls component that can render tool calls in different styles
 * - default: Gray border, table layout (for assistant messages)
 * - request: Blue card, compact layout (for tool-request messages)
 * - confirm: Green card, compact layout with description (for tool-confirm messages)
 */
export function ToolCalls({
  toolCalls,
  variant = 'default',
  title,
  description,
}: ToolCallsProps) {
  if (!toolCalls || toolCalls.length === 0) return null;

  // Determine styling based on variant
  const variantStyles = {
    default: {
      container: "mx-auto grid max-w-3xl grid-rows-[1fr_auto] gap-2",
      card: "overflow-hidden rounded-lg border border-gray-200",
      header: "border-b border-gray-200 bg-gray-50 px-4 py-2",
      headerText: "font-medium text-gray-900",
      useCard: false,
    },
    request: {
      container: "group flex flex-col gap-2",
      card: "p-2 bg-blue-50 border-blue-200",
      header: "flex items-center gap-1.5 text-blue-700 mb-2",
      headerText: "font-medium text-sm",
      icon: <Code className="size-3.5" />,
      useCard: true,
      itemBorder: "border-blue-200",
      itemText: "text-blue-700",
    },
    confirm: {
      container: "group flex flex-col gap-2",
      card: "p-4 bg-green-50 border-green-200",
      header: "flex items-center gap-2 text-green-700 mb-3",
      headerText: "font-medium",
      icon: <CheckCircle className="size-4" />,
      useCard: true,
      itemBorder: "border-green-200",
      itemText: "text-green-700",
    },
  };

  const styles = variantStyles[variant];

  // Render content based on variant
  if (variant === 'default') {
    // Collapsible table-based layout for default variant
    return (
      <div className={styles.container}>
        {toolCalls.map((tc, idx) => (
          <DefaultToolCallItem key={tc.id || idx} toolCall={tc} index={idx} />
        ))}
      </div>
    );
  }

  // Card-based layout for request and confirm variants
  const itemBorder = 'itemBorder' in styles ? styles.itemBorder : undefined;
  const itemText = 'itemText' in styles ? styles.itemText : undefined;

  return (
    <div className={styles.container}>
      <Card className={styles.card}>
        {(title || ('icon' in styles && styles.icon)) && (
          <div className={styles.header}>
            {'icon' in styles && styles.icon}
            <span className={styles.headerText}>
              {title || (variant === 'request' ? 'Tool Request' : 'Tool Execution Confirmation')}
            </span>
          </div>
        )}
        {/* Description text */}
        {description && (
          <p className="mb-2 text-xs text-gray-700">{description}</p>
        )}
        {/* Tool calls list */}
        <div className="space-y-1.5">
          {toolCalls.map((toolCall, index) => (
            <CardToolCallItem
              key={toolCall.id || index}
              toolCall={toolCall}
              index={index}
              itemBorder={itemBorder}
              itemText={itemText}
            />
          ))}
        </div>
      </Card>
    </div>
  );
}

export function ToolResult({ toolResponse }: { toolResponse: ToolResponse }) {
  const [isExpanded, setIsExpanded] = useState(false);

  let parsedContent: any;
  let isJsonContent = false;

  try {
    parsedContent = JSON.parse(toolResponse.responseData);
    isJsonContent = isComplexValue(parsedContent);
  } catch {
    // Content is not JSON, use as is
    parsedContent = toolResponse.responseData;
  }

  return (
    <div className="mx-auto grid max-w-3xl grid-rows-[1fr_auto] gap-2">
      <div className="overflow-hidden rounded-lg border border-gray-200">
        <div className="border-b border-gray-200 bg-gray-50 px-4 py-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="font-medium text-gray-900">
              {toolResponse.name && (
                <>
                  Tool Result:{" "}
                  <code className="rounded bg-gray-100 px-2 py-1">
                    {toolResponse.name}
                  </code>
                </>
              )}
              {toolResponse.id && (
                <code className="ml-2 rounded bg-gray-100 px-2 py-1 text-sm">
                  {toolResponse.id}
                </code>
              )}
            </h3>
          </div>
        </div>
        <AnimatePresence initial={false}>
          {isExpanded && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.3 }}
              className="overflow-hidden"
            >
              <div className="bg-gray-100 p-3">
                {isJsonContent ? (
                  <table className="min-w-full divide-y divide-gray-200">
                    <tbody className="divide-y divide-gray-200">
                      {(Array.isArray(parsedContent)
                        ? parsedContent
                        : Object.entries(parsedContent)
                      ).map((item, argIdx) => {
                        const [key, value] = Array.isArray(parsedContent)
                          ? [argIdx, item]
                          : [item[0], item[1]];
                        return (
                          <tr key={argIdx}>
                            <td className="px-4 py-2 text-sm font-medium whitespace-nowrap text-gray-900">
                              {key}
                            </td>
                            <td className="px-4 py-2 text-sm text-gray-500">
                              {isComplexValue(value) ? (
                                <code className="rounded bg-gray-50 px-2 py-1 font-mono text-sm break-all">
                                  {JSON.stringify(value, null, 2)}
                                </code>
                              ) : (
                                String(value)
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                ) : (
                  <code className="block text-sm">
                    {String(toolResponse.responseData)}
                  </code>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
        <motion.button
          onClick={() => setIsExpanded(!isExpanded)}
          className="flex w-full cursor-pointer items-center justify-center border-t-[1px] border-gray-200 py-2 text-gray-500 transition-all duration-200 ease-in-out hover:bg-gray-50 hover:text-gray-600"
          initial={{ scale: 1 }}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
        >
          {isExpanded ? <ChevronUp /> : <ChevronDown />}
        </motion.button>
      </div>
    </div>
  );
}
