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

package com.alibaba.cloud.ai.example.deepresearch.config;

import com.alibaba.cloud.ai.example.deepresearch.dispatcher.CoordinatorDispatcher;
import com.alibaba.cloud.ai.example.deepresearch.dispatcher.HumanFeedbackDispatcher;
import com.alibaba.cloud.ai.example.deepresearch.dispatcher.PlannerDispatcher;
import com.alibaba.cloud.ai.example.deepresearch.dispatcher.ResearchTeamDispatcher;
import com.alibaba.cloud.ai.example.deepresearch.model.BackgroundInvestigationType;
import com.alibaba.cloud.ai.example.deepresearch.node.*;
import com.alibaba.cloud.ai.example.deepresearch.tool.tavily.TavilySearchApi;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * @author yingzi
 * @since 2025/5/17 17:10
 */
@Configuration
@EnableConfigurationProperties(DeepResearchProperties.class)
public class DeepResearchConfiguration {

	private static final Logger log = LoggerFactory.getLogger(DeepResearchConfiguration.class);

	private final TavilySearchApi tavilySearchApi;
	private final ChatClient backgroundInvestigationAgent;
	private final ChatClient researchAgent;
	private final ChatClient coderAgent;
	private final ChatClient reporterAgent;
	private final DeepResearchProperties properties;

	public DeepResearchConfiguration(
			TavilySearchApi tavilySearchApi,
			ChatClient backgroundInvestigationAgent,
			ChatClient researchAgent,
			ChatClient coderAgent,
			ChatClient reporterAgent,
			DeepResearchProperties properties) {
		this.tavilySearchApi = tavilySearchApi;
		this.backgroundInvestigationAgent = backgroundInvestigationAgent;
		this.researchAgent = researchAgent;
		this.coderAgent = coderAgent;
		this.reporterAgent = reporterAgent;
		this.properties = properties;
	}

	@Bean
	public StateGraph deepResearch(ChatClient.Builder chatClientBuilder,
								   ObjectProvider<List<ToolCallbackProvider>> toolProvider) throws GraphStateException {

		ToolCallback[] toolCallbacks = resolveToolCallbacks(toolProvider.getIfAvailable());

		OverAllStateFactory stateFactory = () -> {
			OverAllState state = new OverAllState();
			List<String> keys = List.of(
					"coordinator_next_node", "planner_next_node", "human_next_node", "research_team_next_node",
					"thread_id", "messages", "output", "background_investigation_results",
					"enable_background_investigation", "plan_iterations", "max_step_num",
					"current_plan", "auto_accepted_plan", "feed_back", "feed_back_content",
					"observations", "final_report"
			);
			keys.forEach(key -> state.registerKeyAndStrategy(key, new ReplaceStrategy()));
			return state;
		};

		StateGraph graph = new StateGraph("deep research", stateFactory)
				.addNode("coordinator", node_async(new CoordinatorNode(chatClientBuilder)))
				.addNode("background_investigator", node_async(createBackgroundInvestigationNodeAction(properties.getBackgroundInvestigationType(), toolCallbacks)))
				.addNode("planner", node_async(new PlannerNode(chatClientBuilder, toolCallbacks)))
				.addNode("human_feedback", node_async(new HumanFeedbackNode()))
				.addNode("research_team", node_async(new ResearchTeamNode()))
				.addNode("researcher", node_async(new ResearcherNode(researchAgent, toolCallbacks)))
				.addNode("coder", node_async(new CoderNode(coderAgent, toolCallbacks)))
				.addNode("reporter", node_async(new ReporterNode(reporterAgent, toolCallbacks)))

				.addEdge(START, "coordinator")
				.addConditionalEdges("coordinator", edge_async(new CoordinatorDispatcher()), Map.of(
						"background_investigator", "background_investigator",
						"planner", "planner",
						END, END
				))
				.addEdge("background_investigator", "planner")
				.addConditionalEdges("planner", edge_async(new PlannerDispatcher()), Map.of(
						"reporter", "reporter",
						"human_feedback", "human_feedback",
						END, END
				))
				.addConditionalEdges("human_feedback", edge_async(new HumanFeedbackDispatcher()), Map.of(
						"planner", "planner",
						"research_team", "research_team",
						"reporter", "reporter",
						END, END
				))
				.addConditionalEdges("research_team", edge_async(new ResearchTeamDispatcher()), Map.of(
						"planner", "planner",
						"researcher", "researcher",
						"coder", "coder"
				))
				.addEdge("researcher", "research_team")
				.addEdge("coder", "research_team")
				.addEdge("reporter", END);

		logGraph(graph);

		return graph;
	}

	private ToolCallback[] resolveToolCallbacks(List<ToolCallbackProvider> providers) {
		if (providers == null || providers.isEmpty()) {
			return new ToolCallback[0];
		}
		return providers.stream()
				.flatMap(p -> Arrays.stream(p.getToolCallbacks()))
				.toArray(ToolCallback[]::new);
	}

	private BackgroundInvestigationNodeAction createBackgroundInvestigationNodeAction(
			BackgroundInvestigationType type, ToolCallback[] toolCallbacks) {
		return switch (type) {
			case JUST_WEB_SEARCH -> new BackgroundInvestigationNode(tavilySearchApi);
			case TOOL_CALLS -> new BackgroundInvestigationToolCallsNode(backgroundInvestigationAgent, toolCallbacks);
		};
	}

	private void logGraph(StateGraph graph) {
		if (log.isDebugEnabled()) {
			GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML, "workflow graph");
			log.debug("\n{}", representation.content());
		}
	}
}