/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.container;

import com.alibaba.cloud.ai.entity.HistoryMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HistoryMsgContainer {

	private final McpClientContainer mcpClientContainer;

	private final Map<String, List<HistoryMessage>> historyMessages = new HashMap<>();

	public HistoryMsgContainer(McpClientContainer mcpClientContainer) {
		this.mcpClientContainer = mcpClientContainer;
	}

	public void addHistoryMessage(String clientName, HistoryMessage historyMessage) {
		if (mcpClientContainer.get(clientName) == null) {
			throw new RuntimeException("没有这个Mcp服务！");
		}
		// 如果client里没有这个名字，那么不应该处理
		historyMessages.computeIfAbsent(clientName, k -> new ArrayList<>()).add(historyMessage);
	}

	// 这里的clientName由后端发送，随后从前端拿到特定的内容
	public List<HistoryMessage> getHistoryMessages(String clientName) {
		if (mcpClientContainer.get(clientName) == null) {
			throw new RuntimeException("没有这个Mcp服务！");
		}
		return historyMessages.get(clientName);
	}

	public void clearHistoryMessages(String clientName) {
		// 这里也许可以做个切片
		if (mcpClientContainer.get(clientName) == null) {
			throw new RuntimeException("没有这个Mcp服务！");
		}
		historyMessages.remove(clientName);
	}

}
