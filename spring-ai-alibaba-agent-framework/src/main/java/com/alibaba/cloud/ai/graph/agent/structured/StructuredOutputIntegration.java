/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.structured;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author logicwu0
 */
public class StructuredOutputIntegration {

    public static ToolCallback prepareStructuredOutput(String outputSchema, String modelName) {
        return prepareStructuredOutput(outputSchema, modelName, null);
    }

    public static ToolCallback prepareStructuredOutput(
            String outputSchema, 
            String modelName, 
            StructuredOutputSupport.Mode mode) {
        
        if (outputSchema == null || outputSchema.trim().isEmpty()) {
            return null;
        }

        Map<String, Object> schema;
        try {
            schema = parseJsonSchema(outputSchema);
        } catch (Exception e) {
            return null;
        }

        if (mode == null) {
            String provider = inferProvider(modelName);
            mode = StructuredOutputSupport.detectMode(provider, null);
        }

        if (mode == StructuredOutputSupport.Mode.TOOLCALL) {
            return createFormatOutputTool(schema);
        }

        return null;
    }

    public static Map<String, Object> extractStructuredOutput(
            Message responseMessage,
            String outputSchema) {
        return extractStructuredOutput(responseMessage, outputSchema, null);
    }

    public static Map<String, Object> extractStructuredOutput(
            Message responseMessage,
            String outputSchema,
            StructuredOutputSupport.Mode mode) {
        
        if (outputSchema == null || responseMessage == null) {
            return new HashMap<>();
        }

        if (mode == null) {
            mode = detectModeFromResponse(responseMessage);
        }

        return StructuredOutputSupport.extractStructuredOutput(responseMessage, mode);
    }

    public static ToolCallingChatOptions prepareStructuredOutputTool(
            String outputSchema,
            ToolCallingChatOptions originalOptions,
            List<ToolCallback> existingToolCallbacks) {

        if (!isJsonSchema(outputSchema)) {
            return originalOptions;
        }

        ToolCallback formatOutputTool = prepareStructuredOutput(outputSchema, null);

        if (formatOutputTool != null) {
            List<ToolCallback> allTools = new ArrayList<>(existingToolCallbacks);
            allTools.add(formatOutputTool);

            return ToolCallingChatOptions.builder()
                .toolCallbacks(allTools)
                .internalToolExecutionEnabled(false)
                .build();
        }

        return originalOptions;
    }

    public static void processStructuredOutput(
            Map<String, Object> updatedState,
            AssistantMessage responseMessage,
            String outputSchema) {

        if (!isJsonSchema(outputSchema)) {
            return;
        }

        try {
            Map<String, Object> structuredOutput = extractStructuredOutput(
                responseMessage,
                outputSchema,
                null
            );

            if (!structuredOutput.isEmpty()) {
                updatedState.put("structured_output", structuredOutput);
            }
        } catch (Exception ignored) {
        }
    }

    private static ToolCallback createFormatOutputTool(Map<String, Object> schema) {
        return StructuredOutputToolBinding.fromSchema(
            schema,
            StructuredOutputSupport.getName()
        ).tool();
    }

    private static Map<String, Object> parseJsonSchema(String schemaString) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.readValue(schemaString, Map.class);
        return map;
    }

    private static String inferProvider(String modelName) {
        if (modelName == null) {
            return null;
        }
        
        String lower = modelName.toLowerCase();
        if (lower.startsWith("gpt-") || lower.startsWith("o1-") || lower.startsWith("o3-")) {
            return "openai";
        }
        if (lower.startsWith("qwen-")) {
            return "tongyi";
        }
        if (lower.startsWith("claude-")) {
            return "anthropic";
        }
        if (lower.startsWith("gemini-")) {
            return "google";
        }
        
        return null;
    }

    private static StructuredOutputSupport.Mode detectModeFromResponse(Message responseMessage) {
        if (responseMessage instanceof AssistantMessage assistantMsg) {
            if (assistantMsg.getToolCalls() != null && !assistantMsg.getToolCalls().isEmpty()) {
                return StructuredOutputSupport.Mode.TOOLCALL;
            }
        }
        return StructuredOutputSupport.Mode.NATIVE;
    }

    private static boolean isJsonSchema(String schema) {
        if (schema == null || schema.trim().isEmpty()) {
            return false;
        }
        String trimmed = schema.trim();
        return trimmed.startsWith("{") && trimmed.contains("\"type\"");
    }
}

