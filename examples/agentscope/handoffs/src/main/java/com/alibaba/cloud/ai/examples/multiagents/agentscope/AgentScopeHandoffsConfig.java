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
package com.alibaba.cloud.ai.examples.multiagents.agentscope;

import com.alibaba.cloud.ai.examples.multiagents.agentscope.route.RouteAfterSalesAction;
import com.alibaba.cloud.ai.examples.multiagents.agentscope.route.RouteAfterSupportAction;
import com.alibaba.cloud.ai.examples.multiagents.agentscope.route.RouteInitialAction;
import com.alibaba.cloud.ai.examples.multiagents.agentscope.state.AgentScopeStateConstants;
import com.alibaba.cloud.ai.examples.multiagents.agentscope.tools.TransferToSalesTool;
import com.alibaba.cloud.ai.examples.multiagents.agentscope.tools.TransferToSupportTool;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.agentscope.AgentScopeAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/**
 * Configures the AgentScope multi-agent handoffs pattern: sales and support agents
 * as separate graph nodes, with handoff tools that navigate between them.
 * <p>
 * Sales agent uses Spring AI ReactAgent (with TransferToSupportTool). Support agent
 * uses AgentScope ReActAgent via AgentScopeAgent with Toolkit + TransferToSalesTool.
 * AgentScope tools receive ToolContext (auto-injected) and use
 * {@link com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper#getStateForUpdate}
 * to update graph state (e.g. active_agent) for routing.
 */
@Configuration
public class AgentScopeHandoffsConfig {

	private static final String SALES_PROMPT = """
			You are a sales agent. Help with sales inquiries, pricing, and product availability.
			If the customer asks about technical issues, troubleshooting, or account problems,
			use transfer_to_support to hand off to the support agent.
			""";

	private static final String SUPPORT_PROMPT = """
			You are a support agent. Help with technical issues, troubleshooting, and account problems.
			If the customer asks about pricing, purchasing, or product availability,
			use transfer_to_sales to hand off to the sales agent.
			""";

	@Bean
	public ReactAgent salesAgent(ChatModel chatModel) {
		return ReactAgent.builder()
				.name(AgentScopeStateConstants.SALES_AGENT)
				.model(chatModel)
				.systemPrompt(SALES_PROMPT)
				.instruction("please assist the customer with their sales inquiry: {input}.")
				.methodTools(TransferToSupportTool.INSTANCE)
				.inputType(String.class)
				.includeContents(true)
				.returnReasoningContents(true)
				.build();
	}

	@Bean
	public AgentScopeAgent supportAgent(@Value("${spring.ai.dashscope.api-key:}") String apiKey) {
		String key = StringUtils.hasText(apiKey) ? apiKey : System.getenv("AI_DASHSCOPE_API_KEY");
		Model scopeModel = DashScopeChatModel.builder()
				.apiKey(key)
				.modelName("qwen-plus")
				.build();

		Toolkit toolkit = new Toolkit();
		toolkit.registerTool(TransferToSalesTool.create());

		ReActAgent.Builder scopeReActBuilder = ReActAgent.builder()
				.name(AgentScopeStateConstants.SUPPORT_AGENT)
				.description("Support agent for technical issues and troubleshooting")
				.sysPrompt(SUPPORT_PROMPT)
				.model(scopeModel)
				.toolkit(toolkit)
				.memory(new InMemoryMemory());

		return AgentScopeAgent.fromBuilder(scopeReActBuilder)
				.name(AgentScopeStateConstants.SUPPORT_AGENT)
				.description("Support agent for technical issues and troubleshooting")
				.instruction("please assist the customer with their product technical inquiry: {input}.")
				.includeContents(true)
				.returnReasoningContents(true)
				.build();
	}

	@Bean
	public CompiledGraph agentScopeHandoffsGraph(ReactAgent salesAgent, AgentScopeAgent supportAgent)
			throws GraphStateException {

		StateGraph graph = new StateGraph("agent_scope_handoffs", () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy(false));
			strategies.put(AgentScopeStateConstants.ACTIVE_AGENT, new ReplaceStrategy());
			return strategies;
		});

		graph.addNode(AgentScopeStateConstants.SALES_AGENT, salesAgent.asNode());
		graph.addNode(AgentScopeStateConstants.SUPPORT_AGENT, supportAgent.asNode());

		// route_initial: default to sales_agent
		graph.addConditionalEdges(START, new RouteInitialAction(), Map.of(
				AgentScopeStateConstants.SALES_AGENT, AgentScopeStateConstants.SALES_AGENT,
				AgentScopeStateConstants.SUPPORT_AGENT, AgentScopeStateConstants.SUPPORT_AGENT));

		// route_after_sales: handoff to support or END
		graph.addConditionalEdges(AgentScopeStateConstants.SALES_AGENT,
				new RouteAfterSalesAction(),
				Map.of(
						AgentScopeStateConstants.SUPPORT_AGENT, AgentScopeStateConstants.SUPPORT_AGENT,
						"__end__", END));

		// route_after_support: handoff to sales or END
		graph.addConditionalEdges(AgentScopeStateConstants.SUPPORT_AGENT,
				new RouteAfterSupportAction(),
				Map.of(
						AgentScopeStateConstants.SALES_AGENT, AgentScopeStateConstants.SALES_AGENT,
						"__end__", END));

		return graph.compile();
	}

	@Bean
	public AgentScopeHandoffsService agentScopeHandoffsService(CompiledGraph agentScopeHandoffsGraph) {
		return new AgentScopeHandoffsService(agentScopeHandoffsGraph);
	}
}
