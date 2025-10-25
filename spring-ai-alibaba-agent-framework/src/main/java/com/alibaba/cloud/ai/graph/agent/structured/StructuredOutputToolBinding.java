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

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author logicwu0
 */

public record StructuredOutputToolBinding(String toolName, Map<String, Object> schema, ToolCallback tool) {

    public static StructuredOutputToolBinding fromSchema(
            Map<String, Object> schema,
            String toolName) {

        String finalToolName = toolName != null ? toolName : "format_output";
        String description = "Format the response according to the specified schema";

        ToolCallback toolCallback = createToolCallback(finalToolName, description, schema);

        return new StructuredOutputToolBinding(finalToolName, schema, toolCallback);
    }

    @Override
    public Map<String, Object> schema() {
        return schema;
    }

    @Override
    public ToolCallback tool() {
        return tool;
    }

    private static ToolCallback createToolCallback(
            String toolName,
            String description,
            Map<String, Object> schema) {

        String schemaJson = convertSchemaToJson(schema);

        BiFunction<String, ToolContext, String> toolFunction = (args, context) -> args;

        return FunctionToolCallback.builder(toolName, toolFunction)
                .description(description)
                .inputSchema(schemaJson)
                .inputType(String.class)
                .build();
    }

    private static String convertSchemaToJson(Map<String, Object> schema) {
        return getString(schema);
    }

    static String getString(Map<String, Object> schema) {
        try {
            Class<?> objectMapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            Object mapper = objectMapperClass.getDeclaredConstructor().newInstance();
            return (String) objectMapperClass.getMethod("writeValueAsString", Object.class)
                    .invoke(mapper, schema);
        } catch (Exception e) {
            return schema.toString();
        }
    }

}

