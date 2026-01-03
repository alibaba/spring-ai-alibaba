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
package com.alibaba.cloud.ai.graph.agent.extension.interceptor;

import com.alibaba.cloud.ai.graph.agent.extension.tools.model.TaskTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SubAgent interceptor that provides subagent invocation capabilities to agents.
 *
 * This interceptor adds a `task` tool to the agent that can be used to invoke subagents.
 * Subagents are useful for handling complex tasks that require multiple steps, or tasks
 * that require a lot of context to resolve.
 *
 * A chief benefit of subagents is that they can handle multi-step tasks, and then return
 * a clean, concise response to the main agent.
 *
 * This interceptor comes with a default general-purpose subagent that can be used to
 * handle the same tasks as the main agent, but with isolated context.
 *
 * Example:
 * <pre>
 * SubAgentInterceptor interceptor = SubAgentInterceptor.builder()
 *     .defaultModel(chatModel)
 *     .addSubAgent(SubAgentSpec.builder()
 *         .name("research-analyst")
 *         .description("Use this agent to conduct thorough research on complex topics")
 *         .systemPrompt("You are a research analyst...")
 *         .build())
 *     .build();
 * </pre>
 */
public class SubAgentInterceptor extends ModelInterceptor {

	private static final String DEFAULT_SUBAGENT_PROMPT = "In order to complete the objective that the user asks of you, you have access to a number of standard tools.";

	private static final String DEFAULT_SYSTEM_PROMPT = """
		## `task` (subagent spawner)
		
		You have access to a `task` tool to launch short-lived subagents that handle isolated tasks. These agents are ephemeral — they live only for the duration of the task and return a single result.
		
		When to use the task tool:
		- When a task is complex and multi-step, and can be fully delegated in isolation
		- When a task is independent of other tasks and can run in parallel
		- When a task requires focused reasoning or heavy token/context usage that would bloat the orchestrator thread
		- When sandboxing improves reliability (e.g. code execution, structured searches, data formatting)
		- When you only care about the output of the subagent, and not the intermediate steps (ex. performing a lot of research and then returned a synthesized report, performing a series of computations or lookups to achieve a concise, relevant answer.)
		
		Subagent lifecycle:
		1. **Spawn** → Provide clear role, instructions, and expected output
		2. **Run** → The subagent completes the task autonomously
		3. **Return** → The subagent provides a single structured result
		4. **Reconcile** → Incorporate or synthesize the result into the main thread
		
		When NOT to use the task tool:
		- If you need to see the intermediate reasoning or steps after the subagent has completed (the task tool hides them)
		- If the task is trivial (a few tool calls or simple lookup)
		- If delegating does not reduce token usage, complexity, or context switching
		- If splitting would add latency without benefit
		
		## Important Task Tool Usage Notes to Remember
		- Whenever possible, parallelize the work that you do. This is true for both tool_calls, and for tasks. Whenever you have independent steps to complete - make tool_calls, or kick off tasks (subagents) in parallel to accomplish them faster. This saves time for the user, which is incredibly important.
		- Remember to use the `task` tool to silo independent tasks within a multi-part objective.
		- You should use the `task` tool whenever you have a complex task that will take multiple steps, and is independent from other tasks that the agent needs to complete. These agents are highly competent and efficient.
		""";

	private static final String DEFAULT_GENERAL_PURPOSE_DESCRIPTION =
			"General-purpose agent for researching complex questions, searching for files and content, " +
			"and executing multi-step tasks. This agent has access to all tools as the main agent.";

