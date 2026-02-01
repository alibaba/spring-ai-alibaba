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
import io.agentscope.runtime.sandbox.tools.browser.ClickTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.metadata.ToolMetadata;

public class SaaBrowserClicker extends BaseSandboxAwareTool<ClickTool, SaaBrowserClicker.ClickToolRequest, SaaBrowserClicker.ClickToolResponse> {

    public SaaBrowserClicker() {
        super(new ClickTool());
    }

    @Override
    public ClickToolResponse apply(ClickToolRequest request, ToolContext toolContext) {
        String result = sandboxTool.browser_click(request.element, request.ref);
        return new ClickToolResponse(new Response(result, "Browser click completed"));
    }

    public record ClickToolRequest(
            @JsonProperty(required = true, value = "element")
            @JsonPropertyDescription("Human-readable element description")
            String element,
            @JsonProperty(required = true, value = "ref")
            @JsonPropertyDescription("Exact target element reference from the page snapshot")
            String ref
    ) {
        public ClickToolRequest(String element, String ref) {
            this.element = element;
            this.ref = ref;
        }
    }

    public record ClickToolResponse(@JsonProperty("Response") Response output) {
        public ClickToolResponse(Response output) {
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
        @JsonPropertyDescription("browser output")
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
            logger.error("Failed to build tool input schema: {}", e.getMessage());
        }

        return RuntimeFunctionToolCallback
                .builder(
                        sandboxTool.getName(),
                        this
                ).description(sandboxTool.getDescription())
                .inputSchema(
                        inputSchema
                ).inputType(ClickToolRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }
}
