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
package com.alibaba.cloud.ai.examples.multiagents.workflow.sqlagent.node;

import com.alibaba.cloud.ai.examples.multiagents.workflow.sqlagent.tools.SqlTools;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lists database tables. Creates a synthetic tool call, invokes list_tables, returns messages.
 * Equivalent to list_tables in sql-agent-workflow.md.
 */
public class ListTablesNode implements NodeAction {

	private final SqlTools sqlTools;

	public ListTablesNode(SqlTools sqlTools) {
		this.sqlTools = sqlTools;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String callId = "list-tables-" + UUID.randomUUID();
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
				callId, "function", "sql_db_list_tables", "{}");
		AssistantMessage toolCallMessage = AssistantMessage.builder()
				.content("")
				.toolCalls(List.of(toolCall))
				.build();

		String result = sqlTools.listTables("");
		ToolResponseMessage toolResponse = ToolResponseMessage.builder()
				.responses(List.of(new ToolResponseMessage.ToolResponse(callId, "sql_db_list_tables", result)))
				.build();

		AssistantMessage responseMessage = new AssistantMessage("Available tables: " + result);

		List<Message> toAppend = List.of(toolCallMessage, toolResponse, responseMessage);
		return Map.of("messages", toAppend);
	}
}
