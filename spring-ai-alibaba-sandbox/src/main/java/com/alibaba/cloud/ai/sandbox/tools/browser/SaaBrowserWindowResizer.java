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

package com.alibaba.cloud.ai.sandbox.tools.browser;

import com.alibaba.cloud.ai.sandbox.BaseSandboxAwareTool;
import com.alibaba.cloud.ai.sandbox.RuntimeFunctionToolCallback;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.tools.browser.ResizeTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.metadata.ToolMetadata;

public class SaaBrowserWindowResizer extends BaseSandboxAwareTool<ResizeTool, SaaBrowserWindowResizer.ResizeToolRequest, SaaBrowserWindowResizer.ResizeToolResponse> {
    public SaaBrowserWindowResizer() {
        super(new ResizeTool());
    }

    @Override
    public ResizeToolResponse apply(ResizeToolRequest request, ToolContext toolContext) {
        String result = sandboxTool.browser_resize(request.width, request.height);
        return new ResizeToolResponse(new Response(result, "Browser resize completed"));
    }

    public record ResizeToolRequest(
            @JsonProperty(required = true, value = "width")
            @JsonPropertyDescription("Width of the browser window")
            Double width,
            @JsonProperty(required = true, value = "height")
            @JsonPropertyDescription("Height of the browser window")
            Double height
    ) {
        public ResizeToolRequest(Double width, Double height) {
            this.width = width;
            this.height = height;
        }
    }

    public record ResizeToolResponse(@JsonProperty("Response") Response output) {
        public ResizeToolResponse(Response output) {
            this.output = output;
        }
    }

    @JsonClassDescription("The result contains browser tool output and message")
    public record Response(String result, String message) {
        public Response(String result, String message) {
            this.result = result;
            this.message = message;
        }

        @JsonProperty(required = true, value = "result")
        public String result() {
            return this.result;
        }

        @JsonProperty(required = true, value = "message")
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
                ).inputType(ResizeToolRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }
}
