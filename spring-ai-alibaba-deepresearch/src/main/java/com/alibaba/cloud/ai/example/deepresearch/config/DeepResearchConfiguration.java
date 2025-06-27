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

import com.alibaba.cloud.ai.example.deepresearch.config.rag.RagProperties;
import com.alibaba.cloud.ai.example.deepresearch.dispatcher.CoordinatorDispatcher;
import com.alibaba.cloud.ai.example.deepresearch.dispatcher.HumanFeedbackDispatcher;
import com.alibaba.cloud.ai.example.deepresearch.dispatcher.InformationDispatcher;
import com.alibaba.cloud.ai.example.deepresearch.dispatcher.ResearchTeamDispatcher;
import com.alibaba.cloud.ai.example.deepresearch.dispatcher.RewriteAndMultiQueryDispatcher;
import com.alibaba.cloud.ai.example.deepresearch.model.ParallelEnum;

import com.alibaba.cloud.ai.example.deepresearch.node.BackgroundInvestigationNode;
import com.alibaba.cloud.ai.example.deepresearch.node.CoderNode;
import com.alibaba.cloud.ai.example.deepresearch.node.CoordinatorNode;
import com.alibaba.cloud.ai.example.deepresearch.node.HumanFeedbackNode;
import com.alibaba.cloud.ai.example.deepresearch.node.InformationNode;
import com.alibaba.cloud.ai.example.deepresearch.node.ParallelExecutorNode;
import com.alibaba.cloud.ai.example.deepresearch.node.PlannerNode;
import com.alibaba.cloud.ai.example.deepresearch.node.RagNode;
import com.alibaba.cloud.ai.example.deepresearch.node.ReporterNode;
import com.alibaba.cloud.ai.example.deepresearch.node.ResearchTeamNode;
import com.alibaba.cloud.ai.example.deepresearch.node.ResearcherNode;
import com.alibaba.cloud.ai.example.deepresearch.node.RewriteAndMultiQueryNode;
import com.alibaba.cloud.ai.example.deepresearch.service.ReportService;

import com.alibaba.cloud.ai.example.deepresearch.serializer.DeepResearchStateSerializer;
import com.alibaba.cloud.ai.example.deepresearch.tool.SearchBeanUtil;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.toolcalling.jinacrawler.JinaCrawlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
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
@EnableConfigurationProperties({ DeepResearchProperties.class, PythonCoderProperties.class,
		McpAssignNodeProperties.class, RagProperties.class })