	private static final String TASK_TOOL_DESCRIPTION = """
			Launch an ephemeral subagent to handle complex, multi-step independent tasks with isolated context.
			
			Available agent types and the tools they have access to:
			{available_agents}
			
			When using the Task tool, you must specify a subagent_type parameter to select which agent type to use.
			
			## Usage notes:
			1. Launch multiple agents concurrently whenever possible to maximize performance
			2. When the agent is done, it will return a single message back to you
			3. Each agent invocation is stateless - provide a highly detailed task description
			4. The agent's outputs should generally be trusted
			5. Clearly tell the agent whether you expect it to create content, perform analysis, or just do research
			6. If the agent description mentions that it should be used proactively, then you should try your best to use it without the user having to ask for it first. Use your judgement.
			7. When only the general-purpose agent is provided, you should use it for all tasks. It is great for isolating context and token usage, and completing specific, complex tasks, as it has all the same capabilities as the main agent.
			
			### Example usage of the general-purpose agent:
			
			<example_agent_descriptions>
			"general-purpose": use this agent for general purpose tasks, it has access to all tools as the main agent.
			</example_agent_descriptions>
			
			<example>
			User: "I want to conduct research on the accomplishments of Lebron James, Michael Jordan, and Kobe Bryant, and then compare them."
			Assistant: *Uses the task tool in parallel to conduct isolated research on each of the three players*
			Assistant: *Synthesizes the results of the three isolated research tasks and responds to the User*
			<commentary>
			Research is a complex, multi-step task in it of itself.
			The research of each individual player is not dependent on the research of the other players.
			The assistant uses the task tool to break down the complex objective into three isolated tasks.
			Each research task only needs to worry about context and tokens about one player, then returns synthesized information about each player as the Tool Result.
			This means each research task can dive deep and spend tokens and context deeply researching each player, but the final result is synthesized information, and saves us tokens in the long run when comparing the players to each other.
			</commentary>
			</example>
			
			<example>
			User: "Analyze a single large code repository for security vulnerabilities and generate a report."
			Assistant: *Launches a single `task` subagent for the repository analysis*
			Assistant: *Receives report and integrates results into final summary*
			<commentary>
			Subagent is used to isolate a large, context-heavy task, even though there is only one. This prevents the main thread from being overloaded with details.
			If the user then asks followup questions, we have a concise report to reference instead of the entire history of analysis and tool calls, which is good and saves us time and money.
			</commentary>
			</example>
			
			<example>
			User: "Schedule two meetings for me and prepare agendas for each."
			Assistant: *Calls the task tool in parallel to launch two `task` subagents (one per meeting) to prepare agendas*
			Assistant: *Returns final schedules and agendas*
			<commentary>
			Tasks are simple individually, but subagents help silo agenda preparation.
			Each subagent only needs to worry about the agenda for one meeting.
			</commentary>
			</example>
			
			<example>
			User: "I want to order a pizza from Dominos, order a burger from McDonald's, and order a salad from Subway."
			Assistant: *Calls tools directly in parallel to order a pizza from Dominos, a burger from McDonald's, and a salad from Subway*
			<commentary>
			The assistant did not use the task tool because the objective is super simple and clear and only requires a few trivial tool calls.
			It is better to just complete the task directly and NOT use the `task`tool.
			</commentary>
			</example>
			
			### Example usage with custom agents:
			
			<example_agent_descriptions>
			"content-reviewer": use this agent after you are done creating significant content or documents
			"greeting-responder": use this agent when to respond to user greetings with a friendly joke
			"research-analyst": use this agent to conduct thorough research on complex topics
			</example_agent_description>
			
			<example>
			user: "Please write a function that checks if a number is prime"
			assistant: Sure let me write a function that checks if a number is prime
			assistant: First let me use the Write tool to write a function that checks if a number is prime
			assistant: I'm going to use the Write tool to write the following code:
			<code>
			function isPrime(n) {{
			  if (n <= 1) return false
			  for (let i = 2; i * i <= n; i++) {{
			    if (n % i === 0) return false
			  }}
			  return true
			}}
			</code>
			<commentary>
			Since significant content was created and the task was completed, now use the content-reviewer agent to review the work
			</commentary>
			assistant: Now let me use the content-reviewer agent to review the code
			assistant: Uses the Task tool to launch with the content-reviewer agent
			</example>
			
			<example>
			user: "Can you help me research the environmental impact of different renewable energy sources and create a comprehensive report?"
			<commentary>
			This is a complex research task that would benefit from using the research-analyst agent to conduct thorough analysis
			</commentary>
			assistant: I'll help you research the environmental impact of renewable energy sources. Let me use the research-analyst agent to conduct comprehensive research on this topic.
			assistant: Uses the Task tool to launch with the research-analyst agent, providing detailed instructions about what research to conduct and what format the report should take
			</example>
			
			<example>
			user: "Hello"
			<commentary>
			Since the user is greeting, use the greeting-responder agent to respond with a friendly joke
			</commentary>
			assistant: "I'm going to use the Task tool to launch with the greeting-responder agent"
			</example>
			""";

	private final List<ToolCallback> tools;
	private final String systemPrompt;
	private final Map<String, ReactAgent> subAgents;
	private final boolean includeGeneralPurpose;

	private SubAgentInterceptor(Builder builder) {
		this.systemPrompt = builder.systemPrompt != null ? builder.systemPrompt : DEFAULT_SYSTEM_PROMPT;
		this.subAgents = new HashMap<>(builder.subAgents);
		this.includeGeneralPurpose = builder.includeGeneralPurpose;

		// Add general-purpose agent if enabled
		if (includeGeneralPurpose && builder.defaultModel != null) {
			ReactAgent generalPurposeAgent = createGeneralPurposeAgent(
				builder.defaultModel,
				builder.defaultTools,
				builder.defaultInterceptors
			);
			this.subAgents.put("general-purpose", generalPurposeAgent);
		}

		// Create task tool using the factory method
		ToolCallback taskTool = TaskTool.createTaskToolCallback(
			this.subAgents,
			buildTaskToolDescription()
		);

		this.tools = Collections.singletonList(taskTool);
	}

