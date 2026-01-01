/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.FilesystemInterceptor;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.LargeResultEvictionInterceptor;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.PatchToolCallsInterceptor;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.SubAgentInterceptor;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.SubAgentSpec;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

import static com.alibaba.cloud.ai.graph.Prompts.researchInstructions;
import static com.alibaba.cloud.ai.graph.Prompts.subCritiquePrompt;
import static com.alibaba.cloud.ai.graph.Prompts.subResearchPrompt;

public class DeepResearchAgent {
	private static final String BASE_AGENT_PROMPT =
			"In order to complete the objective that the user asks of you, " +
					"you have access to a number of standard tools.";

	private String systemPrompt;
	private ChatModel chatModel;

	// Interceptors
	private LargeResultEvictionInterceptor largeResultEvictionInterceptor;
	private FilesystemInterceptor filesystemInterceptor;
	private TodoListInterceptor todoListInterceptor;
	private PatchToolCallsInterceptor patchToolCallsInterceptor;
	private ContextEditingInterceptor contextEditingInterceptor;
	private ToolRetryInterceptor toolRetryInterceptor;

	// Hooks
	private SummarizationHook summarizationHook;
	private HumanInTheLoopHook humanInTheLoopHook;
	private ToolCallLimitHook toolCallLimitHook;
	private ShellToolAgentHook shellToolAgent;

//	private ShellTool shellTool = ShellTool.builder().build();

	public DeepResearchAgent() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

		this.systemPrompt = researchInstructions + "\n\n" + BASE_AGENT_PROMPT;

		// Initialize interceptors
		this.largeResultEvictionInterceptor = LargeResultEvictionInterceptor
				.builder()
				.excludeFilesystemTools()
				.toolTokenLimitBeforeEvict(5000)
				.build();

		this.filesystemInterceptor = FilesystemInterceptor.builder()
				.readOnly(false)
				.build();

		this.todoListInterceptor = TodoListInterceptor.builder().build();

		this.patchToolCallsInterceptor = PatchToolCallsInterceptor.builder().build();

		this.toolRetryInterceptor = ToolRetryInterceptor.builder()
				.maxRetries(1).onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE)
				.build();

		// Initialize hooks
		this.summarizationHook = SummarizationHook.builder()
				.model(chatModel) // should use another model for summarization
				.maxTokensBeforeSummary(120000)
				.messagesToKeep(6)
				.build();

		this.humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("search_web", "Please approve the todos tool.")
				.build();

		this.toolCallLimitHook = ToolCallLimitHook.builder()
				.runLimit(25)
				.build();

		this.shellToolAgent = ShellToolAgentHook.builder().build();

		this.contextEditingInterceptor = ContextEditingInterceptor.builder()
				.trigger(10000)
				.clearAtLeast(6000)
				.keep(4)
				.build();

	}

	private static SubAgentSpec createCritiqueAgent(String subCritiquePrompt) {
		return SubAgentSpec.builder()
				.name("critique-agent")
				.description("Used to critique the final report. Provide information about " +
						"how you want the report to be critiqued.")
				.systemPrompt(subCritiquePrompt)
				.enableLoopingLog(true)
				.build();
	}

	private static SubAgentSpec createResearchAgent(List<ToolCallback> toolsFromMcp, String subResearchPrompt) {
		return SubAgentSpec.builder()
				.name("research-agent")
				.description("Used to research in-depth questions. Only give one topic at a time. " +
						"Break down large topics into components and call multiple research agents " +
						"in parallel for each sub-question.")
				.systemPrompt(subResearchPrompt)
				.tools(toolsFromMcp)
				.enableLoopingLog(true)
				.build();
	}

	public ReactAgent getResearchAgent(List<ToolCallback> toolsFromMcp) {
		// Build the ReactAgent with all interceptors
		return ReactAgent.builder()
				.name("DeepResearchAgent")
				.model(chatModel)
				.tools(toolsFromMcp)
				.systemPrompt(systemPrompt)
				.enableLogging(true)
				.interceptors(todoListInterceptor,
						filesystemInterceptor,
						largeResultEvictionInterceptor,
						patchToolCallsInterceptor,
						contextEditingInterceptor,
//						toolRetryInterceptor,
						subAgentAsInterceptors(toolsFromMcp))
				.hooks(humanInTheLoopHook, summarizationHook, toolCallLimitHook)
				.saver(new MemorySaver())
				.build();

	}

	private Interceptor subAgentAsInterceptors(List<ToolCallback> toolsFromMcp) {
		SubAgentSpec researchAgent = createResearchAgent(toolsFromMcp, subResearchPrompt);
		SubAgentSpec critiqueAgent = createCritiqueAgent(subCritiquePrompt);

		SubAgentInterceptor.Builder subAgentBuilder = SubAgentInterceptor.builder()
				.defaultModel(chatModel)
				.defaultInterceptors(
						todoListInterceptor,
						filesystemInterceptor,
						contextEditingInterceptor,
						patchToolCallsInterceptor,
						largeResultEvictionInterceptor
				)
				.defaultHooks(summarizationHook, toolCallLimitHook)
				.addSubAgent(researchAgent)
				.includeGeneralPurpose(true)
				.addSubAgent(critiqueAgent);
		return subAgentBuilder.build();
	}

}
