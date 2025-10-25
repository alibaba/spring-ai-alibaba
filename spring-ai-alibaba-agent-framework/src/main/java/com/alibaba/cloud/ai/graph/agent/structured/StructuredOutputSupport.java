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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author logicwu0
 */

public class StructuredOutputSupport {

    private static final String STRUCTURED_OUTPUT_NAME = "structured_output";

    public enum Mode {
        NATIVE,
        TOOLCALL,
        PROMPT
    }

    public static Mode detectMode(String provider, List<String> modelTags) {
        if (isOpenAiProvider(provider)) {
            return Mode.NATIVE;
        }

        if (modelTags != null && modelTags.contains("function_call")) {
            return Mode.TOOLCALL;
        }

        return Mode.PROMPT;
    }

    private static boolean isOpenAiProvider(String provider) {
        if (provider == null) {
            return false;
        }
        String p = provider.toLowerCase(Locale.ROOT);
        return p.equals("openai") || p.equals("azure-openai") || p.equals("azure_openai");
    }

    public static Map<String, Object> extractStructuredOutput(
            Message responseMessage,
            Mode mode) {
        
        Map<String, Object> result = new HashMap<>();
        
        if (mode == Mode.TOOLCALL && responseMessage instanceof AssistantMessage assistantMsg) {
            List<AssistantMessage.ToolCall> toolCalls = assistantMsg.getToolCalls();
            if (!toolCalls.isEmpty()) {
                for (AssistantMessage.ToolCall toolCall : toolCalls) {
                    if (STRUCTURED_OUTPUT_NAME.equals(toolCall.name())) {
                        try {
                            Map<String, Object> parsed = parseJson(toolCall.arguments());
                            result.putAll(parsed);
                        } catch (Exception e) {
                            result.put("error", "Failed to parse tool call arguments: " + e.getMessage());
                        }
                        break;
                    }
                }
            }
        } else {
            String text = responseMessage.getText();
            try {
                String json = extractJson(text);
                Map<String, Object> parsed = parseJson(json);
                result.putAll(parsed);
            } catch (Exception e) {
                result.put("error", "Failed to parse response: " + e.getMessage());
                result.put("raw_text", text);
            }
        }
        
        return result;
    }

    private static String extractJson(String text) {
        if (text == null) {
            return "{}";
        }
        
        String trimmed = text.trim();

        Pattern codeBlock = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
        Matcher matcher = codeBlock.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String json) throws Exception {
        try {
            // 尝试使用Jackson（如果可用）
            Class<?> objectMapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            Object mapper = objectMapperClass.getDeclaredConstructor().newInstance();
            Object result = objectMapperClass.getMethod("readValue", String.class, Class.class)
                .invoke(mapper, json, Map.class);
            return (Map<String, Object>) result;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JSON parsing library (Jackson) not available", e);
        }
    }

    public static String getName() {
        return STRUCTURED_OUTPUT_NAME;
    }

    @Deprecated
    public static String getStructuredOutputStateKey() {
        return STRUCTURED_OUTPUT_NAME;
    }

    @Deprecated
    public static String getFormatOutputToolName() {
        return STRUCTURED_OUTPUT_NAME;
    }

}
