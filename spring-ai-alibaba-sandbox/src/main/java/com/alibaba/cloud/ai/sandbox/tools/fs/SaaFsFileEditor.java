/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.sandbox.tools.fs;

import com.alibaba.cloud.ai.sandbox.BaseSandboxAwareTool;
import com.alibaba.cloud.ai.sandbox.RuntimeFunctionToolCallback;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.tools.fs.EditFileTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Map;

public class SaaFsFileEditor extends BaseSandboxAwareTool<EditFileTool, SaaFsFileEditor.EditFileToolRequest, SaaFsFileEditor.EditFileToolResponse> {
    public SaaFsFileEditor() {
        super(new EditFileTool());
    }

    @Override
    public EditFileToolResponse apply(EditFileToolRequest request, ToolContext toolContext) {
        String result = sandboxTool.fs_edit_file(request.path, request.edits);
        return new EditFileToolResponse(new Response(result, "Filesystem edit_file completed"));
    }

    public record EditFileToolRequest(
            @JsonProperty(required = true, value = "path")
            @JsonPropertyDescription("Path to the file to edit")
            String path,
            @JsonProperty(required = true, value = "edits")
            @JsonPropertyDescription("Array of edit objects with oldText and newText properties")
            Map<String, Object>[] edits
    ) {
        public EditFileToolRequest(String path, Map<String, Object>[] edits) {
            this.path = path;
            this.edits = edits;
        }
    }

    public record EditFileToolResponse(@JsonProperty("Response") Response output) {
        public EditFileToolResponse(Response output) {
            this.output = output;
        }
    }

    @JsonClassDescription("The result contains filesystem tool output and execution message")
    public record Response(String result, String message) {
        public Response(String result, String message) {
            this.result = result;
            this.message = message;
        }

        @JsonProperty(required = true, value = "result")
        @JsonPropertyDescription("tool output")
        public String result() {
            return this.result;
        }

        @JsonProperty(required = true, value = "message")
        @JsonPropertyDescription("execute result")
        public String message() {
            return this.message;
        }
    }

    public RuntimeFunctionToolCallback<?, ?> buildTool() {
        ObjectMapper mapper = new ObjectMapper();
        String inputSchema = "";
        try {
            inputSchema = mapper.writeValueAsString(sandboxTool.getSchema());
        } catch (Exception e) {
            logger.error("Error generating input schema: {}", e.getMessage());
        }

        return RuntimeFunctionToolCallback
                .builder(
                        sandboxTool.getName(),
                        this
                ).description(sandboxTool.getDescription())
                .inputSchema(
                        inputSchema
                ).inputType(EditFileToolRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }
}
