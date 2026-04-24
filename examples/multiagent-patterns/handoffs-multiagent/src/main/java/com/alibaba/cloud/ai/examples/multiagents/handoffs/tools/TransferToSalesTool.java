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
package com.alibaba.cloud.ai.examples.multiagents.handoffs.tools;

import com.alibaba.cloud.ai.examples.multiagents.handoffs.state.MultiAgentStateConstants;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;

/**
 * Handoff tool for the support agent: transfers the conversation to the sales agent.
 * Updates {@code active_agent} so the parent graph routes to the sales agent.
 * Uses {@code returnDirect=true} so the agent exits immediately after the tool.
 */
public final class TransferToSalesTool {

	public static final TransferToSalesTool INSTANCE = new TransferToSalesTool();

	private TransferToSalesTool() {
	}

	@Tool(
			name = "transfer_to_sales",
			description = "Transfer the conversation to the sales agent. Use when the customer asks about pricing, purchasing, or product availability.",
			returnDirect = true)
	public String transferToSales(ToolContext toolContext) {
		ToolContextHelper.getStateForUpdate(toolContext).ifPresent(update ->
				update.put(MultiAgentStateConstants.ACTIVE_AGENT, MultiAgentStateConstants.SALES_AGENT));
		return "Transferred to sales agent from support agent.";
	}

	/**
	 * Returns the tool callback for use by the support agent (support → sales handoff).
	 */
	public static ToolCallback[] tool() {
		return ToolCallbacks.from(INSTANCE);
	}
}
