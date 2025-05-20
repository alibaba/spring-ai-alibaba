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
import com.alibaba.cloud.ai.example.deepresearch.node.*;
import com.alibaba.cloud.ai.example.deepresearch.tool.tavily.TavilySearchApi;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * @author yingzi
 * @date 2025/5/17 17:10
 */
@Configuration
public class DeepResearchConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(DeepResearchConfiguration.class);

	@Autowired
	private TavilySearchApi tavilySearchApi;

	@Autowired
	private ChatClient researchAgent;

	@Autowired
	private ChatClient coderAgent;

	@Bean
	public StateGraph deepResearch(ChatClient.Builder chatClientBuilder) throws GraphStateException {

		OverAllStateFactory stateFactory = () -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("coordinator_next_node", new ReplaceStrategy());
			state.registerKeyAndStrategy("planner_next_node", new ReplaceStrategy());
			state.registerKeyAndStrategy("human_next_node", new ReplaceStrategy());
			state.registerKeyAndStrategy("research_team_next_node", new ReplaceStrategy());

			state.registerKeyAndStrategy("messages", new ReplaceStrategy());
			state.registerKeyAndStrategy("output", new ReplaceStrategy());
			state.registerKeyAndStrategy("background_investigation_results", new ReplaceStrategy());
			state.registerKeyAndStrategy("enable_background_investigation", new ReplaceStrategy());
			state.registerKeyAndStrategy("plan_iterations", new ReplaceStrategy());
			state.registerKeyAndStrategy("current_plan", new ReplaceStrategy());
			state.registerKeyAndStrategy("auto_accepted_plan", new ReplaceStrategy());
			state.registerKeyAndStrategy("observations", new ReplaceStrategy());
			state.registerKeyAndStrategy("final_report", new ReplaceStrategy());
			return state;
		};

		StateGraph stateGraph = new StateGraph("deep research", stateFactory)
			.addNode("coordinator", node_async(new CoordinatorNode(chatClientBuilder)))
			.addNode("background_investigator", node_async((new BackgroundInvestigationNode(tavilySearchApi))))
			.addNode("planner", node_async((new PlannerNode(chatClientBuilder))))
			.addNode("human_feedback", node_async(new HumanFeedbackNode()))
			.addNode("research_team", node_async(new ResearchTeamNode()))
			.addNode("researcher", node_async(new ResearcherNode(researchAgent)))
			.addNode("coder", node_async(new CoderNode(coderAgent)))
			.addNode("reporter", node_async((new ReporterNode(chatClientBuilder))))

			.addEdge(START, "coordinator")
			.addConditionalEdges("coordinator", edge_async(new CoordinatorDispatcher()),
					Map.of("background_investigator", "background_investigator", END, END))
			.addEdge("background_investigator", "planner")
			.addConditionalEdges("planner", edge_async(new PlannerDispatcher()),
					Map.of("reporter", "reporter", "human_feedback", "human_feedback", END, END))
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

}
