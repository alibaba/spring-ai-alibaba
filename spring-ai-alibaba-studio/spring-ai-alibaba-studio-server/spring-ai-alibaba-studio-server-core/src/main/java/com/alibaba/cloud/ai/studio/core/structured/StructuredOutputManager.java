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
package com.alibaba.cloud.ai.studio.core.structured;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StructuredOutputManager {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public interface StructuredOutputStrategy {

        PreparedContext prepare(List<Message> messages, ChatOptions options, Map<String, Object> schema);
        

        JsonNode parse(String responseText);
        

        String getName();
    }


    public static class NativeStrategy implements StructuredOutputStrategy {
        @Override
        public PreparedContext prepare(List<Message> messages, ChatOptions options, Map<String, Object> schema) {
            List<Message> newMessages = addSchemaInstruction(messages, schema);
            
            if (options instanceof OpenAiChatOptions openOpts) {
                OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                    .model(openOpts.getModel())
                    .temperature(openOpts.getTemperature())
                    .topP(openOpts.getTopP())
                    .maxTokens(openOpts.getMaxTokens())
                    .presencePenalty(openOpts.getPresencePenalty())
                    .frequencyPenalty(openOpts.getFrequencyPenalty())
                    .seed(openOpts.getSeed())
                    .streamUsage(openOpts.getStreamUsage())
                    .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_OBJECT)
                        .build());
                return new PreparedContext(newMessages, builder.build(), this);
            }
            
            return new PreparedContext(newMessages, options, this);
        }
        
        @Override
        public JsonNode parse(String responseText) {
            return parseJson(responseText);
        }
        
        @Override
        public String getName() {
            return "NATIVE";
        }
    }


    public static class ToolCallStrategy implements StructuredOutputStrategy {
        @Override
        public PreparedContext prepare(List<Message> messages, ChatOptions options, Map<String, Object> schema) {
            String instruction = buildToolInstruction(schema);
            List<Message> newMessages = addSystemMessage(messages, instruction);
            return new PreparedContext(newMessages, options, this);
        }
        
        @Override
        public JsonNode parse(String responseText) {
            return extractFromToolCall(responseText);
        }
        
        @Override
        public String getName() {
            return "TOOLCALL";
        }
    }


    public static class PromptStrategy implements StructuredOutputStrategy {
        @Override
        public PreparedContext prepare(List<Message> messages, ChatOptions options, Map<String, Object> schema) {
            List<Message> newMessages = addSchemaInstruction(messages, schema);
            return new PreparedContext(newMessages, options, this);
        }
        
        @Override
        public JsonNode parse(String responseText) {
            return parseJson(responseText);
        }
        
        @Override
        public String getName() {
            return "PROMPT";
        }
    }

    public static class AutoStrategy implements StructuredOutputStrategy {
        private StructuredOutputStrategy selectedStrategy;
        
        @Override
        public PreparedContext prepare(List<Message> messages, ChatOptions options, Map<String, Object> schema) {
            this.selectedStrategy = selectStrategy(options);
            return this.selectedStrategy.prepare(messages, options, schema);
        }
        
        @Override
        public JsonNode parse(String responseText) {
            if (selectedStrategy == null) {
                throw new StructuredOutputException("Strategy not selected yet", null);
            }
            return selectedStrategy.parse(responseText);
        }
        
        @Override
        public String getName() {
            return "AUTO";
        }
        
        private StructuredOutputStrategy selectStrategy(ChatOptions options) {
            // 如果是OpenAI ChatOptions，优先使用Native
            if (options instanceof OpenAiChatOptions) {
                return new NativeStrategy();
            }
            
            // 其他情况使用Prompt策略
            return new PromptStrategy();
        }
    }


    public PreparedContext prepare(String provider, String modelId, List<Message> messages,
                                 ChatOptions options, Class<?> schemaClass) {
        return prepare(provider, modelId, messages, options, schemaClass, null);
    }


    public PreparedContext prepare(String provider, String modelId, List<Message> messages, 
                                 ChatOptions options, Class<?> schemaClass, List<String> modelTags) {
        Map<String, Object> schema = convertClassToSchema(schemaClass);
        StructuredOutputStrategy strategy = selectStrategy(provider, modelId, options, modelTags);
        return strategy.prepare(messages, options, schema);
    }


    public JsonNode parse(String responseText, StructuredOutputStrategy strategy) {
        return strategy.parse(responseText);
    }


    public <T> T parseToObject(String responseText, StructuredOutputStrategy strategy, Class<T> targetClass) {
        JsonNode jsonNode = parse(responseText, strategy);
        try {
            return OBJECT_MAPPER.treeToValue(jsonNode, targetClass);
        } catch (Exception e) {
            throw new StructuredOutputException("Failed to convert to " + targetClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private StructuredOutputStrategy selectStrategy(String provider, String modelId, ChatOptions options, List<String> modelTags) {
        if (isOpenAiNativeModel(provider)) {
            return new NativeStrategy();
        }

        if (modelTags != null && modelTags.contains("function_call")) {
            return new ToolCallStrategy();
        }

        return new PromptStrategy();
    }

    private boolean isOpenAiNativeModel(String provider) {
        if (provider == null) {
            return false;
        }
        String p = provider.toLowerCase(Locale.ROOT);
        return p.equals("openai") || p.equals("azure-openai") || p.equals("azure_openai");
    }

    private Map<String, Object> convertClassToSchema(Class<?> clazz) {
        try {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "object");
            
            Map<String, Object> properties = new HashMap<>();
            List<String> required = new ArrayList<>();

            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String fieldName = field.getName();
                String fieldType = getJsonType(field.getType());
                properties.put(fieldName, Map.of("type", fieldType));
                required.add(fieldName);
            }
            
            schema.put("properties", properties);
            schema.put("required", required);
            
            return schema;
        } catch (Exception e) {
            throw new StructuredOutputException("Failed to convert class to schema: " + e.getMessage(), e);
        }
    }

    private String getJsonType(Class<?> type) {
        if (type == String.class) {
            return "string";
        } else if (type == Integer.class || type == int.class || type == Long.class || type == long.class) {
            return "integer";
        } else if (type == Double.class || type == double.class || type == Float.class || type == float.class) {
            return "number";
        } else if (type == Boolean.class || type == boolean.class) {
            return "boolean";
        } else {
            return "string";
        }
    }

    private static List<Message> addSchemaInstruction(List<Message> messages, Map<String, Object> schema) {
        String instruction = buildSchemaInstruction(schema);
        return addSystemMessage(messages, instruction);
    }

    private static List<Message> addSystemMessage(List<Message> messages, String instruction) {
        List<Message> result = new ArrayList<>();
        boolean hasSystem = false;

        for (Message msg : messages) {
            if (msg instanceof SystemMessage) {
                result.add(new SystemMessage(msg.getText() + "\n\n" + instruction));
                hasSystem = true;
            } else {
                result.add(msg);
            }
        }

        if (!hasSystem) {
            result.add(0, new SystemMessage(instruction));
        }

        return result;
    }

    private static String buildSchemaInstruction(Map<String, Object> schema) {
        try {
            String schemaJson = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
            return String.format("""
                You must respond with a valid JSON object matching this schema.
                Do not include any text before or after the JSON.
                Do not wrap in markdown code blocks.
                
                Schema: %s
                """, schemaJson);
        } catch (Exception e) {
            return "You must respond with a valid JSON object.";
        }
    }

    private static String buildToolInstruction(Map<String, Object> schema) {
        try {
            String schemaJson = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
            return String.format("""
                To provide your response, you must call the function `format_output` with arguments matching this schema:
                
                %s
                
                Do not provide plain text response. Only call the function with structured data.
                """, schemaJson);
        } catch (Exception e) {
            return "Please call the `format_output` function with your structured response.";
        }
    }

    private static JsonNode parseJson(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            throw new StructuredOutputException("Response text is empty", null);
        }

        String json = extractJson(responseText);
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new StructuredOutputException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    private static JsonNode extractFromToolCall(String responseText) {

        return parseJson(responseText);
    }

    private static String extractJson(String text) {
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

    public record PreparedContext(
        List<Message> messages, 
        ChatOptions options, 
        StructuredOutputStrategy strategy
    ) {}

    public static class StructuredOutputException extends RuntimeException {
        public StructuredOutputException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}