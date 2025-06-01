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
import com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplTool;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.toolcalling.tavily.TavilySearchService;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
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

	private static final Logger logger = LoggerFactory.getLogger(DeepResearchConfiguration.class);

	@Autowired
	private PythonReplTool pythonReplTool;

	@Autowired
	private ChatClient backgroundInvestigationAgent;

	@Autowired
	private ChatClient researchAgent;

	@Autowired
	private ChatClient coderAgent;

	@Autowired
	private ChatClient reporterAgent;

	@Autowired
	private DeepResearchProperties deepResearchProperties;

	@Autowired
	private TavilySearchService tavilySearchService;

	@Bean
	public StateGraph deepResearch(ChatClient.Builder chatClientBuilder,
			ObjectProvider<List<ToolCallbackProvider>> listObjectProvider) throws GraphStateException {
		// TODO Different Tools can be set for different Nodes.
		ToolCallback[] toolCallbacks = convert2ToolCallbacks(listObjectProvider.getIfAvailable());

		OverAllStateFactory stateFactory = () -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("coordinator_next_node", new ReplaceStrategy());
			state.registerKeyAndStrategy("planner_next_node", new ReplaceStrategy());
			state.registerKeyAndStrategy("human_next_node", new ReplaceStrategy());
			state.registerKeyAndStrategy("research_team_next_node", new ReplaceStrategy());

			state.registerKeyAndStrategy("thread_id", new ReplaceStrategy());
			state.registerKeyAndStrategy("messages", new ReplaceStrategy());
			state.registerKeyAndStrategy("output", new ReplaceStrategy());
			state.registerKeyAndStrategy("background_investigation_results", new ReplaceStrategy());
			state.registerKeyAndStrategy("enable_background_investigation", new ReplaceStrategy());
			state.registerKeyAndStrategy("plan_iterations", new ReplaceStrategy());
			state.registerKeyAndStrategy("max_step_num", new ReplaceStrategy());
			state.registerKeyAndStrategy("current_plan", new ReplaceStrategy());
			state.registerKeyAndStrategy("auto_accepted_plan", new ReplaceStrategy());
			state.registerKeyAndStrategy("feed_back", new ReplaceStrategy());
			state.registerKeyAndStrategy("feed_back_content", new ReplaceStrategy());
			state.registerKeyAndStrategy("observations", new ReplaceStrategy());
			state.registerKeyAndStrategy("final_report", new ReplaceStrategy());
			return state;
		};

		BackgroundInvestigationNodeAction backgroundInvestigationNodeAction = createBackgroundInvestigationNodeAction(
				deepResearchProperties.getBackgroundInvestigationType(), toolCallbacks);

		StateGraph stateGraph = new StateGraph("deep research", stateFactory)
			.addNode("coordinator", node_async(new CoordinatorNode(chatClientBuilder)))
			.addNode("background_investigator", node_async(backgroundInvestigationNodeAction))
			.addNode("planner", node_async((new PlannerNode(chatClientBuilder, toolCallbacks))))
			.addNode("human_feedback", node_async(new HumanFeedbackNode()))
			.addNode("research_team", node_async(new ResearchTeamNode()))
			.addNode("researcher", node_async(new ResearcherNode(researchAgent, toolCallbacks)))
			.addNode("coder", node_async(new CoderNode(coderAgent, pythonReplTool)))
			.addNode("reporter", node_async((new ReporterNode(reporterAgent, toolCallbacks))))

			.addEdge(START, "coordinator")
			.addConditionalEdges("coordinator", edge_async(new CoordinatorDispatcher()),
					Map.of("background_investigator", "background_investigator", "planner", "planner", END, END))
			.addEdge("background_investigator", "planner")
			.addConditionalEdges("planner", edge_async(new PlannerDispatcher()),
					Map.of("reporter", "reporter", "human_feedback", "human_feedback", "planner", "planner", END, END))
			.addConditionalEdges("human_feedback", edge_async(new HumanFeedbackDispatcher()),
					Map.of("planner", "planner", "research_team", "research_team", "reporter", "reporter", END, END))
			.addConditionalEdges("research_team", edge_async(new ResearchTeamDispatcher()),
					Map.of("planner", "planner", "researcher", "researcher", "coder", "coder"))
			.addEdge("researcher", "research_team")
			.addEdge("coder", "research_team")
			.addEdge("reporter", END);

		GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
				"workflow graph");

		logger.info("\n\n");
		logger.info(graphRepresentation.content());
		logger.info("\n\n");

		return stateGraph;
	}

	private ToolCallback[] convert2ToolCallbacks(List<ToolCallbackProvider> toolCallbackProviders) {
		List<ToolCallback> res = Lists.newArrayList();
		toolCallbackProviders
			.forEach(toolCallbackProvider -> Collections.addAll(res, toolCallbackProvider.getToolCallbacks()));
		return res.toArray(new ToolCallback[0]);
	}

	/**
	 * Create background investigation node action by type.
	 * @param backgroundInvestigationType background investigation type
	 * @param toolCallbacks tool callbacks
	 * @return background investigation instance
	 */
	private BackgroundInvestigationNodeAction createBackgroundInvestigationNodeAction(
			BackgroundInvestigationType backgroundInvestigationType, ToolCallback[] toolCallbacks) {
		return switch (backgroundInvestigationType) {
			case JUST_WEB_SEARCH -> new BackgroundInvestigationNode(tavilySearchService);
			case TOOL_CALLS -> new BackgroundInvestigationToolCallsNode(backgroundInvestigationAgent, toolCallbacks);
		};
	}

}
