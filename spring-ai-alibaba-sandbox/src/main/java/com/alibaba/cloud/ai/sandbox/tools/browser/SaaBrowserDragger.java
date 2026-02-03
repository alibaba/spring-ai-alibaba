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
import io.agentscope.runtime.sandbox.tools.browser.DragTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.metadata.ToolMetadata;

public class SaaBrowserDragger extends BaseSandboxAwareTool<DragTool, SaaBrowserDragger.DragToolRequest, SaaBrowserDragger.DragToolResponse> {
    public SaaBrowserDragger() {
        super(new DragTool());
    }

    @Override
    public DragToolResponse apply(DragToolRequest request, ToolContext toolContext) {
        String result = sandboxTool.browser_drag(request.startElement, request.startRef, request.endElement, request.endRef);
        return new DragToolResponse(new Response(result, "Browser drag completed"));
    }

    public record DragToolRequest(
            @JsonProperty(required = true, value = "startElement")
            @JsonPropertyDescription("Human-readable source element description")
            String startElement,
            @JsonProperty(required = true, value = "startRef")
            @JsonPropertyDescription("Exact source element reference from the page snapshot")
            String startRef,
            @JsonProperty(required = true, value = "endElement")
            @JsonPropertyDescription("Human-readable target element description")
            String endElement,
            @JsonProperty(required = true, value = "endRef")
            @JsonPropertyDescription("Exact target element reference from the page snapshot")
            String endRef
    ) {
        public DragToolRequest(String startElement, String startRef, String endElement, String endRef) {
            this.startElement = startElement;
            this.startRef = startRef;
            this.endElement = endElement;
            this.endRef = endRef;
        }
    }

    public record DragToolResponse(@JsonProperty("Response") Response output) {
        public DragToolResponse(Response output) {
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
                ).inputType(DragToolRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }
}
