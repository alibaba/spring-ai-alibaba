/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.tool;

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

public class TerminateTool implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(TerminateTool.class);

	private static String PARAMETERS = """
			{
			  "type" : "object",
			  "properties" : {
			    "message" : {
			      "type" : "string",
			      "description" : "终结当前步骤的信息，你需要在这个终结信息里尽可能多的包含所有相关的事实和数据，详细描述执行结果和状态，包含所有收集到的相关事实和数据，关键发现和观察。这个终结信息将作为当前步骤的最终输出，并且应该足够全面，以便为后续步骤或其他代理提供完整的上下文与关键事实。无需输出浏览器可交互元素索引，因为索引会根据页面的变化而变化。"
			    }
			  },
			  "required" : [ "message" ]
			}
			""";

	public static final String name = "terminate";

	private static final String description = """

			Terminate the current execution step with a comprehensive summary message.
			This message will be passed as the final output of the current step and should include:

			- Detailed execution results and status
			- All relevant facts and data collected
			- Key findings and observations
			- Important insights and conclusions
			- Any actionable recommendations

			The summary should be thorough enough to provide complete context for subsequent steps or other agents.

			""";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		return new OpenAiApi.FunctionTool(function);
	}

	public static FunctionToolCallback getFunctionToolCallback(String planId) {
		return FunctionToolCallback.builder(name, new TerminateTool(planId))
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	private String planId;

	private String lastTerminationMessage = "";

	private boolean isTerminated = false;

	private String terminationTimestamp = "";

	@Override
	public String getCurrentToolStateString() {
		return String.format("""
				Termination Tool Status:
				- Current State: %s
				- Last Termination: %s
				- Termination Message: %s
				- Timestamp: %s
				""", isTerminated ? "🛑 Terminated" : "⚡ Active",
				isTerminated ? "Process was terminated" : "No termination recorded",
				lastTerminationMessage.isEmpty() ? "N/A" : lastTerminationMessage,
				terminationTimestamp.isEmpty() ? "N/A" : terminationTimestamp);
	}

	public TerminateTool(String planId) {
		this.planId = planId;
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("Terminate toolInput: {}", toolInput);
		this.lastTerminationMessage = toolInput;
		this.isTerminated = true;
		this.terminationTimestamp = java.time.LocalDateTime.now().toString();

		return new ToolExecuteResult(toolInput);
	}

	@Override
	public ToolExecuteResult apply(String s, ToolContext toolContext) {
		return run(s);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public Class<?> getInputType() {
		return String.class;
	}

	@Override
	public boolean isReturnDirect() {
		return true;
	}

	@Override
	public void setPlanId(String planId) {
		this.planId = planId;
	}

	@Override
	public void cleanup(String planId) {
		// do nothing
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

}
