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

package com.alibaba.cloud.ai.sandbox.tools.base;

import com.alibaba.cloud.ai.sandbox.BaseSandboxAwareTool;
import com.alibaba.cloud.ai.sandbox.RuntimeFunctionToolCallback;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.tools.base.RunShellCommandTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.metadata.ToolMetadata;

public class SaaBaseShellRunner extends BaseSandboxAwareTool<RunShellCommandTool, SaaBaseShellRunner.RunShellCommandToolRequest, SaaBaseShellRunner.RunShellCommandToolResponse> {

    public SaaBaseShellRunner() {
        super(new RunShellCommandTool());
    }

    @Override
    public RunShellCommandToolResponse apply(RunShellCommandToolRequest request, ToolContext toolContext) {
        String result = sandboxTool.run_shell_command(request.command);
        return new RunShellCommandToolResponse(new Response(result, "Shell execution completed"));
    }

    public record RunShellCommandToolRequest(
            @JsonProperty(required = true, value = "command")
            @JsonPropertyDescription("Shell command to be executed")
            String command
    ) {
        public RunShellCommandToolRequest(String command) {
            this.command = command;
        }
    }

    public record RunShellCommandToolResponse(@JsonProperty("Response") Response output) {
        public RunShellCommandToolResponse(Response output) {
            this.output = output;
        }
    }

    @JsonClassDescription("The result contains the shell output and the execute result")
    public record Response(String result, String message) {
        public Response(String result, String message) {
            this.result = result;
            this.message = message;
        }

        @JsonProperty(required = true, value = "result")
        @JsonPropertyDescription("shell output")
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
                ).inputType(RunShellCommandToolRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }
}