public class DeepResearchConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(DeepResearchConfiguration.class);

	@Autowired
	private SearchBeanUtil searchBeanUtil;

	@Autowired
	private ChatClient coderAgent;

	@Autowired
	private ChatClient researchAgent;

	@Autowired
	private ChatClient reporterAgent;

	@Autowired
	private ChatClient coordinatorAgent;

	@Autowired
	private ChatClient plannerAgent;

	@Qualifier("chatClientBuilder")
	@Autowired
	private ChatClient.Builder rewriteAndMultiQueryAgentBuilder;

	@Autowired
	private DeepResearchProperties deepResearchProperties;

	@Autowired(required = false)
	private JinaCrawlerService jinaCrawlerService;

	@Autowired(required = false)
	private RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

	@Autowired
	private ReportService reportService;

	@Bean
	public StateGraph deepResearch(ChatClient researchAgent) throws GraphStateException {

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			// 条件边控制：跳转下一个节点
			keyStrategyHashMap.put("coordinator_next_node", new ReplaceStrategy());
			keyStrategyHashMap.put("rewrite_multi_query_next_node", new ReplaceStrategy());
			keyStrategyHashMap.put("planner_next_node", new ReplaceStrategy());
			keyStrategyHashMap.put("information_next_node", new ReplaceStrategy());
			keyStrategyHashMap.put("human_next_node", new ReplaceStrategy());
			keyStrategyHashMap.put("research_team_next_node", new ReplaceStrategy());
			// 用户输入
			keyStrategyHashMap.put("query", new ReplaceStrategy());
			keyStrategyHashMap.put("optimize_queries", new ReplaceStrategy());
			keyStrategyHashMap.put("thread_id", new ReplaceStrategy());
			keyStrategyHashMap.put("enable_background_investigation", new ReplaceStrategy());
			keyStrategyHashMap.put("auto_accepted_plan", new ReplaceStrategy());
			keyStrategyHashMap.put("plan_max_iterations", new ReplaceStrategy());
			keyStrategyHashMap.put("max_step_num", new ReplaceStrategy());
			keyStrategyHashMap.put("mcp_settings", new ReplaceStrategy());

			keyStrategyHashMap.put("feed_back", new ReplaceStrategy());
			keyStrategyHashMap.put("feed_back_content", new ReplaceStrategy());

			// 节点输出
			keyStrategyHashMap.put("background_investigation_results", new ReplaceStrategy());
			keyStrategyHashMap.put("output", new ReplaceStrategy());
			keyStrategyHashMap.put("plan_iterations", new ReplaceStrategy());
			keyStrategyHashMap.put("current_plan", new ReplaceStrategy());
			keyStrategyHashMap.put("observations", new ReplaceStrategy());
			keyStrategyHashMap.put("final_report", new ReplaceStrategy());
			keyStrategyHashMap.put("planner_content", new ReplaceStrategy());

			for (int i = 0; i < deepResearchProperties.getParallelNodeCount()
				.get(ParallelEnum.RESEARCHER.getValue()); i++) {
				keyStrategyHashMap.put(ParallelEnum.RESEARCHER.getValue() + "_content_" + i, new ReplaceStrategy());
			}
			for (int i = 0; i < deepResearchProperties.getParallelNodeCount().get(ParallelEnum.CODER.getValue()); i++) {
				keyStrategyHashMap.put(ParallelEnum.CODER.getValue() + "_content_" + i, new ReplaceStrategy());
			}

			return keyStrategyHashMap;
		};

		StateGraph stateGraph = new StateGraph("deep research", keyStrategyFactory,
				new DeepResearchStateSerializer(OverAllState::new))
			.addNode("coordinator", node_async(new CoordinatorNode(coordinatorAgent)))
			.addNode("rewrite_multi_query", node_async(new RewriteAndMultiQueryNode(rewriteAndMultiQueryAgentBuilder)))
			.addNode("background_investigator",
					node_async(new BackgroundInvestigationNode(searchBeanUtil, jinaCrawlerService)))
			.addNode("planner", node_async((new PlannerNode(plannerAgent))))
			.addNode("information", node_async((new InformationNode())))
			.addNode("human_feedback", node_async(new HumanFeedbackNode()))
			.addNode("research_team", node_async(new ResearchTeamNode()))
			.addNode("parallel_executor", node_async(new ParallelExecutorNode(deepResearchProperties)))
			.addNode("reporter", node_async((new ReporterNode(reporterAgent, reportService))))
			.addNode("rag_node", node_async(new RagNode(retrievalAugmentationAdvisor, researchAgent)));

		// 添加并行节点块
		configureParallelNodes(stateGraph);

		stateGraph.addEdge(START, "coordinator")
			.addConditionalEdges("coordinator", edge_async(new CoordinatorDispatcher()),
					Map.of("rewrite_multi_query", "rewrite_multi_query", END, END))
			.addConditionalEdges("rewrite_multi_query", edge_async(new RewriteAndMultiQueryDispatcher()),
					Map.of("background_investigator", "background_investigator", "planner", "planner", END, END))
			.addEdge("background_investigator", "planner")
			.addEdge("planner", "information")
			.addConditionalEdges("information", edge_async(new InformationDispatcher()),
					Map.of("reporter", "reporter", "human_feedback", "human_feedback", "planner", "planner",
							"research_team", "research_team", END, END))
			.addConditionalEdges("human_feedback", edge_async(new HumanFeedbackDispatcher()),
					Map.of("planner", "planner", "research_team", "research_team", END, END))
			.addConditionalEdges("research_team", edge_async(new ResearchTeamDispatcher()),
					Map.of("reporter", "reporter", "parallel_executor", "parallel_executor", END, END))
			.addEdge("reporter", END);

		GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
				"workflow graph");

		logger.info("\n\n");
		logger.info(graphRepresentation.content());
		logger.info("\n\n");

		return stateGraph;
	}

	private void configureParallelNodes(StateGraph stateGraph) throws GraphStateException {
		addResearcherNodes(stateGraph);

		addCoderNodes(stateGraph);
	}

	private void addResearcherNodes(StateGraph stateGraph) throws GraphStateException {
		for (int i = 0; i < deepResearchProperties.getParallelNodeCount()
			.get(ParallelEnum.RESEARCHER.getValue()); i++) {
			String nodeId = "researcher_" + i;
			stateGraph.addNode(nodeId, node_async(new ResearcherNode(researchAgent, String.valueOf(i))));
			stateGraph.addEdge("parallel_executor", nodeId).addEdge(nodeId, "research_team");
		}
	}

	private void addCoderNodes(StateGraph stateGraph) throws GraphStateException {
		for (int i = 0; i < deepResearchProperties.getParallelNodeCount().get(ParallelEnum.CODER.getValue()); i++) {
			String nodeId = "coder_" + i;
			stateGraph.addNode(nodeId, node_async(new CoderNode(coderAgent, String.valueOf(i))));
			stateGraph.addEdge("parallel_executor", nodeId).addEdge(nodeId, "research_team");
		}
	}

}
