/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.multiagents.routing.simple;

import com.alibaba.cloud.ai.examples.multiagents.routing.simple.tools.GitHubStubTools;
import com.alibaba.cloud.ai.examples.multiagents.routing.simple.tools.NotionStubTools;
import com.alibaba.cloud.ai.examples.multiagents.routing.simple.tools.SlackStubTools;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures the router workflow using LlmRoutingAgent: classifier + parallel specialist agents
 * (GitHub, Notion, Slack). RouterService wraps the agent and adds synthesis.
 */
@Configuration
public class RoutingConfig {

	/**
	 * Sub-agent instruction. Placeholder {@code {github_input}} follows the fixed format
	 * {@code agentName + "_input"} and receives the sub-task description from the routing node.
	 * To customize the routing prompt (e.g. how the LLM selects agents and generates sub-queries),
	 * use {@link LlmRoutingAgent.LlmRoutingAgentBuilder#systemPrompt(String)} or
	 * {@link LlmRoutingAgent.LlmRoutingAgentBuilder#instruction(String)} when building the router agent.
	 */
	private static final String GITHUB_PROMPT = """
			You are a GitHub expert. Answer questions about code, API references, and implementation \
			details by searching repositories, issues, and pull requests.
			Please respond to the following request: {github_input}
			""";

	/** See GITHUB_INSTRUCTION. Placeholder {@code {notion_input}} = agentName + "_input". */
	private static final String NOTION_INSTRUCTION = """
			You are a Notion expert. Answer questions about internal processes, policies, and team \
			documentation by searching the organization's Notion workspace.
			Please respond to the following request: {notion_input}
			""";

	/** See GITHUB_INSTRUCTION. Placeholder {@code {slack_input}} = agentName + "_input". */
	private static final String SLACK_INSTRUCTION = """
			You are a Slack expert. Answer questions by searching relevant threads and discussions \
			where team members have shared knowledge and solutions.
			Please respond to the following request: {slack_input}
			""";

	@Bean
	public ReactAgent githubAgent(ChatModel chatModel, GitHubStubTools githubStubTools) {
		return ReactAgent.builder()
				.name("github")
				.systemPrompt(GITHUB_PROMPT)
				.model(chatModel)
				.methodTools(githubStubTools)
				.outputKey("github_key")
				.inputType(String.class)
				.build();
	}

	@Bean
	public ReactAgent notionAgent(ChatModel chatModel, NotionStubTools notionStubTools) {
		return ReactAgent.builder()
				.name("notion")
				.instruction(NOTION_INSTRUCTION)
				.model(chatModel)
				.methodTools(notionStubTools)
				.outputKey("notion_key")
				.inputType(String.class)
				.build();
	}

	@Bean
	public ReactAgent slackAgent(ChatModel chatModel, SlackStubTools slackStubTools) {
		return ReactAgent.builder()
				.name("slack")
				.instruction(SLACK_INSTRUCTION)
				.model(chatModel)
				.methodTools(slackStubTools)
				.outputKey("slack_key")
				.inputType(String.class)
				.build();
	}

	@Bean
	public LlmRoutingAgent routerAgent(ChatModel chatModel,
			ReactAgent githubAgent,
			ReactAgent notionAgent,
			ReactAgent slackAgent) {
		return LlmRoutingAgent.builder()
				.name("router")
				.model(chatModel)
				.description("Routes queries to GitHub, Notion, and/or Slack specialists based on relevance.")
				.subAgents(List.of(githubAgent, notionAgent, slackAgent))
				.build();
	}

	@Bean
	public RouterService routerService(ChatModel chatModel, LlmRoutingAgent routerAgent) {
		return new RouterService(chatModel, routerAgent);
	}
}
