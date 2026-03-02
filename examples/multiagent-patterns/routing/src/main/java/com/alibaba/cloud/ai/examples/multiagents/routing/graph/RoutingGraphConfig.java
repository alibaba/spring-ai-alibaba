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
package com.alibaba.cloud.ai.examples.multiagents.routing.graph;

import com.alibaba.cloud.ai.examples.multiagents.routing.graph.tools.GitHubStubTools;
import com.alibaba.cloud.ai.examples.multiagents.routing.graph.tools.NotionStubTools;
import com.alibaba.cloud.ai.examples.multiagents.routing.graph.tools.SlackStubTools;
import com.alibaba.cloud.ai.examples.multiagents.routing.graph.node.PreprocessNode;
import com.alibaba.cloud.ai.examples.multiagents.routing.graph.node.PostprocessNode;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Configures the routing-graph workflow: preprocess → LlmRoutingAgent (as node) → postprocess.
 * LlmRoutingAgent includes routing + merge node internally.
 */
@Configuration
public class RoutingGraphConfig {

	private static final String GITHUB_INSTRUCTION = """
			You are a GitHub expert. Answer questions about code, API references, and implementation \
			details by searching repositories, issues, and pull requests.
			Please respond to the following request: {github_input}
			""";

	private static final String NOTION_INSTRUCTION = """
			You are a Notion expert. Answer questions about internal processes, policies, and team \
			documentation by searching the organization's Notion workspace.
			Please respond to the following request: {notion_input}
			""";

	private static final String SLACK_INSTRUCTION = """
			You are a Slack expert. Answer questions by searching relevant threads and discussions \
			where team members have shared knowledge and solutions.
			Please respond to the following request: {slack_input}
			""";

	@Bean
	public ReactAgent githubAgent(ChatModel chatModel, GitHubStubTools githubStubTools) {
		return ReactAgent.builder()
				.name("github")
				.instruction(GITHUB_INSTRUCTION)
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
	public CompiledGraph routingGraph(LlmRoutingAgent routerAgent) throws GraphStateException {
		KeyStrategyFactory keyFactory = () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("query", new ReplaceStrategy());
			strategies.put("messages", new AppendStrategy(false));
			strategies.put("preprocess_metadata", new ReplaceStrategy());
			strategies.put("merged_result", new ReplaceStrategy());
			strategies.put("final_answer", new ReplaceStrategy());
			strategies.put("postprocess_metadata", new ReplaceStrategy());
			strategies.put("github_key", new ReplaceStrategy());
			strategies.put("notion_key", new ReplaceStrategy());
			strategies.put("slack_key", new ReplaceStrategy());
			return strategies;
		};

		StateGraph graph = new StateGraph("routing_graph", keyFactory)
				.addNode("preprocess", node_async(new PreprocessNode()))
				.addNode("routing", routerAgent.getAndCompileGraph())
				.addNode("postprocess", node_async(new PostprocessNode()))
				.addEdge(START, "preprocess")
				.addEdge("preprocess", "routing")
				.addEdge("routing", "postprocess")
				.addEdge("postprocess", END);

		return graph.compile();
	}

	@Bean
	public RoutingGraphService routingGraphService(CompiledGraph routingGraph) {
		return new RoutingGraphService(routingGraph);
	}
}