	private ReactAgent createGeneralPurposeAgent(
			ChatModel model,
			List<ToolCallback> tools,
			List<? extends Interceptor> interceptors) {

		com.alibaba.cloud.ai.graph.agent.Builder builder = ReactAgent.builder()
				.name("general-purpose")
				.model(model)
				.systemPrompt(DEFAULT_SUBAGENT_PROMPT)
				.saver(new MemorySaver());

		if (tools != null && !tools.isEmpty()) {
			builder.tools(tools);
		}

		if (interceptors != null && !interceptors.isEmpty()) {
			builder.interceptors(interceptors);
		}

		return builder.build();
	}

	private String buildTaskToolDescription() {
		StringBuilder agentDescriptions = new StringBuilder();

		if (includeGeneralPurpose) {
			agentDescriptions.append("- general-purpose: ")
					.append(DEFAULT_GENERAL_PURPOSE_DESCRIPTION)
					.append("\n");
		}

		for (Map.Entry<String, ReactAgent> entry : subAgents.entrySet()) {
			if (!"general-purpose".equals(entry.getKey())) {
				agentDescriptions.append("- ")
						.append(entry.getKey())
						.append(": ")
						.append(entry.getValue().description() != null ?
								entry.getValue().description() : "Custom subagent")
						.append("\n");
			}
		}

		return TASK_TOOL_DESCRIPTION.replace("{available_agents}", agentDescriptions.toString());
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public List<ToolCallback> getTools() {
		return tools;
	}

	@Override
	public String getName() {
		return "SubAgent";
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		// Enhance the system prompt with subagent guidance
		SystemMessage enhancedSystemMessage;

		if (request.getSystemMessage() == null) {
			enhancedSystemMessage = new SystemMessage(this.systemPrompt);
		} else {
			enhancedSystemMessage = new SystemMessage(request.getSystemMessage().getText() + "\n\n" + systemPrompt);
		}

		// Create enhanced request
		ModelRequest enhancedRequest = ModelRequest.builder(request)
				.systemMessage(enhancedSystemMessage)
				.build();

		// Call the handler with enhanced request
		return handler.call(enhancedRequest);
	}

	public static class Builder {
		private String systemPrompt;
		private ChatModel defaultModel;
		private List<ToolCallback> defaultTools;
		private List<Interceptor> defaultInterceptors;
		private List<Hook> defaultHooks;
		private Map<String, ReactAgent> subAgents = new HashMap<>();
		private boolean includeGeneralPurpose = true;

		/**
		 * Set custom system prompt to guide subagent usage.
		 */
		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		/**
		 * Set the default model to use for subagents.
		 */
		public Builder defaultModel(ChatModel model) {
			this.defaultModel = model;
			return this;
		}

		/**
		 * Set the default tools available to subagents.
		 */
		public Builder defaultTools(List<ToolCallback> tools) {
			this.defaultTools = tools;
			return this;
		}


		public Builder defaultInterceptors(Interceptor... interceptors) {
			this.defaultInterceptors = Arrays.asList(interceptors);
			return this;
		}

		/**
		 * Set the default hooks to apply to subagents.
		 */
		public Builder defaultHooks(Hook... hooks) {
			this.defaultHooks = Arrays.asList(hooks);
			return this;
		}

		/**
		 * Add a custom subagent.
		 */
		public Builder addSubAgent(String name, ReactAgent agent) {
			this.subAgents.put(name, agent);
			return this;
		}

		/**
		 * Add a subagent from specification.
		 */
		public Builder addSubAgent(SubAgentSpec spec) {
			ReactAgent agent = createSubAgentFromSpec(spec);
			this.subAgents.put(spec.getName(), agent);
			return this;
		}

		/**
		 * Whether to include the default general-purpose subagent.
		 */
		public Builder includeGeneralPurpose(boolean include) {
			this.includeGeneralPurpose = include;
			return this;
		}

		private ReactAgent createSubAgentFromSpec(SubAgentSpec spec) {
			com.alibaba.cloud.ai.graph.agent.Builder builder = ReactAgent.builder()
					.name(spec.getName())
					.description(spec.getDescription())
					.instruction(spec.getSystemPrompt())
					.saver(new MemorySaver());

			ChatModel model = spec.getModel() != null ? spec.getModel() : defaultModel;
			if (model != null) {
				builder.model(model);
			}

			List<ToolCallback> tools = spec.getTools() != null ? spec.getTools() : defaultTools;
			if (tools != null && !tools.isEmpty()) {
				builder.tools(tools);
			}

			// Apply default interceptors first, then custom ones
			List<Interceptor> allInterceptors = new ArrayList<>();
			if (defaultInterceptors != null) {
				allInterceptors.addAll(defaultInterceptors);
			}
			if (spec.getInterceptors() != null) {
				allInterceptors.addAll(spec.getInterceptors());
			}

			if (!allInterceptors.isEmpty()) {
				builder.interceptors(allInterceptors);
			}

			if (defaultHooks != null) {
				builder.hooks(defaultHooks);
			}

			builder.enableLogging(spec.isEnableLoopingLog());

			return builder.build();
		}

		public SubAgentInterceptor build() {
			return new SubAgentInterceptor(this);
		}
	}
}

