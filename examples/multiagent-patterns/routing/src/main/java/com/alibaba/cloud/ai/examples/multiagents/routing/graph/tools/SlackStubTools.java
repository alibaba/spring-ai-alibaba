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
package com.alibaba.cloud.ai.examples.multiagents.routing.graph.tools;

import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Stub tools for the Slack vertical (messages, threads) using @Tool annotation.
 * In production these would call real APIs.
 */
@Component
public class SlackStubTools {

	@Tool(name = "search_slack", description = "Search Slack messages and threads.")
	public String searchSlack(@ToolParam(description = "Search query") String query) {
		return "Found discussion in #engineering: 'Use Bearer tokens for API auth, see docs for refresh flow'";
	}

	@Tool(name = "get_thread", description = "Get a specific Slack thread.")
	public String getThread(@ToolParam(description = "Slack thread ID") String threadId) {
		return "Thread discusses best practices for API key rotation";
	}
}
