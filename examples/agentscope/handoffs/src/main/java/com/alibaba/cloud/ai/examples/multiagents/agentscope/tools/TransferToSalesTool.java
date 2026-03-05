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
package com.alibaba.cloud.ai.examples.multiagents.agentscope.tools;

import com.alibaba.cloud.ai.examples.multiagents.agentscope.state.AgentScopeStateConstants;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.ai.chat.model.ToolContext;

/**
 * AgentScope handoff tool for the support agent: transfers the conversation to the sales agent.
 * <p>
 * Uses AgentScope {@link io.agentscope.core.tool.Tool} with {@link ToolContext} (auto-injected).
 * Updates {@code active_agent} via {@link ToolContextHelper#getStateForUpdate(ToolContext)} so the
 * parent graph's conditional edges route to the sales agent when the node completes.
 * <p>
 * Register via {@link io.agentscope.core.tool.Toolkit#registerTool(Object)}.
 */
public final class TransferToSalesTool {

	public static final String TOOL_NAME = "transfer_to_sales";

	private TransferToSalesTool() {
	}

	@Tool(
			name = TOOL_NAME,
			description = "Transfer the conversation to the sales agent. Use when the customer asks about pricing, purchasing, or product availability.")
	public String transferToSales(
			@ToolParam(name = "reason", description = "Brief reason for the transfer (e.g. customer asked about pricing)") String reason,
			ToolContext toolContext) {
		ToolContextHelper.getStateForUpdate(toolContext).ifPresent(update ->
				update.put(AgentScopeStateConstants.ACTIVE_AGENT, AgentScopeStateConstants.SALES_AGENT));
		return "Transferred to sales agent from support agent. Reason: " + (reason != null ? reason : "customer needs sales");
	}

	public static TransferToSalesTool create() {
		return new TransferToSalesTool();
	}
}
