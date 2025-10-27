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
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author logicwu0
 */
public class StructuredOutputIntegration {

    public static ChatOptions prepareStructuredOutputOptions(
            String outputSchema,
            ToolCallingChatOptions baseToolCallingOptions,
            List<ToolCallback> toolCallbacks,
            Object chatClient) {
        if (!isJsonSchema(outputSchema)) {
            return baseToolCallingOptions;
        }
        Map<String, Object> schema;
        try {
            schema = parseJsonSchema(outputSchema);
        } catch (Exception e) {
            return baseToolCallingOptions;
        }

        String modelName = null;
        if (baseToolCallingOptions != null) {
            modelName = baseToolCallingOptions.getModel();
        }
        if (modelName == null && chatClient != null) {
            modelName = extractModelNameFromChatClient(chatClient);
        }
        
        String provider = inferProvider(modelName);
        StructuredOutputSupport.Mode mode = StructuredOutputSupport.detectMode(provider, null);
        
        // NATIVE
        if (mode == StructuredOutputSupport.Mode.NATIVE) {
            ChatOptions nativeOptions = trySetResponseFormatWithReflection(baseToolCallingOptions);
            if (nativeOptions != null) {
                return nativeOptions;
            }
            return baseToolCallingOptions;
        }
        
        // TOOLCALL
        if (mode == StructuredOutputSupport.Mode.TOOLCALL) {
            ToolCallback formatOutputTool = StructuredOutputToolBinding.fromSchema(
                schema,
                StructuredOutputSupport.getName()
            ).tool();
            // TOOLCALL
            List<ToolCallback> all = new ArrayList<>(toolCallbacks);
            all.add(formatOutputTool);
            return copyToolCalling(baseToolCallingOptions)
                    .toolCallbacks(all)
                    .internalToolExecutionEnabled(false)
                    .build();
        }
        return baseToolCallingOptions;
    }

    private static String extractModelNameFromChatClient(Object chatClient) {
        if (chatClient == null) {
            return null;
        }
        
        try {
            java.lang.reflect.Field defaultOptionsField = chatClient.getClass().getDeclaredField("defaultOptions");
            defaultOptionsField.setAccessible(true);
            ChatOptions defaultOptions = (ChatOptions) defaultOptionsField.get(chatClient);
            
            if (defaultOptions != null) {
                return defaultOptions.getModel();
            }
        } catch (Exception e) {
        }
        
        return null;
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

    private static Map<String, Object> parseJsonSchema(String schemaString) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.readValue(schemaString, Map.class);
        return map;
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
    private static ToolCallingChatOptions.Builder copyToolCalling(ToolCallingChatOptions base) {
        return ToolCallingChatOptions.builder()
                .model(base.getModel())
                .temperature(base.getTemperature())
                .maxTokens(base.getMaxTokens())
                .topP(base.getTopP())
                .frequencyPenalty(base.getFrequencyPenalty())
                .presencePenalty(base.getPresencePenalty())
                .toolCallbacks(base.getToolCallbacks());
    }

    private static ChatOptions trySetResponseFormatWithReflection(ToolCallingChatOptions base) {
        if (base == null) {
            return null;
        }

        try {
            Class<?> openAiOptionsClass = Class.forName("org.springframework.ai.openai.OpenAiChatOptions");

            Method builderMethod = openAiOptionsClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);

            Class<?> builderClass = builder.getClass();
            setFieldIfExists(builderClass, builder, "model", base.getModel());
            setFieldIfExists(builderClass, builder, "temperature", base.getTemperature());
            setFieldIfExists(builderClass, builder, "maxTokens", base.getMaxTokens());
            setFieldIfExists(builderClass, builder, "topP", base.getTopP());
            setFieldIfExists(builderClass, builder, "frequencyPenalty", base.getFrequencyPenalty());
            setFieldIfExists(builderClass, builder, "presencePenalty", base.getPresencePenalty());

            Object responseFormat = createJsonObjectResponseFormat();
            if (responseFormat != null) {
                Method responseFormatMethod = null;
                for (Method m : builderClass.getMethods()) {
                    if (m.getName().equals("responseFormat") && 
                        m.getParameterCount() == 1 &&
                        m.getParameterTypes()[0].isAssignableFrom(responseFormat.getClass())) {
                        responseFormatMethod = m;
                        break;
                    }
                }
                
                if (responseFormatMethod != null) {
                    responseFormatMethod.invoke(builder, responseFormat);
                }
            }

            Method buildMethod = builderClass.getMethod("build");
            return (ChatOptions) buildMethod.invoke(builder);
            
        } catch (Exception e) {
            return null;
        }
    }

    private static Object createJsonObjectResponseFormat() {
        try {
            Class<?> responseFormatClass = Class.forName("org.springframework.ai.openai.api.ResponseFormat");

            Method builderMethod = responseFormatClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);

            Class<?> typeClass = Class.forName("org.springframework.ai.openai.api.ResponseFormat$Type");

            Object jsonObjectType = null;
            Object[] enumConstants = typeClass.getEnumConstants();
            
            if (enumConstants != null) {
                for (Object enumConstant : enumConstants) {
                    String enumName = enumConstant.toString();
                    if ("JSON_OBJECT".equals(enumName) || 
                        "json_object".equals(enumName) ||
                        "JSON".equals(enumName)) {
                        jsonObjectType = enumConstant;
                        break;
                    }
                }
            }
            
            if (jsonObjectType == null) {
                return null;
            }

            Class<?> builderClass = builder.getClass();
            Method typeMethod = builderClass.getMethod("type", typeClass);
            typeMethod.invoke(builder, jsonObjectType);

            Method buildMethod = builderClass.getMethod("build");
            return buildMethod.invoke(builder);
            
        } catch (Exception e) {
            return null;
        }
    }

    private static void setFieldIfExists(Class<?> builderClass, Object builder, String fieldName, Object value) {
        if (value == null) {
            return;
        }
        
        try {
            Method method = builderClass.getMethod(fieldName, value.getClass());
            method.invoke(builder, value);
        } catch (Exception ignored) {
        }
    }
}

