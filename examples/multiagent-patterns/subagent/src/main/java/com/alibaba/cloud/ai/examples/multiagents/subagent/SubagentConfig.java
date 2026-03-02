/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.subagent;

import java.util.List;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import com.alibaba.cloud.ai.graph.agent.tools.GlobSearchTool;
import com.alibaba.cloud.ai.graph.agent.tools.GrepSearchTool;
import com.alibaba.cloud.ai.graph.agent.tools.WebFetchTool;
import com.alibaba.cloud.ai.graph.agent.tools.task.TaskToolsBuilder;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Configures the Tech Due Diligence Assistant: a main orchestrator agent that delegates
 * to specialized sub-agents via TaskTool and TaskOutputTool.
 * <p>
 * Sub-agents can be defined in two ways:
 * <ol>
 * <li><strong>Markdown (file-based)</strong>: Files under {@code classpath:agents/} loaded by
 * TaskToolsBuilder (codebase-explorer, web-researcher, general-purpose).</li>
 * <li><strong>API (programmatic)</strong>: ReactAgent instances created in Java and registered via
 * {@link TaskToolsBuilder#subAgent(String, ReactAgent)} (e.g., dependency-analyzer).</li>
 * </ol>
 */
@Configuration
public class SubagentConfig {

	private static final String ORCHESTRATOR_SYSTEM_PROMPT = """
			You are a Tech Due Diligence Assistant. You help users evaluate software projects by combining:
			- Codebase analysis (structure, dependencies, patterns, technical debt)
			- Web research (documentation, alternatives, benchmarks, ecosystem)

			**Workflow:**
			1. Use write_todos to plan complex multi-step requests
			2. Delegate to specialized sub-agents via the Task tool:
			   - **codebase-explorer**: Finding files, searching code, analyzing structure. Use for "find X in codebase", "what frameworks does this use", "list all Java files"
			   - **web-researcher**: Fetching URLs, researching docs, comparing technologies. Use for "what is Spring AI", "fetch this URL", "compare framework X and Y"
			   - **general-purpose**: Complex tasks needing both code and web. Use for "evaluate this project's tech stack and suggest alternatives"
			   - **dependency-analyzer**: Deep dependency analysis. Use for "analyze dependencies", "version conflicts", "outdated libraries", "security vulnerabilities in deps"
			3. For independent tasks, use run_in_background=true and TaskOutput to retrieve results later
			4. Synthesize sub-agent results into a coherent response

			**When to delegate:**
			- Codebase exploration → codebase-explorer
			- Web/documentation research → web-researcher
			- Combined analysis → general-purpose
			- Dependency analysis → dependency-analyzer (API-defined sub-agent)
			- Simple single-step tasks → use direct tools (glob_search, grep_search, web_fetch) yourself

			**When to use write_todos:**
			- Requests with 3+ distinct steps
			- Multi-domain analysis (code + web)
			- User explicitly asks for a plan
			- Tasks that may need iteration

			**Output:**
			- Provide clear, structured findings
			- Cite sources (file paths, URLs)
			- Include actionable recommendations when appropriate
			""";

	private static final String DEPENDENCY_ANALYZER_SYSTEM_PROMPT = """
			You are a dependency analysis specialist. Your job is to analyze project dependencies.

			**Use glob_search and grep_search to:**
			- Find pom.xml, build.gradle, package.json, etc.
			- Extract dependency declarations
			- Identify version conflicts, outdated libraries, or security concerns
			- Map dependency tree and transitive dependencies

			**Output format:**
			- List dependencies by category (direct, transitive, optional)
			- Note version conflicts or duplicates
			- Flag outdated or deprecated versions
			- Provide clear, actionable recommendations
			""";

	@Bean
	public TodoListInterceptor todoListInterceptor() {
		return TodoListInterceptor.builder().build();
	}

	@Bean
	public List<ToolCallback> defaultTools(ChatModel chatModel,
			@Value("${subagent.workspace-path:${user.dir}}") String workspacePath) {

		ChatClient chatClient = ChatClient.builder(chatModel).build();

		ToolCallback globSearch = new GlobSearchTool.Builder(workspacePath).build();
		ToolCallback grepSearch = new GrepSearchTool.Builder(workspacePath).build();
		ToolCallback webFetch = WebFetchTool.builder(chatClient).build();

		return List.of(globSearch, grepSearch, webFetch);
	}

	/**
	 * Programmatically defined sub-agent: dependency analyzer.
	 * Uses only glob_search and grep_search for focused dependency analysis.
	 */
	@Bean("dependencyAnalyzerAgent")
	public ReactAgent dependencyAnalyzerAgent(ChatModel chatModel, List<ToolCallback> defaultTools) {
		ToolCallback globSearch = defaultTools.stream()
			.filter(t -> "glob_search".equals(t.getToolDefinition().name()))
			.findFirst()
			.orElseThrow();
		ToolCallback grepSearch = defaultTools.stream()
			.filter(t -> "grep_search".equals(t.getToolDefinition().name()))
			.findFirst()
			.orElseThrow();

		return ReactAgent.builder()
			.name("dependency-analyzer")
			.description("Analyzes project dependencies (pom.xml, package.json, etc.). Use for version conflicts, outdated libs, security vulnerabilities.")
			.model(chatModel)
			.systemPrompt(DEPENDENCY_ANALYZER_SYSTEM_PROMPT)
			.tools(globSearch, grepSearch)
			.build();
	}

	@Bean
	public List<ToolCallback> taskTools(ChatModel chatModel, List<ToolCallback> defaultTools,
			@Qualifier("dependencyAnalyzerAgent") ReactAgent dependencyAnalyzerAgent)
			throws Exception {

		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resolver.getResources("classpath:agents/*.md");

		TaskToolsBuilder builder = TaskToolsBuilder.builder()
			.chatModel(chatModel)
			.defaultTools(defaultTools.toArray(new ToolCallback[0]))
			// API-defined sub-agent (programmatic ReactAgent)
			.subAgent("dependency-analyzer", dependencyAnalyzerAgent);

		for (Resource resource : resources) {
			builder.addAgentResource(resource);
		}

		return builder.build();
	}

	@Bean("orchestratorAgent")
	public ReactAgent orchestratorAgent(ChatModel chatModel, TodoListInterceptor todoListInterceptor,
			List<ToolCallback> taskTools, List<ToolCallback> defaultTools) {

		// Main agent has: task tools (Task, TaskOutput), todo interceptor tools (write_todos), and direct tools
		return ReactAgent.builder()
			.name("tech-due-diligence-assistant")
			.description("Orchestrates technical due diligence by delegating to codebase-explorer, web-researcher, and general-purpose sub-agents")
			.model(chatModel)
			.systemPrompt(ORCHESTRATOR_SYSTEM_PROMPT)
			.interceptors(todoListInterceptor)
			.tools(taskTools)
			.tools(defaultTools)
			.saver(new MemorySaver())
			.build();
	}

}
