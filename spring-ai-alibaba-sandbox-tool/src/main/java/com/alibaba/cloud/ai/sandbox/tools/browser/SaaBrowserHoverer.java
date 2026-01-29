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
import io.agentscope.runtime.sandbox.tools.browser.HoverTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.metadata.ToolMetadata;

public class SaaBrowserHoverer extends BaseSandboxAwareTool<HoverTool, SaaBrowserHoverer.HoverToolRequest, SaaBrowserHoverer.HoverToolResponse> {
    public SaaBrowserHoverer() {
        super(new HoverTool());
    }

    @Override
    public HoverToolResponse apply(HoverToolRequest request, ToolContext toolContext) {
        String result = sandboxTool.browser_hover(request.element, request.ref);
        return new HoverToolResponse(new Response(result, "Browser hover completed"));
    }

    public record HoverToolRequest(
            @JsonProperty(required = true, value = "element")
            @JsonPropertyDescription("Human-readable element description")
            String element,
            @JsonProperty(required = true, value = "ref")
            @JsonPropertyDescription("Exact target element reference from the page snapshot")
            String ref
    ) {
        public HoverToolRequest(String element, String ref) {
            this.element = element;
            this.ref = ref;
        }
    }

    public record HoverToolResponse(@JsonProperty("Response") Response output) {
        public HoverToolResponse(Response output) {
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
                ).inputType(HoverToolRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }
}
